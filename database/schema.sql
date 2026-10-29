-- =====================================================================
-- 酒店客房预订系统 数据库建库建表脚本 schema.sql
-- 数据库: MySQL 8.0+  引擎: InnoDB  字符集: utf8mb4
-- 对应文档: docs/03-数据库设计-DBD.md
-- 说明: 本脚本用于【首次初始化】，会先 DROP 本系统的业务表后重建。
--       采用逻辑外键(不建物理 FOREIGN KEY)，关系由应用与索引保证，
--       理由见 DBD「设计约定」。幂等: 可重复执行(会清空数据)。
-- =====================================================================

CREATE DATABASE IF NOT EXISTS hotel_booking
  DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE hotel_booking;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS notification_log;
DROP TABLE IF EXISTS review;
DROP TABLE IF EXISTS payment;
DROP TABLE IF EXISTS reservation_item;
DROP TABLE IF EXISTS reservation;
DROP TABLE IF EXISTS room_inventory;
DROP TABLE IF EXISTS room_rate;
DROP TABLE IF EXISTS room;
DROP TABLE IF EXISTS room_type;
DROP TABLE IF EXISTS hotel;
DROP TABLE IF EXISTS `user`;

-- ---------------------------------------------------------------------
-- 1. 用户表 user（会员 + 员工，用 role 区分，对应 RBAC）
-- ---------------------------------------------------------------------
CREATE TABLE `user` (
  id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  phone         VARCHAR(20)  NOT NULL COMMENT '手机号(登录账号)',
  password_hash CHAR(64)     NOT NULL COMMENT '密码哈希(SHA-256(盐+密码)的十六进制)',
  salt          VARCHAR(32)  NOT NULL COMMENT '密码盐',
  name          VARCHAR(50)  NULL COMMENT '姓名/昵称',
  id_card       VARCHAR(32)  NULL COMMENT '身份证号(生产应加密存储, MVP明文占位)',
  gender        TINYINT      NOT NULL DEFAULT 0 COMMENT '性别:0未知 1男 2女',
  role          VARCHAR(20)  NOT NULL DEFAULT 'MEMBER' COMMENT '角色:MEMBER会员 RECEPTIONIST前台 ADMIN管理员',
  member_level  TINYINT      NOT NULL DEFAULT 1 COMMENT '会员等级',
  points        INT          NOT NULL DEFAULT 0 COMMENT '会员积分(迭代)',
  status        TINYINT      NOT NULL DEFAULT 1 COMMENT '状态:1正常 0停用',
  created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_phone (phone),
  KEY idx_user_role (role)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表(会员与员工)';

-- ---------------------------------------------------------------------
-- 1.5 酒店表 hotel（2026-10-29 改版新增：由单酒店模型升级为多酒店平台模型，
--     对应美团式酒店列表页；房型通过 room_type.hotel_id 归属到酒店）
-- ---------------------------------------------------------------------
CREATE TABLE hotel (
  id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '酒店ID',
  name          VARCHAR(120) NOT NULL COMMENT '酒店名称, 如 明天假日酒店(郑州白庙地铁站店)',
  kind          VARCHAR(20)  NULL COMMENT '档次:经济型/舒适型/高档型',
  score         DECIMAL(2,1) NOT NULL DEFAULT 5.0 COMMENT '展示评分(1-5, 一位小数)',
  score_word    VARCHAR(10)  NULL COMMENT '评分文案:超棒/很好/不错',
  quote         VARCHAR(120) NULL COMMENT '住客代表性短评(列表页展示)',
  tags          JSON         NULL COMMENT '酒店标签, 如["影音酒店","免费停车场"]',
  distance_km   DECIMAL(5,1) NULL COMMENT '距参考点距离(公里, 用于距离排序)',
  distance_text VARCHAR(120) NULL COMMENT '距离展示文案',
  landmark      VARCHAR(50)  NULL COMMENT '地标(搜索关键词), 如 白庙地铁站/二七广场',
  streak        INT          NULL COMMENT '连续好评住客数(列表页展示, 可空)',
  sales_text    VARCHAR(30)  NULL COMMENT '消费量展示文案, 如 1万+消费',
  badge         VARCHAR(20)  NULL COMMENT '角标:优美会/美团住吧',
  promo_type    VARCHAR(10)  NULL COMMENT '促销类型:神券/会员',
  promo_text    VARCHAR(80)  NULL COMMENT '促销文案, 如 立减294',
  member        TINYINT      NOT NULL DEFAULT 0 COMMENT '是否黄金会员价酒店:1是 0否',
  cover_image   VARCHAR(255) NULL COMMENT '封面图URL(迭代)',
  icon          VARCHAR(8)   NULL COMMENT '封面占位图标(emoji)',
  status        TINYINT      NOT NULL DEFAULT 1 COMMENT '状态:1上架 0下架',
  sort_order    INT          NOT NULL DEFAULT 0 COMMENT '排序值(小在前)',
  created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_hotel_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='酒店表(多酒店平台)';

-- ---------------------------------------------------------------------
-- 2. 房型表 room_type（客房类型，售卖与计价的主体；hotel_id 归属酒店）
-- ---------------------------------------------------------------------
CREATE TABLE room_type (
  id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '房型ID',
  hotel_id      BIGINT UNSIGNED NOT NULL COMMENT '所属酒店ID(逻辑外键)',
  code          VARCHAR(32)  NOT NULL COMMENT '房型编码',
  name          VARCHAR(50)  NOT NULL COMMENT '房型名称',
  bed_type      VARCHAR(32)  NULL COMMENT '床型, 如 大床1.8m/双床1.2m',
  max_occupancy INT          NOT NULL DEFAULT 2 COMMENT '最大入住人数',
  area          DECIMAL(6,2) NULL COMMENT '面积(平方米)',
  base_price    DECIMAL(10,2) NOT NULL COMMENT '基础房价(元/间夜, MVP按此计价)',
  market_price  DECIMAL(10,2) NULL COMMENT '划线价/门市价(元/间夜, 列表页展示, 可空)',
  description   VARCHAR(500) NULL COMMENT '房型描述',
  cover_image   VARCHAR(255) NULL COMMENT '封面图URL(对象存储, 迭代)',
  amenities     JSON         NULL COMMENT '设施清单, 如["wifi","早餐","窗户"]',
  status        TINYINT      NOT NULL DEFAULT 1 COMMENT '状态:1上架可售 0停用',
  sort_order    INT          NOT NULL DEFAULT 0 COMMENT '排序值(小在前)',
  created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_room_type_code (code),
  KEY idx_room_type_status (status),
  KEY idx_room_type_hotel (hotel_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客房类型表';

-- ---------------------------------------------------------------------
-- 3. 物理房间表 room（一间真实客房，用于排房与房态管理）
-- ---------------------------------------------------------------------
CREATE TABLE room (
  id           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '房间ID',
  room_no      VARCHAR(16)  NOT NULL COMMENT '房间号, 如 8101',
  room_type_id BIGINT UNSIGNED NOT NULL COMMENT '所属房型ID',
  floor        INT          NULL COMMENT '楼层',
  status       VARCHAR(8)   NOT NULL DEFAULT 'VC' COMMENT '房态:VC空闲可售 VD待清洁 OC在住 OOO维修停用',
  remark       VARCHAR(255) NULL COMMENT '备注',
  created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_room_no (room_no),
  KEY idx_room_type (room_type_id),
  KEY idx_room_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='物理房间表';

-- ---------------------------------------------------------------------
-- 4. 房型每日库存表 room_inventory（按"夜"售卖的核心: 房型 x 日期 x 可售量）
--    可售间数 = total_count - booked_count，不做冗余列以避免不一致。
-- ---------------------------------------------------------------------
CREATE TABLE room_inventory (
  id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '库存ID',
  room_type_id   BIGINT UNSIGNED NOT NULL COMMENT '房型ID',
  inventory_date DATE         NOT NULL COMMENT '库存日期(对应某一"夜")',
  total_count    INT          NOT NULL DEFAULT 0 COMMENT '当日总可售间数(房量)',
  booked_count   INT          NOT NULL DEFAULT 0 COMMENT '当日已订(占用)间数',
  created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_inventory_type_date (room_type_id, inventory_date),
  KEY idx_inventory_date (inventory_date),
  CONSTRAINT chk_inventory_count CHECK (booked_count >= 0 AND booked_count <= total_count)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='房型每日库存表(按夜)';

-- ---------------------------------------------------------------------
-- 5. 订单主表 reservation
-- ---------------------------------------------------------------------
CREATE TABLE reservation (
  id                 BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '订单ID',
  order_no           VARCHAR(32)  NOT NULL COMMENT '订单号(业务唯一)',
  user_id            BIGINT UNSIGNED NOT NULL COMMENT '下单会员ID',
  contact_name       VARCHAR(50)  NOT NULL COMMENT '入住联系人姓名',
  contact_phone      VARCHAR(20)  NOT NULL COMMENT '联系人手机',
  check_in_date      DATE         NOT NULL COMMENT '入住日期',
  check_out_date     DATE         NOT NULL COMMENT '离店日期',
  nights             INT          NOT NULL COMMENT '间夜数(离店-入住)',
  total_amount       DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '订单总额(元)',
  status             VARCHAR(20)  NOT NULL DEFAULT 'PENDING_PAYMENT'
                     COMMENT '状态:PENDING_PAYMENT待支付 CONFIRMED已确认 CHECKED_IN已入住 COMPLETED已完成 CANCELLED已取消 NO_SHOW未到店 REFUNDING退款中 REFUNDED已退款',
  source             VARCHAR(16)  NOT NULL DEFAULT 'ONLINE' COMMENT '来源:ONLINE线上 FRONT前台',
  remark             VARCHAR(255) NULL COMMENT '订单备注(如无烟/高楼层)',
  deposit            DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '押金(元)',
  expire_at          DATETIME     NULL COMMENT '待支付截止时间(超时自动取消)',
  paid_at            DATETIME     NULL COMMENT '支付完成时间',
  actual_check_in_at DATETIME     NULL COMMENT '实际办理入住时间',
  actual_check_out_at DATETIME    NULL COMMENT '实际办理退房时间',
  cancel_time        DATETIME     NULL COMMENT '取消时间',
  created_at         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_reservation_order_no (order_no),
  KEY idx_reservation_user (user_id),
  KEY idx_reservation_status_expire (status, expire_at),
  KEY idx_reservation_dates (check_in_date, check_out_date),
  KEY idx_reservation_status (status),
  CONSTRAINT chk_reservation_dates CHECK (check_out_date > check_in_date),
  CONSTRAINT chk_reservation_nights CHECK (nights >= 1)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='预订订单主表';

-- ---------------------------------------------------------------------
-- 6. 订单明细表 reservation_item
--    一行 = 预订【一间】客房住 nights 晚; 订多间同房型则多行。
--    room_id 在前台排房后回填，未排房为 NULL。
-- ---------------------------------------------------------------------
CREATE TABLE reservation_item (
  id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '明细ID',
  reservation_id BIGINT UNSIGNED NOT NULL COMMENT '订单ID',
  room_type_id   BIGINT UNSIGNED NOT NULL COMMENT '房型ID',
  room_id        BIGINT UNSIGNED NULL COMMENT '排房分配的具体房间ID(未排房为空)',
  unit_price     DECIMAL(10,2) NOT NULL COMMENT '下单锁定的每夜单价(元)',
  nights         INT          NOT NULL COMMENT '间夜数',
  subtotal       DECIMAL(10,2) NOT NULL COMMENT '小计 = 单价 x 夜数',
  created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_item_reservation (reservation_id),
  KEY idx_item_room (room_id),
  KEY idx_item_type (room_type_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单明细(一行一间)';

-- ---------------------------------------------------------------------
-- 7. 支付流水表 payment（支付/退款; 第三方交易号唯一以保证回调幂等）
-- ---------------------------------------------------------------------
CREATE TABLE payment (
  id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '支付流水ID',
  payment_no     VARCHAR(32)  NOT NULL COMMENT '支付流水号',
  reservation_id BIGINT UNSIGNED NOT NULL COMMENT '订单ID',
  order_no       VARCHAR(32)  NOT NULL COMMENT '订单号(冗余便于对账)',
  channel        VARCHAR(16)  NOT NULL COMMENT '渠道:WECHAT微信 ALIPAY支付宝 CASH现金',
  type           TINYINT      NOT NULL DEFAULT 1 COMMENT '类型:1支付 2退款',
  amount         DECIMAL(10,2) NOT NULL COMMENT '金额(元)',
  status         VARCHAR(16)  NOT NULL DEFAULT 'INIT' COMMENT '状态:INIT初始 SUCCESS成功 FAIL失败 REFUNDED已退款',
  transaction_id VARCHAR(64)  NULL COMMENT '第三方交易号(回调幂等依据)',
  paid_at        DATETIME     NULL COMMENT '交易完成时间',
  created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_payment_no (payment_no),
  UNIQUE KEY uk_payment_transaction (transaction_id),
  KEY idx_payment_reservation (reservation_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付流水表';

-- ---------------------------------------------------------------------
-- 8. 评价表 review（房型评分与历史评论展示）
-- ---------------------------------------------------------------------
CREATE TABLE review (
  id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '评价ID',
  reservation_id BIGINT UNSIGNED NOT NULL COMMENT '订单ID(一单一评)',
  user_id        BIGINT UNSIGNED NOT NULL COMMENT '评价会员ID',
  room_type_id   BIGINT UNSIGNED NOT NULL COMMENT '房型ID',
  score          TINYINT      NOT NULL DEFAULT 5 COMMENT '评分1-5',
  content        VARCHAR(1000) NULL COMMENT '评价文字',
  images         JSON         NULL COMMENT '评价图片URL数组',
  status         TINYINT      NOT NULL DEFAULT 1 COMMENT '1显示 0隐藏',
  created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_review_reservation (reservation_id),
  KEY idx_review_type_status (room_type_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评价表';

-- ---------------------------------------------------------------------
-- 9. 日历房价表 room_rate（动态/周末/节假日房价, 迭代启用; MVP用room_type.base_price）
-- ---------------------------------------------------------------------
CREATE TABLE room_rate (
  id           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '房价ID',
  room_type_id BIGINT UNSIGNED NOT NULL COMMENT '房型ID',
  rate_date    DATE         NOT NULL COMMENT '适用日期',
  price        DECIMAL(10,2) NOT NULL COMMENT '当日房价(元/间夜)',
  created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_rate_type_date (room_type_id, rate_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='日历房价表(动态房价预留)';

-- ---------------------------------------------------------------------
-- 10. 通知记录表 notification_log（短信/站内信, 迭代预留）
-- ---------------------------------------------------------------------
CREATE TABLE notification_log (
  id         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '通知ID',
  user_id    BIGINT UNSIGNED NULL COMMENT '接收用户ID',
  type       VARCHAR(20)  NOT NULL COMMENT '类型:ORDER_CONFIRM订单确认 CANCEL取消 CHECKIN入住提醒',
  channel    VARCHAR(20)  NOT NULL DEFAULT 'SMS' COMMENT '渠道:SMS短信 EMAIL邮件 IN_APP站内',
  title      VARCHAR(100) NULL COMMENT '标题',
  content    VARCHAR(500) NULL COMMENT '内容',
  biz_no     VARCHAR(32)  NULL COMMENT '关联业务单号(订单号)',
  status     TINYINT      NOT NULL DEFAULT 0 COMMENT '0待发 1成功 2失败',
  send_time  DATETIME     NULL COMMENT '发送时间',
  created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_notify_user (user_id),
  KEY idx_notify_biz (biz_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通知记录表(迭代预留)';

SET FOREIGN_KEY_CHECKS = 1;
