with import <nixpkgs> {};

let
in stdenv.mkDerivation rec {
  name = "iframe-jwt-postMessage-example";
  env = buildEnv {
    name = name;
    paths = buildInputs;
  };
  buildInputs = [
    python37Full
    python37Packages.flask
    python37Packages.pyjwt
  ];

  SERVE_OUTER_PORT = "10000";
  SERVE_INNER_PORT = "20000";

  shellHook = ''
    echo "1. start the inner (iframe) server:"
    echo "  python serve_inner.py"
    echo "2. start the outer (host) server:"
    echo "  python serve_outer.py"
    echo "3. visit host server"
    echo "  http://localhost:${SERVE_OUTER_PORT}"
  '';
}

