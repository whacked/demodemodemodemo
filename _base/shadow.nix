with import <nixpkgs> {};
stdenv.mkDerivation rec {
    name = "shadow-setup-base";
    env = buildEnv {
        name = name;
        paths = buildInputs;
    };
    buildInputs = [
        fuse-overlayfs
    ];
    shellHook = ''
    _TERMINAL=bash
    _CHROOT_DIR=shadow-root
    _SHADOW_HOME=shadow-home
    _BASH_INIT="source ~/.nix-profile/etc/profile.d/nix.sh"
    _SHADOW_METHOD=overlay 
    
    function __setup_shadow() {
        _BASE_DIR=$1
        _ORIGINAL_DIR=$PWD
        if [ ! -e $_BASE_DIR ]; then
            mkdir $_BASE_DIR
        fi
        cd $_BASE_DIR
        _SHADOW_METHOD=''${2-$_SHADOW_METHOD}
        mkdir -p $_CHROOT_DIR/run
        case $_SHADOW_METHOD in
            chroot)
                sudo mount -o bind / $_CHROOT_DIR
                ;;
            overlay)
                OVERLAY_LOWER=/
                OVERLAY_UPPER=overlay.add
                OVERLAY_WORK=overlay.work
                OVERLAY_MERGED=$_CHROOT_DIR
                mkdir -p $OVERLAY_UPPER $OVERLAY_WORK $_SHADOW_HOME
                sudo mount -t overlay \
                    -o lowerdir=$OVERLAY_LOWER,upperdir=$OVERLAY_UPPER,workdir=$OVERLAY_WORK \
                    overlay $OVERLAY_MERGED
                mkdir -p $_SHADOW_HOME/.nix-profile
                # needed for resolvconf
                sudo mount -o bind /run $OVERLAY_MERGED/run
                sudo mount -o bind $_SHADOW_HOME $OVERLAY_MERGED/$HOME
                sudo mount -o bind $HOME/.nix-profile $OVERLAY_MERGED/$HOME/.nix-profile
                ;;
            *)
                echo "bad method: $_SHADOW_METHOD"
                exit -1
                ;;
        esac
    }
    
    function __run_shadow() {
        sudo chroot --userspec $USER:$USER $_CHROOT_DIR bash -t -c "$_BASH_INIT; cd \$HOME; $_TERMINAL"
    
        case $_SHADOW_METHOD in
            overlay)
                set -x
                sudo umount $OVERLAY_MERGED/run
                sudo umount $OVERLAY_MERGED/$HOME/.nix-profile
                sudo umount $OVERLAY_MERGED/$HOME
                set +x
                ;;
        esac
    
        sudo umount $_CHROOT_DIR
        set +x
    }
    
    function __cleanup_shadow() {
        case $_SHADOW_METHOD in
            overlay)
            sudo chown -R $USER: $OVERLAY_UPPER $OVERLAY_WORK
            ;;
        esac
        cd $_ORIGINAL_DIR
    }
    
    function shadow-start() {
        _target=''${1-$PWD}
        __setup_shadow $_target
        __run_shadow
        __cleanup_shadow
    }
    '';
}
