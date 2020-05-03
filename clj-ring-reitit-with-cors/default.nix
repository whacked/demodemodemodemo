with import <nixpkgs> {};

let
in stdenv.mkDerivation rec {
  name = "clj-ring-reitit-with-cors";
  env = buildEnv {
    name = name;
    paths = buildInputs;
  };
  buildInputs = [
    leiningen
  ];
  shellHook = ''
    alias repl='lein repl'
    alias run='lein run'
    cat default.nix | grep '[a]lias'
  '';
}
