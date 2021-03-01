DROP TABLE IF EXISTS `host`;
CREATE TABLE `host` (
  `id` INT(11) NOT NULL,
  `version` INT(11) NULL DEFAULT NULL,
  `hostname` varchar(50) NOT NULL,
  `ipv6` varchar(40) NOT NULL,
  `admin_password` varchar(20) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE `host`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `hostname` (`hostname`),
  ADD UNIQUE KEY `ipv6` (`ipv6`);
  
UPDATE `host` SET version=0;

ALTER TABLE `host`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--

DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
  `id` INT(11) NOT NULL,
  `version` INT(11) NULL DEFAULT NULL,
  `fullname` varchar(50) NOT NULL,
  `email` varchar(60) NOT NULL,
  `ssh_public_key` text NOT NULL,
  `role` varchar(10) NOT NULL COMMENT 'user role type: ADMIN or USER',
  `default_permission` varchar(100) NOT NULL COMMENT 'Comma separated technical users'
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE `user`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `email` (`email`);

UPDATE `user` SET version=0;

ALTER TABLE `user`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--

DROP TABLE IF EXISTS `project`;
CREATE TABLE `project` (
  `id` INT(11) NOT NULL,
  `version` INT(11) NULL DEFAULT NULL,
  `label` varchar(16) NOT NULL,
  `fullname` varchar(60) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE `project`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `label` (`label`);

UPDATE `project` SET version=0;

ALTER TABLE `project`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--

DROP TABLE IF EXISTS `container`;
CREATE TABLE `container` (
  `id` INT(11) NOT NULL,
  `version` INT(11) NULL DEFAULT NULL,
  `label` varchar(60) NOT NULL,
  `host_id` INT(11) NOT NULL,
  `project_id` INT(11) NOT NULL,
  `ipv6` varchar(40) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE `container`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE( `label`, `host_id`, `project_id`);

UPDATE `container` SET version=0;

ALTER TABLE `container`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--

DROP TABLE IF EXISTS `container_permission`;
CREATE TABLE `container_permission` (
  `id` INT(11) NOT NULL,
  `version` INT(11) NULL DEFAULT NULL,
  `container_id` INT(11) NOT NULL,
  `user_id` INT(11) NOT NULL,
  `permission` varchar(16) NOT NULL COMMENT 'Technical username to grant access to'
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE `container_permission`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `permission` (`container_id`, `user_id`, `permission`);

UPDATE `container_permission` SET version=0;

ALTER TABLE `container_permission`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--

DROP TABLE IF EXISTS `backend`;
CREATE TABLE `backend` (
  `id` INT(11) NOT NULL,
  `version` INT(11) NULL DEFAULT NULL,
  `label` varchar(16) NOT NULL,
  `project_id` INT(11) NOT NULL,
  `port` INT(6) NOT NULL,
  `params` TEXT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE `backend`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `label` (`label`, `project_id`);

  UPDATE `backend` SET version=0;
  
ALTER TABLE `backend`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--

DROP TABLE IF EXISTS `backend_container`;
CREATE TABLE `backend_container` (
  `id` INT(11) NOT NULL,
  `version` INT(11) NULL DEFAULT NULL,
  `backend_id` INT(11) NOT NULL,
  `container_id` INT(11) NOT NULL,
  `params` varchar(100) NULL COMMENT 'HAProxy params, e.g. check fall 3 rise 2'
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE `backend_container`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `bcke_cntr` (`backend_id`, `container_id`);

UPDATE `backend_container` SET version=0;

ALTER TABLE `backend_container`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;
  
--

DROP TABLE IF EXISTS `domain`;
CREATE TABLE `domain` (
  `id` INT(11) NOT NULL,
  `version` INT(11) NULL DEFAULT NULL,
  `domain` varchar(100) NOT NULL,
  `backend_id` INT(11) NOT NULL,
  `certprovided` TINYINT(1) NOT NULL,
  `letsencrypt` TINYINT(1) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE `domain`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `domain` (`domain`);

UPDATE `domain` SET version=0;

ALTER TABLE `domain`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

-- optimistic lock checks
DELIMITER $$
DROP PROCEDURE IF EXISTS optlock_check$$
CREATE DEFINER=root@localhost PROCEDURE optlock_check (IN oldversion INT, INOUT newversion INT)
BEGIN
  IF oldversion > newversion THEN
    signal sqlstate '45000' set message_text = 'Optimistic Lock';
  END IF;
  
  SET newversion = newversion+1;
END;$$
DELIMITER ;

DELIMITER $$
DROP TRIGGER IF EXISTS `host_optlock_check`$$
CREATE DEFINER=root@localhost TRIGGER `host_optlock_check` BEFORE UPDATE ON `host`
FOR EACH ROW
BEGIN
	CALL optlock_check(OLD.version, NEW.version);
END;$$
DELIMITER ;

DELIMITER $$
DROP TRIGGER IF EXISTS `user_optlock_check`$$
CREATE DEFINER=root@localhost TRIGGER `user_optlock_check` BEFORE UPDATE ON `user`
FOR EACH ROW
BEGIN
	CALL optlock_check(OLD.version, NEW.version);
END;$$
DELIMITER ;

DELIMITER $$
DROP TRIGGER IF EXISTS `project_optlock_check`$$
CREATE DEFINER=root@localhost TRIGGER `project_optlock_check` BEFORE UPDATE ON `project`
FOR EACH ROW
BEGIN
	CALL optlock_check(OLD.version, NEW.version);
END;$$
DELIMITER ;

DELIMITER $$
DROP TRIGGER IF EXISTS `container_optlock_check`$$
CREATE DEFINER=root@localhost TRIGGER `container_optlock_check` BEFORE UPDATE ON `container`
FOR EACH ROW
BEGIN
	CALL optlock_check(OLD.version, NEW.version);
END;$$
DELIMITER ;

DELIMITER $$
DROP TRIGGER IF EXISTS `container_permission_optlock_check`$$
CREATE DEFINER=root@localhost TRIGGER `container_permission_optlock_check` BEFORE UPDATE ON `container_permission`
FOR EACH ROW
BEGIN
	CALL optlock_check(OLD.version, NEW.version);
END;$$
DELIMITER ;

DELIMITER $$
DROP TRIGGER IF EXISTS `backend_optlock_check`$$
CREATE DEFINER=root@localhost TRIGGER `backend_optlock_check` BEFORE UPDATE ON `backend`
FOR EACH ROW
BEGIN
	CALL optlock_check(OLD.version, NEW.version);
END;$$
DELIMITER ;

DELIMITER $$
DROP TRIGGER IF EXISTS `backend_container_optlock_check`$$
CREATE DEFINER=root@localhost TRIGGER `backend_container_optlock_check` BEFORE UPDATE ON `backend_container`
FOR EACH ROW
BEGIN
	CALL optlock_check(OLD.version, NEW.version);
END;$$
DELIMITER ;

DELIMITER $$
DROP TRIGGER IF EXISTS `domain_optlock_check`$$
CREATE DEFINER=root@localhost TRIGGER `domain_optlock_check` BEFORE UPDATE ON `domain`
FOR EACH ROW
BEGIN
	CALL optlock_check(OLD.version, NEW.version);
END;$$
DELIMITER ;

-- sample data
INSERT INTO `host` (`id`, `version`, `hostname`, `ipv6`, `admin_password`) VALUES ('1', '0', 'host36.agitos.de', '2a01:4f8:173:25ef::2', 'testit');
INSERT INTO `user` (`id`, `version`, `fullname`, `email`, `ssh_public_key`, `role`, `default_permission`) VALUES
(1, 1, 'Florian Sager', 'sager@agitos.de', 'ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQDRA8T6Td/OGEyoxr0IK42K3hq6jcx8kYg9eJoa72lTcazOI7o4gTW1LRpdRzmc4VfdTbWtii8rIHtQG8AGFZHnlcCRqxG36QmWq8/RwexdbC3fLgSPJXfEyOSg5I99Os1ixjaqWomXaDf+YpFDM+oBIC0WfBedmZ44Ef95Nvo9HefotjBc+PwqX0vyn2wYczdJd7n9JeHi9HCbWcxtoxAgafWx9o77fUdDE6lfPaCV7NgjDaVkj/CLkKl3ICJ4R9j3SrCmdfmbzwm3i6n+v6zYCEzQhDYkBDaYdvKfEwuVdSvxWcjP3StwCdHSxonuIFVeTjWsXfT2DrZDZeztq5Ct sager@agitos.de', 'USER', 'www-data,root');
