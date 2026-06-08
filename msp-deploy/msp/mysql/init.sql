-- 密算平台数据库初始化脚本 (MySQL 8.0)

-- 强制使用 UTF-8 字符集加载, 避免 Docker-entrypoint 用 latin1 加载造成双重编码
SET NAMES utf8mb4;

-- 创建 Kuscia 数据库（供 Kuscia Master 使用）
CREATE DATABASE IF NOT EXISTS kuscia_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 给 msp 用户授权访问 kuscia_db
GRANT ALL PRIVILEGES ON kuscia_db.* TO 'msp'@'%';
FLUSH PRIVILEGES;

-- 节点表
CREATE TABLE IF NOT EXISTS msp_nodes (
    node_id VARCHAR(64) PRIMARY KEY,
    node_name VARCHAR(128) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'OFFLINE',
    node_mode VARCHAR(32) NOT NULL DEFAULT 'RAY' COMMENT '节点部署模式: RAY / KUSCIA',
    endpoint VARCHAR(256),
    external_endpoint VARCHAR(256),
    capabilities TEXT,
    tags TEXT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 任务表
CREATE TABLE IF NOT EXISTS msp_tasks (
    task_id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(256) NOT NULL,
    type VARCHAR(32) NOT NULL,
    algorithm VARCHAR(64),
    status VARCHAR(32) NOT NULL DEFAULT 'CREATED',
    node_mode VARCHAR(32) DEFAULT 'ray' COMMENT '节点模式: ray / kuscia',
    participants TEXT,
    inputs TEXT,
    parameters TEXT,
    description TEXT,
    code TEXT COMMENT '任务代码/DAG规格',
    result TEXT COMMENT '任务执行结果',
    execution_log TEXT COMMENT '任务执行过程日志（JSON：分发 / 节点执行轨迹）',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 兼容老库（init.sql 早期版本没有 node_mode / execution_log 列），已存在则忽略
-- 用存储过程判断列存在再加，避免 MySQL 5.7 / 8.0 都不支持的 IF NOT EXISTS 语法
DROP PROCEDURE IF EXISTS msp_add_task_columns;
DELIMITER //
CREATE PROCEDURE msp_add_task_columns()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'msp_tasks'
          AND COLUMN_NAME = 'node_mode'
    ) THEN
        ALTER TABLE msp_tasks ADD COLUMN node_mode VARCHAR(32) DEFAULT 'ray' COMMENT '节点模式: ray / kuscia';
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'msp_tasks'
          AND COLUMN_NAME = 'execution_log'
    ) THEN
        ALTER TABLE msp_tasks ADD COLUMN execution_log TEXT COMMENT '任务执行过程日志（JSON：分发 / 节点执行轨迹）';
    END IF;
END //
DELIMITER ;
CALL msp_add_task_columns();
DROP PROCEDURE msp_add_task_columns;

-- 数据源表
CREATE TABLE IF NOT EXISTS msp_datasources (
    datasource_id VARCHAR(64) PRIMARY KEY,
    node_id VARCHAR(64) NOT NULL,
    name VARCHAR(256) NOT NULL,
    type VARCHAR(32) NOT NULL,
    host VARCHAR(256),
    port INTEGER,
    database_name VARCHAR(128),
    username VARCHAR(128),
    password VARCHAR(256),
    table_name VARCHAR(128),
    columns TEXT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (node_id) REFERENCES msp_nodes(node_id)
);

-- 兼容旧表：若表已存在但缺少 username/password 列，补上
SET @c1 := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA='msp_db' AND TABLE_NAME='msp_datasources' AND COLUMN_NAME='username');
SET @sql1 := IF(@c1=0, 'ALTER TABLE msp_datasources ADD COLUMN username VARCHAR(128) AFTER database_name', 'DO 0');
PREPARE stmt1 FROM @sql1; EXECUTE stmt1; DEALLOCATE PREPARE stmt1;

SET @c2 := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA='msp_db' AND TABLE_NAME='msp_datasources' AND COLUMN_NAME='password');
SET @sql2 := IF(@c2=0, 'ALTER TABLE msp_datasources ADD COLUMN password VARCHAR(256) AFTER username', 'DO 0');
PREPARE stmt2 FROM @sql2; EXECUTE stmt2; DEALLOCATE PREPARE stmt2;

