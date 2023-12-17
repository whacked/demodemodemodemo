{
  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixpkgs-unstable";
    flake-utils.url = "github:numtide/flake-utils";
  };

  outputs = { self, nixpkgs, flake-utils }: {

    packages.x86_64-linux.example = (
      let 
        janet-musl-with-jpm = (
          nixpkgs.legacyPackages.x86_64-linux.stdenv.mkDerivation {
          name = "janet-musl-with-jpm";
          src = nixpkgs.legacyPackages.x86_64-linux.janet.src;
          CC = "musl-gcc";
          LD = "musl-gcc";
          buildInputs = [
            nixpkgs.legacyPackages.x86_64-linux.musl.dev
            nixpkgs.legacyPackages.x86_64-linux.janet.src
          ];
          
          buildPhase = ''
            unset NIX_LDFLAGS
            export CC="musl-gcc";
            export LD="musl-gcc";
            # looks unnecessary, but left for reference:
            # export LIBRARY_PATH=${nixpkgs.legacyPackages.x86_64-linux.musl}/lib
            export CFLAGS="-static"
            make -j$(nproc)

            # modified from janet Makefile install-jpm-git
            cp -R ${nixpkgs.legacyPackages.x86_64-linux.jpm.src} build/jpm
            chmod -R a+w build/jpm
            pushd build/jpm
            export DESTDIR=jpm-build
            export PREFIX=""
            # hacky, because the file outputs are influenced by this
            # but it also matters at CLI invocation time
            export JANET_PATH=""
            ../janet ./bootstrap.janet
            cp ./jpm/jpm $DESTDIR/bin
            popd
          '';
          installPhase = ''
            mkdir -p $out/bin
            cp -R src/include $out/include
            cp src/conf/* $out/include/
            cp build/janet $out/bin/
            mkdir -p $out/lib
            cp build/libjanet.so build/libjanet.a $out/lib
            cp -R build/jpm/jpm-build $out/jpm
            cp $out/jpm/bin/jpm $out/bin/jpm
          '';
        });
      in

      nixpkgs.legacyPackages.x86_64-linux.stdenv.mkDerivation {
        name = "janet-static-sample";
        src = ./.;
        buildInputs = [
          nixpkgs.legacyPackages.x86_64-linux.musl.dev
          nixpkgs.legacyPackages.x86_64-linux.musl
          janet-musl-with-jpm
        ];
        JANET_PATH = "${janet-musl-with-jpm}/jpm";
        CFLAGS = "-static";
        buildPhase = ''
          # HACK: the jpm script starts with #/usr/bin/env janet
          # but in the build env we don't have /usr/bin/env,
          # so we call janet directly
          janet ${janet-musl-with-jpm}/bin/jpm --libpath=${janet-musl-with-jpm}/lib build
        '';
        installPhase = ''
          mkdir -p $out/bin
          cp build/static-sample $out/bin/
        '';
        shellHook = ''
          # sanity checks
          which janet jpm
          alias jpm='${janet-musl-with-jpm}/bin/jpm --libpath=${janet-musl-with-jpm}/lib'
        '';
      });
        
    defaultPackage.x86_64-linux = self.packages.x86_64-linux.example;
  };
}

