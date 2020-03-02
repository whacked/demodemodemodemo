with import <nixpkgs> {};

let
in stdenv.mkDerivation rec {
  name = "python-sqlalchemy-postgres-jsonb";
  env = buildEnv {
    name = name;
    paths = buildInputs;
  };
  buildInputs = [
    python37Packages.ipython
    python37Full
    python37Packages.flask
    python37Packages.flask-admin
    python37Packages.flask-cors
    python37Packages.sqlalchemy
    python37Packages.flask_sqlalchemy
    python37Packages.psycopg2
    python37Packages.faker
    postgresql_12
    glibcLocales
  ];
  shellHook = ''
    _BASH_SHARED_DIR=$CLOUDSYNC/main/dev/setup/bash
    . $_BASH_SHARED_DIR/nix_shortcuts.sh
    SHORTCUTS_FILE=$_BASH_SHARED_DIR/postgresql_shortcuts.sh
    if [ -e $SHORTCUTS_FILE ]; then
        source $SHORTCUTS_FILE
    else
        echo no shortcuts in $SHORTCUTS_FILE
    fi

    VIRTUAL_ENV=''${VIRTUAL_ENV-$USERCACHE/$name-venv}
    if [ -e $VIRTUAL_ENV ]; then
        source $VIRTUAL_ENV/bin/activate
    else
        python -m venv $VIRTUAL_ENV
        source $VIRTUAL_ENV/bin/activate
        pip install -U git+ssh://git@bitbucket.org/whacked/BeanBunny
    fi
  '';
}
