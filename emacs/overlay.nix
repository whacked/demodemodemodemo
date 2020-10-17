with import <nixpkgs> {};

let
    overlay = import ../nix-overlay/default.nix;
    base = import ./default.nix;
in stdenv.mkDerivation rec {
    name = "emacs-dev";
    buildInputs = overlay.buildInputs ++ base.buildInputs ++ [
    ];
    shellHook = overlay.shellHook + base.shellHook + ''
      echo setting up emacs
    '';
}
