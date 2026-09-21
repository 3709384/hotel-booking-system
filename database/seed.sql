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
(3, '13900000001', '9d41382e67d656e28e200958c4e081752e9cf7daf0f4c081c6e1f2333591246e', 'membersalt01','会员王女士', 2, 'MEMBER',       2, 1200, 1),
(4, '13900000002', '9d41382e67d656e28e200958c4e081752e9cf7daf0f4c081c6e1f2333591246e', 'membersalt01','华住会金会员', 2, 'MEMBER',       1, 120,  1),
(5, '13900000003', '9d41382e67d656e28e200958c4e081752e9cf7daf0f4c081c6e1f2333591246e', 'membersalt01','华住会铂金会员', 1, 'MEMBER',       1, 260,  1),
(6, '13900000004', '9d41382e67d656e28e200958c4e081752e9cf7daf0f4c081c6e1f2333591246e', 'membersalt01','华住会星会员', 2, 'MEMBER',       2, 880,  1),
(7, '13900000005', '9d41382e67d656e28e200958c4e081752e9cf7daf0f4c081c6e1f2333591246e', 'membersalt01','华住会钻石会员', 1, 'MEMBER',       1, 310,  1);

-- ---------------------------------------------------------------------
-- 2. 房型
-- ---------------------------------------------------------------------
INSERT INTO room_type (id, code, name, bed_type, max_occupancy, area, base_price, description, cover_image, amenities, status, sort_order) VALUES
(1, 'STD', '标准大床房', '大床1.8m', 2, 28.00, 268.00, '温馨大床房，含独立卫浴、空调、免费WiFi，适合商务与休闲出行。', '/images/rooms/std.jpg', JSON_ARRAY('wifi','空调','独立卫浴','电视'), 1, 1),
(2, 'TWN', '标准双床房', '双床1.2m', 2, 30.00, 288.00, '两张单人床，适合同行好友或亲子入住。',                       '/images/rooms/twn.jpg', JSON_ARRAY('wifi','空调','独立卫浴','电视','窗户'), 1, 2),
(3, 'DLX', '豪华大床房', '大床2.0m', 2, 38.00, 388.00, '空间宽敞，高楼层城市景观，配迷你吧与办公区。',               '/images/rooms/dlx.jpg', JSON_ARRAY('wifi','空调','独立卫浴','迷你吧','景观','早餐'), 1, 3),
(4, 'SUI', '商务套房',   '大床2.0m', 3, 60.00, 688.00, '独立客厅与会客区，行政礼遇，适合高端商务接待。',             '/images/rooms/suite.jpg', JSON_ARRAY('wifi','空调','独立卫浴','客厅','迷你吧','早餐','行政礼遇'), 1, 4),
(5, 'ADV', '高级大床房', '大床1.8m', 2, 32.00, 328.00, '高层舒适大床房，配备办公区和升级床品，适合商务出行。',       '/images/rooms/adv.jpg',  JSON_ARRAY('wifi','空调','独立卫浴','电视','办公桌','窗户'), 1, 5),
(6, 'ADT', '高级双床房', '双床1.2m', 2, 34.00, 348.00, '两张独立单人床，空间明亮，适合朋友或同事同行入住。',           '/images/rooms/adt.jpg',  JSON_ARRAY('wifi','空调','独立卫浴','电视','窗户','办公桌'), 1, 6),
(7, 'ZPY', '零压大床房', '零压大床1.8m', 2, 35.00, 368.00, '零压床垫和静音睡眠环境，帮助旅途中的客人获得更好休息。',       '/images/rooms/zpy.jpg',  JSON_ARRAY('wifi','空调','独立卫浴','零压床垫','静音','助眠用品'), 1, 7),
(8, 'FAM', '家庭房',     '大床+单人床', 4, 45.00, 498.00, '一张大床搭配一张单人床，适合亲子家庭或三人结伴出行。',         '/images/rooms/fam.jpg',  JSON_ARRAY('wifi','空调','独立卫浴','电视','亲子设施','储物空间'), 1, 8);

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
(12, '8801', 4, 8, 'VC', '套房'),
(13, '8501', 5, 8, 'VC', '高级大床房'),
(14, '8502', 5, 8, 'VC', '高级大床房'),
(15, '8601', 6, 8, 'VC', '高级双床房'),
(16, '8602', 6, 8, 'VC', '高级双床房'),
(17, '8701', 7, 8, 'VC', '零压大床房'),
(18, '8702', 7, 8, 'VC', '零压大床房'),
(19, '8901', 8, 8, 'VC', '家庭房'),
(20, '8902', 8, 8, 'VC', '家庭房');

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


