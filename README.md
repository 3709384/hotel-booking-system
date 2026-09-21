# 酒店客房预订系统 · 基础架构包

一套可直接作为开发依据的基础架构：**产品/设计文档 + 可执行数据库脚本 + 可编译运行的 Spring Boot 3 后端骨架 + 容器化部署 + 可视化架构总览**。

文档分类与编写要点严格遵循《软件产品文档入门手册》（需求规划 / 设计架构 / 部署运维 / 测试质量四大类齐备，并落实手册“小项目至少保留需求文档、设计概要、测试用例”的要求）。酒店业务表结构、接口与代码为在手册框架下的具体落地设计。

---

## 一、先看这里

双击打开根目录的 **`architecture-overview.html`**（纯静态、离线可用、无需联网），一屏看懂：
总体分层架构 → 按“夜”库存与并发防超卖机制 → 在线预订事务流程 → 订单状态机 → 数据模型 ER → 部署拓扑 → 技术选型。

---

## 二、产物清单

```
酒店客房预订系统-基础架构/
├── architecture-overview.html      # ★ 可视化架构总览（先看这个）
├── README.md                        # 本文件：导航 + 快速开始
├── docs/                            # —— 设计文档（对应手册文档体系）——
│   ├── 00-项目执行总路线.md           #   项目阶段、执行顺序和完成门槛
│   ├── 01-产品需求文档-PRD.md        #   角色/Persona/User Story/功能模块/业务规则 BR/验收 AC
│   ├── 02-概要设计-HLD.md            #   分层架构图/模块职责/技术选型/状态机/部署图/目录结构
│   ├── 03-数据库设计-DBD.md          #   ER 图/10 张表数据字典/索引/可售与排房冲突 SQL
│   ├── 04-接口设计-API.md            #   统一约定/鉴权/错误码/21 个接口及请求响应示例
│   ├── 05-非功能需求与部署方案.md     #   性能/并发一致/安全/可靠 NFR + Docker/手工部署 + Runbook
│   ├── 06-开发路线与测试要点.md       #   M0-M5 里程碑/风险登记/正常·边界·异常测试用例（含并发防超卖）
│   └── 07-个人学习路线与答辩清单.md    #   个人学习、练习、八周安排与答辩问题
├── database/                        # —— 数据库脚本（MySQL 8.0，可重复执行）——
│   ├── schema.sql                   #   建库建表（含 CREATE DATABASE，先 DROP 后 CREATE）
│   └── seed.sql                     #   种子数据：3 账号 / 4 房型 / 12 房间 / 未来 90 天库存
├── backend/                         # —— Spring Boot 3 后端（Maven Wrapper 已配置）——
│   ├── mvnw  mvnw.cmd  pom.xml  Dockerfile
│   └── src/main/...                 #   auth/user/room/reservation/payment/pms 模块
├── frontend/                        # —— Vue 3 单页应用（会员/前台/管理员）——
│   ├── src/                         #   页面、路由、登录状态、API 封装
│   └── package.json
├── scripts/                         # —— 本地环境脚本 ——
│   ├── start-local.ps1              #   一键启动 MySQL、后端、前端
│   ├── stop-local.ps1               #   停止应用进程
│   ├── init-db.ps1                  #   初始化和重置开发数据库
│   └── api-smoke.ps1                #   主业务流程接口回归
└── deploy/                          # —— 容器化部署配置 ——
    ├── docker-compose.yml           #   nginx + app + mysql + redis（可选）
    ├── nginx.conf  .env.example
```

---

## 三、技术栈

| 层 | 选型 | 说明 |
|---|---|---|
| 运行时 | JDK 17+ | 当前项目按 Java 17 编译，本机可用 JDK 21 运行 |
| 后端框架 | Spring Boot 3.1.5（Web / Validation） | 内嵌 Tomcat，上下文路径 `/api`，端口 8080 |
| 前端框架 | Vue 3 + Vite 6 + Element Plus | 单页应用覆盖会员、前台、管理员 |
| 持久层 | MyBatis-Plus 3.5.5（mybatis-plus-spring-boot3-starter） | 单表零 SQL，库存扣减写条件 UPDATE |
| 数据库 | MySQL 8.0（InnoDB / utf8mb4） | 行锁 + 本地事务保证并发一致 |
| 缓存 | Redis 7（**预留，骨架未强依赖**） | 令牌/缓存/热点锁，后续接入 |
| 鉴权 | JWT（JDK 原生 HMAC-SHA256 自实现）+ RBAC | 无第三方 jjwt 依赖；`@RequireRole` 接口级控权 |
| 构建 | Maven（版本锁定） | 产出可执行 fat jar |
| 部署 | Docker / Docker Compose + Nginx | 也提供手工部署方式 |

---

## 四、快速开始

### 方式 A：Windows 本机一键启动（推荐）

    pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\start-local.ps1

脚本会自动选择 JDK 17/21、启动 MySQL80、运行后端并启动前端：

- 前端：http://127.0.0.1:5173
- 后端：http://127.0.0.1:8080/api

停止：

    pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\stop-local.ps1

主流程接口回归：

    pwsh -NoProfile -File .\scripts\api-smoke.ps1

### 方式 B：Docker Compose（需本机装有 Docker）

```bash
cd deploy
cp .env.example .env        # Windows 下用 copy，按需修改里面的数据库/JWT 口令
docker compose up -d --build
# 首次启动 MySQL 数据卷为空，会自动执行 schema.sql、seed.sql
```
启动后：接口经 `http://localhost/api/...`（Nginx）转发到应用；健康检查 `http://localhost/api/health`。

