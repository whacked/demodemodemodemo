with import <nixpkgs> {};

let
    base = import ../_base/docker.nix;
in stdenv.mkDerivation rec {
    name = "base-bash";
    buildInputs = base.buildInputs ++ [
    ];
    shellHook = base.shellHook + ''
      docker \
        run --rm \
        --name ${name}-$CONTAINER_NAME -it \
        -v $PWD:/media/docker \
        bash:4 \
        bash --rcfile /media/docker/example.sh
      exit
    '';
}
