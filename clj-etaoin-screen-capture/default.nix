with import <nixpkgs> {};

let
in stdenv.mkDerivation rec {
    name = "clj-etaoin-screen-capture";
    buildInputs = [
      leiningen
      ffmpeg
      geckodriver
    ];
    shellHook = ''
      alias repl='lein repl'

      export DEMO_WINDOW_WIDTH=1200
      export DEMO_WINDOW_HEIGHT=600
      export DEMO_WINDOW_X=40
      export DEMO_WINDOW_Y=10
      alias run-demo='(echo|lein run) & sleep 10; timeout 20 ffmpeg -f x11grab -s "$DEMO_WINDOW_WIDTH"x"$DEMO_WINDOW_HEIGHT" -r 25 -i $DISPLAY+$DEMO_WINDOW_X,$DEMO_WINDOW_Y -qscale 0 demo-out.mpg'
    '';
}
