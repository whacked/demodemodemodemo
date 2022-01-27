{ pkgs ? import <nixpkgs> {} }:
pkgs.mkShell {
  buildInputs = [
    pkgs.jq
    pkgs.nodejs
    pkgs.pastel
    pkgs.watchexec
    pkgs.yarn
  ];  # join lists with ++

  shellHook = ''
    export PATH=$PATH:$(yarn bin)

    function initialize-project-js-env() {  # sets up the bare node environment requirements
      pastel paint blue "installing react deps..."
      yarn add shadow-cljs react react-dom
      macchiato_deps=$(curl -s https://raw.githubusercontent.com/macchiato-framework/macchiato-core/85c5e3b0b55095565543a3dc876849f71df354d4/package.json |
        jq -r '.dependencies | keys[] as $k | "\($k)@\(.[$k])"' | paste -d ' ' -s -)
      pastel paint yellow "installing macchiato deps: $macchiato_deps"
      yarn add $macchiato_deps
    }

    alias build='shadow-cljs watch main server'
    alias serve="watchexec --restart --no-ignore --watch build/ node build/server.js"

    cat ${__curPos.file} | grep '^ \+\(function\|alias\) .\+'
  '';  # join strings with +
}
