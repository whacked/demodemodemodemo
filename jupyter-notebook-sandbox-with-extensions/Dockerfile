FROM ubuntu:latest
ENV USER testuser

RUN apt-get -y update
RUN apt-get -y install sudo curl tar bzip2 xz-utils

RUN useradd -m $USER -s /bin/bash; echo "$USER ALL=(ALL) NOPASSWD:ALL" | tee -a /etc/sudoers.d/$USER

# note that if the envvar USER is empty, nix.sh will fail to load!
USER $USER
RUN curl -L --proto '=https' --tlsv1.2 https://nixos.org/nix/install | sh
RUN echo '. ~/.nix-profile/etc/profile.d/nix.sh' >> ~/.bashrc
WORKDIR /home/$USER

COPY default.nix /tmp/default.nix
RUN bash -c '. ~/.nix-profile/etc/profile.d/nix.sh; cd /tmp; nix-shell --run "jupyter --version"'
WORKDIR /opt/demo

RUN bash -c '. ~/.nix-profile/etc/profile.d/nix.sh; nix-env -i tmux vim git'
