# 酒店客房预订系统 接口设计文档（API）



| 项目       | 内容                                |
| -------- | --------------------------------- |
| 文档名称     | 接口设计文档（API Document）              |
| 文档版本     | v1.0                              |
| 更新日期     | 2026-09-21                        |
| 风格       | RESTful + JSON（对应手册设计与架构类・API 文档） |
| Base URL | `http://{host}:8080/api`          |

> 后续建议接入 
>
> **springdoc-openapi**
>
> ，启动后访问 
>
> `/swagger-ui.html`
>
>  自动生成可调试文档（对应手册 "Swagger 自动生成"）。



***

## 1. 通用约定

### 1.1 通信与数据格式



* 请求 / 响应均使用 `application/json; charset=UTF-8`；

* 日期：`yyyy-MM-dd`（如 `2026-09-25`）；日期时间：ISO `yyyy-MM-ddTHH:mm:ss`；金额：数字，保留两位小数，单位元；

* 分页请求参数：`page`（从 1 开始，默认 1）、`size`（默认 10，最大 100）。

### 1.2 鉴权



* 登录成功后返回 `token`；受保护接口在请求头携带：`Authorization: Bearer <token>`；

* 角色：`MEMBER`（会员）、`RECEPTIONIST`（前台）、`ADMIN`（管理员）；

* 未带 / 令牌失效返回 **401**；令牌有效但角色不足返回 **403**。

### 1.3 统一响应结构（所有接口、包括异常都返回此结构）



```
{ "code": 0, "message": "success", "data": { } }
```

分页时 `data` 为：



```
{ "records": \[ ], "total": 100, "page": 1, "size": 10 }
```

> HTTP 状态码表达协议层（401/403/404/400/500），业务 
>
> `code`
>
>  表达业务结果；业务规则不通过时 HTTP 一般为 200、
>
> `code`
>
>  为对应业务码。

### 1.4 错误码对照表



| code | 含义            | 典型触发              |
| ---- | ------------- | ----------------- |
| 0    | 成功            | -                 |
| 1000 | 参数校验失败        | `@Valid` 不通过、日期非法 |
| 1001 | 未认证（401）      | 未登录、token 过期      |
| 1002 | 无权限（403）      | 会员访问前台接口          |
| 1003 | 资源不存在（404）    | 路径资源不存在           |
| 1005 | 系统繁忙（500）     | 未预期异常（不向前端泄露堆栈）   |
| 2001 | 手机号已注册        | 注册重复              |
| 2002 | 手机号或密码错误      | 登录失败              |
| 2003 | 账号已停用         | status=0          |
| 3001 | 房型不存在或已下架     | 下单 / 查询下架房型       |
| 3002 | 房间不存在         | 排房传错房间            |
| 3003 | 房间当前状态不可排房    | 排到 OOO/OC 房       |
| 4001 | 日期参数非法        | 离店≤入住、夜数 < 1      |
| 4002 | 所选日期库存不足      | 并发超卖拦截            |
| 4003 | 订单不存在         | 单号错误              |
| 4004 | 当前订单状态不允许该操作  | 非法状态流转            |
| 4005 | 订单已超时取消       | 支付已过期订单           |
| 4006 | 排房冲突：房间该区间已占用 | 区间重叠              |
| 4007 | 已超出免费取消时限     | 过时限取消             |
| 5001 | 支付失败          | 渠道失败              |
| 5003 | 重复回调（已处理，幂等）  | 支付渠道重复通知          |



***

## 2. 接口总览



| #  | 方法   | 路径                                    | 权限            | 说明            |
| -- | ---- | ------------------------------------- | ------------- | ------------- |
| 1  | GET  | /health                               | 公开            | 健康检查          |
| 2  | POST | /auth/register                        | 公开            | 会员注册          |
| 3  | POST | /auth/login                           | 公开            | 登录（会员 / 员工）   |
| 4  | GET  | /auth/me                              | 登录            | 当前用户信息        |
| 5  | GET  | /room-types                           | 公开            | 房型列表（可按关键字）   |
| 6  | GET  | /room-types/{id}                      | 公开            | 房型详情          |
| 7  | GET  | /availability                         | 公开            | 按日期区间查询可售与总价  |
| 8  | POST | /reservations                         | MEMBER        | 创建订单（事务扣库存）   |
| 9  | GET  | /reservations/{orderNo}               | 登录            | 订单详情          |
| 10 | GET  | /reservations/mine                    | MEMBER        | 我的订单（分页）      |
| 11 | POST | /reservations/{orderNo}/cancel        | MEMBER        | 取消订单（回滚库存）    |
| 12 | POST | /payments                             | MEMBER        | 发起支付（Mock）    |
| 13 | POST | /payments/callback/{channel}          | 内部            | 支付结果回调（幂等）    |
| 14 | GET  | /pms/rooms/board                      | RECEPTIONIST+ | 房态盘           |
| 15 | POST | /pms/reservations/{orderNo}/check-in  | RECEPTIONIST+ | 排房并入住         |
| 16 | POST | /pms/reservations/{orderNo}/check-out | RECEPTIONIST+ | 退房结账          |
| 17 | PUT  | /pms/rooms/{id}/status                | RECEPTIONIST+ | 修改房态（清洁 / 维修） |
| 18 | POST | /admin/room-types                     | ADMIN         | 新增房型          |
| 19 | PUT  | /admin/room-types/{id}                | ADMIN         | 修改房型          |
| 20 | POST | /admin/rooms                          | ADMIN         | 新增房间          |
| 21 | POST | /admin/inventory/init                 | ADMIN         | 初始化 / 补齐日期库存  |
| 22 | GET  | /admin/room-types                     | ADMIN         | 后台房型列表        |
| 23 | GET  | /admin/rooms                          | ADMIN         | 后台房间列表        |



