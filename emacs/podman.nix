with import <nixpkgs> {};

let
    base = import ../_base/daemonless-container.nix;
in stdenv.mkDerivation rec {
    name = "emacs-dev";
    buildInputs = base.buildInputs ++ [
        emacs
    ];
    shellHook = base.shellHook + ''
      # required to allow X access from container
      xhost +
      CONTAINER_NAME=${name}-container
      echo launching $CONTAINER_NAME
      podman \
        --config $PWD/podman.conf \
        run --rm -it \
        --name $CONTAINER_NAME  \
        -e DISPLAY=$DISPLAY \
        -v /tmp/.X11-unix:/tmp/.X11-unix \
        jare/emacs \
        emacs
      exit
    '';

}
