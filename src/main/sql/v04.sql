ALTER TABLE `user` ADD `agiprx_permission` VARCHAR(200) NULL DEFAULT NULL AFTER `default_permission`;

UPDATE `user` SET role='CONTACT' WHERE role='USER';

---

CREATE TABLE `api_user` (
  `id` INT(11) NOT NULL,
  `version` INT(11) NULL DEFAULT NULL,
  `username` VARCHAR(50) NOT NULL,
  `password` VARCHAR(50) NOT NULL,
  `email` varchar(60) NOT NULL,
  `agiprx_permission` varchar(200) NULL DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE `api_user`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `username` (`username`);

UPDATE `api_user` SET version=0;

ALTER TABLE `api_user`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;
