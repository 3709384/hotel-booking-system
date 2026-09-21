-- =====================================================================
-- 酒店客房预订系统 初始化种子数据 seed.sql
-- 前置: 先执行 schema.sql
-- 幂等: 可重复执行(会先清空业务数据再插入)
-- 默认账号(详见 README, 生产环境请立即改密):
--   管理员  13800000000 / admin123
--   前台    13700000001 / front123
--   会员    13900000001 / 123456
-- =====================================================================
USE hotel_booking;
SET NAMES utf8mb4;

-- 清空(按依赖顺序; 本系统无物理外键)
DELETE FROM notification_log;
DELETE FROM review;
DELETE FROM payment;
DELETE FROM reservation_item;
DELETE FROM reservation;
DELETE FROM room_inventory;
DELETE FROM room_rate;
DELETE FROM room;
DELETE FROM room_type;
DELETE FROM `user`;

-- ---------------------------------------------------------------------
-- 1. 账号（密码哈希算法: SHA-256(盐 + 明文密码) 的十六进制, 与后端 PasswordUtil 一致）
-- ---------------------------------------------------------------------
INSERT INTO `user` (id, phone, password_hash, salt, name, gender, role, member_level, points, status) VALUES
(1, '13800000000', 'bc931c0be5457680328c1b801077e8afd22c1be36e58578565f82982ba1efc90', 'adminsalt01',  '系统管理员', 1, 'ADMIN',        1, 0,    1),
(2, '13700000001', '3271c00c30afe2856bb633d812f29be482811f032593a3b3db34c6bcf441aa72', 'frontsalt01', '前台小李',   1, 'RECEPTIONIST', 1, 0,    1),
(3, '13900000001', '9d41382e67d656e28e200958c4e081752e9cf7daf0f4c081c6e1f2333591246e', 'membersalt01','会员王女士', 2, 'MEMBER',       2, 1200, 1);

-- ---------------------------------------------------------------------
-- 2. 房型
-- ---------------------------------------------------------------------
INSERT INTO room_type (id, code, name, bed_type, max_occupancy, area, base_price, description, cover_image, amenities, status, sort_order) VALUES
(1, 'STD', '标准大床房', '大床1.8m', 2, 28.00, 268.00, '温馨大床房，含独立卫浴、空调、免费WiFi，适合商务与休闲出行。', NULL, JSON_ARRAY('wifi','空调','独立卫浴','电视'), 1, 1),
(2, 'TWN', '标准双床房', '双床1.2m', 2, 30.00, 288.00, '两张单人床，适合同行好友或亲子入住。',                       NULL, JSON_ARRAY('wifi','空调','独立卫浴','电视','窗户'), 1, 2),
(3, 'DLX', '豪华大床房', '大床2.0m', 2, 38.00, 388.00, '空间宽敞，高楼层城市景观，配迷你吧与办公区。',               NULL, JSON_ARRAY('wifi','空调','独立卫浴','迷你吧','景观','早餐'), 1, 3),
(4, 'SUI', '商务套房',   '大床2.0m', 3, 60.00, 688.00, '独立客厅与会客区，行政礼遇，适合高端商务接待。',             NULL, JSON_ARRAY('wifi','空调','独立卫浴','客厅','迷你吧','早餐','行政礼遇'), 1, 4);

-- ---------------------------------------------------------------------
-- 3. 物理房间（8 楼，共 12 间，初始均为空闲可售 VC）
-- ---------------------------------------------------------------------
INSERT INTO room (id, room_no, room_type_id, floor, status, remark) VALUES
( 1, '8101', 1, 8, 'VC', NULL),
( 2, '8102', 1, 8, 'VC', NULL),
( 3, '8103', 1, 8, 'VC', NULL),
( 4, '8104', 1, 8, 'VC', NULL),
( 5, '8105', 1, 8, 'VC', NULL),
( 6, '8201', 2, 8, 'VC', NULL),
( 7, '8202', 2, 8, 'VC', NULL),
( 8, '8203', 2, 8, 'VC', NULL),
( 9, '8204', 2, 8, 'VC', NULL),
(10, '8301', 3, 8, 'VC', '高楼层景观'),
(11, '8302', 3, 8, 'VC', '高楼层景观'),
(12, '8801', 4, 8, 'VC', '套房');

-- ---------------------------------------------------------------------
-- 4. 生成未来 90 天的房型每日库存（total_count = 该房型非维修房间数, booked_count = 0）
--    使用 MySQL 8 递归 CTE 生成日期序列。
-- ---------------------------------------------------------------------
INSERT INTO room_inventory (room_type_id, inventory_date, total_count, booked_count)
WITH RECURSIVE cal(d) AS (
    SELECT CURDATE()
    UNION ALL
    SELECT d + INTERVAL 1 DAY FROM cal
    WHERE d < CURDATE() + INTERVAL 89 DAY
)
SELECT rt.id,
       c.d,
       (SELECT COUNT(*) FROM room r
         WHERE r.room_type_id = rt.id AND r.status <> 'OOO'),
       0
FROM cal c CROSS JOIN room_type rt;

-- 校验：每种房型应有 90 条库存记录
-- SELECT room_type_id, COUNT(*) FROM room_inventory GROUP BY room_type_id;
