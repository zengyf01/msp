// 任务状态
export enum TaskStatus {
  CREATED = 'CREATED',
  PENDING = 'PENDING',
  RUNNING = 'RUNNING',
  COMPLETED = 'COMPLETED',
  FAILED = 'FAILED',
  CANCELLED = 'CANCELLED'
}

// 任务类型
export enum TaskType {
  PSI = 'PSI',
  MPC = 'MPC',
  FEDERATED_LEARNING = 'FEDERATED_LEARNING',
  CUSTOM_CODE = 'CUSTOM_CODE',
  VERTICAL_FL = 'VERTICAL_FL',
  COMPOUND_TASK = 'COMPOUND_TASK'
}

// 节点状态
export enum NodeStatus {
  ONLINE = 'ONLINE',
  OFFLINE = 'OFFLINE',
  BUSY = 'BUSY',
  MAINTAIN = 'MAINTAIN'
}

// 设备类型
export enum DeviceType {
  PYU = 'PYU',
  SPU = 'SPU',
  HEU = 'HEU',
  TEEU = 'TEEU'
}

// 数据源类型
export enum DataSourceType {
  MYSQL = 'MYSQL',
  POSTGRESQL = 'POSTGRESQL',
  API = 'API',
  FILE = 'FILE'
}

// 数据源
export interface DataSource {
  dataSourceId?: string;
  nodeId?: string;
  name?: string;
  type: DataSourceType;
  host?: string;
  port?: number;
  database?: string;
  username?: string;
  password?: string;
  tableName?: string;
  columns?: string[];
}

// 数据源请求
export interface DataSourceRequest {
  nodeId: string;
  name: string;
  type: DataSourceType;
  host?: string;
  port?: number;
  database?: string;
  username?: string;
  password?: string;
  tableName?: string;
  columns?: string[];
}

// 数据源创建响应
export interface DataSourceCreateResponse {
  datasourceId: string;
}

// 数据库表信息（name + 来自 INFORMATION_SCHEMA.TABLES.TABLE_COMMENT 的中文注释）
export interface TableInfo {
  name: string;
  comment: string;
}

// 数据库字段信息（name + 来自 INFORMATION_SCHEMA.COLUMNS.COLUMN_COMMENT 的中文注释）
export interface ColumnInfo {
  name: string;
  comment: string;
}

// 连接测试响应
export interface ConnectionTestResponse {
  success: boolean;
  message: string;
}

// 任务
export interface Task {
  taskId: string;
  name: string;
  type: TaskType;
  algorithm?: string;
  status: TaskStatus;
  nodeMode?: string;      // 节点模式: ray 或 kuscia
  participants?: string[];
  inputs?: Record<string, DataSource>;
  parameters?: Record<string, string>;
  description?: string;
  code?: string;        // 任务代码/DAG规格
  result?: string;      // 任务执行结果
  createTime: number;
  updateTime: number;
}

// MPC计算类型
export enum MpcType {
  ADDITION = 'addition',
  MULTIPLICATION = 'multiplication',
  COMPARISON = 'comparison'
}

// 任务请求
export interface TaskRequest {
  name: string;
  type: TaskType;
  algorithm?: string;
  participants?: string[];
  inputs?: Record<string, DataSource>;
  parameters?: Record<string, string>;
  description?: string;
  mpcType?: MpcType;
  // 纵向联邦学习字段
  labelParty?: string;
  labelColumn?: string;
  modelType?: 'logistic_regression' | 'secureboost';
  featureParties?: Record<string, string[]>;
  numTrees?: number;
  maxDepth?: number;
  // PSI 协议
  psiProtocol?: 'ecdh' | 'kkrt' | 'bc22' | 'unbalanced';
}

// 节点
export interface Node {
  nodeId: string;
  nodeName: string;
  status: NodeStatus;
  nodeMode?: 'RAY' | 'KUSCIA';
  endpoint?: string;
  externalEndpoint?: string;
  capabilities?: DeviceType[];
  tags?: string[];
  createTime?: number;
  updateTime?: number;
}

