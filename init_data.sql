-- Online Judge System 数据库初始化脚本
CREATE DATABASE IF NOT EXISTS `oj` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `oj`;

-- 1. 用户表 (user)
CREATE TABLE IF NOT EXISTS `user` (
    `id` BIGINT(20) NOT NULL AUTO_INCREMENT,
    `createtime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `username` VARCHAR(30) NOT NULL UNIQUE,
    `password` VARCHAR(60) NOT NULL,
    `name` VARCHAR(250) NOT NULL DEFAULT '',
    `email` VARCHAR(250) NOT NULL DEFAULT '',
    `intro` VARCHAR(250) NOT NULL DEFAULT '',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. 用户配置表 (user_profile)
CREATE TABLE IF NOT EXISTS `user_profile` (
    `id` BIGINT(20) NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT(20) NOT NULL,
    `score` INT(11) NOT NULL DEFAULT 0,
    `accepted` INT(11) NOT NULL DEFAULT 0,
    `submitted` INT(11) NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_id` (`user_id`),
    CONSTRAINT `fk_user_profile_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. 题目表 (problem)
CREATE TABLE IF NOT EXISTS `problem` (
    `id` BIGINT(20) NOT NULL AUTO_INCREMENT,
    `title` VARCHAR(50) NOT NULL UNIQUE,
    `description` LONGTEXT NOT NULL,
    `input` LONGTEXT NOT NULL,
    `output` LONGTEXT NOT NULL,
    `sample_input` LONGTEXT NOT NULL,
    `sample_output` LONGTEXT NOT NULL,
    `hint` LONGTEXT NOT NULL,
    `source` LONGTEXT NOT NULL,
    `time_limit` INT(11) NOT NULL DEFAULT 1000,
    `memory_limit` INT(11) NOT NULL DEFAULT 65536,
    `status` VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    `score` INT(11) NOT NULL DEFAULT 0,
    `submitted` INT(11) NOT NULL DEFAULT 0,
    `accepted` INT(11) NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. 标签表 (tag)
CREATE TABLE IF NOT EXISTS `tag` (
    `id` BIGINT(20) NOT NULL AUTO_INCREMENT,
    `name` VARCHAR(200) NOT NULL UNIQUE,
    `score` BIGINT(20) NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5. 题目-标签关联表 (problem_tags)
CREATE TABLE IF NOT EXISTS `problem_tags` (
    `problem_id` BIGINT(20) NOT NULL,
    `tags_id` BIGINT(20) NOT NULL,
    PRIMARY KEY (`problem_id`, `tags_id`),
    CONSTRAINT `fk_problem_tags_problem` FOREIGN KEY (`problem_id`) REFERENCES `problem` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT `fk_problem_tags_tag` FOREIGN KEY (`tags_id`) REFERENCES `tag` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 6. 比赛表 (contest)
CREATE TABLE IF NOT EXISTS `contest` (
    `id` BIGINT(20) NOT NULL AUTO_INCREMENT,
    `title` VARCHAR(255) NOT NULL,
    `description` TEXT NOT NULL,
    `privilege` VARCHAR(20) NOT NULL DEFAULT 'public',
    `password` VARCHAR(200) NOT NULL DEFAULT '',
    `start_time` DATETIME(6) NOT NULL,
    `end_time` DATETIME(6) NOT NULL,
    `creator_id` BIGINT(20) NOT NULL,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `pattern` VARCHAR(20) NOT NULL DEFAULT 'acm',
    `freeze_rank` BIT(1) NOT NULL DEFAULT 1,
    `team_id` BIGINT(20) DEFAULT NULL,
    `status` VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    PRIMARY KEY (`id`),
    CONSTRAINT `fk_contest_creator` FOREIGN KEY (`creator_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 7. 提交表 (solution)
CREATE TABLE IF NOT EXISTS `solution` (
    `id` BIGINT(20) NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT(20) NOT NULL,
    `problem_id` BIGINT(20) NOT NULL,
    `language` VARCHAR(40) NOT NULL DEFAULT 'c',
    `source` LONGTEXT NOT NULL,
    `submit_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `ip` VARCHAR(255) NOT NULL,
    `time` INT(11) NOT NULL DEFAULT -1,
    `memory` INT(11) NOT NULL DEFAULT -1,
    `length` INT(11) NOT NULL DEFAULT 0,
    `result` VARCHAR(50) NOT NULL DEFAULT 'Wrong Answer',
    `share` BIT(1) NOT NULL DEFAULT 0,
    `info` TEXT NOT NULL,
    `case_number` INT(11) NOT NULL DEFAULT 0,
    `contest_id` BIGINT(20) DEFAULT NULL,
    PRIMARY KEY (`id`),
    CONSTRAINT `fk_solution_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT `fk_solution_problem` FOREIGN KEY (`problem_id`) REFERENCES `problem` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT `fk_solution_contest` FOREIGN KEY (`contest_id`) REFERENCES `contest` (`id`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 8. 比赛题目表 (contest_problem)
CREATE TABLE IF NOT EXISTS `contest_problem` (
    `id` BIGINT(20) NOT NULL AUTO_INCREMENT,
    `problem_id` BIGINT(20) NOT NULL,
    `contest_id` BIGINT(20) NOT NULL,
    `temp_id` BIGINT(20) NOT NULL,
    `temp_title` VARCHAR(255) NOT NULL,
    `submitted` INT(11) NOT NULL DEFAULT 0,
    `accepted` INT(11) NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    CONSTRAINT `fk_contest_problem_problem` FOREIGN KEY (`problem_id`) REFERENCES `problem` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT `fk_contest_problem_contest` FOREIGN KEY (`contest_id`) REFERENCES `contest` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 9. 评论表 (comment)
CREATE TABLE IF NOT EXISTS `comment` (
    `id` BIGINT(20) NOT NULL AUTO_INCREMENT,
    `father_id` BIGINT(20) DEFAULT NULL,
    `post_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `user_id` BIGINT(20) NOT NULL,
    `text` LONGTEXT NOT NULL,
    PRIMARY KEY (`id`),
    CONSTRAINT `fk_comment_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT `fk_comment_father` FOREIGN KEY (`father_id`) REFERENCES `comment` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 10. 文章表 (article)
CREATE TABLE IF NOT EXISTS `article` (
    `id` BIGINT(20) NOT NULL AUTO_INCREMENT,
    `title` VARCHAR(255) NOT NULL,
    `text` TEXT NOT NULL,
    `post_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `user_id` BIGINT(20) NOT NULL,
    PRIMARY KEY (`id`),
    CONSTRAINT `fk_article_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 11. 文章评论表 (article_comment)
CREATE TABLE IF NOT EXISTS `article_comment` (
    `id` BIGINT(20) NOT NULL,
    `article_id` BIGINT(20) NOT NULL,
    PRIMARY KEY (`id`),
    CONSTRAINT `fk_article_comment_comment` FOREIGN KEY (`id`) REFERENCES `comment` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT `fk_article_comment_article` FOREIGN KEY (`article_id`) REFERENCES `article` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 12. 题解表 (analysis)
CREATE TABLE IF NOT EXISTS `analysis` (
    `id` BIGINT(20) NOT NULL AUTO_INCREMENT,
    `text` TEXT NOT NULL,
    `user_id` BIGINT(20) NOT NULL,
    `problem_id` BIGINT(20) NOT NULL,
    `post_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    CONSTRAINT `fk_analysis_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT `fk_analysis_problem` FOREIGN KEY (`problem_id`) REFERENCES `problem` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 13. 题解评论表 (analysis_comment)
CREATE TABLE IF NOT EXISTS `analysis_comment` (
    `id` BIGINT(20) NOT NULL,
    `analysis_id` BIGINT(20) NOT NULL,
    PRIMARY KEY (`id`),
    CONSTRAINT `fk_analysis_comment_comment` FOREIGN KEY (`id`) REFERENCES `comment` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT `fk_analysis_comment_analysis` FOREIGN KEY (`analysis_id`) REFERENCES `analysis` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 14. 比赛评论表 (contest_comment)
CREATE TABLE IF NOT EXISTS `contest_comment` (
    `id` BIGINT(20) NOT NULL,
    `contest_id` BIGINT(20) NOT NULL,
    PRIMARY KEY (`id`),
    CONSTRAINT `fk_contest_comment_comment` FOREIGN KEY (`id`) REFERENCES `comment` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT `fk_contest_comment_contest` FOREIGN KEY (`contest_id`) REFERENCES `contest` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 15. 队伍表 (team)
CREATE TABLE IF NOT EXISTS `team` (
    `id` BIGINT(20) NOT NULL AUTO_INCREMENT,
    `name` VARCHAR(100) NOT NULL UNIQUE DEFAULT '',
    `description` TEXT NOT NULL,
    `creator_id` BIGINT(20) NOT NULL,
    `attend` VARCHAR(50) NOT NULL DEFAULT 'public',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    CONSTRAINT `fk_team_creator` FOREIGN KEY (`creator_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 16. 队员表 (teammate)
CREATE TABLE IF NOT EXISTS `teammate` (
    `id` BIGINT(20) NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT(20) NOT NULL,
    `team_id` BIGINT(20) NOT NULL,
    `level` INT(11) NOT NULL DEFAULT 2,
    PRIMARY KEY (`id`),
    CONSTRAINT `fk_teammate_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT `fk_teammate_team` FOREIGN KEY (`team_id`) REFERENCES `team` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 17. 队伍申请表 (team_apply)
CREATE TABLE IF NOT EXISTS `team_apply` (
    `id` BIGINT(20) NOT NULL AUTO_INCREMENT,
    `team_id` BIGINT(20) NOT NULL,
    `user_id` BIGINT(20) NOT NULL,
    `time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `active` BIT(1) NOT NULL DEFAULT 1,
    `result` VARCHAR(20) NOT NULL DEFAULT 'rejected',
    PRIMARY KEY (`id`),
    CONSTRAINT `fk_team_apply_team` FOREIGN KEY (`team_id`) REFERENCES `team` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT `fk_team_apply_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 18. 用户题目关系表 (user_problem)
CREATE TABLE IF NOT EXISTS `user_problem` (
    `id` BIGINT(20) NOT NULL AUTO_INCREMENT,
    `problem_id` BIGINT(20) NOT NULL,
    `user_id` BIGINT(20) NOT NULL,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_problem_user` (`problem_id`, `user_id`),
    CONSTRAINT `fk_user_problem_problem` FOREIGN KEY (`problem_id`) REFERENCES `problem` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT `fk_user_problem_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 插入用户数据
INSERT INTO `user` VALUES 
(1, '2026-02-25 17:12:09', 'admin', '$2b$12$EaI3hX9dGF8e4hYFmUmhOONcfbk5tpREbGWz3QVL6JJPMCvvWk/5y', '管理员', 'user1@example.com', 'admin 的简介'),
(2, '2026-02-25 17:12:09', 'user_2', '$2b$12$EaI3hX9dGF8e4hYFmUmhOONcfbk5tpREbGWz3QVL6JJPMCvvWk/5y', '用户 2', 'user2@example.com', 'user_2 的简介'),
(3, '2026-02-25 17:12:09', 'user_3', '$2b$12$EaI3hX9dGF8e4hYFmUmhOONcfbk5tpREbGWz3QVL6JJPMCvvWk/5y', '用户 3', 'user3@example.com', 'user_3 的简介'),
(4, '2026-02-25 17:12:09', 'user_4', '$2b$12$EaI3hX9dGF8e4hYFmUmhOONcfbk5tpREbGWz3QVL6JJPMCvvWk/5y', '用户 4', 'user4@example.com', 'user_4 的简介'),
(5, '2026-02-25 17:12:09', 'user_5', '$2b$12$EaI3hX9dGF8e4hYFmUmhOONcfbk5tpREbGWz3QVL6JJPMCvvWk/5y', '用户 5', 'user5@example.com', 'user_5 的简介'),
(6, '2026-02-25 17:12:09', 'user_6', '$2b$12$EaI3hX9dGF8e4hYFmUmhOONcfbk5tpREbGWz3QVL6JJPMCvvWk/5y', '用户 6', 'user6@example.com', 'user_6 的简介'),
(7, '2026-02-25 17:12:09', 'user_7', '$2b$12$EaI3hX9dGF8e4hYFmUmhOONcfbk5tpREbGWz3QVL6JJPMCvvWk/5y', '用户 7', 'user7@example.com', 'user_7 的简介'),
(8, '2026-02-25 17:12:09', 'user_8', '$2b$12$EaI3hX9dGF8e4hYFmUmhOONcfbk5tpREbGWz3QVL6JJPMCvvWk/5y', '用户 8', 'user8@example.com', 'user_8 的简介'),
(9, '2026-02-25 17:12:09', 'user_9', '$2b$12$EaI3hX9dGF8e4hYFmUmhOONcfbk5tpREbGWz3QVL6JJPMCvvWk/5y', '用户 9', 'user9@example.com', 'user_9 的简介'),
(10, '2026-02-25 17:12:09', 'user_10', '$2b$12$EaI3hX9dGF8e4hYFmUmhOONcfbk5tpREbGWz3QVL6JJPMCvvWk/5y', '用户 10', 'user10@example.com', 'user_10 的简介'),
(11, '2026-02-25 17:12:09', 'user_11', '$2b$12$EaI3hX9dGF8e4hYFmUmhOONcfbk5tpREbGWz3QVL6JJPMCvvWk/5y', '用户 11', 'user11@example.com', 'user_11 的简介');

-- 插入用户配置数据
INSERT INTO `user_profile` VALUES 
(1, 1, 0, 0, 0),
(2, 2, 0, 0, 0),
(3, 3, 0, 0, 0),
(4, 4, 0, 0, 0),
(5, 5, 0, 0, 0),
(6, 6, 0, 0, 0),
(7, 7, 0, 0, 0),
(8, 8, 0, 0, 0),
(9, 9, 0, 0, 0),
(10, 10, 0, 0, 0),
(11, 11, 0, 0, 0);

-- 插入题目数据
INSERT INTO `problem` VALUES 
(1, 'A + B 问题', '计算两个整数的和。', '输入包含两个整数 a 和 b。', '输出 a 和 b 的和。', '1 2', '3', '提示', '系统题目', 1000, 65536, 'APPROVED', 100, 0, 0),
(2, '数组求和', '计算包含 N 个整数的数组的总和。', '第一行包含一个整数 N。第二行包含 N 个整数。', '输出总和。', '3\n1 2 3', '6', '提示', '系统题目', 1000, 65536, 'APPROVED', 100, 0, 0),
(3, '字符串反转', '反转给定的字符串。', '输入包含一个字符串。', '输出反转后的字符串。', 'hello', 'olleh', '提示', '系统题目', 1000, 65536, 'APPROVED', 100, 0, 0),
(4, '寻找最大值', '在给定的 N 个整数中找出最大的那个。', '第一行 N。第二行 N 个整数。', '输出最大的整数。', '3\n1 5 2', '5', '提示', '系统题目', 1000, 65536, 'APPROVED', 100, 0, 0),
(5, '数组排序', '将包含 N 个整数的数组按升序排列。', '第一行 N。第二行 N 个整数。', '按升序输出排序后的整数，用空格分隔。', '3\n3 1 2', '1 2 3', '提示', '系统题目', 1000, 65536, 'APPROVED', 100, 0, 0),
(6, '回文判断', '判断给定的字符串是否是回文串（正读和反读一样）。', '输入一个字符串。', '如果是回文串输出 \'Yes\'，否则输出 \'No\'。', 'aba', 'Yes', '提示', '系统题目', 1000, 65536, 'APPROVED', 100, 0, 0),
(7, '阶乘计算', '计算非负整数 N 的阶乘 (N!)。', '输入一个整数 N (0 <= N <= 15)。', '输出 N! 的值。', '3', '6', '提示', '系统题目', 1000, 65536, 'APPROVED', 100, 0, 0),
(8, '素数判定', '判断一个整数是否为素数（质数）。', '输入一个整数 N。', '如果是素数输出 \'Yes\'，否则输出 \'No\'。', '7', 'Yes', '提示', '系统题目', 1000, 65536, 'APPROVED', 100, 0, 0),
(9, '矩阵加法', '计算两个 R 行 C 列矩阵的和。', '第一行包含 R 和 C。接下来的 R 行是矩阵 A，再接下来的 R 行是矩阵 B。', '输出结果矩阵。', '2 2\n1 1\n1 1\n2 2\n2 2', '3 3\n3 3', '提示', '系统题目', 1000, 65536, 'APPROVED', 100, 0, 0),
(10, '斐波那契数列', '计算第 N 个斐波那契数 (F0=0, F1=1)。', '输入一个整数 N。', '输出第 N 个斐波那契数。', '5', '5', '提示', '系统题目', 1000, 65536, 'APPROVED', 100, 0, 0),
(11, '有效的括号', '判断输入的括号字符串是否有效（闭合正确）。', '包含 \'()[]{}\' 的字符串。', '如果是有效的输出 \'True\'，否则输出 \'False\'。', '()[]{}', 'True', '提示', '系统题目', 1000, 65536, 'APPROVED', 100, 0, 0);

-- 插入标签数据
INSERT INTO `tag` VALUES 
(1, '标签 1', 0),
(2, '标签 2', 0),
(3, '标签 3', 0),
(4, '标签 4', 0),
(5, '标签 5', 0),
(6, '标签 6', 0),
(7, '标签 7', 0),
(8, '标签 8', 0),
(9, '标签 9', 0),
(10, '标签 10', 0),
(11, '标签 11', 0);

-- 插入题目标签关联数据
INSERT INTO `problem_tags` VALUES 
(1, 1),
(2, 2),
(3, 3),
(4, 4),
(5, 5),
(6, 6),
(7, 7),
(8, 8),
(9, 9),
(10, 10),
(11, 11);

-- 插入比赛数据
INSERT INTO `contest` VALUES 
(1, '竞赛 1', '描述', 'public', '', '2026-02-25 17:12:09', '2026-02-25 17:12:09', 1, '2026-02-25 17:12:09', 'acm', 1, NULL, 'APPROVED'),
(2, '竞赛 2', '描述', 'public', '', '2026-02-25 17:12:09', '2026-02-25 17:12:09', 1, '2026-02-25 17:12:09', 'acm', 1, NULL, 'APPROVED'),
(3, '竞赛 3', '描述', 'public', '', '2026-02-25 17:12:09', '2026-02-25 17:12:09', 1, '2026-02-25 17:12:09', 'acm', 1, NULL, 'APPROVED'),
(4, '竞赛 4', '描述', 'public', '', '2026-02-25 17:12:09', '2026-02-25 17:12:09', 1, '2026-02-25 17:12:09', 'acm', 1, NULL, 'APPROVED'),
(5, '竞赛 5', '描述', 'public', '', '2026-02-25 17:12:09', '2026-02-25 17:12:09', 1, '2026-02-25 17:12:09', 'acm', 1, NULL, 'APPROVED'),
(6, '竞赛 6', '描述', 'public', '', '2026-02-25 17:12:09', '2026-02-25 17:12:09', 1, '2026-02-25 17:12:09', 'acm', 1, NULL, 'APPROVED'),
(7, '竞赛 7', '描述', 'public', '', '2026-02-25 17:12:09', '2026-02-25 17:12:09', 1, '2026-02-25 17:12:09', 'acm', 1, NULL, 'APPROVED'),
(8, '竞赛 8', '描述', 'public', '', '2026-02-25 17:12:09', '2026-02-25 17:12:09', 1, '2026-02-25 17:12:09', 'acm', 1, NULL, 'APPROVED'),
(9, '竞赛 9', '描述', 'public', '', '2026-02-25 17:12:09', '2026-02-25 17:12:09', 1, '2026-02-25 17:12:09', 'acm', 1, NULL, 'APPROVED'),
(10, '竞赛 10', '描述', 'public', '', '2026-02-25 17:12:09', '2026-02-25 17:12:09', 1, '2026-02-25 17:12:09', 'acm', 1, NULL, 'APPROVED'),
(11, '竞赛 11', '描述', 'public', '', '2026-02-25 17:12:09', '2026-02-25 17:12:09', 1, '2026-02-25 17:12:09', 'acm', 1, NULL, 'APPROVED');

-- 插入比赛题目关联数据
INSERT INTO `contest_problem` VALUES 
(1, 1, 1, 1, '题目1', 0, 0),
(2, 2, 2, 2, '题目2', 0, 0),
(3, 3, 3, 3, '题目3', 0, 0),
(4, 4, 4, 4, '题目4', 0, 0),
(5, 5, 5, 5, '题目5', 0, 0),
(6, 6, 6, 6, '题目6', 0, 0),
(7, 7, 7, 7, '题目7', 0, 0),
(8, 8, 8, 8, '题目8', 0, 0),
(9, 9, 9, 9, '题目9', 0, 0),
(10, 10, 10, 10, '题目10', 0, 0),
(11, 11, 11, 11, '题目11', 0, 0);

-- 插入论坛文章数据
INSERT INTO `article` VALUES 
(1, '欢迎使用Online Judge系统', '这是系统的第一篇论坛文章，欢迎大家使用本系统进行编程练习和比赛。\n\n系统特点：\n- 支持多种编程语言\n- 实时判题反馈\n- 丰富的题目资源\n- 公平的比赛环境\n\n祝大家在编程的道路上不断进步！', '2026-02-25 17:12:09', 1),
(2, '如何高效学习算法', '学习算法是编程的重要组成部分，以下是一些学习建议：\n\n1. 理解基本概念\n2. 多做练习题\n3. 分析优秀代码\n4. 参加比赛锻炼\n\n坚持下去，你会发现算法的魅力！', '2026-02-25 17:12:09', 1),
(3, 'C++编程技巧分享', 'C++是一种强大的编程语言，以下是一些实用技巧：\n\n- 使用STL库提高开发效率\n- 注意内存管理\n- 掌握常见的算法模板\n- 养成良好的编码风格\n\n希望这些技巧对大家有所帮助！', '2026-02-25 17:12:09', 2);

-- 插入评论数据
INSERT INTO `comment` VALUES 
(1, NULL, '2026-02-25 17:12:09', 2, '感谢分享，系统很棒！'),
(2, 1, '2026-02-25 17:12:09', 3, '赞同，使用体验很好。'),
(3, NULL, '2026-02-25 17:12:09', 3, '学习算法确实需要持之以恒。'),
(4, NULL, '2026-02-25 17:12:09', 4, 'STL库真的很强大，推荐大家使用。');

-- 插入文章评论关联数据
INSERT INTO `article_comment` VALUES 
(1, 1),
(2, 1),
(3, 2),
(4, 3);