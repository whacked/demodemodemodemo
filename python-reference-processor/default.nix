with import <nixpkgs> {};

let
in stdenv.mkDerivation rec {
  name = "reference-processor";
  env = buildEnv {
    name = name;
    paths = buildInputs;
  };
  buildInputs = [
    python37Packages.ipython
    python37Full
    python37Packages.pdfx
    python37Packages.requests-cache
    jq
  ];

  nativeBuildInputs = [
    ((builtins.getEnv "HOME") + "/setup/bash/nix_shortcuts.sh")
  ];
  DEBUG_LEVEL = 1;
  shellHook = ''
    function initialize-venv() {
        pip install crossrefapi
    }
    ensure-venv initialize-venv
  '';
}