// 节点注册请求
export interface NodeRegisterRequest {
  nodeId: string;
  nodeName: string;
  nodeMode?: 'RAY' | 'KUSCIA';
  capabilities: DeviceType[];
  endpoint: string;
  externalEndpoint?: string;
  tags?: string[];
}

// API 响应
export interface ApiResponse<T> {
  success: boolean;
  code: string;
  message: string;
  data: T;
  timestamp: number;
}

// 分页
export interface Page<T> {
  content: T[];
  total: number;
  page: number;
  size: number;
}

// 任务创建响应
export interface TaskCreateResponse {
  taskId: string;
  status: TaskStatus;
}

// 任务状态响应
export interface TaskStatusResponse {
  taskId: string;
  status: TaskStatus;
  progress?: number;
}

// 任务结果响应
export interface TaskResultResponse {
  taskId: string;
  status: TaskStatus;
  result?: any;
}

// 任务执行过程响应（含实际执行日志和 DAG 定义）
export interface TaskExecutionResponse {
  taskId: string;
  status: TaskStatus;
  nodeMode?: string;
  // 数组字符串，结构：[{ts, stage, level, message, nodeId?, role?, dagNodeId?, compId?, label?, durationMs?}, ...]
  executionLog: string;
  // DAG 定义的 JSON 字符串
  dagDefinition?: string;
}

// 节点注册响应
export interface NodeRegisterResponse {
  nodeId: string;
  status: NodeStatus;
}

// 用户角色
export enum UserRole {
  ADMIN = 'ADMIN',
  NODE_ADMIN = 'NODE_ADMIN',
  USER = 'USER'
}

// 用户
export interface User {
  userId: string;
  username: string;
  role: UserRole;
  enabled?: boolean;
  createTime?: number;
  updateTime?: number;
}

// 登录请求
export interface LoginRequest {
  username: string;
  password: string;
}

// 登录响应
export interface LoginResponse {
  token: string;
  userId: string;
  username: string;
}

// 审计日志
export interface AuditLog {
  logId: string;
  userId?: string;
  action: string;
  resourceType?: string;
  resourceId?: string;
  details?: Record<string, any>;
  ipAddress?: string;
  createTime: number;
}

// ========== 用户、角色、权限模块 ==========

// 用户状态
export enum UserStatus {
  ACTIVE = 'ACTIVE',
  LOCKED = 'LOCKED',
  DISABLED = 'DISABLED'
}

// 用户
export interface User {
  userId?: string;
  username?: string;
  email?: string;
  phone?: string;
  role?: UserRole;
  status?: UserStatus;
  enabled?: boolean;
  createTime?: number;
  updateTime?: number;
}

// 用户创建请求
export interface UserCreateRequest {
  username: string;
  password: string;
  email?: string;
  phone?: string;
  role?: UserRole;
}

// 用户更新请求
export interface UserUpdateRequest {
  email?: string;
  phone?: string;
  role?: UserRole;
  enabled?: boolean;
}

// 角色状态
export enum RoleStatus {
  ACTIVE = 'ACTIVE',
  DISABLED = 'DISABLED'
}

// 角色
export interface Role {
  roleId?: string;
  roleCode?: string;
  roleName?: string;
  description?: string;
  status?: RoleStatus;
  permissions?: string[];
  createTime?: number;
  updateTime?: number;
}

// 角色创建请求
export interface RoleCreateRequest {
  roleCode: string;
  roleName: string;
  description?: string;
  status?: RoleStatus;
  permissions?: string[];
}

// 权限资源类型
export enum ResourceType {
  MENU = 'MENU',
  BUTTON = 'BUTTON',
  DATA = 'DATA'
}

// 权限
export interface Permission {
  permissionId?: string;
  permissionCode?: string;
  permissionName?: string;
  resourceType?: ResourceType;
  parentId?: string;
  path?: string;
  icon?: string;
  sortOrder?: number;
  children?: Permission[];
}

// 当前用户信息（包含角色）
export interface CurrentUser {
  userId: string;
  username: string;
  role: string;
  permissions?: string[];
}