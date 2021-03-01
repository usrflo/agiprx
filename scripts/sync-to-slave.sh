#!/bin/bash

# Sync haproxy config to slave agiprx server and apply new configuration

SLAVEIP="$1"

# stop on error
set -e

# rsync /etc/letsencrypt/
rsync -azHAX -e'ssh -p2222 -o StrictHostKeyChecking=no' --timeout=300 --inplace --delete --delete-excluded -F /etc/letsencrypt/ root@[$SLAVEIP]:/etc/letsencrypt/

# rsync /etc/haproxy/
rsync -azHAX -e'ssh -p2222 -o StrictHostKeyChecking=no' --timeout=300 --inplace --delete --delete-excluded -F /etc/haproxy/ root@[$SLAVEIP]:/etc/haproxy/

# rsync /opt/agiprx/.ssh/authorized_keys
rsync -azHAX -e'ssh -p2222 -o StrictHostKeyChecking=no' --timeout=300 /opt/agiprx/.ssh/authorized_keys root@[$SLAVEIP]:/opt/agiprx/.ssh/authorized_keys

# transfer and import database
mysqldump --defaults-file=/etc/mysql/debian.cnf --opt --single-transaction --order-by-primary --flush-logs --events --routines agiprx | ssh -p2222 root@$SLAVEIP mysql --defaults-file=/etc/mysql/debian.cnf agiprx

# restart agiprx to re-generate HAProxy config and reload and to re-generate SSH proxy config
ssh -p2222 -o StrictHostKeyChecking=no root@$SLAVEIP '/usr/bin/systemctl restart agiprx'
