with import <nixpkgs> {};

let
    base = import ../_base/docker.nix;
in stdenv.mkDerivation rec {
    name = "emacs-dev";
    buildInputs = base.buildInputs ++ [
        emacs
    ];
    shellHook = base.shellHook + ''
      CONTAINER_NAME=${name}-$CONTAINER_NAME
      echo launching $CONTAINER_NAME
      docker \
        run --rm \
        --name ${name}-$CONTAINER_NAME -it \
        -e DISPLAY=$DISPLAY \
        -v /tmp/.X11-unix:/tmp/.X11-unix \
        jare/emacs \
        emacs
      exit
    '';

}