***

## 3. 认证模块

### 3.1 注册 `POST /auth/register`

请求体：



```
{ "phone": "13900000002", "password": "123456", "name": "张先生" }
```



| 参数       | 类型     | 必填 | 说明      |
| -------- | ------ | -- | ------- |
| phone    | string | 是  | 11 位手机号 |
| password | string | 是  | 6\~32 位 |
| name     | string | 否  | 昵称      |

成功响应 `data`：



```
{ "userId": 4, "phone": "13900000002", "role": "MEMBER" }
```

失败：`2001 手机号已注册`。

### 3.2 登录 `POST /auth/login`

请求体：`{ "phone": "13800000000", "password": "admin123" }`

成功响应：



```
{

&#x20; "token": "eyJ1c2VySWQiOjEsInJvbGUiOiJBRE1JTiJ9.xxxx.sig",

&#x20; "userId": 1, "name": "系统管理员", "role": "ADMIN", "expiresIn": 7200

}
```

失败：`2002 手机号或密码错误`、`2003 账号已停用`。

### 3.3 当前用户 `GET /auth/me`（需登录）



```
{ "code": 0, "message": "success",

&#x20; "data": { "userId": 1, "phone": "13800000000", "name": "系统管理员", "role": "ADMIN" } }
```



***

## 4. 客房与可售查询

### 4.1 房型列表 `GET /room-types?keyword=&page=1&size=10`

仅返回上架房型，按 `sortOrder` 排序。`data.records` 元素：



```
{ "id": 1, "code": "STD", "name": "标准大床房", "bedType": "大床1.8m",

&#x20; "maxOccupancy": 2, "area": 28.00, "basePrice": 268.00,

&#x20; "amenities": \["wifi","空调","独立卫浴","电视"], "coverImage": null }
```

### 4.2 房型详情 `GET /room-types/1`：返回单个房型完整字段。

### 4.3 可售查询 `GET /availability?checkIn=2026-09-25&checkOut=2026-09-27`



| 参数       | 类型   | 必填 | 说明         |
| -------- | ---- | -- | ---------- |
| checkIn  | date | 是  | 入住日期       |
| checkOut | date | 是  | 离店日期（> 入住） |

响应（`minAvailable` 为区间内**每夜最小可售间数**，`totalPrice` 为一间住 N 晚总价）：



```
{ "code": 0, "message": "success", "data": \[

&#x20; { "roomTypeId": 1, "code": "STD", "name": "标准大床房", "bedType": "大床1.8m",

&#x20;   "basePrice": 268.00, "nights": 2, "minAvailable": 5, "totalPrice": 536.00,

&#x20;   "amenities": \["wifi","空调"] },

&#x20; { "roomTypeId": 4, "code": "SUI", "name": "商务套房",

&#x20;   "basePrice": 688.00, "nights": 2, "minAvailable": 1, "totalPrice": 1376.00 }

]}
```

任一夜无房则该房型 `minAvailable=0`，前端置灰 "不可订"。



***

## 5. 预订订单模块

### 5.1 创建订单 `POST /reservations`（MEMBER）

请求体（`items` 按 "房型 + 间数"，后端展开为一行一间并逐夜扣库存）：



```
{

&#x20; "contactName": "王女士",

&#x20; "contactPhone": "13900000001",

&#x20; "checkInDate": "2026-09-25",

&#x20; "checkOutDate": "2026-09-27",

&#x20; "remark": "需要无烟房",

&#x20; "items": \[ { "roomTypeId": 1, "quantity": 1 }, { "roomTypeId": 4, "quantity": 1 } ]

}
```



| 参数                         | 类型     | 必填 | 说明       |
| -------------------------- | ------ | -- | -------- |
| contactName                | string | 是  | 入住联系人    |
| contactPhone               | string | 是  | 联系手机     |
| checkInDate / checkOutDate | date   | 是  | 离店必须晚于入住 |
| items\[].roomTypeId        | long   | 是  | 房型       |
| items\[].quantity          | int    | 是  | 间数，≥1    |
| remark                     | string | 否  | 备注       |

成功响应（状态 `PENDING_PAYMENT`，含支付截止时间）：