### 方式 C：本机手工启动

**1）初始化数据库**（脚本已含 `CREATE DATABASE hotel_booking`，MySQL 8.0+）

```bash
mysql -u root -p < database/schema.sql
mysql -u root -p < database/seed.sql
```

**2）修改数据源账号密码**

编辑 `backend/src/main/resources/application.yml`，把 `spring.datasource.username/password` 改成本机 MySQL 的真实账号（dev 默认占位为 root/root）。
> dev 配置设置了 `hikari.initialization-fail-timeout: -1`，数据库连不上时应用仍可启动（便于先看健康检查）；生产 `application-prod.yml` 不设此项，数据库不可用即启动失败。

**3）运行**

```bash
cd backend
.\mvnw.cmd spring-boot:run
# 或打包后运行：
.\mvnw.cmd clean package -DskipTests
java -jar target/hotel-booking-1.0.0.jar
```

**4）验证**

```bash
curl http://localhost:8080/api/health
# 期望返回 {"code":0,"message":"success","data":{"service":"hotel-booking","status":"UP",...}}
```

---

## 五、内置演示账号（种子数据，生产请立即改密）

| 角色 | 手机号（登录账号） | 密码 | 可访问范围 |
|---|---|---|---|
| 管理员 ADMIN | `13800000000` | `admin123` | 全部，含房型/房间/库存管理 |
| 前台 RECEPTIONIST | `13700000001` | `front123` | 房态盘、排房入住、退房 |
| 会员 MEMBER | `13900000001` | `123456` | 查可售、下单、支付、查看/取消本人订单 |

登录：`POST /api/auth/login`，body `{"phone":"...","password":"..."}`，返回 `token`；后续请求头加 `Authorization: Bearer <token>`。
> 种子密码使用 `SHA-256(盐+明文)` 存储（与后端 `PasswordUtil` 一致），仅用于演示；生产环境请改用 BCrypt（文档已注明）。

种子业务数据：4 种房型（标准大床 268 / 标准双床 288 / 豪华大床 388 / 商务套房 688）、12 间 8 楼房、未来 90 天每日库存。

---

## 六、核心设计要点（酒店业务的关键差异）

1. **库存按“夜”管理**：库存表 `room_inventory` 粒度为「房型 × 日期 × 可售间数」，可售量 = `total_count - booked_count`，不落冗余列。
2. **并发防超卖**：下单在**一个事务**内对区间 `[入住日, 离店日)` 每一夜执行条件更新
   `UPDATE ... SET booked_count = booked_count + ? WHERE ... AND total_count - booked_count >= ?`，
   任一夜受影响行数为 0 即整单回滚，从数据库层面杜绝超卖。
3. **订单状态机**：待支付 → 已确认 → 已入住 → 已完成，旁支已取消 / 退款中·已退款 / 未到店；非法跳转在 Service 层拦截。
4. **超时释放**：待支付订单默认 15 分钟由定时任务取消并逐夜回滚库存。
5. **支付幂等**：支付回调以第三方交易号唯一索引保证，重复回调不重复确认。
6. **排房防冲突**：入住排房用区间重叠 SQL 校验同房、同房型、时间不冲突，并联动房态（VC/VD/OC/OOO）。

---

## 七、验证情况说明（如实声明）

**已在本机实际验证通过：**
- `mvn clean compile` 编译 **BUILD SUCCESS**（Lombok 生效、依赖经阿里云镜像正常下载）；
- `mvn clean package -DskipTests` 成功产出可执行包 `target/hotel-booking-1.0.0.jar`；
- `java -jar` **实际启动成功**，全部 Bean / Mapper / 拦截器 / 控制器装配正确；
- `GET /api/health` 返回 **200** 与统一 JSON；未带令牌访问受保护接口 `GET /api/auth/me` 返回 **401**（鉴权链路生效）。

**尚未做端到端验证（覆盖缺口，需你在自己环境补测）：**
- 本机 MySQL 的 root 账号无法免密/用默认口令登录，且本机无 Docker，因此**未在真实数据库上导入 SQL、跑 CRUD 与并发防超卖压测**；
- `schema.sql / seed.sql` 已按 MySQL 8 语法人工静态核对（含递归 CTE 生成 90 天库存），请按第四节导入后，结合 `docs/06` 中的用例（尤其并发用例 **TC-O-003**）联调验证。

---

## 八、后续开发路线（详见 docs/06）

- 已完成：M0 工程与建库 → M1 认证/房型/可售 → M2 预订交易与防超卖 → M3 支付与定时任务 → M4 PMS 排房入住 → M5 管理后台；Vue 3 前端已覆盖三类角色。
- 后续扩展：评价 `review`、日历房价 `room_rate`、通知 `notification_log`、真实支付/短信/OSS 对接。

## 九、与手册文档体系的对应

| 手册文档大类 | 本包对应产物 |
|---|---|
| 需求规划（PRD/SRS/Persona/User Story） | `docs/01-产品需求文档-PRD.md` |
| 设计架构（HLD/DBD/API/部署方案） | `docs/02` `docs/03` `docs/04` `docs/05` + `architecture-overview.html` |
| 开发实现（工程骨架/编码约定/依赖管理） | `backend/`（分层、统一返回、配置外置、Maven 锁版本） |
| 测试质量（测试计划/用例） | `docs/06-开发路线与测试要点.md` |
| 部署运维（部署指南/Runbook/回滚） | `docs/05` + `deploy/` |
