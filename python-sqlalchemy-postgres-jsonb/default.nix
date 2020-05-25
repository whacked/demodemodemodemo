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
  userhome = (builtins.getEnv "HOME");
  nativeBuildInputs = [
    (userhome + "/setup/bash/nix_shortcuts.sh")
    (userhome + "/setup/bash/postgresql_shortcuts.sh")
  ];
  DEBUG_LEVEL = 1;
  shellHook = ''
    function initialize-venv() {
        pip install -U git+ssh://git@bitbucket.org/whacked/BeanBunny
    }
    ensure-venv initialize-venv
  '';
}