```
{ "code": 0, "message": "success", "data": {

&#x20; "orderNo": "H20260921001", "status": "PENDING\_PAYMENT",

&#x20; "checkInDate": "2026-09-25", "checkOutDate": "2026-09-27", "nights": 2,

&#x20; "totalAmount": 1912.00, "expireAt": "2026-09-21T10:15:00",

&#x20; "items": \[

&#x20;   { "roomTypeId": 1, "roomTypeName": "标准大床房", "unitPrice": 268.00, "nights": 2, "subtotal": 536.00 },

&#x20;   { "roomTypeId": 4, "roomTypeName": "商务套房",   "unitPrice": 688.00, "nights": 2, "subtotal": 1376.00 }

&#x20; ] } }
```

失败：`4001 日期参数非法`、`3001 房型不存在或已下架`、`4002 所选日期库存不足`（整单回滚，不产生任何扣减）。

### 5.2 订单详情 `GET /reservations/{orderNo}`（登录，会员仅能查本人，前台可查全部）

返回订单字段 + `items`（排房后含 `roomId`/`roomNo`）。

### 5.3 我的订单 `GET /reservations/mine?page=1&size=10&status=`：分页返回当前会员订单，按创建时间倒序。

### 5.4 取消订单 `POST /reservations/{orderNo}/cancel`（MEMBER）



* 免费取消时限内：已支付走退款、未支付直接取消；**同一事务逐夜回滚库存**；

* 响应返回最新订单状态 `CANCELLED`（或 `REFUNDING`）；

* 失败：`4004 当前订单状态不允许该操作`、`4007 已超出免费取消时限`。



***

## 6. 支付模块（MVP 为 Mock，真实渠道预留）

### 6.1 发起支付 `POST /payments`（MEMBER）

请求：`{ "orderNo": "H20260921001", "channel": "WECHAT" }`

响应（Mock 返回模拟支付参数与 "模拟完成回调" 地址）：



```
{ "code": 0, "data": { "paymentNo": "P20260921001", "orderNo": "H20260921001",

&#x20; "amount": 1912.00, "channel": "WECHAT", "status": "INIT",

&#x20; "payUrl": "MOCK: 实际为微信预支付参数", "mockCallbackUrl": "/api/payments/callback/WECHAT?paymentNo=P20260921001" } }
```

### 6.2 支付回调 `POST /payments/callback/{channel}?paymentNo=...&transactionId=...`（内部 / 渠道）



* 依据 `transactionId` 唯一约束与订单状态做**幂等**：重复通知返回 `5003` 或直接返回成功不重复处理；

* 处理成功：支付流水置 `SUCCESS`、订单 `CONFIRMED`、记录 `paidAt`；

* 真实接入时由微信 / 支付宝服务端回调，校验签名后复用本服务逻辑。



***

## 7. 前台接待 PMS（RECEPTIONIST / ADMIN）

### 7.1 房态盘 `GET /pms/rooms/board?floor=8`



```
{ "code": 0, "data": \[

&#x20; { "roomId": 1, "roomNo": "8101", "floor": 8, "roomTypeId": 1,

&#x20;   "roomTypeName": "标准大床房", "status": "VC", "orderNo": null }

]}
```

### 7.2 排房并入住 `POST /pms/reservations/{orderNo}/check-in`

请求体（为订单中 "一行一间" 的每个明细分配具体房间）：



```
{ "assignments": \[ { "itemId": 1, "roomId": 1 }, { "itemId": 2, "roomId": 12 } ] }
```

服务端校验：订单为 `CONFIRMED`；房间与明细房型一致；房间为 `VC`；与在住 / 已确认订单**区间不重叠**。成功后：回填 `room_id`、相关房间置 `OC`、订单置 `CHECKED_IN`、记录入住时间。

失败：`4004 状态不允许`、`3003 房间不可排`、`4006 排房冲突`。

### 7.3 退房 `POST /pms/reservations/{orderNo}/check-out`

成功后：订单置 `COMPLETED`、记录退房时间、关联房间置 `VD（待清洁）`。

### 7.4 修改房态 `PUT /pms/rooms/{id}/status`

请求：`{ "status": "VC", "remark": "清洁完成" }`，用于 清洁完成 VD→VC、维修 OOO↔VC。



***

## 8. 后台管理（ADMIN，示意）



* `GET /admin/room-types` / `POST /admin/room-types` / `PUT /admin/room-types/{id}`：房型列表、新增、修改或上下架；

* `GET /admin/rooms` / `POST /admin/rooms`：房间列表与新增物理房间；

* `POST /admin/inventory/init?days=90`：为未来 N 天补齐库存行（房量取非维修房间数），供上线初始化与每日滚动调用。

> 入参约束示例（Bean Validation）：
>
> `@NotBlank phone`
>
> 、
>
> `@Size(min=6,max=32) password`
>
> 、
>
> `@NotNull checkInDate`
>
> 、
>
> `@Min(1) quantity`
>
> 、
>
> `@Future`
>
>  等；校验失败统一返回 
>
> `1000`
>
>  并在 
>
> `message`
>
> /
>
> `data`
>
>  中给出字段错误。