-- 审计日志表
CREATE TABLE IF NOT EXISTS msp_audit_logs (
    log_id VARCHAR(64) PRIMARY KEY,
    user_id VARCHAR(64),
    action VARCHAR(64) NOT NULL,
    resource_type VARCHAR(32),
    resource_id VARCHAR(64),
    details TEXT,
    ip_address VARCHAR(64),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 用户表
CREATE TABLE IF NOT EXISTS msp_users (
    user_id VARCHAR(64) PRIMARY KEY,
    username VARCHAR(128) NOT NULL UNIQUE,
    password VARCHAR(256) NOT NULL,
    email VARCHAR(128),
    phone VARCHAR(32),
    role VARCHAR(32) NOT NULL DEFAULT 'USER',
    status VARCHAR(16) DEFAULT 'ACTIVE',
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 角色表
CREATE TABLE IF NOT EXISTS msp_roles (
    role_id VARCHAR(64) PRIMARY KEY,
    role_code VARCHAR(64) NOT NULL UNIQUE,
    role_name VARCHAR(128) NOT NULL,
    description TEXT,
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 用户角色关联表
CREATE TABLE IF NOT EXISTS msp_user_roles (
    user_id VARCHAR(64) NOT NULL,
    role_id VARCHAR(64) NOT NULL,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, role_id),
    FOREIGN KEY (user_id) REFERENCES msp_users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (role_id) REFERENCES msp_roles(role_id) ON DELETE CASCADE
);

-- 权限表
CREATE TABLE IF NOT EXISTS msp_permissions (
    permission_id VARCHAR(64) PRIMARY KEY,
    permission_code VARCHAR(128) NOT NULL UNIQUE,
    permission_name VARCHAR(128) NOT NULL,
    resource_type VARCHAR(32),
    parent_id VARCHAR(64),
    path VARCHAR(256),
    icon VARCHAR(64),
    sort_order INT DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (parent_id) REFERENCES msp_permissions(permission_id) ON DELETE SET NULL
);

-- 角色权限关联表
CREATE TABLE IF NOT EXISTS msp_role_permissions (
    role_id VARCHAR(64) NOT NULL,
    permission_id VARCHAR(64) NOT NULL,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (role_id, permission_id),
    FOREIGN KEY (role_id) REFERENCES msp_roles(role_id) ON DELETE CASCADE,
    FOREIGN KEY (permission_id) REFERENCES msp_permissions(permission_id) ON DELETE CASCADE
);

-- 插入默认管理员用户 (密码: admin123, SHA-256 hash then Base64 encoded)
INSERT INTO msp_users (user_id, username, password, role, enabled, create_time, update_time)
VALUES ('admin-default-id', 'admin', 'JAvlGPq9JyTdtvBO6x2llnRI1+gxwIyPqCKAn3THIKk=', 'ADMIN', TRUE, NOW(), NOW())
ON DUPLICATE KEY UPDATE password = 'JAvlGPq9JyTdtvBO6x2llnRI1+gxwIyPqCKAn3THIKk=';

-- 插入默认角色
INSERT INTO msp_roles (role_id, role_code, role_name, description, status, create_time, update_time)
VALUES
    ('role-admin', 'ROLE_ADMIN', _utf8mb4'管理员', _utf8mb4'系统管理员，拥有所有权限', 'ACTIVE', NOW(), NOW()),
    ('role-user', 'ROLE_USER', _utf8mb4'普通用户', _utf8mb4'普通用户，基本操作权限', 'ACTIVE', NOW(), NOW())
ON DUPLICATE KEY UPDATE role_name = role_name;

-- 插入默认权限（树形结构）
INSERT INTO msp_permissions (permission_id, permission_code, permission_name, resource_type, parent_id, path, icon, sort_order)
VALUES
    -- 系统管理菜单
    ('perm-system', 'system', _utf8mb4'系统管理', 'MENU', NULL, '/system', 'Setting', 100),
    ('perm-users', 'system:user', _utf8mb4'用户管理', 'MENU', 'perm-system', '/users', 'User', 1),
    ('perm-users:view', 'system:user:view', _utf8mb4'用户查看', 'BUTTON', 'perm-users', NULL, NULL, 0),
    ('perm-users:create', 'system:user:create', _utf8mb4'用户创建', 'BUTTON', 'perm-users', NULL, NULL, 1),
    ('perm-users:update', 'system:user:update', _utf8mb4'用户更新', 'BUTTON', 'perm-users', NULL, NULL, 2),
    ('perm-users:delete', 'system:user:delete', _utf8mb4'用户删除', 'BUTTON', 'perm-users', NULL, NULL, 3),
    ('perm-roles', 'system:role', _utf8mb4'角色管理', 'MENU', 'perm-system', '/roles', 'Key', 2),
    ('perm-roles:view', 'system:role:view', _utf8mb4'角色查看', 'BUTTON', 'perm-roles', NULL, NULL, 0),
    ('perm-roles:create', 'system:role:create', _utf8mb4'角色创建', 'BUTTON', 'perm-roles', NULL, NULL, 1),
    ('perm-roles:update', 'system:role:update', _utf8mb4'角色更新', 'BUTTON', 'perm-roles', NULL, NULL, 2),
    ('perm-roles:delete', 'system:role:delete', _utf8mb4'角色删除', 'BUTTON', 'perm-roles', NULL, NULL, 3),
    ('perm-permissions', 'system:permission', _utf8mb4'权限管理', 'MENU', 'perm-system', '/permissions', 'Lock', 3),
    -- 业务菜单
    ('perm-task', 'task', _utf8mb4'任务管理', 'MENU', NULL, '/tasks', 'List', 1),
    ('perm-task:view', 'task:view', _utf8mb4'任务查看', 'BUTTON', 'perm-task', NULL, NULL, 0),
    ('perm-task:create', 'task:create', _utf8mb4'任务创建', 'BUTTON', 'perm-task', NULL, NULL, 1),
    ('perm-task:cancel', 'task:cancel', _utf8mb4'任务取消', 'BUTTON', 'perm-task', NULL, NULL, 2),
    ('perm-node', 'node', _utf8mb4'节点管理', 'MENU', NULL, '/nodes', 'Connection', 2),
    ('perm-node:view', 'node:view', _utf8mb4'节点查看', 'BUTTON', 'perm-node', NULL, NULL, 0),
    ('perm-datasource', 'datasource', _utf8mb4'数据源管理', 'MENU', NULL, '/datasources', 'Database', 3),
    ('perm-datasource:view', 'datasource:view', _utf8mb4'数据源查看', 'BUTTON', 'perm-datasource', NULL, NULL, 0),
    ('perm-datasource:create', 'datasource:create', _utf8mb4'数据源创建', 'BUTTON', 'perm-datasource', NULL, NULL, 1),
    ('perm-audit', 'audit', _utf8mb4'审计日志', 'MENU', NULL, '/audit-logs', 'Document', 4),
    ('perm-audit:view', 'audit:view', _utf8mb4'审计查看', 'BUTTON', 'perm-audit', NULL, NULL, 0)
ON DUPLICATE KEY UPDATE permission_name = permission_name;

-- 给管理员角色分配所有权限
INSERT INTO msp_role_permissions (role_id, permission_id, create_time)
SELECT 'role-admin', permission_id, NOW() FROM msp_permissions p
ON DUPLICATE KEY UPDATE create_time = NOW();

-- 创建索引
CREATE INDEX idx_nodes_status ON msp_nodes(status);
CREATE INDEX idx_tasks_status ON msp_tasks(status);
CREATE INDEX idx_tasks_type ON msp_tasks(type);
CREATE INDEX idx_audit_logs_user ON msp_audit_logs(user_id);
CREATE INDEX idx_audit_logs_time ON msp_audit_logs(create_time);
CREATE INDEX idx_users_username ON msp_users(username);
CREATE INDEX idx_roles_role_code ON msp_roles(role_code);
CREATE INDEX idx_permissions_code ON msp_permissions(permission_code);
CREATE INDEX idx_permissions_parent ON msp_permissions(parent_id);

-- 初始化三个医疗机构节点 (使用 _utf8mb4 前缀强制 UTF-8, 防止双重编码)
INSERT INTO msp_nodes (node_id, node_name, status, node_mode, endpoint, external_endpoint, capabilities, tags) VALUES
('node-hospital', _utf8mb4'医院', 'ONLINE', 'RAY', 'node-a:50051', 'localhost:50051', 'PSI,FEDERATED_LEARNING,MPC', _utf8mb4'医院'),
('node-research', _utf8mb4'医疗研究所', 'ONLINE', 'RAY', 'node-b:50051', 'localhost:50052', 'PSI,FEDERATED_LEARNING,MPC', _utf8mb4'医疗研究所'),
('node-insurance', _utf8mb4'保险公司', 'ONLINE', 'RAY', 'node-c:50051', 'localhost:50053', 'PSI,FEDERATED_LEARNING,MPC', _utf8mb4'保险公司')
ON DUPLICATE KEY UPDATE status=VALUES(status), node_mode=VALUES(node_mode);

-- 初始化三个医疗机构数据源（数据源=数据库，包含多张表）
-- host/database_name/username/password 须与 node-*-db 容器实际配置一致
INSERT INTO msp_datasources (datasource_id, node_id, name, type, host, port, database_name, username, password, table_name, columns) VALUES
-- 医院数据库（node-a-db, 实际库: node_a_data, 凭据: root/nodea123）
('ds-hosp', 'node-hospital', _utf8mb4'医院数据库', 'MYSQL', 'node-a-db', 3306, 'node_a_data', 'root', 'nodea123', NULL, NULL),
-- 医疗研究所数据库（node-b-db, 实际库: node_b_data, 凭据: root/nodeb123）
('ds-research', 'node-research', _utf8mb4'医疗研究所数据库', 'MYSQL', 'node-b-db', 3306, 'node_b_data', 'root', 'nodeb123', NULL, NULL),
-- 保险公司数据库（node-c-db, 实际库: node_c_data, 凭据: root/nodec123）
('ds-insurance', 'node-insurance', _utf8mb4'保险公司数据库', 'MYSQL', 'node-c-db', 3306, 'node_c_data', 'root', 'nodec123', NULL, NULL)
ON DUPLICATE KEY UPDATE
    name=VALUES(name),
    host=VALUES(host),
    port=VALUES(port),
    database_name=VALUES(database_name),
    username=VALUES(username),
    password=VALUES(password);
