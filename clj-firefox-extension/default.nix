with import <nixpkgs> {};
let
  ff_base = (import ./firefox.nix);
  jet = callPackage (import ~/setup/nix/pkgs/development/tools/jet) {};
in stdenv.mkDerivation rec {
  name = "clj-firefox-extension";
  env = buildEnv {
    name = name;
    paths = buildInputs;
  };
  buildInputs = ff_base.buildInputs ++ [
    leiningen
    nodejs-10_x
    jet
  ];
  nativeBuildInputs = ff_base.nativeBuildInputs ++ [
  ];

  shellHook = ff_base.shellHook + ''
    export PATH=$PATH:$(npm bin)
    alias start-server='lein run'
    alias compile-cljs='lein shadow compile app'

    alias start-all='compile-cljs; start-server'
  '';
}
