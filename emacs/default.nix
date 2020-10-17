with import <nixpkgs> {};

let
in stdenv.mkDerivation rec {
    name = "emacs-nice";
    buildInputs = [
      emacs

      # required for building emacs-libvterm
      # note that libvterm is the incorrect lib!
      libvterm-neovim
      cmake
    ];
    shellHook = ''
    '';
}
