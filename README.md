# Campus Classroom Reservation System

# 项目说明文档

## 一、项目背景

本项目是一个面向校园场景的教室预约系统，支持学生进行**座位级预约**，教师进行**整间教室预约**，用于解决以下问题：

- 自习室/教室资源利用不均
- 多人预约冲突（同一时间重复占用）
- 权限混乱（学生误占整间教室）
- 缺乏统一管理与状态控制

系统核心目标是实现一个**具备权限控制 + 冲突检测 + 状态管理的预约系统后端服务**。

------

## 二、技术栈

| 类别     | 技术                  |
| -------- | --------------------- |
| 后端框架 | Spring Boot           |
| 安全认证 | Spring Security + JWT |
| 持久层   | MyBatis               |
| 数据库   | MySQL                 |
| 参数校验 | Jakarta Validation    |
| 构建工具 | Maven                 |

------

## 三、系统架构

系统采用经典三层架构：

```
Controller → Service → Mapper → Database
```

同时引入以下模块：

- `security`：JWT 认证与权限控制
- `dto / vo`：数据传输与返回封装
- `exception`：统一异常处理
- `config`：系统配置

------

## 四、核心功能模块

### 1. 用户认证与授权

- 用户注册 / 登录
- JWT Token 鉴权
- 获取当前用户信息（/me）
- 基于角色的权限控制（学生 / 管理员）

------

### 2. 教室管理

- 教室 CRUD
- 教室容量、状态管理
- 支持启用 / 禁用

------

### 3. 座位管理

- 按教室生成座位（seatRows × seatCols）
- 控制座位状态（可用 / 禁用）
- 支持按教室查询座位

------

### 4. 预约系统（核心模块）

#### 支持两种预约类型：

1. 学生 → 座位预约
2. 教师 → 整间教室预约

------

## 五、核心业务设计（重点）

### 1. 预约冲突控制

系统设计了多层冲突校验机制：

#### （1）时间冲突

- 同一用户在同一时间段只能有一个预约
- 禁止预约过去时间

#### （2）资源冲突

- 同一座位同一时间不可重复预约
- 教室整间预约时：
  - 禁止座位预约
- 座位预约存在时：
  - 禁止整间教室预约

实现思路：

- 查询同一用户下已有的预约记录
- 使用数据库锁（如 `FOR UPDATE`）防止多用户并发冲突
- 在 Service 层统一校验

------

### 2. 权限控制

| 角色        | 权限          |
| ----------- | ------------- |
| 学生        | 座位预约      |
| 教师/管理员 | 教室预约      |
| 管理员      | 教室/座位管理 |

使用 Spring Security 实现：

- JWT 解析用户身份
- 基于角色控制接口访问

------

### 3. 状态管理

预约状态设计：

- ACTIVE（有效）
- CANCELLED（已取消）
- EXPIRED（已过期）

用于：

- 防止重复取消
- 历史记录管理
- 查询优化

------

## 六、数据库设计

核心表：

### user

- id
- username
- password
- nickname
- email
- role
- status

### classroom

- id
- room_number
- building
- seat_rows
- seat_cols
- status
- remark

### seat

- id
- classroom_id
- seat_number
- row_number
- col_number
- status
- remark

### reservation

- id
- user_id
- resource_type
- resource_id
- classroom_id
- start_time
- end_time
- reason
- status

------

## 七、接口示例

### 登录

```
POST /auth/login
```

返回：

```
{
  "code": 200,
  "message": "登录成功",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMDAwNCIsInVzZXJuYW1lIjoiMjMzIiwicm9sZSI6IlNUVURFTlQiLCJpYXQiOjE3NzU4MDQ0MDgsImV4cCI6MTc3NTg5MDgwOH0.OA7gyuHm0iBI4iU9Te_clRVIhwsLXavX_NDHNhgxqjQ",
    "userInfo": {
      "id": 10004,
      "username": "233",
      "nickname": "233",
      "email": "223@qq.com",
      "role": "STUDENT"
    }
  }
}
```

------

### 获取当前用户

```
GET /auth/me
Authorization: Bearer xxx
```

------

### 创建预约

```
POST /reservations/classroom
```

参数：

```
{
  "classroom_id": "6",
  "start_time": "2026-04-10T18:42:06",
  "end_time": "2026-04-10T23:52:20"
}
```

返回:

```
{
  "code": 200,
  "message": "预约教室成功",
  "data": 43
}
```

____

## 八、项目亮点

✅ 1. 真实业务场景
 不只是 CRUD，而是包含**资源竞争与冲突控制**的系统

✅ 2. 权限体系完整
 JWT + Spring Security，实现角色隔离

✅ 3. 冲突控制设计

- 时间冲突
- 资源冲突
- 角色冲突

✅ 4. 分层清晰
 Controller / Service / Mapper 职责明确

✅ 5. 可扩展性
 支持后续扩展：

- Redis 缓存
- 分布式锁
- 消息队列（预约提醒）

------

## 九、TODO

- 暂未实现高并发优化（如 Redis / 分布式锁）
- 冲突控制主要在数据库层，存在性能瓶颈
- 测试覆盖率不足（缺少核心业务测试）
- 未实现接口文档（Swagger）
- 未部署线上环境

后续优化方向：

- 引入 Redis 做预约缓存
- 使用乐观锁 / 分布式锁优化并发
- 增加单元测试与集成测试
- 增加接口文档与部署脚本

------

## 十、运行项目

```
# 1. 克隆项目
git clone https://github.com/UmiK233/CampusClassroomReservationSystem

# 2. 配置数据库
修改 application.yaml

# 3. 启动项目
mvn spring-boot:run
```