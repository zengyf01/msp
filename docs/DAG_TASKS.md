# MSP DAG 任务场景文档

本文档记录 MSP 平台上创建的各种 DAG 任务场景，可直接通过 API 创建。

## 环境信息

| 项目 | 值 |
|------|-----|
| 节点数 | 3 |
| 节点ID | node-hospital, node-research, node-insurance |
| 节点名称 | 医院, 医疗研究所, 保险公司 |
| 数据源ID | ds-hosp, ds-research, ds-insurance |
| 数据库 | MySQL |

## 数据库表信息

### 医院 (node-hospital)
- patients: 患者信息表
- diagnoses: 诊断记录表
- lab_results: 检验结果表
- patient_stats: 患者统计表

### 医疗研究所 (node-research)
- samples: 样本表
- lab_analyses: 实验室分析表
- publications: 出版物表
- research_projects: 研究项目表

### 保险公司 (node-insurance)
- policy_holders: 投保人表
- policies: 保单表
- claims: 理赔记录表

---

## 场景1：两方PSI（医院 × 研究所）

**目标**：找出同时在医院有诊断又在研究所有样本的患者

**参与方**：node-hospital, node-research

**组件**：
1. read_table（医院患者数据）
2. read_table（研究所样本数据）
3. psi（PSI对齐）
4. write_table（写入结果）

```bash
curl -X POST http://localhost:8092/api/v1/msp/tasks \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <TOKEN>" \
  -d '{
    "name": "医院-研究所 两方PSI任务",
    "type": "COMPONENT_DAG",
    "algorithm": "component_dag",
    "participants": ["node-hospital", "node-research"],
    "inputs": {},
    "parameters": {
      "dag_definition": "{\"name\":\"医院-研究所 两方PSI任务\",\"nodes\":[{\"nodeId\":\"n1\",\"compId\":\"read_table\",\"label\":\"读取医院患者数据\",\"x\":100,\"y\":80,\"attrs\":{\"datasource_id\":\"ds-hosp\",\"table_name\":\"patients\",\"columns\":[\"id\",\"name\",\"age\",\"gender\"]}},{\"nodeId\":\"n2\",\"compId\":\"read_table\",\"label\":\"读取研究所样本数据\",\"x\":100,\"y\":280,\"attrs\":{\"datasource_id\":\"ds-research\",\"table_name\":\"samples\",\"columns\":[\"id\",\"patient_id\",\"sample_type\",\"collect_date\"]}},{\"nodeId\":\"n3\",\"compId\":\"psi\",\"label\":\"PSI对齐\",\"x\":400,\"y\":180,\"attrs\":{\"key_column\":\"id\",\"psi_type\":\"ecdh\"}},{\"nodeId\":\"n4\",\"compId\":\"write_table\",\"label\":\"写入结果\",\"x\":650,\"y\":180,\"attrs\":{\"output_datasource_id\":\"ds-hosp\",\"output_table\":\"psi_result\",\"write_mode\":\"overwrite\"}}],\"edges\":[{\"from\":\"n1\",\"to\":\"n3\"},{\"from\":\"n2\",\"to\":\"n3\"},{\"from\":\"n3\",\"to\":\"n4\"}],\"description\":\"医院与研究所两方PSI任务\",\"participants\":[\"node-hospital\",\"node-research\"]}"
    }
  }'
```

---

## 场景2：三方PSI（医院 × 研究所 × 保险公司）

**目标**：三方联合求交，找出同时有医疗记录和保险记录的患者

**参与方**：node-hospital, node-research, node-insurance

**组件**：
1. read_table（医院患者数据）
2. read_table（研究所样本数据）
3. read_table（保险公司数据）
4. psi_tp（三方PSI对齐）
5. write_table（写入结果）

