# Clean target node for the Event Feedback Dashboard: a bare Ubuntu 24.04 server.
#
# It contains only what a freshly installed cloud server has: systemd, an SSH server, Python 3 and an
# administrator account ("ansible", key-based login, passwordless sudo). Everything the application needs
# (Java, service user, folders, configuration, systemd service) is added by the Ansible playbook.
# provision.yml builds this image and starts a new node container from it whenever a clean node is needed.
FROM ubuntu:24.04

ENV container=docker \
    DEBIAN_FRONTEND=noninteractive

RUN apt-get update \
 && apt-get install -y --no-install-recommends systemd systemd-sysv dbus openssh-server sudo python3 iproute2 \
 && apt-get clean && rm -rf /var/lib/apt/lists/* \
 # units that make no sense inside a container. systemd-binfmt must not run: the node is privileged and
 # binfmt_misc is shared by the whole VM, so it would wipe the registration WSL uses to run Windows programs
 && systemctl mask systemd-udevd.service systemd-udev-trigger.service systemd-logind.service getty.target console-getty.service \
        systemd-binfmt.service proc-sys-fs-binfmt_misc.automount proc-sys-fs-binfmt_misc.mount \
 && systemctl enable ssh.service \
 && mkdir -p /run/sshd \
 # administrator account used by Ansible (the public key is added by provision.yml)
 && useradd -m -s /bin/bash ansible \
 && echo 'ansible ALL=(ALL) NOPASSWD:ALL' > /etc/sudoers.d/ansible && chmod 0440 /etc/sudoers.d/ansible \
 && install -d -m 0700 -o ansible -g ansible /home/ansible/.ssh

EXPOSE 22
STOPSIGNAL SIGRTMIN+3
CMD ["/lib/systemd/systemd"]
