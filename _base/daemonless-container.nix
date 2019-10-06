let
  pkgs = import <nixpkgs> {};
  stdenv = pkgs.stdenv;
in stdenv.mkDerivation rec {
  name = "env";
  env = pkgs.buildEnv { name = name; paths = buildInputs; };
  buildInputs = [
    pkgs.buildah
    pkgs.conmon
    pkgs.podman
    pkgs.runc
    pkgs.shadow
    pkgs.skopeo
    pkgs.slirp4netns
  ];
  shellHook = ''
  if [ ! -e /etc/containers ]; then
  sudo mkdir -p /etc/containers
  cat <<EOF | sudo tee /etc/containers/policy.json
  {
      "default": [
          {
              "type": "insecureAcceptAnything"
          }
      ]
  }
  EOF
  cat <<EOF | sudo tee /etc/containers/registries.conf
  [registries.search]
  registries = [ 'docker.io' ]
  EOF
  fi
  
  # ref https://github.com/containers/libpod/blob/master/docs/libpod.conf.5.md
  cat <<EOF | tee $PWD/podman.conf
  conmon_path = [ "$(which conmon)" ]
  events_logger = "file"
  [runtimes]
  runc = [ "$(which runc)" ]
  EOF
  echo $(whoami):100000:65536 | sudo tee /etc/sub{u,g}id

  head /etc/subuid /etc/subgid
  sudo chown root: $(which new{u,g}idmap)
  sudo chmod 4555 $(which new{u,g}idmap)
  alias docker="podman --config $PWD/podman.conf \$*"
  '';
}

