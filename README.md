# AgiPrx - HAProxy configuration CLI UI and SSH Proxy config tool

Manage thousands of domains, backends and servers/virtual machines/containers by a SQL based CLI tool and generate automated HAProxy and SSH proxy configuration.

## Outline

Think about you have thousands of domain names to administer, lots of internal servers/virtual-machines/containers that serve web-content for these domain names. You need to get SSL certificates assigned and updated for these domains. You need to manage those domain names in groups/projects and be able to silently switch those groups from old server:port to a new server:port without external disturbance. And think about you want to additionally manage SSH/SFTP user accounts including SSH public keys that should be assigned to certain backends and switched over silently while user access data from the outside remains the same. And think about you like to give some self service to your advanced users / DevOps that like to be able to administer their systems by their own, e.g. assign domain names, replace external certificates or create new backends. And think about you like to offer services from your internal (IPv6 only) network to the outside world by IPv4.
Then you get an idea what AgiPrx can be used for:

![AgiPrx-Overview](docs/agiprx-overview.svg)

## Features

- management of HAProxy backends, domain names and servers/virtual machines/containers: this facilitates the management in a multi-customer, multi-project, multi-backend and multi-domain environment (e.g. serving hundreds of customers and thousands of domain names via HAProxy)
- IPv4 to IPv6 gateway for web services and SSH access 
- SSL certificate management: automated Let's Encrypt cert issuance and renewal, timely cert renewal notifications
- optional customer SSH permission management: configuration of a SSH proxy setup with tunneling to the target servers/containers; this way customer credentials and servernames remain stable even if internal systems are reorganized
- user management: AgiPrx-admin and customer contact and SSH public key administration
- search functionality to jump into specific user/project/container/backend/domain configuration
- configuration in SQL database: run mass-updates with SQL
- archiving of configuration before and after configuration changes
- safe HAProxy-reload / fallback to last configuration in case of errors
- REST interface for mass-domain updates on backends (may be extended upon request)
- no config interference: manual configuration of HAProxy or system users can co-exist besides generated configuration; AgiPrx can be removed and the generated configuration continues to work independently
- optional master-slave setup with multiple slave servers: all configurations including database and certificate data can be synchronized to slave servers to prevent a SPOF regarding AgiPrx, HAProxy and the SSH Proxy setup

## Console Screenshot

![AgiPrx-Overview](docs/agiprx-console-start.png)

## Master-Slave-Setup

![AgiPrx-Master-Slave-Setup](docs/agiprx-master-slave.svg)

## Setup and System Requirements

### Packages

- MariaDB
- openssl
- certbot
- cron
- AgiPrx deployment via ansible: AgiPrx comes with a custom reduced Java11-JRE

### Hardware

- Small setup: 130M of reserved RAM
- Large setup with >300 backends, >2600 SSL certificates, >7000 domains: 260M of reserved RAM

### Setup Instructions

See repository [AgiPrx-Setup](https://github.com/usrflo/agiprx-setup) for an automated deployment by ansible.

## REST-Interface

See **/agiprx/src/main/java/de/agitos/agiprx/rest** for current implementation status.
`TODO: add interface documentation`

## TODO

- run AgiPrx as non-root: assure that HAProxy can be reloaded by sudo permissions, assure that AgiPrx can create/update/remove local system users by sudo permissions, assure that certbot can create Letsencrypt certificates with limited permissions
- USER role: remove permission to assign any ip address to containers to prevent foreign container access
- USER role: extended backend configuration, prevent malicious parameter configuration
- add optional editing of individual user-specific SSH private keys in permission handling (replacement of default SSH key)
- add audit tables to track changes to the database
- add simple HTTP basic authentication administration and assignment to HAProxy backends
- remove LXD/LXC references (AgiPrx can be used independently from LXD)
- copy functionality, e.g. copy user permissions from one container setup to another
- optional support of longer usernames that may become long because of projectlabel-containerlabel_techusername, see https://serverfault.com/a/1043812
- add unit tests: there are currently only a handful of tests with no noteworthy code coverage

## Licenses

AgiPrx is licensed under [GNU GPLv3](https://opensource.org/licenses/GPL-3.0).

Package de.agitos.agiprx.db.* contains a modified version of [Spring JDBC](https://github.com/spring-projects/spring-framework/tree/master/spring-jdbc), its license is [Apache License v2.0](https://www.apache.org/licenses/LICENSE-2.0).
