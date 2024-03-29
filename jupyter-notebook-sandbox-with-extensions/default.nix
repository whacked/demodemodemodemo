with import <nixpkgs> {};

let
in stdenv.mkDerivation rec {
  name = "jupyter-notebook-sandbox-with-extensions";
  env = buildEnv {
    name = name;
    paths = buildInputs;
  };
  buildInputs = [
    gcc
    # NOTE working directly from the nix-provided jupyter,
    # e.g. python37Packages.jupyter, seems less flexible
    # when setting up pip installed extensions
    python38Full
    graphviz
    nodejs
    xorg.libX11
    libGL
  ];

  # this is probably an abuse
  userhome = (builtins.getEnv "HOME");
  nativeBuildInputs = [
    (userhome + "/setup/bash/nix_shortcuts.sh")
  ];

  DEBUG_LEVEL = 1;
  shellHook = ''
    ensure-usercache
    unset PYTHONPATH
    # FIX for ImportError: libstdc++.so.6: cannot open shared object file: No such file or directory
    # but costly import (gcc-9.2.0)
    export LD_LIBRARY_PATH=${gcc-unwrapped.lib}/lib:$LD_LIBRARY_PATH
    # needed to build pygraphviz
    export PKG_CONFIG_PATH=''${PKG_CONFIG_PATH+$PKG_CONFIG_PATH:}${graphviz}/lib/pkgconfig
    export PATH=$PATH:$(npm bin)

    function enable-jupyter-extension() {
        jupyter nbextension enable --user --py $*
    }

    function disable-jupyter-extension() {
        jupyter nbextension disable --user --py $*
    }

    function setup-tslab() {
        npm install tslab
        tslab install
    }

    function setup-first-run() {
        # base + official contrib dependencies
        pip install jupyter jupyter_contrib_nbextensions autopep8
        jupyter contrib nbextension install --user
        ENABLE_BUNDLED_EXTENSIONS=(
            code_prettify/autopep8
            codefolding/main
            collapsible_headings/main
            contrib_nbextensions_help_item/main
            datestamper/main
            execute_time/ExecuteTime
            freeze/main
            scratchpad/main
            toc2/main
            toggle_all_line_numbers/main
            varInspector/main
        );
        for ext in ''${ENABLE_BUNDLED_EXTENSIONS[@]}; do
            jupyter nbextension enable $ext
        done
        # additional extensions
        pip install nodebook
        jupyter nbextension install --user --py nodebook
        # not enabling immediately; requires a magic cell setup to work, e.g.:
        # --------------------------
        # #pragma nodebook off
        # %load_ext nodebook.ipython
        # %nodebook memory my-nb
        # --------------------------
        # see https://github.com/stitchfix/nodebook
        # enable-jupyter-extension nodebook

        pip install git+https://github.com/whacked/Jupyter-multi_outputs
        jupyter nbextension install --py lc_multi_outputs --user
        jupyter nbextension enable --py lc_multi_outputs --user
    }

    function setup-additional-packages() { 
        pip install toolz matplotlib numpy pandas mplfinance seaborn more_itertools
    }

    function run-unprotected-server() {
        _token=''${1-asdf}
        if [ -e /.dockerenv ]; then
            IP_ARG="--ip=0.0.0.0"
        else
            IP_ARG=
        fi
        jupyter notebook \
            --no-browser $IP_ARG \
            --NotebookApp.token=$_token
    }

    if [ "x$JUPYTER_CONFIG_DIR" == "x" ]; then
        ALTHOME=$USERCACHE/$name-home
        mkdir -p $ALTHOME
        export JUPYTER_CONFIG_DIR=$ALTHOME/jupyter
        export IPYTHONDIR=''${IPYTHONDIR-$ALTHOME/ipython}
        echo " - setting jupyter working directory to $JUPYTER_CONFIG_DIR"
        echo " - setting python working directory to $IPYTHONDIR"
        
        JUPYTER_WORK_DIR=$USERCACHE/$name-jupyter-work
        export JUPYTER_DATA_DIR=$JUPYTER_WORK_DIR/data
        export JUPYTER_RUNTIME_DIR=$JUPYTER_WORK_DIR/runtime
    fi

    ensure-venv setup-first-run
    jupyter nbextension list
    
    alias build-docker-container='sudo $(which docker) build . -t emacs-with-nix'
    alias run-nix-docker-container='sudo $(which docker) run -p 8888:8888 -v $PWD:/opt/demo -w /opt/demo --rm -it emacs-with-nix /bin/bash'
    alias run='jupyter notebook --no-browser'
    echo-shortcuts ${__curPos.file}
    export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:${xorg.libX11}/lib:${libGL}/lib
  '';
}

