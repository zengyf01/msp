-- 医疗研究所数据源默认表与示例数据 (node_b_data)
-- 对应数据源 ds-research，业务方：医疗研究所
-- user_id 命名空间：P003-P014（与医院 P003-P012 交集 10 人，与保险 P008-P012 交集 5 人）

USE node_b_data;

SET NAMES utf8mb4;

-- 科研项目
CREATE TABLE IF NOT EXISTS research_projects (
    project_id   VARCHAR(16)   PRIMARY KEY,
    project_name VARCHAR(128)  NOT NULL                                COMMENT '项目名称',
    principal    VARCHAR(64)                                            COMMENT '负责人',
    start_date   DATE                                                    COMMENT '开始日期',
    end_date     DATE                                                    COMMENT '结束日期',
    status       VARCHAR(16)   DEFAULT 'ONGOING'                        COMMENT '状态（ONGOING/FINISHED/PAUSED）',
    budget       DECIMAL(12,2)                                         COMMENT '预算（元）'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='科研项目';

-- 科研样本（按 user_id 与医院对齐，PSI 可联）
CREATE TABLE IF NOT EXISTS samples (
    sample_id        VARCHAR(16)  PRIMARY KEY                            COMMENT '样本 ID（S001-S012）',
    user_id          VARCHAR(16)  NOT NULL                                COMMENT '患者 ID（与医院库 PSI 对齐）',
    sample_type      VARCHAR(32)                                            COMMENT '样本类型（血液/组织/唾液/尿液/骨髓）',
    collection_date  DATE                                                COMMENT '采集日期',
    project_id       VARCHAR(16)                                        COMMENT '所属项目 ID',
    storage_location VARCHAR(64)                                        COMMENT '存储位置',
    KEY idx_user (user_id),
    KEY idx_project (project_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='科研样本（按 user_id 与医院对齐，PSI 可联）';

-- 实验分析结果（analysis_value 用于三方 MPC 联合统计）
CREATE TABLE IF NOT EXISTS lab_analyses (
    analysis_id     BIGINT        PRIMARY KEY AUTO_INCREMENT            COMMENT '分析 ID',
    sample_id       VARCHAR(16)   NOT NULL                                COMMENT '样本 ID（FK -> samples.sample_id）',
    user_id         VARCHAR(16)   NOT NULL                                COMMENT '患者 ID（冗余便于按人聚合）',
    indicator_name  VARCHAR(64)                                          COMMENT '指标名（gene_expression / protein_level / biomarker_score / methylation_rate）',
    analysis_value  DECIMAL(10,2)                                          COMMENT '三方 MPC 联合统计数值列',
    unit            VARCHAR(16)                                          COMMENT '单位',
    analysis_date   DATE                                                 COMMENT '分析日期',
    KEY idx_user (user_id),
    KEY idx_sample (sample_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='实验分析结果（analysis_value 用于三方 MPC 联合统计）';

-- 论文
CREATE TABLE IF NOT EXISTS publications (
    paper_id      VARCHAR(16)   PRIMARY KEY                            COMMENT '论文 ID',
    project_id    VARCHAR(16)                                            COMMENT '所属项目 ID（FK -> research_projects.project_id）',
    title         VARCHAR(256)  NOT NULL                                COMMENT '论文标题',
    journal       VARCHAR(128)                                            COMMENT '期刊名称',
    publish_date  DATE                                                    COMMENT '发表日期',
    impact_factor DECIMAL(5,2)                                            COMMENT '影响因子'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='论文';

-- 科研项目 (4 行)
INSERT INTO research_projects (project_id, project_name, principal, start_date, end_date, status, budget) VALUES
('PRJ001', '2型糖尿病精准治疗研究',         '王明远教授', '2023-01-01', '2025-12-31', 'ONGOING',  1800000.00),
('PRJ002', '新型抗癌药物临床试验',           '李慧研究员', '2023-06-01', '2026-05-31', 'ONGOING',  3200000.00),
('PRJ003', '高血压相关基因易感性研究',       '张志成教授', '2022-09-01', '2024-12-31', 'ONGOING',  1500000.00),
('PRJ004', '深度学习辅助医学影像诊断系统',   '陈思涵研究员', '2024-03-01', '2026-02-28', 'ONGOING', 2100000.00);

-- 科研样本 (12 行, P003-P014)
INSERT INTO samples (sample_id, user_id, sample_type, collection_date, project_id, storage_location) VALUES
('S001', 'P003', '血液', '2024-02-15', 'PRJ001', 'A区冰箱-01'),
('S002', 'P004', '组织', '2024-06-18', 'PRJ002', 'B区液氮罐-03'),
('S003', 'P005', '血液', '2024-01-25', 'PRJ003', 'A区冰箱-02'),
('S004', 'P006', '唾液', '2024-10-10', 'PRJ001', 'C区冰箱-01'),
('S005', 'P007', '血液', '2024-11-18', 'PRJ003', 'A区冰箱-03'),
('S006', 'P008', '组织', '2024-03-05', 'PRJ002', 'B区液氮罐-01'),
('S007', 'P009', '血液', '2024-08-12', 'PRJ001', 'A区冰箱-04'),
('S008', 'P010', '尿液', '2024-04-02', 'PRJ003', 'D区冰箱-01'),
('S009', 'P011', '血液', '2024-12-05', 'PRJ004', 'A区冰箱-05'),
('S010', 'P012', '组织', '2024-04-20', 'PRJ002', 'B区液氮罐-02'),
('S011', 'P013', '血液', '2024-07-22', 'PRJ001', 'A区冰箱-06'),
('S012', 'P014', '唾液', '2024-06-01', 'PRJ004', 'C区冰箱-02');

-- 实验分析结果 (24 行)
INSERT INTO lab_analyses (sample_id, user_id, indicator_name, analysis_value, unit, analysis_date) VALUES
('S001', 'P003', 'gene_expression',     8.42,  'log2(TPM)', '2024-03-01'),
('S001', 'P003', 'protein_level',       145.6, 'ng/mL',     '2024-03-05'),
('S001', 'P003', 'hba1c',               8.5,   '%',         '2024-03-10'),
('S002', 'P004', 'biomarker_score',     6.7,   'score',     '2024-07-02'),
('S003', 'P005', 'gene_expression',     7.85,  'log2(TPM)', '2024-02-05'),
('S003', 'P005', 'protein_level',       132.0, 'ng/mL',     '2024-02-10'),
('S003', 'P005', 'methylation_rate',    62.5,  '%',         '2024-02-15'),
('S004', 'P006', 'gene_expression',     5.20,  'log2(TPM)', '2024-10-20'),
('S005', 'P007', 'protein_level',       98.4,  'ng/mL',     '2024-11-25'),
('S006', 'P008', 'biomarker_score',     9.1,   'score',     '2024-03-15'),
('S006', 'P008', 'gene_expression',     9.35,  'log2(TPM)', '2024-03-20'),
('S006', 'P008', 'protein_level',       178.2, 'ng/mL',     '2024-03-25'),
('S007', 'P009', 'gene_expression',     5.85,  'log2(TPM)', '2024-08-20'),
('S008', 'P010', 'methylation_rate',    71.0,  '%',         '2024-04-10'),
('S008', 'P010', 'protein_level',       156.3, 'ng/mL',     '2024-04-12'),
('S009', 'P011', 'biomarker_score',     7.2,   'score',     '2024-12-10'),
('S010', 'P012', 'gene_expression',     9.05,  'log2(TPM)', '2024-05-01'),
('S010', 'P012', 'biomarker_score',     8.8,   'score',     '2024-05-05'),
('S010', 'P012', 'protein_level',       165.7, 'ng/mL',     '2024-05-10'),
('S011', 'P013', 'gene_expression',     5.95,  'log2(TPM)', '2024-07-30'),
('S011', 'P013', 'protein_level',       105.0, 'ng/mL',     '2024-08-02'),
('S012', 'P014', 'methylation_rate',    58.0,  '%',         '2024-06-10'),
('S012', 'P014', 'biomarker_score',     6.9,   'score',     '2024-06-15');

-- 论文 (4 行)
INSERT INTO publications (paper_id, project_id, title, journal, publish_date, impact_factor) VALUES
('PUB001', 'PRJ001', '2型糖尿病精准治疗的多组学研究进展',                '中华医学杂志',         '2024-08-15', 4.85),
('PUB002', 'PRJ002', '新型PD-1抑制剂在晚期非小细胞肺癌的临床应用观察',   '中国肿瘤临床',         '2024-10-20', 3.92),
('PUB003', 'PRJ003', '中国汉族人群ACE基因多态性与原发性高血压的相关性',  '中华遗传学杂志',       '2024-05-30', 2.76),
('PUB004', 'PRJ004', '基于深度学习的CT影像肺结节自动检测系统',            '中国生物医学工程学报', '2024-12-08', 1.95);