```bash
curl -X POST http://localhost:8092/api/v1/msp/tasks \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <TOKEN>" \
  -d '{
    "name": "三方PSI任务-医院-研究所-保险",
    "type": "COMPONENT_DAG",
    "algorithm": "component_dag",
    "participants": ["node-hospital", "node-research", "node-insurance"],
    "inputs": {},
    "parameters": {
      "dag_definition": "{\"name\":\"三方PSI任务\",\"nodes\":[{\"nodeId\":\"n1\",\"compId\":\"read_table\",\"label\":\"医院患者数据\",\"x\":80,\"y\":60,\"attrs\":{\"datasource_id\":\"ds-hosp\",\"table_name\":\"patients\",\"columns\":[\"id\",\"name\",\"age\",\"gender\"]}},{\"nodeId\":\"n2\",\"compId\":\"read_table\",\"label\":\"研究所样本数据\",\"x\":80,\"y\":220,\"attrs\":{\"datasource_id\":\"ds-research\",\"table_name\":\"samples\",\"columns\":[\"id\",\"patient_id\",\"sample_type\"]}},{\"nodeId\":\"n3\",\"compId\":\"read_table\",\"label\":\"保险公司数据\",\"x\":80,\"y\":380,\"attrs\":{\"datasource_id\":\"ds-insurance\",\"table_name\":\"policy_holders\",\"columns\":[\"id\",\"name\",\"age\",\"gender\"]}},{\"nodeId\":\"n4\",\"compId\":\"psi_tp\",\"label\":\"三方PSI对齐\",\"x\":400,\"y\":220,\"attrs\":{\"key_column\":\"id\"}},{\"nodeId\":\"n5\",\"compId\":\"write_table\",\"label\":\"写入结果\",\"x\":650,\"y\":220,\"attrs\":{\"output_datasource_id\":\"ds-hosp\",\"output_table\":\"triple_psi_result\",\"write_mode\":\"overwrite\"}}],\"edges\":[{\"from\":\"n1\",\"to\":\"n4\"},{\"from\":\"n2\",\"to\":\"n4\"},{\"from\":\"n3\",\"to\":\"n4\"},{\"from\":\"n4\",\"to\":\"n5\"}],\"description\":\"三方PSI联合任务\",\"participants\":[\"node-hospital\",\"node-research\",\"node-insurance\"]}"
    }
  }'
```

---

## 场景3：联合建模-SS-GLM线性回归

**目标**：利用医院特征和保险标签训练线性回归模型

**参与方**：node-hospital, node-insurance

**组件**：
1. read_table（医院诊断数据）
2. read_table（保险理赔数据）
3. filter_null（空值处理）
4. filter_column（列过滤）
5. binning（特征分箱）
6. ss_glm_train（GLM训练）
7. ss_glm_predict（GLM预测）
8. biclassification_eval（模型评估）
9. write_table（写入结果）

