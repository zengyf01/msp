# 任务创建文档

## 概述

所有任务均通过 REST API 创建，使用 `POST /api/v1/msp/tasks/save` 接口（保存但不执行，状态为 CREATED）。

## 前置准备

### 1. 登录获取 Token

```bash
TOKEN=$(curl -s -X POST "http://localhost:8092/api/v1/msp/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' | jq -r '.data.token')
```

### 2. 数据源信息

| 数据源ID | 节点 | 数据库 | 表 |
|----------|------|--------|-----|
| ds-hosp | node-hospital | node_a_data | patients, lab_results, patient_stats |
| ds-insurance | node-insurance | node_c_data | claims, policies, policy_holders |
| ds-research | node-research | node_b_data | lab_analyses, publications, research_projects, samples |

## 任务1：医院-保险PSI任务

### DAG 结构
- read_table (医院 patients) → PSI → write_table

### API 调用

```bash
curl -X POST "http://localhost:8092/api/v1/msp/tasks/save" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "name": "医院-保险PSI任务",
    "type": "COMPONENT_DAG",
    "algorithm": "component_dag",
    "participants": ["node-hospital", "node-insurance"],
    "parameters": {
      "dag_definition": "{\"name\":\"医院-保险PSI任务\",\"nodes\":[{\"nodeId\":\"read_hosp\",\"compId\":\"read_table\",\"label\":\"读取数据表\",\"x\":80,\"y\":99,\"config\":{\"datasource_id\":\"ds-hosp\",\"table_name\":\"patients\",\"columns\":[\"user_id\",\"name\",\"gender\",\"age\"]}},{\"nodeId\":\"read_ins\",\"compId\":\"read_table\",\"label\":\"读取数据表\",\"x\":80,\"y\":280,\"config\":{\"datasource_id\":\"ds-insurance\",\"table_name\":\"claims\",\"columns\":[\"user_id\",\"claim_amount\",\"default_flag\"]}},{\"nodeId\":\"psi\",\"compId\":\"psi\",\"label\":\"PSI\",\"x\":320,\"y\":180,\"config\":{\"key_column\":\"user_id\",\"psi_type\":\"ecdh\"}},{\"nodeId\":\"write\",\"compId\":\"write_table\",\"label\":\"写入数据表\",\"x\":560,\"y\":180,\"config\":{\"output_datasource_id\":\"ds-hosp\",\"output_table\":\"psi_result\",\"write_mode\":\"overwrite\"}}],\"edges\":[{\"from\":\"read_hosp\",\"to\":\"psi\",\"fromPort\":0,\"toPort\":0},{\"from\":\"read_ins\",\"to\":\"psi\",\"fromPort\":0,\"toPort\":1},{\"from\":\"psi\",\"to\":\"write\",\"fromPort\":0,\"toPort\":0}],\"description\":\"医院与保险公司的隐私集合求交\",\"participants\":[\"node-hospital\",\"node-insurance\"]}"
    }
  }'
```

## 任务2：三方PSI任务

### DAG 结构
- read_table (医院) → PSI (三方) → write_table
- read_table (保险) ↗
- read_table (研究所) ↗

### API 调用

```bash
curl -X POST "http://localhost:8092/api/v1/msp/tasks/save" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "name": "三方PSI任务",
    "type": "COMPONENT_DAG",
    "algorithm": "component_dag",
    "participants": ["node-hospital", "node-insurance", "node-research"],
    "parameters": {
      "dag_definition": "{\"name\":\"三方PSI任务\",\"nodes\":[{\"nodeId\":\"read_hosp\",\"compId\":\"read_table\",\"label\":\"读取数据表\",\"x\":79,\"y\":58,\"config\":{\"datasource_id\":\"ds-hosp\",\"table_name\":\"patients\",\"columns\":[\"user_id\",\"name\",\"gender\",\"age\"]}},{\"nodeId\":\"read_ins\",\"compId\":\"read_table\",\"label\":\"读取数据表\",\"x\":80,\"y\":180,\"config\":{\"datasource_id\":\"ds-insurance\",\"table_name\":\"claims\",\"columns\":[\"user_id\",\"claim_amount\",\"default_flag\"]}},{\"nodeId\":\"read_res\",\"compId\":\"read_table\",\"label\":\"读取数据表\",\"x\":80,\"y\":300,\"config\":{\"datasource_id\":\"ds-research\",\"table_name\":\"samples\",\"columns\":[\"user_id\"]}},{\"nodeId\":\"psi_tp\",\"compId\":\"psi_tp\",\"label\":\"三方PSI\",\"x\":320,\"y\":180,\"config\":{\"key_column\":\"user_id\"}},{\"nodeId\":\"write\",\"compId\":\"write_table\",\"label\":\"写入数据表\",\"x\":560,\"y\":180,\"config\":{\"output_datasource_id\":\"ds-hosp\",\"output_table\":\"psi_3party_result\",\"write_mode\":\"overwrite\"}}],\"edges\":[{\"from\":\"read_hosp\",\"to\":\"psi_tp\",\"fromPort\":0,\"toPort\":0},{\"from\":\"read_ins\",\"to\":\"psi_tp\",\"fromPort\":0,\"toPort\":1},{\"from\":\"read_res\",\"to\":\"psi_tp\",\"fromPort\":0,\"toPort\":2},{\"from\":\"psi_tp\",\"to\":\"write\",\"fromPort\":0,\"toPort\":0}],\"description\":\"三方隐私集合求交\",\"participants\":[\"node-hospital\",\"node-insurance\",\"node-research\"]}"
    }
  }'
```

