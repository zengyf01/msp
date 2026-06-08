-- 医院数据源默认表与示例数据 (node_a_data)
-- 对应数据源 ds-hosp，业务方：医院
-- user_id 命名空间：P001-P015，与研究所/保险库 PSI 交集
--   医院 ∩ 研究所 = P003-P012 (10)
--   医院 ∩ 保险   = P008-P015 (8)
--   三方交集       = P008-P012 (5)

USE node_a_data;

SET NAMES utf8mb4;

-- 患者基本信息（PSI 主键 user_id + id_card_hash 双键）
CREATE TABLE IF NOT EXISTS patients (
    user_id        VARCHAR(16)  PRIMARY KEY                                COMMENT '患者 ID（P001-P015）',
    name           VARCHAR(64)  NOT NULL                                   COMMENT '姓名',
    gender         ENUM('M','F') NOT NULL                                  COMMENT '性别',
    age            INT          NOT NULL                                   COMMENT '年龄',
    phone          VARCHAR(20)                                              COMMENT '手机号',
    id_card_hash   CHAR(64)     NOT NULL                                   COMMENT '身份证 SHA-256',
    address        VARCHAR(256)                                             COMMENT '居住地址',
    create_time    TIMESTAMP    DEFAULT CURRENT_TIMESTAMP                  COMMENT '建档时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='患者基本信息（PSI 主键 user_id + id_card_hash 双键）';

-- 诊断记录
CREATE TABLE IF NOT EXISTS diagnoses (
    record_id      BIGINT       PRIMARY KEY AUTO_INCREMENT                 COMMENT '诊断记录 ID',
    user_id        VARCHAR(16)  NOT NULL                                  COMMENT '患者 ID（FK -> patients.user_id）',
    diagnosis_code VARCHAR(16)                                             COMMENT 'ICD-10 诊断编码',
    diagnosis_name VARCHAR(128)                                            COMMENT '诊断名称',
    visit_date     DATE                                                    COMMENT '就诊日期',
    department     VARCHAR(64)                                             COMMENT '就诊科室',
    doctor         VARCHAR(64)                                             COMMENT '主治医生',
    KEY idx_user_date (user_id, visit_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='诊断记录';

-- 化验结果（FL 数值特征来源）
CREATE TABLE IF NOT EXISTS lab_results (
    lab_id      BIGINT        PRIMARY KEY AUTO_INCREMENT                    COMMENT '化验记录 ID',
    user_id     VARCHAR(16)   NOT NULL                                     COMMENT '患者 ID（FK -> patients.user_id）',
    test_name   VARCHAR(64)                                                COMMENT '化验项名称（glucose / blood_pressure / cholesterol / hba1c / hemoglobin）',
    test_value  DECIMAL(10,2)                                             COMMENT '化验值',
    unit        VARCHAR(16)                                                COMMENT '单位（mg/dL / mmHg / % / g/dL）',
    test_date   DATE                                                       COMMENT '化验日期',
    KEY idx_user_test (user_id, test_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='化验结果（FL 数值特征来源）';

-- 患者汇总特征（FL 训练 / MPC 三方联合统计直接读取）
CREATE TABLE IF NOT EXISTS patient_stats (
    user_id        VARCHAR(16) PRIMARY KEY                                 COMMENT '患者 ID（FK -> patients.user_id）',
    visit_count    INT                                                     COMMENT '累计就诊次数',
    avg_lab_value  DECIMAL(10,2)                                          COMMENT '三方 MPC 联合统计数值列',
    chronic_flag   TINYINT                                                 COMMENT '0/1，是否有慢性病诊断（FL 特征）'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='患者汇总特征（FL 训练 / MPC 三方联合统计直接读取）';

-- 15 名患者 (P001-P015)
INSERT INTO patients (user_id, name, gender, age, phone, id_card_hash, address) VALUES
('P001', '张伟', 'M', 30, '13800138001', '85d0b00edf85dc453b947dc6048319226bf974c31bfbfcff9bc7eae7873dcee3', '北京市朝阳区'),
('P002', '王芳', 'F', 45, '13800138002', 'a4dc822e5158d20d7e629b4dac57745ff2c69d2a14de9c24a972709edb6d99a8', '上海市浦东新区'),
('P003', '李娜', 'F', 52, '13800138003', '4cb0cd6620d463f8dbd03416c92398d98ea17acb0cf3edafbe82f91889300ad4', '广州市天河区'),
('P004', '刘洋', 'M', 38, '13800138004', 'b4b33f0c3f0a98cec44b671d1d6f6ee668a8b75ff1de1fe72c1f69923edb4f9b', '深圳市福田区'),
('P005', '陈静', 'F', 60, '13800138005', 'c764c417b180ef65bfee1a7ace9a10c37feff52c54ab8aa815bd305d5401079d', '杭州市西湖区'),
('P006', '杨阳', 'M', 28, '13800138006', '6d9972b4a66d0268dda514766773d89a24784e671d4e345d9561f4a61bf9b030', '成都市锦江区'),
('P007', '赵敏', 'F', 41, '13800138007', '8b0964056a7a29261353729dbd335f02013b28a5d2b3037984bb47b4551fa3a5', '武汉市江汉区'),
('P008', '黄磊', 'M', 55, '13800138008', '33e97b0a6eb215e12eaae8126ca2e935f41fe3b6aa2a174c3e4f01a24a5e311b', '西安市雁塔区'),
('P009', '周婷', 'F', 35, '13800138009', 'bfb71ce9e5124c3454d0d79ac5a245f7ee636b29b7d4314fd0cdc2f3ffe6c577', '南京市鼓楼区'),
('P010', '吴昊', 'M', 48, '13800138010', 'a37e18653ff48412df6b96339c706772838f893bd7f0a4ccee5fc4b07bd2377f', '重庆市渝中区'),
('P011', '徐丽', 'F', 50, '13800138011', '7f0e654b365f5c0c0b28f051928015bce7552f42ecfc34e45359f583cde7c6bd', '天津市和平区'),
('P012', '孙鹏', 'M', 42, '13800138012', '017466e8ee8656230eeadbb65b9cda2d73f7f1f3c9be8522888541184983b333', '苏州市姑苏区'),
('P013', '马超', 'M', 33, '13800138013', '52078147bc607363374114aeff4ad35c4cc5eebda11f382264021a6a9cf6c7fe', '青岛市市南区'),
('P014', '朱琳', 'F', 58, '13800138014', '77781def8000df62dbba7a638e60eecdf32717477d6ff45d0a14b7ce242ee861', '长沙市岳麓区'),
('P015', '胡明', 'M', 47, '13800138015', '2fe9693aa9b7c9728b0391a1b744714d251ed5a144970d5dad4078aba2bfa566', '沈阳市和平区');

-- 诊断记录 (21 行)
INSERT INTO diagnoses (user_id, diagnosis_code, diagnosis_name, visit_date, department, doctor) VALUES
('P001', 'J18.9',  '肺炎',               '2024-03-15', '呼吸科',     '王医生'),
('P001', 'R51',    '头痛',               '2024-09-20', '神经内科',   '陈医生'),
('P002', 'K21.9',  '胃食管反流病',       '2024-05-08', '消化科',     '李医生'),
('P003', 'E11.9',  '2型糖尿病',          '2024-02-10', '内分泌科',   '刘医生'),
('P003', 'E78.5',  '高脂血症',           '2024-08-25', '内分泌科',   '刘医生'),
('P004', 'M54.5',  '腰痛',               '2024-06-12', '骨科',       '张医生'),
('P005', 'I10',    '原发性高血压',       '2024-01-18', '心内科',     '黄医生'),
('P005', 'I10',    '原发性高血压',       '2024-07-30', '心内科',     '黄医生'),
('P005', 'E11.9',  '2型糖尿病',          '2024-04-22', '内分泌科',   '刘医生'),
('P006', 'J45.9',  '哮喘',               '2024-10-05', '呼吸科',     '王医生'),
('P007', 'N39.0',  '泌尿道感染',         '2024-11-14', '内科',       '杨医生'),
('P008', 'I25.10', '冠状动脉粥样硬化性心脏病', '2024-02-28', '心内科', '黄医生'),
('P008', 'I10',    '原发性高血压',       '2024-06-15', '心内科',     '黄医生'),
('P009', 'F32.9',  '抑郁症',             '2024-08-08', '精神科',     '周医生'),
('P010', 'I10',    '原发性高血压',       '2024-03-22', '心内科',     '黄医生'),
('P010', 'E78.5',  '高脂血症',           '2024-09-10', '内分泌科',   '刘医生'),
('P011', 'M17.9',  '膝关节骨关节炎',     '2024-12-01', '骨科',       '张医生'),
('P012', 'E11.9',  '2型糖尿病',          '2024-04-15', '内分泌科',   '刘医生'),
('P012', 'I10',    '原发性高血压',       '2024-10-20', '心内科',     '黄医生'),
('P013', 'K29.7',  '胃炎',               '2024-07-18', '消化科',     '李医生'),
('P014', 'E78.5',  '高脂血症',           '2024-05-30', '内分泌科',   '刘医生'),
('P015', 'J18.9',  '肺炎',               '2024-11-25', '呼吸科',     '王医生');

-- 化验结果 (28 行)
INSERT INTO lab_results (user_id, test_name, test_value, unit, test_date) VALUES
('P001', 'glucose',         92.5,  'mg/dL',  '2024-03-15'),
('P001', 'hemoglobin',      14.2,  'g/dL',   '2024-03-15'),
('P002', 'glucose',         105.0, 'mg/dL',  '2024-05-08'),
('P002', 'cholesterol',     198.0, 'mg/dL',  '2024-05-08'),
('P003', 'glucose',         185.0, 'mg/dL',  '2024-02-10'),
('P003', 'hba1c',           8.5,   '%',      '2024-02-10'),
('P003', 'cholesterol',     245.0, 'mg/dL',  '2024-08-25'),
('P004', 'glucose',         88.0,  'mg/dL',  '2024-06-12'),
('P005', 'blood_pressure',  158.0, 'mmHg',   '2024-01-18'),
('P005', 'cholesterol',     220.0, 'mg/dL',  '2024-01-18'),
('P005', 'glucose',         142.0, 'mg/dL',  '2024-04-22'),
('P005', 'hba1c',           7.2,   '%',      '2024-04-22'),
('P006', 'glucose',         82.0,  'mg/dL',  '2024-10-05'),
('P007', 'hemoglobin',      12.5,  'g/dL',   '2024-11-14'),
('P008', 'cholesterol',     268.0, 'mg/dL',  '2024-02-28'),
('P008', 'blood_pressure',  165.0, 'mmHg',   '2024-02-28'),
('P008', 'glucose',         118.0, 'mg/dL',  '2024-06-15'),
('P009', 'hemoglobin',      13.0,  'g/dL',   '2024-08-08'),
('P010', 'blood_pressure',  152.0, 'mmHg',   '2024-03-22'),
('P010', 'cholesterol',     235.0, 'mg/dL',  '2024-03-22'),
('P010', 'glucose',         108.0, 'mg/dL',  '2024-09-10'),
('P011', 'cholesterol',     210.0, 'mg/dL',  '2024-12-01'),
('P012', 'glucose',         175.0, 'mg/dL',  '2024-04-15'),
('P012', 'hba1c',           8.0,   '%',      '2024-04-15'),
('P012', 'blood_pressure',  148.0, 'mmHg',   '2024-10-20'),
('P013', 'glucose',         95.0,  'mg/dL',  '2024-07-18'),
('P014', 'cholesterol',     255.0, 'mg/dL',  '2024-05-30'),
('P015', 'glucose',         98.0,  'mg/dL',  '2024-11-25');

-- 患者汇总特征 (15 行)
INSERT INTO patient_stats (user_id, visit_count, avg_lab_value, chronic_flag) VALUES
('P001', 2, 5.8,  0),
('P002', 3, 6.2,  0),
('P003', 5, 9.1,  1),
('P004', 2, 5.5,  0),
('P005', 6, 8.5,  1),
('P006', 1, 5.2,  0),
('P007', 2, 6.0,  0),
('P008', 4, 7.5,  1),
('P009', 2, 5.7,  0),
('P010', 3, 8.0,  1),
('P011', 1, 5.4,  0),
('P012', 4, 7.8,  1),
('P013', 2, 5.6,  0),
('P014', 3, 6.8,  1),
('P015', 2, 5.9,  0);
