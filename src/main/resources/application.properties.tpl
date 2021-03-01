# email configuration
email.sender=agiprx@example.org
email.adminReceiver=webmaster@example.org
email.username=agiprx@example.org
email.password=mySmtpAuth!
email.server=smtp.example.org
email.smtpport=587
email.subjectMaintenanceTool=master agiprx maintenance status
email.footer=Example ORG

# pre notification before end of SSL certificate periods
cert.eolnotificationdays=14


### BEGIN: master instance configuration ###
agiprx.masterinstance=true

# define slave instance IPs on distributed setup; a list of IPs is space separated, ip4 can be mixed with ip6
# slave = proxy2.infomaxnet.de
agiprx.slaveIpList=1.2.3.5

# path to sync script for slave synchronization; first and only argument is the slave server ip
agiprx.slaveSyncCommand=/opt/agiprx/scripts/sync-to-slave.sh

# accepted IPs in domain name validation; a list of IPs is space separated, ip4 can be mixed with ip6
# those IPs directly or indirectly need to refer/forward to the configured master/slave proxies;
domain.trustedIps=1.2.3.4

### END: master instance configuration ###


### BEGIN: slave instance configuration ###
#agiprx.masterinstance=false

# master IP
#agiprx.masterIp=127.0.0.1
#agiprx.masterIp=::2

### END: slave instance configuration ###

# HAProxy reload command
haproxy.reloadCommand=/usr/bin/systemctl reload haproxy

# informative SSH proxy domainname and port for end-user notifications 
proxy.domainname=proxy.example.org
proxy.port=2222

# DNS for domainname resolution
proxy.nameServer=1.2.3.10

# Let's Encrypt Issuer Partial Name
cert.lesslissuerparname=Let's Encrypt
# Let's Encrypt cert request, run 'certbot register' to create a new account
cert.certbotnewcertcommand=/usr/bin/certbot certonly --account 12345 --non-interactive --standalone --force-renewal --agree-tos -m webmaster@example.org --preferred-challenges http --http-01-port 8001 --disable-hook-validation --deploy-hook "/opt/agiprx/scripts/concat-lesslcert-for-haproxy.sh" --post-hook "/opt/agiprx/scripts/haproxy-safe-reload.sh" -d %s
# Let's Encrypt cert renewal
cert.certbotrenewcertscommand=/usr/bin/certbot renew -q --non-interactive --disable-hook-validation --deploy-hook "/opt/agiprx/scripts/concat-lesslcert-for-haproxy.sh" --post-hook "/opt/agiprx/scripts/haproxy-safe-reload.sh"

db.url=jdbc\:mysql\://localhost\:3306/agiprx?useSSL=false&autoReconnect=true&serverTimezone=Europe/Berlin
db.user=agiprx
db.password=6OohdOeVgMYW

# mysqldump
db.database=agiprx
db.host=localhost
db.port=3306

# AgiPrx SSH console configuration
agiprx.user=agiprx
agiprx.port=2223
agiprx.defaultSshKey=/opt/agiprx/etc/prx_rsa
# files will be generated if not available
agiprx.authorizedAccessKeys=/opt/agiprx/.ssh/authorized_keys
agiprx.hostKeys=/opt/agiprx/.ssh/hostkey.ser

# AgiPrx REST-API
server.port=8002
server.jetty.max-http-post-size=5000000B
server.jetty.max-threads=8

# cron configuration: SEC MIN HOUR DAY MONTH WEEKDAY
cron.maintenancejob=0 5 0 * * *

# lxc API, create certs via $ lxc remote add local-rest-api https://127.0.0.1:8443, leave empty to disable
# lxd.fetchcontainers=curl -s -k --cert /root/snap/lxd/current/.config/lxc/client.crt --key /root/snap/lxd/current/.config/lxc/client.key https://127.0.0.1:8443/1.0/containers?recursion=2
lxd.fetchcontainers=

application.secret=eTxjSWK6tKul
