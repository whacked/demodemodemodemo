#!/usr/bin/env bash

apk add git

cd $HOME
git clone https://github.com/basherpm/basher $HOME/.basher
export PATH=$PATH:$HOME/.basher/bin
eval $(basher init -)

basher install bash-it/bash-it
export BASH_IT=$(basher package-path bash-it)/bash-it
export BASH_IT_THEME="nwinkler"
source $BASH_IT/bash_it.sh
