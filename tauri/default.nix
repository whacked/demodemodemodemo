with import <nixpkgs> {};

let
in stdenv.mkDerivation rec {
    name = "tauri-base";
    buildInputs = [
      cairo
      cargo
      glibc
      glibcLocales
      gnome3.gtk
      gnome3.libsoup
      gnome3.webkitgtk
      nodejs-12_x
      pkgconfig
      rustc
    ];
    shellHook = ''
      export CARGO_HOME=''${CARGO_HOME-$HOME/.cargo}
      export PATH=$PATH:$(npm bin):$CARGO_HOME/bin
      if [ ! -x "$(command -v cargo-tauri-bundler)" ]; then
          echo "did not detect cargo-tauri-bundler; attempting to install..."
          cargo install tauri-bundler
      fi
      if [ ! -x "$(command -v tauri)" ]; then
          echo "did not detect npm tauri; attempting to install..."
          npm install tauri
      fi
      cargo-tauri-bundler --version
      echo "tauri $(tauri --version)"
      grep devPath src-tauri/tauri.conf.json 2>/dev/null
    '';

}
