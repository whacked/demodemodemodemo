with import <nixpkgs> {};

stdenv.mkDerivation rec {
    name = "vscode-plugin-shell-process-runner";
    env = buildEnv {
        name = name;
        paths = buildInputs;
    };

    buildInputs = [
        nodejs-10_x
        vscode
        dejavu_fonts
        fontconfig
    ];

    shellHook = ''
      unset name
      export PATH=$PATH:$(npm bin)

      function vscode() {
          if [ "x$VSCODE_USER_DATA_DIR" == "x" ]; then
              code $*
          else
              CMD="code --extensions-dir $VSCODE_USER_DATA_DIR/extensions --user-data-dir $VSCODE_USER_DATA_DIR/data"
              if [ $# -eq 0 ]; then
                  $CMD .
              else
                  $CMD $*
              fi
          fi
      }

      function quickstart() {
          if [ "x$VSCODE_USER_DATA_DIR" == "x" ]; then
              TMPDIR=$(mktemp -d)
              echo creating temp directory $TMPDIR...
              export VSCODE_USER_DATA_DIR=$TMPDIR
              mkdir -p $VSCODE_USER_DATA_DIR/{extensions,data}

              __cleanup() {
                  echo cleaning up temp directory $TMPDIR...
                  rm -rf $TMPDIR
              }

              DEFAULT_EXTENSIONS=(
              )

              set -x
              for extension in ''${DEFAULT_EXTENSIONS[*]}; do
                  vscode --install-extension $extension --force
              done
              set +x

              trap __cleanup EXIT
          fi
          vscode $*
      }

      function setup-fonts() {
          mkdir -p $HOME/.fonts
          for path in $(find ${pkgs.dejavu_fonts.out}/share/fonts/truetype/ -name '*.ttf'); do
              fontfile=$(basename $path)
              target=$HOME/.fonts/$fontfile
              if [ ! -e $target ]; then
                  ln -s $path $target
              fi
          done
          fc-cache -fv
      }

      function initialize-plugin-scaffolding() {
          npm install yo generator-code
          yo code
      }
      cat default.nix | grep '^ \+\(function\|alias\) .\+'
    '';
}

