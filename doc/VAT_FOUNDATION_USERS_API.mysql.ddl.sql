CREATE TABLE IF NOT EXISTS `foundation_users_user` (
	`removed` BOOLEAN NOT NULL DEFAULT FALSE,
	`creator` BIGINT NOT NULL DEFAULT -1,
	`created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
	`modifier` BIGINT NOT NULL DEFAULT -1,
	`modified_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP on update CURRENT_TIMESTAMP,
	`id` BIGINT AUTO_INCREMENT PRIMARY KEY,
	`version` INT NOT NULL DEFAULT 0,
	`profile` JSON NOT NULL
);
CREATE TABLE IF NOT EXISTS `foundation_users_certificate` (
	`removed` BOOLEAN NOT NULL DEFAULT FALSE,
	`creator` BIGINT NOT NULL DEFAULT -1,
	`created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
	`modifier` BIGINT NOT NULL DEFAULT -1,
	`modified_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP on update CURRENT_TIMESTAMP,
	`id` BIGINT AUTO_INCREMENT PRIMARY KEY,
	`version` INT NOT NULL DEFAULT 0,
	`history` JSON,
	`user` BIGINT NOT NULL,
	`kind` INT NOT NULL,
	`identifier` VARCHAR(128) NOT NULL,
	`certificate` JSON NOT NULL,
	`profile` JSON NOT NULL,
	INDEX `idx_foundation_users_certificate-user` (`user`),
	CONSTRAINT `udx_foundation_users_certificate-identifier_kind` UNIQUE (`identifier`, `kind`)
);