-- ---------------------------------------------------------------------
-- 5. 华住会公开住客点评（采集后匿名化整理，不保留会员昵称和个人信息）
-- ---------------------------------------------------------------------
INSERT INTO review (reservation_id, user_id, room_type_id, score, content, images, status, created_at) VALUES
(9001, 4, 1, 5, '每次出差都住这家，前台服务很好，早餐也好吃，很推荐。', NULL, 1, NOW() - INTERVAL 2 DAY),
(9002, 5, 1, 4, '位置方便，WiFi稳定。房间面积不算很大，但布局合理，性价比不错。', NULL, 1, NOW() - INTERVAL 8 DAY),
(9003, 6, 1, 5, '很舒适，很新，服务很周到。', NULL, 1, NOW() - INTERVAL 15 DAY),
(9004, 7, 2, 5, '和同事出差订的双床房，两张床互不打扰，卫生间干湿分离。', NULL, 1, NOW() - INTERVAL 3 DAY),
(9005, 4, 2, 4, '采光很好，房间安静，适合和朋友一起住。', NULL, 1, NOW() - INTERVAL 11 DAY),
(9006, 5, 2, 5, '整体很整洁，窗户可以打开通风，早餐种类也够用。', NULL, 1, NOW() - INTERVAL 20 DAY),
(9007, 6, 3, 5, '楼层视野很好，房间比标准房宽敞，办公桌使用起来很舒服。', NULL, 1, NOW() - INTERVAL 4 DAY),
(9008, 7, 3, 5, '床品品质很好，晚上看城市夜景很舒服，隔音表现也不错。', NULL, 1, NOW() - INTERVAL 9 DAY),
(9009, 4, 3, 4, '前台设计比较有特点，房间设施新，整体入住体验不错。', NULL, 1, NOW() - INTERVAL 18 DAY),
(9010, 5, 4, 5, '套房空间很大，客厅和卧室分区明确，接待客户非常方便。', NULL, 1, NOW() - INTERVAL 5 DAY),
(9011, 6, 4, 5, '行政礼遇不错，房间安静舒适，适合商务出行。', NULL, 1, NOW() - INTERVAL 12 DAY),
(9012, 7, 4, 4, '整体设施齐全，客厅适合临时办公，退房效率很高。', NULL, 1, NOW() - INTERVAL 25 DAY),
(9013, 4, 5, 5, '空间大，卫生，早餐特别棒。', NULL, 1, NOW() - INTERVAL 1 DAY),
(9014, 5, 5, 5, '每次来上海都住这边，特别棒，值得推荐。', NULL, 1, NOW() - INTERVAL 6 DAY),
(9015, 6, 5, 4, '房间干净安静，前台热情贴心，早餐丰富可口。', NULL, 1, NOW() - INTERVAL 10 DAY),
(9016, 7, 6, 5, '很不错，很干净，服务态度很好，下次还去。', NULL, 1, NOW() - INTERVAL 2 DAY),
(9017, 4, 6, 4, '环境很好，前台热情，两张床适合同事一起出差。', NULL, 1, NOW() - INTERVAL 7 DAY),
(9018, 5, 6, 5, '设施设备完善，来上海首选，早餐丰富，房间整洁。', NULL, 1, NOW() - INTERVAL 14 DAY),
(9019, 6, 7, 5, '床垫很舒服，晚上很安静，睡眠质量比普通房型更好。', NULL, 1, NOW() - INTERVAL 3 DAY),
(9020, 7, 7, 5, '房间很新，服务周到，连续住了几天都很满意。', NULL, 1, NOW() - INTERVAL 9 DAY),
(9021, 4, 7, 4, '零压床垫确实舒服，房间隔音和遮光都做得好。', NULL, 1, NOW() - INTERVAL 17 DAY),
(9022, 5, 8, 4, '家庭房空间安排合理，一张大床和一张单人床很实用。', NULL, 1, NOW() - INTERVAL 4 DAY),
(9023, 6, 8, 5, '带孩子入住很方便，房间整洁，早餐选择也比较多。', NULL, 1, NOW() - INTERVAL 13 DAY),
(9024, 7, 8, 4, '位置方便，一家人短住体验不错，退房速度很快。', NULL, 1, NOW() - INTERVAL 22 DAY),
(9025, 4, 1, 5, '早餐丰富，服务员热情，推荐大家都来入住，十分好。', NULL, 1, NOW() - INTERVAL 6 DAY),
(9026, 5, 2, 4, '房间里安静，床品舒服，晚上休息得很好。', NULL, 1, NOW() - INTERVAL 10 DAY),
(9027, 6, 3, 5, '前台服务很好，每次朋友来都订这家酒店，推荐入住。', NULL, 1, NOW() - INTERVAL 8 DAY),
(9028, 7, 4, 5, '很喜欢，推荐入住，设施设备完善。', NULL, 1, NOW() - INTERVAL 16 DAY),
(9029, 4, 5, 5, '设施设备很好，来上海首选，推荐入住，早餐丰富，房间干净整洁。', NULL, 1, NOW() - INTERVAL 7 DAY),
(9030, 5, 6, 4, '房间设施新，早餐一般，整体性价比可以。', NULL, 1, NOW() - INTERVAL 19 DAY),
(9031, 6, 7, 5, '前台办理入住效率高，退房也很顺畅，休息得很好。', NULL, 1, NOW() - INTERVAL 5 DAY),
(9032, 7, 8, 5, '房间整洁，服务周到，一家人住得很舒服。', NULL, 1, NOW() - INTERVAL 21 DAY);
-- 校验：每种房型应有 90 条库存记录
-- SELECT room_type_id, COUNT(*) FROM room_inventory GROUP BY room_type_id;
