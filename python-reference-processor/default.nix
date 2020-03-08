with import <nixpkgs> {};

let
in stdenv.mkDerivation rec {
  name = "reference-processor";
  env = buildEnv {
    name = name;
    paths = buildInputs;
  };
  buildInputs = [
    python37Packages.ipython
    python37Full
    python37Packages.pdfx
    python37Packages.requests-cache
    jq
  ];
  shellHook = ''
    _BASH_SHARED_DIR=$CLOUDSYNC/main/dev/setup/bash
    . $_BASH_SHARED_DIR/nix_shortcuts.sh

    VIRTUAL_ENV=''${VIRTUAL_ENV-$USERCACHE/$name-venv}
    if [ -e $VIRTUAL_ENV ]; then
        source $VIRTUAL_ENV/bin/activate
    else
        python -m venv $VIRTUAL_ENV
        source $VIRTUAL_ENV/bin/activate
        pip install crossrefapi
    fi
  '';
}
