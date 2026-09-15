-- AI 智能助手 Demo 初始化脚本
-- 适用于 MySQL 8.x；脚本只创建本项目要求的 3 张表。

CREATE DATABASE IF NOT EXISTS ai_assistant_demo
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE ai_assistant_demo;

CREATE TABLE IF NOT EXISTS `user` (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
    username VARCHAR(50) NOT NULL COMMENT '用户名',
    password VARCHAR(100) NOT NULL COMMENT 'BCrypt密码哈希',
    created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_user_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

CREATE TABLE IF NOT EXISTS conversation (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '会话ID',
    user_id BIGINT NOT NULL COMMENT '所属用户ID',
    title VARCHAR(100) NOT NULL DEFAULT '新对话' COMMENT '会话标题',
    created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    KEY idx_conversation_user_updated (user_id, updated_time),
    CONSTRAINT fk_conversation_user
        FOREIGN KEY (user_id) REFERENCES `user` (id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='聊天会话表';

CREATE TABLE IF NOT EXISTS message (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '消息ID',
    conversation_id BIGINT NOT NULL COMMENT '所属会话ID',
    role ENUM('user', 'assistant') NOT NULL COMMENT '消息角色',
    content TEXT NOT NULL COMMENT '消息内容',
    created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    KEY idx_message_conversation_id (conversation_id, id),
    CONSTRAINT fk_message_conversation
        FOREIGN KEY (conversation_id) REFERENCES conversation (id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='聊天消息表';

-- 本地演示账号：test / 123456
-- 再次执行脚本时也会把旧的明文测试密码更新成 BCrypt 哈希。
INSERT INTO `user` (username, password)
VALUES ('test', '$2a$10$n0t0xvSJO1i0cOQSAAuMVeWL2Q8f3aRD50vpH22l.Mcyu15MkwAI2')
ON DUPLICATE KEY UPDATE password = VALUES(password);
