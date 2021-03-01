ALTER TABLE `domain` ADD `redirect_to_url` VARCHAR(100) NULL DEFAULT NULL COMMENT 'redirect target url' AFTER `letsencrypt`;
