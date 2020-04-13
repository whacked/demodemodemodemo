with import <nixpkgs> {};

let
in stdenv.mkDerivation rec {
  name = "jupyter-notebook-sandbox-with-extensions";
  env = buildEnv {
    name = name;
    paths = buildInputs;
  };
  buildInputs = [
    python37Full
    # python37Packages.jupyter
  ];
  shellHook = ''
    _BASH_SHARED_DIR=$CLOUDSYNC/main/dev/setup/bash
    . $_BASH_SHARED_DIR/nix_shortcuts.sh

    USERCACHE=''${USERCACHE-/tmp/cache}
    if [ ! -e $USERCACHE ]; then
        mkdir -p $USERCACHE
    fi

    function enable-jupyter-extension() {
        jupyter nbextension enable --user --py $*
    }

    function disable-jupyter-extension() {
        jupyter nbextension disable --user --py $*
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
    }

    function run-unprotected-server() {
        _token=''${1-asdf}
        jupyter notebook --no-browser --ip=0.0.0.0 --NotebookApp.token=$_token
    }

    unset PYTHONPATH
    # FIX for ImportError: libstdc++.so.6: cannot open shared object file: No such file or directory
    # when starting jupyter from nix-shell
    if [ -e /usr/lib/x86_64-linux-gnu ]; then
        export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:/usr/lib/x86_64-linux-gnu
    fi

    export VIRTUAL_ENV=''${VIRTUAL_ENV-$USERCACHE/$name-venv}

    if [ "x$JUPYTER_CONFIG_DIR" == "x" ]; then
        ALTHOME=$USERCACHE/$name-home
        mkdir -p $ALTHOME
        export JUPYTER_CONFIG_DIR=$ALTHOME/jupyter
        export IPYTHONDIR=''${IPYTHONDIR-$ALTHOME/ipython}
        echo "setting jupyter working directory to $JUPYTER_CONFIG_DIR"
        echo "setting python working directory to $IPYTHONDIR"
        
        JUPYTER_WORK_DIR=$USERCACHE/$name-jupyter-work
        export JUPYTER_DATA_DIR=$JUPYTER_WORK_DIR/data
        export JUPYTER_RUNTIME_DIR=$JUPYTER_WORK_DIR/runtime
    fi

    if [ ! -e $VIRTUAL_ENV ]; then
        python -m venv $VIRTUAL_ENV
        source $VIRTUAL_ENV/bin/activate
        setup-first-run
    else
        source $VIRTUAL_ENV/bin/activate
    fi
    jupyter nbextension list
  '';
}

