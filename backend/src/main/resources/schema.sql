-- 知联汇学 - MySQL 建表脚本
SET NAMES utf8mb4;
SET CHARACTER_SET_CLIENT = utf8mb4;
SET CHARACTER_SET_CONNECTION = utf8mb4;

CREATE DATABASE IF NOT EXISTS zhi_platform DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE zhi_platform;

-- 用户表
CREATE TABLE IF NOT EXISTS `sys_user` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `username` VARCHAR(50) NOT NULL UNIQUE COMMENT '账号',
    `password` VARCHAR(200) NOT NULL COMMENT '密码(BCrypt加密)',
    `role` VARCHAR(20) NOT NULL COMMENT '角色: TEACHER/STUDENT',
    `real_name` VARCHAR(50) COMMENT '姓名',
    `class_name` VARCHAR(50) COMMENT '班级',
    `school` VARCHAR(100) COMMENT '学校',
    `avatar` VARCHAR(200) COMMENT '头像URL',
    `status` TINYINT DEFAULT 1 COMMENT '状态: 1正常 0禁用',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 学科表
CREATE TABLE IF NOT EXISTS `subject` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `name` VARCHAR(50) NOT NULL COMMENT '学科名称',
    `type` VARCHAR(20) NOT NULL COMMENT 'SINGLE:单一学科 / CROSS:交叉专题',
    `parent_id` BIGINT COMMENT '父学科ID',
    `color` VARCHAR(20) COMMENT '节点配色',
    `description` VARCHAR(500) COMMENT '描述',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 习题表
CREATE TABLE IF NOT EXISTS `exercise` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `content` TEXT NOT NULL COMMENT '习题内容',
    `answer` TEXT COMMENT '答案',
    `analysis` TEXT COMMENT '解析',
    `difficulty` TINYINT DEFAULT 1 COMMENT '难度: 1-5',
    `subject_ids` VARCHAR(200) COMMENT '关联学科ID(JSON)',
    `knowledge_ids` VARCHAR(500) COMMENT '关联知识点ID(JSON)',
    `exam_outline` VARCHAR(200) COMMENT '考纲要求',
    `type` VARCHAR(30) DEFAULT 'CHOICE' COMMENT '题型: CHOICE/FILL/JUDGE/ESSAY',
    `create_by` BIGINT COMMENT '创建教师ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 学习记录表
CREATE TABLE IF NOT EXISTS `study_record` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `action_type` VARCHAR(30) NOT NULL COMMENT 'PRACTICE/FAVORITE/QA/VIEW',
    `target_id` BIGINT COMMENT '关联对象ID',
    `target_type` VARCHAR(30) COMMENT 'KNOWLEDGE/EXERCISE/GRAPH',
    `extra` TEXT COMMENT '扩展信息(JSON)',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_action (`user_id`, `action_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 笔记表
CREATE TABLE IF NOT EXISTS `note` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `knowledge_id` BIGINT COMMENT '关联知识点ID(Neo4j)',
    `knowledge_name` VARCHAR(100) COMMENT '知识点名称',
    `content` TEXT NOT NULL COMMENT '笔记内容',
    `category` VARCHAR(50) COMMENT '笔记分类(交叉专题)',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 操作日志表
CREATE TABLE IF NOT EXISTS `operation_log` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `username` VARCHAR(50),
    `action` VARCHAR(100) NOT NULL COMMENT '操作描述',
    `module` VARCHAR(50) COMMENT '模块',
    `ip` VARCHAR(50),
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_time (`user_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 图谱配置表
CREATE TABLE IF NOT EXISTS `graph_config` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `name` VARCHAR(100) NOT NULL COMMENT '配置名称',
    `layout` VARCHAR(30) DEFAULT 'force' COMMENT '布局: force/circular/radial/dagre',
    `subject_filter` VARCHAR(200) COMMENT '学科筛选(JSON)',
    `config_json` TEXT COMMENT '详细配置(JSON)',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 问句模板表
CREATE TABLE IF NOT EXISTS `qa_template` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `question` VARCHAR(500) NOT NULL COMMENT '问句模板',
    `category` VARCHAR(50) COMMENT '分类',
    `subject_ids` VARCHAR(200) COMMENT '关联学科ID(JSON)',
    `create_by` BIGINT COMMENT '创建者ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 初始学科数据
INSERT IGNORE INTO `subject` (`name`, `type`, `color`, `description`) VALUES
('高中物理', 'SINGLE', '#5B9BD5', '高中物理学科'),
('高中化学', 'SINGLE', '#ED7D31', '高中化学学科'),
('高中生物', 'SINGLE', '#70AD47', '高中生物学科'),
('航天拓展', 'SINGLE', '#8859A3', '航天科技拓展知识'),
('环境科普', 'SINGLE', '#39B5A8', '环境科学与水污染治理'),
('航天中的物理与化学', 'CROSS', '#4F46E5', '航天科技中物理与化学的交叉应用'),
('水污染治理中的生物与化学', 'CROSS', '#39B5A8', '水污染治理中生物与化学的交叉知识'),
('环境科学中的物理与生物', 'CROSS', '#5B9BD5', '环境科学中物理与生物的交叉关联');

-- 初始管理员账号 (密码: admin123, BCrypt加密)
INSERT IGNORE INTO `sys_user` (`username`, `password`, `role`, `real_name`, `status`) VALUES
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', 'TEACHER', '系统管理员', 1);

-- 初始问句模板
INSERT IGNORE INTO `qa_template` (`question`, `category`, `create_by`) VALUES
('航天燃料的燃烧涉及哪些化学原理和物理知识？', '航天交叉', 1),
('水污染治理中，生物降解与化学氧化的关联是什么？', '环境交叉', 1),
('酶催化在化学反应和生物体中的作用有什么异同？', '生化交叉', 1),
('火箭发射中的推力与哪些物理公式相关？', '航天物理', 1),
('污水处理中的物理过滤与生物净化如何协同工作？', '环境交叉', 1);

