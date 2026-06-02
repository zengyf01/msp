# DAG设计器模块

## TC-DAG-001: 简单 DAG

**测试目标**: 验证简单 DAG 创建和执行（PSI + 训练）

**前置条件**: 已启动前端服务，DAG 设计器页面可访问

**测试步骤**:
1. 打开 DAG 设计器页面
2. 从组件面板拖拽 `read_table` 到画布
3. 拖拽 `psi` 组件到画布
4. 拖拽 `ss_glm_train` 组件到画布
5. 拖拽 `write_table` 组件到画布
6. 连接节点形成 DAG
7. 配置各节点参数
8. 点击"预览 DAG"查看执行顺序
9. 点击"执行 DAG"

**请求**
```bash
POST /api/v1/msp/tasks
Content-Type: application/json
Authorization: Bearer <token>

{
  "name": "DAG Task Test",
  "type": "COMPONENT_DAG",
  "algorithm": "dag",
  "participants": ["alice", "bob"],
  "parameters": {
    "dag_definition": "read_table->psi->ss_glm_train"
  }
}
```

**预期结果**:
- DAG 正确显示各节点和连接
- 执行顺序正确（拓扑排序）
- 执行成功

**执行结果**: ✅ 任务创建成功 (2026-05-25)

---

## TC-DAG-007: read_table 组件数据源选择

**测试目标**: 验证 `read_table` 节点能从下拉框正常选定数据源

**前置条件**: 已注册至少一个数据源（`/datasources` 列表非空）

**测试步骤**:
1. 打开 DAG 设计器
2. 拖拽 `read_table` 到画布，点击该节点打开配置面板
3. 在"数据源"下拉框中观察选项列表
4. 选择某个数据源
5. 验证"数据表"下拉框根据所选数据源动态加载
6. 选定表后验证"选择列"下拉框加载列名

**预期结果**:
- 数据源下拉框正确列出全部已注册数据源（label 为名称，value 为 ID）
- 选择后 `v-model` 写入正确的 `dataSourceId`
- 表/列下拉框联动刷新（`getDataSourceTables` / `getDataSourceColumns` 成功返回）

**执行结果**: ✅ 已修复 (2026-06-02)
- 修复文件：`msp-frontend/src/views/components/NodeConfigPanel.vue:31,33,129,131`
- 修复内容：`ds.datasourceId` → `ds.dataSourceId`（与后端 `getDataSourceId()` 驼峰命名一致）
- 同步修复 `write_table` 组件的目标数据源下拉框

---

## TC-DAG-002: 三方 PSI

**测试目标**: 验证使用 psi_tp 组件进行三方 PSI

**前置条件**: 已启动三方节点

**测试步骤**:
1. 在 DAG 设计器中添加 `psi_tp` 组件
2. 配置 key_column 参数
3. 添加三个数据输入节点连接到 psi_tp
4. 执行 DAG

**预期结果**:
- 三方 PSI 正确执行
- 返回正确的交集数量

**执行结果**: ⏳ 待集成测试 (psi_tp 组件代码已完成)

---

## TC-DAG-003: 完整工作流

**测试目标**: 验证完整工作流（数据输入→预处理→训练→预测→评估）

**前置条件**: DAG 设计器可用

**测试步骤**:
1. 创建完整 DAG：
   - read_table (数据输入)
   - binning (预处理)
   - sgb_train (训练)
   - sgb_predict (预测)
   - biclassification_eval (评估)
2. 配置各组件参数
3. 执行 DAG

**预期结果**:
- 完整流程正确执行
- 返回评估指标

**执行结果**: ⏳ 待集成测试 (组件代码已完成)

---

## TC-DAG-004: DAG 保存与加载

**测试目标**: 验证 DAG 的保存和加载功能

**前置条件**: DAG 设计器可用

**测试步骤**:
1. 创建 DAG 并配置参数
2. 点击"保存 DAG"
3. 刷新页面
4. 打开已保存的 DAG

**预期结果**:
- DAG 正确保存
- 加载后恢复所有配置

**执行结果**: ⏳ 待前端实现 (后端 API 已就绪)

---

## TC-DAG-005: 复合任务 - PSI→FL→MPC

**测试目标**: 验证三阶段复合任务的执行

**前置条件**: 所有节点服务正常运行

**测试步骤**:
1. 创建复合任务
2. 第一阶段：PSI 样本对齐
3. 第二阶段：FL 联合建模
4. 第三阶段：MPC 综合评分
5. 执行复合任务

**请求**
```bash
POST /api/v1/msp/tasks
Content-Type: application/json
Authorization: Bearer <token>

{
  "name": "Compound Flow Test",
  "type": "COMPOUND_TASK",
  "algorithm": "compound",
  "participants": ["alice", "bob"],
  "parameters": {
    "stages": "psi,fl,mpc"
  }
}
```

**预期结果**:
- 三阶段依次执行
- 每阶段结果正确传递

**执行结果**: ✅ 任务创建成功 (2026-05-25)

---

## TC-DAG-006: 组件类型校验

**测试目标**: 验证 DAG 设计器对组件连接的类型校验

**前置条件**: DAG 设计器页面可访问

**测试步骤**:
1. 打开 DAG 设计器页面
2. 添加 read_table 组件和 sgb_predict 组件
3. 尝试连接 read_table 输出到 sgb_predict 输入
4. 验证是否出现警告阻止非法连接

**预期结果**:
- sgb_predict 只接受来自 sgb_train 的模型输出
- 系统显示警告信息阻止非法连接

**执行结果**: ✅ 已修复 (2026-05-25)

---

## 汇总

| 用例ID | 名称 | 优先级 | 执行结果 |
|--------|------|--------|----------|
| TC-DAG-001 | 简单 DAG | P0 | ✅ 任务创建成功 (2026-05-25) |
| TC-DAG-002 | 三方 PSI | P0 | ⏳ 待集成测试 |
| TC-DAG-003 | 完整工作流 | P1 | ⏳ 待集成测试 |
| TC-DAG-004 | DAG 保存与加载 | P1 | ⏳ 待前端实现 |
| TC-DAG-005 | 复合任务 | P0 | ✅ 任务创建成功 (2026-05-25) |
| TC-DAG-006 | 组件类型校验 | P0 | ✅ 已修复 (2026-05-25) |
| TC-DAG-007 | read_table 数据源选择 | P0 | ✅ 已修复 (2026-06-02) |
