-- 任务创建记录
-- 这些任务通过 REST API 创建，存储在 msp_db.tasks 表中
-- 以下 SQL 仅作为参考，显示已创建任务的基本信息

-- 查询所有任务
SELECT task_id, name, type, status, node_mode, participants, create_time
FROM msp_db.msp_task
ORDER BY create_time DESC;

-- 任务列表
-- | task_id                               | name                    | type           | status   |
-- |---------------------------------------|-------------------------|----------------|----------|
-- | 361a2737-da30-4db5-9445-947bc91d26e0 | 医院-保险PSI任务        | COMPONENT_DAG | CREATED  |
-- | 59b683ad-215b-4b51-84ab-1a7e1fcc139b | 三方PSI任务             | COMPONENT_DAG | CREATED  |
-- | 81975385-e21b-4359-b10f-593f0a1212b1 | 数据预处理Pipeline      | COMPONENT_DAG | CREATED  |
-- | a68ae39f-c7c0-449f-9a39-dafe473f8ce7 | 联邦学习训练预测Pipeline| COMPONENT_DAG | CREATED  |
-- | ec72503e-6bc7-4651-a17b-dab905281f8b | 医院-保险纵向联邦学习任务| COMPONENT_DAG | CREATED  |

-- 节点信息
-- | node_id        | node_name | party       |
-- |----------------|-----------|-------------|
-- | node-hospital  | 医院      | node-hospital |
-- | node-insurance | 保险公司  | node-insurance |
-- | node-research  | 医疗研究所| node-research |

-- 数据源信息
-- | datasource_id | node_id        | database     |
-- |---------------|----------------|--------------|
-- | ds-hosp       | node-hospital  | node_a_data  |
-- | ds-insurance  | node-insurance | node_c_data  |
-- | ds-research   | node-research  | node_b_data  |

-- 示例：删除任务
-- DELETE FROM msp_db.msp_task WHERE task_id = 'task_id_here';

-- 示例：重置任务状态为 CREATED
-- UPDATE msp_db.msp_task SET status = 'CREATED' WHERE task_id = 'task_id_here';