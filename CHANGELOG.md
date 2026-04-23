# Changelog

## [v0.3.0] - 2026-04-21

### ✨ 新增功能

* 新增每日预约总时长限制

### 🔄 更新

* 单次预约时长上限设置为 3 小时
* 每日预约上限设置为 9 小时
* 增加reservation_usage表记录预约时长和日期
* 管理端新增用户管理、预约管理和站内通知能力

### 🐛 修复

* 修复跨天预约未校验的问题
* 修复并发穿透问题:

同一个用户同时发两个请求：

```
请求A：查 → 当前 6h → 可以预约
请求B：查 → 当前 6h → 也可以预约

A 插入 +3h → 总 9h
B 插入 +3h → 总 12h （超了）
```

___

## [v0.4.0] - 2026-04-22

### ✨ 新增功能

* 新增前端页面

### 🐛 修复

* 修复取消预约未返还预约配额的问题

___

## [v0.5.0] - 2026-04-23

### ✨ 新增功能

* 新增管理员单独操作座位的接口

### 🔄 更新

* 修改接口语义,使其更符合RESTFul风格:

```
POST /admin/classrooms/{classroom_id}/seats/init -> POST /admin/classrooms/{id}/seats
PUT /admin/classrooms/{classroom_id}/seats/status -> PUT /admin/classrooms/{classroom_id}/seats
```

* 修改Service层接口命名,使其更符合语义:

```
batchUpdateSeatStatus -> batchUpdateSeats
```

* 添加新接口:

```
batchDeleteSeats

```

___

## [v0.6.0] - 2026-04-23

### ✨ 新增功能

* 新增管理员对用户和预约的管理接口:

```aiignore
GET /admin/users -> 获取用户列表
PUT /admin/users/{id}/status -> 修改用户状态（启用/禁用）
GET /admin/reservations -> 获取预约列表
DELETE /admin/reservations//{id} -> 删除预约
```

* 新增站内通知接口
* 后端新增 GET /classrooms/preferred_buildings
* 它按当前登录用户的预约历史，基于 reservation.classroom_id -> classroom.building 聚合统计教学楼使用次数
* 前端预约页不再自己拉 reservations + history 拼楼偏好，而是直接用这个接口做：
    * 教学楼默认选中
    * 教学楼下拉排序
    * 教室列表排序参考

### 🐛 修复

* 优化前端页面默认不选择预约日期和楼层的问题
* 管理员初始化教室座位的接口:initSeats的语义为“补齐缺失座位”：
    * 已有座位保持不动，只把教室行列范围内缺的座位补出来