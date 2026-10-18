#!/usr/bin/env bash
# Runs a playbook on the Ansible control node (Ubuntu on WSL):
#   bash run.sh <playbook.yml> [ansible-playbook options]
# Ansible ignores ansible.cfg in world-writable folders such as /mnt/c, so the playbooks run from a copy
# in the Linux home folder. The organiser password comes from the environment (EFD_ORGANISER_PASSWORD).
set -euo pipefail
# provision.yml runs docker.exe through WSL interop. A privileged container whose systemd runs systemd-binfmt
# clears the VM-wide binfmt_misc table and with it WSL's interop entry; restore WSL's standard entry if missing.
if ! ls /proc/sys/fs/binfmt_misc/ | grep -q '^WSLInterop'; then
    echo ':WSLInterop:M::MZ::/init:PF' > /proc/sys/fs/binfmt_misc/register
    echo "# restored the WSL interop registration"
fi
SRC="$(cd "$(dirname "$0")" && pwd)"
WORK="$HOME/efd-ansible"
rm -rf "$WORK"
cp -r "$SRC" "$WORK"
chmod -R go-w "$WORK"
find "$WORK" -type f \( -name '*.yml' -o -name '*.cfg' -o -name '*.j2' -o -name '*.sh' \) -exec sed -i 's/\r$//' {} +
cd "$WORK"
export ANSIBLE_FORCE_COLOR=0 ANSIBLE_NOCOLOR=1
echo "# $(TZ=Asia/Kolkata date '+%Y-%m-%d %H:%M:%S %Z') ansible-playbook $* (ansible-core $(ansible --version | head -1 | grep -o '[0-9][0-9.]*' | head -1), control node $(hostname))"
exec ansible-playbook "$@"
