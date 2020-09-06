with import <nixpkgs> {};

stdenv.mkDerivation rec {
    name = "cljs-material-ui-example";
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
      if (which shadow-cljs 2> /dev/null); then
          echo "shadow-cljs is in $(which shadow-cljs)"
      else
          npm install
      fi

      alias watch='shadow-cljs watch main'
      cat default.nix | grep '^ \+\(function\|alias\) .\+'
    '';
}