## 任务3：数据预处理Pipeline

### DAG 结构
read_table → filter_null → filter_duplicate → binning → write_table

### API 调用

```bash
curl -X POST "http://localhost:8092/api/v1/msp/tasks/save" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "name": "数据预处理Pipeline",
    "type": "COMPONENT_DAG",
    "algorithm": "component_dag",
    "participants": ["node-hospital"],
    "parameters": {
      "dag_definition": "{\"name\":\"数据预处理Pipeline\",\"nodes\":[{\"nodeId\":\"read_1\",\"compId\":\"read_table\",\"label\":\"读取数据表\",\"x\":80,\"y\":100,\"config\":{\"datasource_id\":\"ds-hosp\",\"table_name\":\"patients\"}},{\"nodeId\":\"filter_null\",\"compId\":\"filter_null\",\"label\":\"过滤空值\",\"x\":280,\"y\":100,\"config\":{\"columns\":[\"user_id\",\"name\",\"id_card_hash\"]}},{\"nodeId\":\"filter_dup\",\"compId\":\"filter_duplicate\",\"label\":\"去重\",\"x\":480,\"y\":100,\"config\":{\"key_column\":\"id_card_hash\"}},{\"nodeId\":\"binning\",\"compId\":\"binning\",\"label\":\"分箱\",\"x\":680,\"y\":100,\"config\":{\"column\":\"age\",\"num_bins\":5}},{\"nodeId\":\"write\",\"compId\":\"write_table\",\"label\":\"写入数据表\",\"x\":880,\"y\":100,\"config\":{\"output_datasource_id\":\"ds-hosp\",\"output_table\":\"patient_stats_clean\",\"write_mode\":\"overwrite\"}}],\"edges\":[{\"from\":\"read_1\",\"to\":\"filter_null\",\"fromPort\":0,\"toPort\":0},{\"from\":\"filter_null\",\"to\":\"filter_dup\",\"fromPort\":0,\"toPort\":0},{\"from\":\"filter_dup\",\"to\":\"binning\",\"fromPort\":0,\"toPort\":0},{\"from\":\"binning\",\"to\":\"write\",\"fromPort\":0,\"toPort\":0}],\"description\":\"医院数据预处理\"}"
    }
  }'
```

## 任务4：联邦学习训练预测Pipeline

### DAG 结构
read_table (医院) → PSI → ss_glm_train → ss_glm_predict → biclassification_eval → write_table
read_table (保险) ↗

### API 调用

```bash
curl -X POST "http://localhost:8092/api/v1/msp/tasks/save" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "name": "联邦学习训练预测Pipeline",
    "type": "COMPONENT_DAG",
    "algorithm": "component_dag",
    "participants": ["node-hospital", "node-insurance"],
    "parameters": {
      "dag_definition": "{\"name\":\"联邦学习训练预测Pipeline\",\"nodes\":[{\"nodeId\":\"read_hosp\",\"compId\":\"read_table\",\"label\":\"读取数据表\",\"x\":80,\"y\":60,\"config\":{\"datasource_id\":\"ds-hosp\",\"table_name\":\"patient_stats\"}},{\"nodeId\":\"read_ins\",\"compId\":\"read_table\",\"label\":\"读取数据表\",\"x\":80,\"y\":200,\"config\":{\"datasource_id\":\"ds-insurance\",\"table_name\":\"claims\"}},{\"nodeId\":\"psi\",\"compId\":\"psi\",\"label\":\"PSI\",\"x\":300,\"y\":130,\"config\":{\"key_column\":\"user_id\",\"psi_type\":\"kkrt\"}},{\"nodeId\":\"train\",\"compId\":\"ss_glm_train\",\"label\":\"联邦学习训练\",\"x\":520,\"y\":130,\"config\":{\"label_column\":\"default_flag\",\"epochs\":10,\"batch_size\":64}},{\"nodeId\":\"predict\",\"compId\":\"ss_glm_predict\",\"label\":\"联邦学习预测\",\"x\":740,\"y\":130,\"config\":{\"threshold\":0.5}},{\"nodeId\":\"eval\",\"compId\":\"biclassification_eval\",\"label\":\"二分类评估\",\"x\":960,\"y\":130,\"config\":{\"metrics\":[\"auc\",\"accuracy\",\"precision\",\"recall\"]}},{\"nodeId\":\"write\",\"compId\":\"write_table\",\"label\":\"写入数据表\",\"x\":1180,\"y\":130,\"config\":{\"output_datasource_id\":\"ds-hosp\",\"output_table\":\"fl_prediction_result\",\"write_mode\":\"overwrite\"}}],\"edges\":[{\"from\":\"read_hosp\",\"to\":\"psi\",\"fromPort\":0,\"toPort\":0},{\"from\":\"read_ins\",\"to\":\"psi\",\"fromPort\":0,\"toPort\":1},{\"from\":\"psi\",\"to\":\"train\",\"fromPort\":0,\"toPort\":0},{\"from\":\"train\",\"to\":\"predict\",\"fromPort\":0,\"toPort\":0},{\"from\":\"predict\",\"to\":\"eval\",\"fromPort\":0,\"toPort\":0},{\"from\":\"eval\",\"to\":\"write\",\"fromPort\":0,\"toPort\":0}],\"description\":\"联邦学习训练与预测\"}"
    }
  }'
```

