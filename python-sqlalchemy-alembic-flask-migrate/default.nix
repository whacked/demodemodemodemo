with import <nixpkgs> {};

stdenv.mkDerivation rec {
    name = "flask-bootstrap-admin-migrate-demo";
    env = buildEnv {
        name = name;
        paths = buildInputs;
    };

    buildInputs = [
        python37Full
        sqlite
    ];

    shellHook = ''
      VIRTUAL_ENV=''${VIRTUAL_ENV-$USERCACHE/$name-venv}
      unset SOURCE_DATE_EPOCH

      function setup-venv() {
          pip install flask_bootstrap Flask-WTF WTForms-Alchemy flask-admin flask-sqlalchemy flask-migrate
      }

      function initialize-database() {
          python models.py
          flask db init
      }
      function create-database-migration() {
          flask db migrate -m "$*"
      }
      function update-database() {
          flask db update
      }

      if [ -e $VIRTUAL_ENV ]; then
          source $VIRTUAL_ENV/bin/activate
      else
          python -m venv $VIRTUAL_ENV
          source $VIRTUAL_ENV/bin/activate
          setup-venv
      fi

      alias run='python app.py'
      alias run-in-memory='IN_MEMORY=1 python app.py'
      cat default.nix | grep '^ \+\(function\|alias\) .\+'
    '';
}
