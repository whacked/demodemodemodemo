with import <nixpkgs> {};

stdenv.mkDerivation rec {
    name = "schema-server";
    env = buildEnv {
        name = name;
        paths = buildInputs;
    };

    buildInputs = [
        nodejs
        watchexec
    ];

    shellHook = ''
      unset name
      export PATH=$PATH:$(npm bin)
      if (which shadow-cljs 2> /dev/null); then
          echo "shadow-cljs is in $(which shadow-cljs)"
      else
          npm install shadow-cljs react create-react-class react-dom
      fi

      alias watch='shadow-cljs watch main server'
      alias wserver="watchexec --restart --no-ignore --watch app/ node app/server.js"

      cat default.nix | grep '^ \+\(function\|alias\) .\+'
    '';
}

