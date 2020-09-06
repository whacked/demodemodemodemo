with import <nixpkgs> {};

stdenv.mkDerivation rec {
    name = "electron-golden-layout-xtermjs";
    env = buildEnv {
        name = name;
        paths = buildInputs;
    };

    buildInputs = [
        electron
        emacs
        nodejs-10_x
        yarn
    ];

    shellHook = ''
      unset name
      export PATH=$PATH:$(yarn bin):$(npm bin)
      if $(which shadow-cljs 2> /dev/null); then
          echo "shadow-cljs is in $(which shadow-cljs)"
      else
          yarn add shadow-cljs react create-react-class react-dom
      fi

      alias watch-electron='shadow-cljs watch main renderer'

      # one problem with watching both server and front at the same time
      # is that CLJS auto reload on the front will happen before the
      # server process restarts
      alias watch-web='shadow-cljs watch web-server web-front'
      alias watch-web-server='shadow-cljs watch web-server'
      alias watch-web-front='shadow-cljs watch web-front'

      alias pack-css='shadow-cljs run app.pack/css'  # run after watch server started
      alias launch='electron .'

      cat default.nix | grep '^ \+\(function\|alias\) .\+'
      alias wserver="watchexec --restart --no-ignore --watch app/ node app/server.js"
    '';
}