## 任务5：医院-保险纵向联邦学习任务

### API 调用

```bash
curl -X POST "http://localhost:8092/api/v1/msp/tasks/save" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "name": "医院-保险纵向联邦学习任务",
    "type": "COMPONENT_DAG",
    "algorithm": "component_dag",
    "participants": ["node-hospital", "node-insurance"],
    "parameters": {
      "dag_definition": "{\"name\":\"医院-保险纵向联邦学习任务\",\"nodes\":[{\"nodeId\":\"read_hosp\",\"compId\":\"read_table\",\"label\":\"读取数据表\",\"x\":80,\"y\":60,\"config\":{\"datasource_id\":\"ds-hosp\",\"table_name\":\"patient_stats\"}},{\"nodeId\":\"read_ins\",\"compId\":\"read_table\",\"label\":\"读取数据表\",\"x\":80,\"y\":220,\"config\":{\"datasource_id\":\"ds-insurance\",\"table_name\":\"claims\"}},{\"nodeId\":\"vertical_fl\",\"compId\":\"vertical_fl\",\"label\":\"纵向联邦学习\",\"x\":320,\"y\":140,\"config\":{\"model_type\":\"secureboost\",\"num_trees\":10,\"max_depth\":6,\"learning_rate\":0.1,\"label_party\":\"node-insurance\",\"label_column\":\"default_flag\"}},{\"nodeId\":\"write\",\"compId\":\"write_table\",\"label\":\"写入数据表\",\"x\":560,\"y\":140,\"config\":{\"output_datasource_id\":\"ds-insurance\",\"output_table\":\"vertical_fl_result\",\"write_mode\":\"overwrite\"}}],\"edges\":[{\"from\":\"read_hosp\",\"to\":\"vertical_fl\",\"fromPort\":0,\"toPort\":0},{\"from\":\"read_ins\",\"to\":\"vertical_fl\",\"fromPort\":0,\"toPort\":1},{\"from\":\"vertical_fl\",\"to\":\"write\",\"fromPort\":0,\"toPort\":0}],\"description\":\"医院与保险公司纵向联邦学习\"}"
    }
  }'
```

## 节点配置字段说明

### read_table
```json
{
  "datasource_id": "ds-hosp",
  "table_name": "patients",
  "columns": ["user_id", "name", "gender", "age"],
  "limit": 1000
}
```

### psi
```json
{
  "key_column": "user_id",
  "psi_type": "ecdh"
}
```
- psi_type: ecdh, kkrt, bc22

### filter_null
```json
{
  "columns": ["user_id", "name", "id_card_hash"]
}
```

### filter_duplicate
```json
{
  "key_column": "id_card_hash"
}
```

### binning
```json
{
  "column": "age",
  "num_bins": 5
}
```

### write_table
```json
{
  "output_datasource_id": "ds-hosp",
  "output_table": "result_table",
  "write_mode": "overwrite"
}
```

## 任务状态说明

- CREATED: 已创建，未执行
- PENDING: 等待执行
- RUNNING: 执行中
- COMPLETED: 执行完成
- FAILED: 执行失败

## 执行任务

将任务状态从 CREATED 改为 PENDING 并开始执行：

```bash
curl -X POST "http://localhost:8092/api/v1/msp/tasks/{taskId}/execute" \
  -H "Authorization: Bearer $TOKEN"
```