-- ============================================================
-- CODEMAN 测试 / 演示数据(可选,便于试用与培训)
-- 适用 PostgreSQL / MySQL 5.7+/8.0(均兼容的可移植写法,布尔用 TRUE/FALSE)。
-- 前置:已执行 schema 脚本(及可选 init-admin.sql)。生产环境请勿导入。
-- 仅含不依赖服务端签名的数据(租户/客户/产品/套餐/须知);
-- License/合同等需经业务接口生成(含签名/存证),不在此脚本内。
-- ============================================================

-- 租户
INSERT INTO tenant(code,name,plan_code,plan_key,version,isolation,mode,status,admin_email,email_sent,expire_at,created_at)
SELECT 'T-900001','北京云科信息技术有限公司','ENTERPRISE','ent','v1','SCHEMA','ONLINE','ACTIVE','admin@yunke-demo.com',TRUE,DATE '2027-12-31',CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM tenant WHERE code='T-900001');
INSERT INTO tenant(code,name,plan_code,plan_key,version,isolation,mode,status,admin_email,email_sent,expire_at,created_at)
SELECT 'T-900002','上海某智能制造股份有限公司','PROFESSIONAL','pro','v1','SCHEMA','HYBRID','ACTIVE','admin@zhizao-demo.com',TRUE,DATE '2027-06-30',CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM tenant WHERE code='T-900002');

-- 客户(名称与上面租户/后续业务单据对应,便于"客户360"聚合)
INSERT INTO customer(code,name,tenant_code,contact,email,phone,industry,status,note,created_at)
SELECT 'CUST-DEMO01','北京云科信息技术有限公司','T-900001','李明','li@yunke-demo.com','13800000001','云计算/SaaS','ACTIVE','演示客户',CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM customer WHERE name='北京云科信息技术有限公司');
INSERT INTO customer(code,name,tenant_code,contact,email,phone,industry,status,note,created_at)
SELECT 'CUST-DEMO02','上海某智能制造股份有限公司','T-900002','王芳','wang@zhizao-demo.com','13800000002','智能制造','ACTIVE','演示客户',CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM customer WHERE name='上海某智能制造股份有限公司');
INSERT INTO customer(code,name,tenant_code,contact,email,phone,industry,status,note,created_at)
SELECT 'CUST-DEMO03','广州前海数字科技有限公司',NULL,'陈强','chen@qianhai-demo.com','13800000003','金融科技','ACTIVE','潜在客户',CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM customer WHERE name='广州前海数字科技有限公司');

-- 产品(CODEMAN 已由 schema 种子内置,这里补一个演示产品)
INSERT INTO product(code,name)
SELECT 'DEMOAPP','演示业务系统'
WHERE NOT EXISTS (SELECT 1 FROM product WHERE code='DEMOAPP');

-- 套餐
INSERT INTO plan(code,name,plan_key,price,version_range,seats,modules,features,status,sort)
SELECT 'ENTERPRISE','企业版','ent',88000,'>=1.0.0 <3.0.0',50,'CORE,RISK,REPORT','{"REPORT.EXPORT":true}','ACTIVE',1
WHERE NOT EXISTS (SELECT 1 FROM plan WHERE code='ENTERPRISE');
INSERT INTO plan(code,name,plan_key,price,version_range,seats,modules,features,status,sort)
SELECT 'PROFESSIONAL','专业版','pro',36000,'>=1.0.0 <3.0.0',20,'CORE,REPORT','{"REPORT.EXPORT":false}','ACTIVE',2
WHERE NOT EXISTS (SELECT 1 FROM plan WHERE code='PROFESSIONAL');

-- 用户须知(强制确认,登录后弹层)
INSERT INTO notice(type,title,content_html,version,status,force_ack,effective_at,created_at)
SELECT 'TERMS','服务条款与用户须知','<p>欢迎使用 CODEMAN 软件授权运营平台。使用即表示您同意相关条款。</p>',1,'PUBLISHED',TRUE,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM notice WHERE type='TERMS' AND version=1);
INSERT INTO notice(type,title,content_html,version,status,force_ack,effective_at,created_at)
SELECT 'PRIVACY','隐私政策','<p>我们仅收集为提供服务所必需的信息……</p>',1,'PUBLISHED',FALSE,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM notice WHERE type='PRIVACY' AND version=1);