```bash
curl -X POST http://localhost:8092/api/v1/msp/tasks \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <TOKEN>" \
  -d '{
    "name": "联合建模-SS-GLM线性回归",
    "type": "COMPONENT_DAG",
    "algorithm": "component_dag",
    "participants": ["node-hospital", "node-insurance"],
    "inputs": {},
    "parameters": {
      "dag_definition": "{\"name\":\"联合建模-SS-GLM线性回归\",\"nodes\":[{\"nodeId\":\"n1\",\"compId\":\"read_table\",\"label\":\"医院诊断数据\",\"x\":80,\"y\":60,\"attrs\":{\"datasource_id\":\"ds-hosp\",\"table_name\":\"diagnoses\",\"columns\":[\"patient_id\",\"diagnosis_code\",\"visit_date\"]}},{\"nodeId\":\"n2\",\"compId\":\"read_table\",\"label\":\"保险理赔数据\",\"x\":80,\"y\":260,\"attrs\":{\"datasource_id\":\"ds-insurance\",\"table_name\":\"claims\",\"columns\":[\"patient_id\",\"claim_amount\",\"has_claim\"]}},{\"nodeId\":\"n3\",\"compId\":\"filter_null\",\"label\":\"空值处理\",\"x\":320,\"y\":60,\"attrs\":{\"null_action\":\"drop\"}},{\"nodeId\":\"n4\",\"compId\":\"filter_column\",\"label\":\"列过滤\",\"x\":320,\"y\":260,\"attrs\":{\"keep_columns\":[\"patient_id\",\"diagnosis_code\",\"claim_amount\",\"has_claim\"]}},{\"nodeId\":\"n5\",\"compId\":\"binning\",\"label\":\"特征分箱\",\"x\":560,\"y\":160,\"attrs\":{\"num_bins\":10}},{\"nodeId\":\"n6\",\"compId\":\"ss_glm_train\",\"label\":\"GLM训练\",\"x\":800,\"y\":160,\"attrs\":{\"epochs\":100,\"batch_size\":32,\"learning_rate\":0.01}},{\"nodeId\":\"n7\",\"compId\":\"ss_glm_predict\",\"label\":\"GLM预测\",\"x\":1000,\"y\":160,\"attrs\":{}},{\"nodeId\":\"n8\",\"compId\":\"biclassification_eval\",\"label\":\"模型评估\",\"x\":1200,\"y\":160,\"attrs\":{\"label_column\":\"has_claim\"}},{\"nodeId\":\"n9\",\"compId\":\"write_table\",\"label\":\"写入结果\",\"x\":1200,\"y\":320,\"attrs\":{\"output_datasource_id\":\"ds-insurance\",\"output_table\":\"glm_predictions\",\"write_mode\":\"overwrite\"}}],\"edges\":[{\"from\":\"n1\",\"to\":\"n3\"},{\"from\":\"n2\",\"to\":\"n4\"},{\"from\":\"n3\",\"to\":\"n4\"},{\"from\":\"n4\",\"to\":\"n5\"},{\"from\":\"n5\",\"to\":\"n6\"},{\"from\":\"n6\",\"to\":\"n7\"},{\"from\":\"n7\",\"to\":\"n8\"},{\"from\":\"n8\",\"to\":\"n9\"}],\"description\":\"联邦学习-GLM线性回归\",\"participants\":[\"node-hospital\",\"node-insurance\"]}"
    }
  }'
```

---

## 场景4：联合建模-SGB梯度提升树

**目标**：利用医院患者统计和保险理赔数据训练 SGB 模型

**参与方**：node-hospital, node-insurance

**组件**：
1. read_table（医院患者统计）
2. read_table（保险理赔数据）
3. filter_null（空值处理）
4. filter_duplicate（去重）
5. filter_column（列过滤）
6. sample（数据采样）
7. sgb_train（SGB训练）
8. sgb_predict（SGB预测）
9. biclassification_eval（二分类评估）
10. write_table（写入预测结果）

