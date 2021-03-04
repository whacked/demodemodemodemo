with import <nixpkgs> {};
let
    base = import ../_base/daemonless-container.nix;
  in stdenv.mkDerivation rec {
    name = "nix-docker-base";
    env = buildEnv {
        name = name;
        paths = buildInputs;
    };
    buildInputs = [
    ];
    nativeBuildInputs = [
      ~/setup/bash/nix_shortcuts.sh
    ];

    shellHook = base.shellHook + ''
      alias build-redis-example="nix-build '<nixpkgs>' -A dockerTools.examples.redis"
      alias load="podman load < result"
      echo-shortcuts ${__curPos.file}
    '';
}
