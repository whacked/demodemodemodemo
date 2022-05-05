{ pkgs ? import <nixpkgs> {} }:
pkgs.mkShell {
  buildInputs = [
    pkgs.nodejs
    pkgs.yarn
  ];

  shellHook = ''
    export PATH=$PATH:$(yarn bin)
    if ! (which shadow-cljs 2> /dev/null); then
        echo "initializing..."
        yarn
    fi

    alias dev='shadow-cljs watch workspaces'
    echo "run 'dev' to start"
  '';
}
