-- 保险公司数据源默认表与示例数据 (node_c_data)
-- 对应数据源 ds-insurance，业务方：保险公司
-- user_id 命名空间：P008-P019（与医院 P008-P015 交集 8 人，与研究所 P008-P012 交集 5 人）
-- default_flag = 1 的 claim 行约 30%，作为 FL 标签（label_party = 保险）

USE node_c_data;

SET NAMES utf8mb4;

-- 投保人
CREATE TABLE IF NOT EXISTS policy_holders (
    user_id        VARCHAR(16)  PRIMARY KEY                              COMMENT '投保人 ID（P008-P019）',
    name           VARCHAR(64)  NOT NULL                                 COMMENT '姓名',
    gender         ENUM('M','F') NOT NULL                                COMMENT '性别',
    age            INT          NOT NULL                                 COMMENT '年龄',
    phone          VARCHAR(20)                                            COMMENT '手机号',
    id_card_hash   CHAR(64)     NOT NULL                                 COMMENT '身份证 SHA-256',
    occupation     VARCHAR(64)                                            COMMENT '职业',
    health_status  VARCHAR(16)  COMMENT '健康/亚健康/慢性病/既往病史' COMMENT '健康状况'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='投保人';

-- 保单
CREATE TABLE IF NOT EXISTS policies (
    policy_id       VARCHAR(16)   PRIMARY KEY                            COMMENT '保单 ID（POL001-POL015）',
    user_id         VARCHAR(16)   NOT NULL                                COMMENT '投保人 ID（FK -> policy_holders.user_id）',
    product_type    VARCHAR(32)   COMMENT '重疾险/医疗险/寿险/意外险/养老险' COMMENT '产品类型',
    premium         DECIMAL(10,2)                                         COMMENT '年保费（元）',
    coverage_amount DECIMAL(12,2)                                         COMMENT '保额（元）',
    start_date      DATE                                                  COMMENT '生效日期',
    end_date        DATE                                                  COMMENT '到期日期',
    status          VARCHAR(16)   DEFAULT 'ACTIVE'                        COMMENT '保单状态（ACTIVE/EXPIRED/LAPSED）',
    KEY idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='保单';

-- 理赔记录（default_flag 是 FL 训练标签；claim_amount 是三方 MPC 数值列）
CREATE TABLE IF NOT EXISTS claims (
    claim_id        VARCHAR(16)   PRIMARY KEY                            COMMENT '理赔 ID（CLM001-CLM015）',
    policy_id       VARCHAR(16)   NOT NULL                                COMMENT '保单 ID（FK -> policies.policy_id）',
    user_id         VARCHAR(16)   NOT NULL                                COMMENT '投保人 ID（FK -> policy_holders.user_id）',
    claim_type      VARCHAR(32)   COMMENT '住院/门诊/手术/重大疾病/意外伤害' COMMENT '理赔类型',
    claim_amount    DECIMAL(10,2) COMMENT '三方 MPC 联合统计数值列'   COMMENT '三方 MPC 联合统计数值列',
    claim_date      DATE                                                  COMMENT '申请日期',
    diagnosis_code  VARCHAR(16)                                          COMMENT '诊断编码（ICD-10）',
    default_flag    TINYINT       COMMENT '0/1，FL 标签：是否违约/欺诈' COMMENT '0/1，FL 标签：是否违约/欺诈',
    status          VARCHAR(16)   DEFAULT 'PENDING'                        COMMENT '理赔状态（PENDING/APPROVED/REJECTED/SETTLED）',
    settlement_date DATE                                                  COMMENT '结算日期',
    KEY idx_user (user_id),
    KEY idx_policy (policy_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='理赔记录（default_flag 是 FL 训练标签；claim_amount 是三方 MPC 数值列）';

-- 投保人 (12 人, P008-P019)
INSERT INTO policy_holders (user_id, name, gender, age, phone, id_card_hash, occupation, health_status) VALUES
('P008', '黄磊', 'M', 55, '13800138008', '33e97b0a6eb215e12eaae8126ca2e935f41fe3b6aa2a174c3e4f01a24a5e311b', '工程师',     '慢性病'),
('P009', '周婷', 'F', 35, '13800138009', 'bfb71ce9e5124c3454d0d79ac5a245f7ee636b29b7d4314fd0cdc2f3ffe6c577', '教师',       '健康'),
('P010', '吴昊', 'M', 48, '13800138010', 'a37e18653ff48412df6b96339c706772838f893bd7f0a4ccee5fc4b07bd2377f', '销售经理',   '慢性病'),
('P011', '徐丽', 'F', 50, '13800138011', '7f0e654b365f5c0c0b28f051928015bce7552f42ecfc34e45359f583cde7c6bd', '公务员',     '健康'),
('P012', '孙鹏', 'M', 42, '13800138012', '017466e8ee8656230eeadbb65b9cda2d73f7f1f3c9be8522888541184983b333', '自由职业',   '慢性病'),
('P013', '马超', 'M', 33, '13800138013', '52078147bc607363374114aeff4ad35c4cc5eebda11f382264021a6a9cf6c7fe', '设计师',     '健康'),
('P014', '朱琳', 'F', 58, '13800138014', '77781def8000df62dbba7a638e60eecdf32717477d6ff45d0a14b7ce242ee861', '医生',       '亚健康'),
('P015', '胡明', 'M', 47, '13800138015', '2fe9693aa9b7c9728b0391a1b744714d251ed5a144970d5dad4078aba2bfa566', '会计',       '健康'),
('P016', '冯刚', 'M', 39, '13800138016', 'c52c34e8b43b31dd7e4e51fbe19f8d62fd03192541cbd289f2ecd4ec69e0f9db', '律师',       '健康'),
('P017', '沈玉', 'F', 29, '13800138017', '4bd68be8b882dc0e818a660624550323e7b5b161d16d4d285d8b44cf650411bb', '研究员',     '健康'),
('P018', '韩雪', 'F', 51, '13800138018', 'a25f4fbf1169e33e46fe0b1fdd852075e2408cd79eb46e3f642201bd0b3737d8', '顾问',       '既往病史'),
('P019', '杨帆', 'M', 36, '13800138019', '3a1252cf5b5c10bae639d968dba87d90a9f191c0f024d2e1814941f11b7e97c0', '公务员',     '健康');

-- 保单 (15 张)
INSERT INTO policies (policy_id, user_id, product_type, premium, coverage_amount, start_date, end_date, status) VALUES
('POL001', 'P008', '重疾险', 12500.00,  500000.00, '2022-03-01', '2032-03-01', 'ACTIVE'),
('POL002', 'P008', '医疗险',  3500.00,  300000.00, '2022-03-01', '2025-03-01', 'ACTIVE'),
('POL003', 'P009', '寿险',    6800.00,  800000.00, '2023-05-15', '2043-05-15', 'ACTIVE'),
('POL004', 'P010', '重疾险', 10800.00,  450000.00, '2021-09-01', '2031-09-01', 'ACTIVE'),
('POL005', 'P010', '医疗险',  4200.00,  500000.00, '2021-09-01', '2025-09-01', 'ACTIVE'),
('POL006', 'P011', '养老险', 15000.00, 1000000.00, '2020-06-01', '2050-06-01', 'ACTIVE'),
('POL007', 'P012', '重疾险',  9800.00,  400000.00, '2022-11-20', '2032-11-20', 'ACTIVE'),
('POL008', 'P013', '意外险',   880.00,  200000.00, '2024-01-10', '2025-01-10', 'ACTIVE'),
('POL009', 'P014', '医疗险',  4800.00,  600000.00, '2023-08-05', '2026-08-05', 'ACTIVE'),
('POL010', 'P015', '寿险',    7600.00,  900000.00, '2022-04-18', '2042-04-18', 'ACTIVE'),
('POL011', 'P016', '重疾险',  8500.00,  350000.00, '2024-02-12', '2034-02-12', 'ACTIVE'),
('POL012', 'P017', '医疗险',  2800.00,  300000.00, '2024-06-01', '2027-06-01', 'ACTIVE'),
('POL013', 'P018', '重疾险', 11200.00,  480000.00, '2021-12-01', '2031-12-01', 'ACTIVE'),
('POL014', 'P018', '寿险',    9200.00,  700000.00, '2021-12-01', '2041-12-01', 'ACTIVE'),
('POL015', 'P019', '意外险',   650.00,  150000.00, '2024-09-15', '2025-09-15', 'ACTIVE');

-- 理赔记录 (15 行, default_flag=1 占 5 行 ~33%)
INSERT INTO claims (claim_id, policy_id, user_id, claim_type, claim_amount, claim_date, diagnosis_code, default_flag, status, settlement_date) VALUES
('CLM001', 'POL002', 'P008', '住院',     28500.00, '2024-04-10', 'I25.10', 1, 'SETTLED',    '2024-04-28'),
('CLM002', 'POL005', 'P010', '住院',     35200.00, '2024-05-22', 'I10',    1, 'SETTLED',    '2024-06-15'),
('CLM003', 'POL007', 'P012', '重大疾病', 120000.00,'2024-07-08', 'E11.9',  1, 'SETTLED',    '2024-08-02'),
('CLM004', 'POL008', 'P013', '意外伤害',  8500.00, '2024-08-15', 'S52.5',  0, 'SETTLED',    '2024-08-30'),
('CLM005', 'POL009', 'P014', '门诊',      3200.00, '2024-06-20', 'E78.5',  0, 'SETTLED',    '2024-07-05'),
('CLM006', 'POL010', 'P015', '住院',     42000.00, '2024-09-12', 'K35.8',  1, 'SETTLED',    '2024-10-10'),
('CLM007', 'POL011', 'P016', '门诊',      1800.00, '2024-10-05', 'J06.9',  0, 'SETTLED',    '2024-10-15'),
('CLM008', 'POL013', 'P018', '住院',     56800.00, '2024-03-18', 'C50.9',  1, 'SETTLED',    '2024-04-20'),
('CLM009', 'POL001', 'P008', '门诊',      2400.00, '2024-11-08', 'I10',    0, 'PENDING',    NULL),
('CLM010', 'POL003', 'P009', '手术',     18000.00, '2024-10-22', 'K80.0',  0, 'SETTLED',    '2024-11-12'),
('CLM011', 'POL004', 'P010', '门诊',      1500.00, '2024-12-03', 'I10',    0, 'PENDING',    NULL),
('CLM012', 'POL006', 'P011', '住院',     22000.00, '2024-08-28', 'M17.9',  0, 'SETTLED',    '2024-09-18'),
('CLM013', 'POL012', 'P017', '门诊',       980.00, '2024-11-30', 'J18.9',  0, 'PENDING',    NULL),
('CLM014', 'POL014', 'P018', '门诊',      2100.00, '2024-12-10', 'E78.5',  0, 'PENDING',    NULL),
('CLM015', 'POL015', 'P019', '意外伤害',  4500.00, '2024-11-05', 'S93.4',  0, 'SETTLED',    '2024-11-20');
