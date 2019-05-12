with import <nixpkgs> {};

{
    buildInputs = [
        zsh
        docker
    ];
    shellHook = ''
      if ! $(groups | grep -q docker); then
          alias docker="sudo $(which docker)"
      fi
      CONTAINER_NAME=container-$$
    '';
}