```bash
curl -X POST http://localhost:8092/api/v1/msp/tasks \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <TOKEN>" \
  -d '{
    "name": "联合建模-SGB梯度提升树",
    "type": "COMPONENT_DAG",
    "algorithm": "component_dag",
    "participants": ["node-hospital", "node-insurance"],
    "inputs": {},
    "parameters": {
      "dag_definition": "{\"name\":\"联合建模-SGB梯度提升树\",\"nodes\":[{\"nodeId\":\"n1\",\"compId\":\"read_table\",\"label\":\"医院患者统计\",\"x\":80,\"y\":60,\"attrs\":{\"datasource_id\":\"ds-hosp\",\"table_name\":\"patient_stats\",\"columns\":[\"patient_id\",\"age\",\"visit_count\",\"avg_cost\"]}},{\"nodeId\":\"n2\",\"compId\":\"read_table\",\"label\":\"保险理赔数据\",\"x\":80,\"y\":260,\"attrs\":{\"datasource_id\":\"ds-insurance\",\"table_name\":\"claims\",\"columns\":[\"patient_id\",\"claim_amount\",\"has_claim\",\"claim_count\"]}},{\"nodeId\":\"n3\",\"compId\":\"filter_null\",\"label\":\"空值处理\",\"x\":320,\"y\":60,\"attrs\":{\"null_action\":\"drop\"}},{\"nodeId\":\"n4\",\"compId\":\"filter_duplicate\",\"label\":\"去重\",\"x\":320,\"y\":260,\"attrs\":{\"duplicate_keep\":\"first\"}},{\"nodeId\":\"n5\",\"compId\":\"filter_column\",\"label\":\"列过滤\",\"x\":560,\"y\":160,\"attrs\":{\"keep_columns\":[\"patient_id\",\"age\",\"visit_count\",\"avg_cost\",\"claim_amount\",\"has_claim\"]}},{\"nodeId\":\"n6\",\"compId\":\"sample\",\"label\":\"数据采样\",\"x\":800,\"y\":160,\"attrs\":{}},{\"nodeId\":\"n7\",\"compId\":\"sgb_train\",\"label\":\"SGB训练\",\"x\":1000,\"y\":160,\"attrs\":{\"num_trees\":50,\"max_depth\":6,\"learning_rate\":0.1}},{\"nodeId\":\"n8\",\"compId\":\"sgb_predict\",\"label\":\"SGB预测\",\"x\":1200,\"y\":160,\"attrs\":{}},{\"nodeId\":\"n9\",\"compId\":\"biclassification_eval\",\"label\":\"二分类评估\",\"x\":1400,\"y\":160,\"attrs\":{\"label_column\":\"has_claim\"}},{\"nodeId\":\"n10\",\"compId\":\"write_table\",\"label\":\"写入预测结果\",\"x\":1400,\"y\":320,\"attrs\":{\"output_datasource_id\":\"ds-insurance\",\"output_table\":\"sgb_predictions\",\"write_mode\":\"overwrite\"}}],\"edges\":[{\"from\":\"n1\",\"to\":\"n3\"},{\"from\":\"n2\",\"to\":\"n4\"},{\"from\":\"n3\",\"to\":\"n5\"},{\"from\":\"n4\",\"to\":\"n5\"},{\"from\":\"n5\",\"to\":\"n6\"},{\"from\":\"n6\",\"to\":\"n7\"},{\"from\":\"n7\",\"to\":\"n8\"},{\"from\":\"n8\",\"to\":\"n9\"},{\"from\":\"n9\",\"to\":\"n10\"}],\"description\":\"联邦学习-SGB梯度提升树\",\"participants\":[\"node-hospital\",\"node-insurance\"]}"
    }
  }'
```

---

## 场景5：医疗数据清洗分析Pipeline

**目标**：清洗医院患者数据，进行空值填充、去重、列过滤、范围过滤和分箱

**参与方**：node-hospital

**组件**：
1. read_table（读取患者数据）
2. filter_null（空值处理）
3. filter_duplicate（去重）
4. filter_column（列过滤）
5. filter_range（范围过滤）
6. binning（年龄分箱）
7. write_table（写入清洗结果）

```bash
curl -X POST http://localhost:8092/api/v1/msp/tasks \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <TOKEN>" \
  -d '{
    "name": "医疗数据清洗分析Pipeline",
    "type": "COMPONENT_DAG",
    "algorithm": "component_dag",
    "participants": ["node-hospital"],
    "inputs": {},
    "parameters": {
      "dag_definition": "{\"name\":\"医疗数据清洗分析Pipeline\",\"nodes\":[{\"nodeId\":\"n1\",\"compId\":\"read_table\",\"label\":\"读取患者数据\",\"x\":80,\"y\":100,\"attrs\":{\"datasource_id\":\"ds-hosp\",\"table_name\":\"patients\",\"columns\":[\"id\",\"name\",\"age\",\"gender\",\"phone\",\"address\"]}},{\"nodeId\":\"n2\",\"compId\":\"filter_null\",\"label\":\"空值处理\",\"x\":280,\"y\":100,\"attrs\":{\"null_action\":\"fill\",\"fill_value\":\"unknown\"}},{\"nodeId\":\"n3\",\"compId\":\"filter_duplicate\",\"label\":\"去重\",\"x\":480,\"y\":100,\"attrs\":{\"duplicate_keep\":\"first\"}},{\"nodeId\":\"n4\",\"compId\":\"filter_column\",\"label\":\"列过滤\",\"x\":680,\"y\":100,\"attrs\":{\"keep_columns\":[\"id\",\"name\",\"age\",\"gender\",\"phone\"]}},{\"nodeId\":\"n5\",\"compId\":\"filter_range\",\"label\":\"范围过滤\",\"x\":880,\"y\":100,\"attrs\":{\"range_column\":\"age\",\"range_min\":0,\"range_max\":120,\"range_inclusive\":\"both\"}},{\"nodeId\":\"n6\",\"compId\":\"binning\",\"label\":\"年龄分箱\",\"x\":1080,\"y\":100,\"attrs\":{\"num_bins\":5}},{\"nodeId\":\"n7\",\"compId\":\"write_table\",\"label\":\"写入清洗结果\",\"x\":1280,\"y\":100,\"attrs\":{\"output_datasource_id\":\"ds-hosp\",\"output_table\":\"patients_cleaned\",\"write_mode\":\"overwrite\"}}],\"edges\":[{\"from\":\"n1\",\"to\":\"n2\"},{\"from\":\"n2\",\"to\":\"n3\"},{\"from\":\"n3\",\"to\":\"n4\"},{\"from\":\"n4\",\"to\":\"n5\"},{\"from\":\"n5\",\"to\":\"n6\"},{\"from\":\"n6\",\"to\":\"n7\"}],\"description\":\"医疗数据清洗分析Pipeline\",\"participants\":[\"node-hospital\"]}"
    }
  }'
```

