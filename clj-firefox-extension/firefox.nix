with import <nixpkgs> {};
let
in stdenv.mkDerivation rec {
  name = "firefox-base";
  env = buildEnv {
    name = name;
    paths = buildInputs;
  };
  buildInputs = [
    firefox
  ];
  nativeBuildInputs = [
    ~/setup/bash/nix_shortcuts.sh
  ];

  shellHook = ''
    export TMPDIR=$(mktemp -d)
    echo "using scratch at $TMPDIR"

    alias ff="firefox --profile $TMPDIR"

    __cleanup() {
      rmdir $TMPDIR 2>/dev/null
      if [ $? -eq 0 ]; then
        echo "cleaned up empty scratch at $TMPDIR"
      else
        echo "cleaning up scratch at $TMPDIR; press return to continue..."
        read
        rm -rf $TMPDIR
      fi
    }
    trap __cleanup EXIT
    echo-shortcuts default.nix
  '';
}
