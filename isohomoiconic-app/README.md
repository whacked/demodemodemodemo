# Basic example using Macchiato server for "isohomomorphic" ClojureScript app

This example contains a functional webserver that serves a single page React page.

ClojureScript is the only language used, and all static assets (JS, CSS) are generated from ClojureScript (some on the fly, some not)


# how to run

See shell.nix for the environment setup and commands (build and serve).

If you have the [nix package manager](https://nixos.org/download.html#download-nix) installed, build+watch the app:

```sh
git clone https://github.com/whacked/demodemodemodemo
cd demodemodemodemo/isohomoiconic-app
nix-shell
yarn
build
```

Open another terminal and go to the same directory, and run

```
nix-shell
serve
```

to start the server. Then, navigate to http://localhost:8888

