# hotel-booking 后端骨架

酒店客房预订系统后端，Spring Boot 3 单体、按业务能力分包。已通过编译、打包与启动验证。

## 环境要求
- JDK 17
- Maven 3.6+
- MySQL 8.0（本地运行需要；不连库也可启动看到 `/api/health`，但业务接口不可用）

## 目录结构
```
src/main/java/com/hotel/booking/
├── HotelBookingApplication.java      # 启动类（@EnableScheduling、@MapperScan）
├── common/                            # 统一返回 Result/ResultCode/PageResult、全局异常
│   └── constant/                      # 订单状态、房态常量
├── config/                            # MyBatis-Plus 分页插件、MVC 拦截器注册
├── security/                          # JWT 工具、登录上下文、鉴权拦截器、@RequireRole
└── module/
    ├── user/                          # 用户实体/Mapper/密码工具
    ├── auth/                          # 注册、登录、当前用户
    ├── room/                          # 房型、房间、每日库存、可售查询、条件扣减/回滚
    ├── reservation/                   # 建单(事务防超卖)、查询、取消、支付确认、超时任务
    ├── payment/                       # 支付单、回调幂等
    └── pms/                           # 房态盘、排房入住(冲突校验)、退房、房态维护
src/main/resources/
├── application.yml                    # dev（默认占位数据源 root/root，按需修改）
└── application-prod.yml               # prod（全部走环境变量，启动即校验数据库）
```

## 本地运行
```bash
# 1. 初始化数据库（在项目根 database/ 目录）
mysql -u root -p < ../database/schema.sql
mysql -u root -p < ../database/seed.sql

# 2. 按本机 MySQL 修改 src/main/resources/application.yml 的账号密码

# 3. 运行
mvn spring-boot:run

# 打包
mvn clean package -DskipTests
java -jar target/hotel-booking-1.0.0.jar
```

## 关键配置（application.yml）
| 配置项 | 默认值 | 含义 |
|---|---|---|
| `server.port` / context-path | 8080 / `/api` | 服务端口与前缀 |
| `app.jwt.secret` / `expire-seconds` | 内置占位 / 7200 | JWT 密钥与有效期（生产用环境变量覆盖） |
| `app.order.pay-timeout-minutes` | 15 | 待支付订单超时时长 |
| `app.order.free-cancel-hour` | 18 | 免费取消时限（小时） |
| `app.booking.max-advance-days` | 90 | 最多可提前预订天数 |

## 鉴权与放行
- 放行（无需登录）：`/error`、`/health`、`/auth/login`、`/auth/register`、`/payments/callback/**`。
- 其余接口需请求头 `Authorization: Bearer <token>`；管理/前台接口再由 `@RequireRole` 校验角色。

## 统一约定
- 响应体：`{ "code":0, "message":"success", "data":... }`；分页：`{records,total,page,size}`。
- 错误码见 `docs/04-接口设计-API.md`（如 4002 库存不足、4006 排房冲突、5003 重复回调）。

## 演示账号
| 角色 | 手机号 | 密码 |
|---|---|---|
| 管理员 | 13800000000 | admin123 |
| 前台 | 13700000001 | front123 |
| 会员 | 13900000001 | 123456 |

## 已验证 / 未验证
- 已验证：编译 BUILD SUCCESS、打包产出 fat jar、启动成功、`/api/health` 返回 200、未认证访问受保护接口返回 401。
- 未验证：因本机数据库凭据未知，未做真实 MySQL 的 CRUD 与并发防超卖端到端测试；请导入脚本后按 `docs/06` 用例联调（重点 TC-O-003 并发防超卖）。
