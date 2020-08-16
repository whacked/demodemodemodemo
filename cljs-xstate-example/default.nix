with import <nixpkgs> {};

stdenv.mkDerivation rec {
    name = "cljs-xstate-example";
    env = buildEnv {
        name = name;
        paths = buildInputs;
    };

    buildInputs = [
        nodejs-10_x
    ];

    shellHook = ''
      unset name
      export PATH=$PATH:$(npm bin)
      if $(which shadow-cljs 2> /dev/null); then
          echo "shadow-cljs is in $(which shadow-cljs)"
      else
          echo initializing...
          npm install
      fi

      alias start-server='lein run'
      alias compile-cljs='lein shadow compile app'
      alias watch-cljs='lein shadow watch app'
      cat default.nix | grep '^ \+\(function\|alias\) .\+'
    '';
}

