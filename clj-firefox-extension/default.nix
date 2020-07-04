/*

NOTES:

* asset paths
  ** borderify
    https://github.com/mdn/webextensions-examples/tree/master/borderify/icons
  ** beastify
    https://github.com/mdn/webextensions-examples/tree/master/beastify

* testing / building (beastify)

  generate-beastify-assets
  lein shadow release beastify-content
  lein shadow release beastify-popup

  launch throwaway firefox:
  ff 'about:debugging#/runtime/this-firefox' 'https://example.com'

* reloading changes
  you do not need to Reload the plugin to make simple javascript
  / html / css changes propagate for the popup plugin

* dev tools
  open from Debugging -> Temporary Extensions -> <extension> -> Inspect
  - pop out the window
  - switch to the tab / window containing the test extension
  - actions will log to the console if applicable
  - the console is otherwise useless because it gets cleared on
    mouseover /in the console/; you cannot expand stack traces.
  - difficult to observe otherwise because the popup plugin clears
    its own debug console on activation (mouse hover over icon!)
  - if the extension is visible in the Extension's own tab,
    activating also runs the same actions like clearing the
    console

*/

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

    alias generate-manifest='lein run -m firefox-extension.generate-manifest'
    alias generate-beastify-assets='lein run -m firefox-extension.beastify-generator'
    alias compile-extension='lein shadow compile'
    alias release-extension='lein shadow release'
  '';
}