---

## 场景6：PSI对齐后联合建模

**目标**：三方PSI对齐后，利用对齐的数据进行 SGB 建模

**参与方**：node-hospital, node-research, node-insurance

**组件**：
1. read_table（医院诊断数据）
2. read_table（研究所样本数据）
3. read_table（保险理赔数据）
4. psi_tp（三方PSI对齐）
5. filter_null（空值处理）
6. filter_column（特征列过滤）
7. binning（特征分箱）
8. sgb_train（SGB训练）
9. sgb_predict（SGB预测）
10. biclassification_eval（模型评估）
11. write_table（写入结果）

```bash
curl -X POST http://localhost:8092/api/v1/msp/tasks \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <TOKEN>" \
  -d '{
    "name": "PSI对齐后联合建模",
    "type": "COMPONENT_DAG",
    "algorithm": "component_dag",
    "participants": ["node-hospital", "node-research", "node-insurance"],
    "inputs": {},
    "parameters": {
      "dag_definition": "{\"name\":\"PSI对齐后联合建模\",\"nodes\":[{\"nodeId\":\"n1\",\"compId\":\"read_table\",\"label\":\"医院诊断数据\",\"x\":80,\"y\":60,\"attrs\":{\"datasource_id\":\"ds-hosp\",\"table_name\":\"diagnoses\",\"columns\":[\"patient_id\",\"diagnosis_code\",\"visit_date\"]}},{\"nodeId\":\"n2\",\"compId\":\"read_table\",\"label\":\"研究所样本数据\",\"x\":80,\"y\":220,\"attrs\":{\"datasource_id\":\"ds-research\",\"table_name\":\"samples\",\"columns\":[\"id\",\"patient_id\",\"sample_type\"]}},{\"nodeId\":\"n3\",\"compId\":\"read_table\",\"label\":\"保险理赔数据\",\"x\":80,\"y\":380,\"attrs\":{\"datasource_id\":\"ds-insurance\",\"table_name\":\"claims\",\"columns\":[\"patient_id\",\"claim_amount\",\"has_claim\"]}},{\"nodeId\":\"n4\",\"compId\":\"psi_tp\",\"label\":\"三方PSI对齐\",\"x\":350,\"y\":220,\"attrs\":{\"key_column\":\"patient_id\"}},{\"nodeId\":\"n5\",\"compId\":\"filter_null\",\"label\":\"空值处理\",\"x\":580,\"y\":220,\"attrs\":{\"null_action\":\"drop\"}},{\"nodeId\":\"n6\",\"compId\":\"filter_column\",\"label\":\"特征列过滤\",\"x\":810,\"y\":220,\"attrs\":{\"keep_columns\":[\"patient_id\",\"diagnosis_code\",\"sample_type\",\"claim_amount\",\"has_claim\"]}},{\"nodeId\":\"n7\",\"compId\":\"binning\",\"label\":\"特征分箱\",\"x\":1040,\"y\":220,\"attrs\":{\"num_bins\":10}},{\"nodeId\":\"n8\",\"compId\":\"sgb_train\",\"label\":\"SGB训练\",\"x\":1270,\"y\":220,\"attrs\":{\"num_trees\":50,\"max_depth\":6,\"learning_rate\":0.1}},{\"nodeId\":\"n9\",\"compId\":\"sgb_predict\",\"label\":\"SGB预测\",\"x\":1500,\"y\":220,\"attrs\":{}},{\"nodeId\":\"n10\",\"compId\":\"biclassification_eval\",\"label\":\"模型评估\",\"x\":1730,\"y\":220,\"attrs\":{\"label_column\":\"has_claim\"}},{\"nodeId\":\"n11\",\"compId\":\"write_table\",\"label\":\"写入结果\",\"x\":1730,\"y\":400,\"attrs\":{\"output_datasource_id\":\"ds-insurance\",\"output_table\":\"psi_sgb_result\",\"write_mode\":\"overwrite\"}}],\"edges\":[{\"from\":\"n1\",\"to\":\"n4\"},{\"from\":\"n2\",\"to\":\"n4\"},{\"from\":\"n3\",\"to\":\"n4\"},{\"from\":\"n4\",\"to\":\"n5\"},{\"from\":\"n5\",\"to\":\"n6\"},{\"from\":\"n6\",\"to\":\"n7\"},{\"from\":\"n7\",\"to\":\"n8\"},{\"from\":\"n8\",\"to\":\"n9\"},{\"from\":\"n9\",\"to\":\"n10\"},{\"from\":\"n10\",\"to\":\"n11\"}],\"description\":\"PSI对齐后联合建模\",\"participants\":[\"node-hospital\",\"node-research\",\"node-insurance\"]}"
    }
  }'
```

