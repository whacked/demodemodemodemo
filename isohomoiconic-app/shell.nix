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
    initialize-project-js-env() {  # sets up the bare node environment requirements
      pastel paint blue "installing react deps..."
      yarn add shadow-cljs react react-dom
      macchiato_deps=$(curl -s https://raw.githubusercontent.com/macchiato-framework/macchiato-core/85c5e3b0b55095565543a3dc876849f71df354d4/package.json |
        jq -r '.dependencies | keys[] as $k | "\($k)@\(.[$k])"' | paste -d ' ' -s -)
      pastel paint yellow "installing macchiato deps: $macchiato_deps"
      yarn add $macchiato_deps
    }

    if [ ! -e ./node_modules ]; then
      pastel paint red "node_modules not found"
      read -p "auto-initialize? [Y/n] " doinit
      case ''${doinit-y} in
        y|Y)
          yarn
          ;;
      esac
    fi

    build() {  # run this FIRST, to start the build loop
        shadow-cljs watch main server
    }

    serve() {  # after 'build' finishes its first build, run this to start the webserver
        server_script=xbuild/server.js
        if [ ! -e $server_script ]; then
            pastel paint yellow "did not find server script in $server_script; did you build it yet?"
            return
        fi
        watchexec --restart --no-ignore --watch build/ node build/server.js
    }

    browser-repl() {  # after your browser opened the address from `serve`, run this to connect a REPL to the browser
        shadow-cljs cljs-repl main
    }

    cat ${__curPos.file} | grep '^ \+\([-a-z]\+().\+\|function\|alias\) .\+'
  '';  # join strings with +
}