---

## 通用创建脚本

```bash
#!/bin/bash
# 创建所有DAG任务

TOKEN=$(curl -s -X POST http://localhost:8092/api/v1/msp/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['token'])")

# 场景1：两方PSI
# ... (见上方curl命令)

# 场景2：三方PSI
# ... (见上方curl命令)

# 场景3：SS-GLM
# ... (见上方curl命令)

# 场景4：SGB
# ... (见上方curl命令)

# 场景5：数据清洗
# ... (见上方curl命令)

# 场景6：PSI+建模
# ... (见上方curl命令)
```

---

## 组件参数说明

| 组件ID | 组件名称 | 必需参数 |
|--------|----------|----------|
| read_table | 读取数据表 | datasource_id, table_name, columns |
| read_api | 读取API数据 | api_url, api_method |
| write_table | 写入数据表 | output_datasource_id, output_table, write_mode |
| write_csv | 写入CSV文件 | file_path, delimiter |
| psi | PSI对齐 | key_column, psi_type (ecdh/kkrt/bc22) |
| psi_tp | 三方PSI对齐 | key_column |
| unbalance_psi | 不平衡PSI | key_column, psi_type |
| filter_null | 空值处理 | null_action (drop/fill/fill_mean/fill_median), fill_value |
| filter_duplicate | 去重 | duplicate_keep (first/last) |
| filter_column | 列过滤 | keep_columns, drop_columns |
| filter_rows | 行过滤 | filter_column, filter_operator, filter_value |
| filter_range | 范围过滤 | range_column, range_min, range_max, range_inclusive |
| binning | 分箱 | num_bins, feature_columns |
| vert_binning | 纵向分箱 | num_bins |
| woe_binning | WOE分箱 | label_column |
| sample | 采样 | - |
| ss_glm_train | SS-GLM训练 | epochs, batch_size, learning_rate |
| ss_glm_predict | SS-GLM预测 | - |
| sgb_train | SGB训练 | num_trees, max_depth, learning_rate |
| sgb_predict | SGB预测 | - |
| biclassification_eval | 二分类评估 | label_column, prediction_column |
| regression_eval | 回归评估 | label_column, prediction_column |