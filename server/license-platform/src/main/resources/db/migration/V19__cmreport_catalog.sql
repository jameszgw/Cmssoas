-- CmReport 报表平台产品线接入:产品 / 模块 / 能力 / 5 档版本套餐
-- 说明:CMREPORT-* 套餐订阅时自动签发 CmReport 产品格式 License(RSA SHA256,
--   features.cmEdition 为产品商业版本);价格取《商业版本与定价方案》建议区间下限(分,年订阅)。


-- RSA-2048 签名(base64url)约 344 字符,原 VARCHAR(256) 为 Ed25519 而设 → 扩至 512
ALTER TABLE license ALTER COLUMN signature TYPE VARCHAR(512);
ALTER TABLE license_history ALTER COLUMN signature TYPE VARCHAR(512);

INSERT INTO product(code,name) VALUES ('CMREPORT','CmReport 报表平台');

INSERT INTO module(product_code,code,name,sort) VALUES
 ('CMREPORT','CM-CORE','设计器与查看(明细/分组/H5)',1),
 ('CMREPORT','CM-VISUAL','交叉/图表/大屏',2),
 ('CMREPORT','CM-ENTERPRISE','企业能力(联动/实时/填报/权限审计/告警)',3),
 ('CMREPORT','CM-ULTIMATE','旗舰能力(自助分析/建模/CUBE/AI)',4);

INSERT INTO feature(module_code,code,name,sort) VALUES
 ('CM-CORE','CM.DESIGNER','可视化设计器/明细/分组',1),
 ('CM-CORE','CM.MOBILE.H5','移动端 H5 查看器',2),
 ('CM-CORE','CM.EMBED.URL','URL/iframe 嵌入',3),
 ('CM-VISUAL','CM.CROSSTAB','交叉/统计/图表/条码',4),
 ('CM-VISUAL','CM.DASHBOARD','数据大屏',5),
 ('CM-VISUAL','CM.EMBED.SDK','嵌入式 SDK',6),
 ('CM-ENTERPRISE','CM.REALTIME','实时大屏(SSE)',7),
 ('CM-ENTERPRISE','CM.WRITEBACK','填报/数据回写',8),
 ('CM-ENTERPRISE','CM.PERM.AUDIT','RBAC+行列级权限+审计',9),
 ('CM-ENTERPRISE','CM.ALERT','订阅告警+协作批注',10),
 ('CM-ULTIMATE','CM.SELFBI','自助分析+数据建模',11),
 ('CM-ULTIMATE','CM.CUBE','多维 CUBE',12),
 ('CM-ULTIMATE','CM.AI','AI 智能分析包',13);

-- 五档版本套餐(features.cmEdition 驱动产品格式签发;limits 为产品 License 限额)
INSERT INTO plan(code,name,plan_key,price,version_range,seats,modules,features,status,sort) VALUES
 ('CMREPORT-COMMUNITY','CmReport 社区版','plan.cmreport.community',0,'>=1.0.0 <2.0.0',1,'CM-CORE',
   '{"cmEdition":"community","addons":[],"limits":{"concurrency":10}}','ACTIVE',11),
 ('CMREPORT-LITE','CmReport 轻量版','plan.cmreport.lite',9800,'>=1.0.0 <2.0.0',1,'CM-CORE,CM-VISUAL',
   '{"cmEdition":"lite","addons":[],"limits":{"concurrency":30,"instances":1}}','ACTIVE',12),
 ('CMREPORT-PRO','CmReport 专业版','plan.cmreport.pro',50000,'>=1.0.0 <2.0.0',5,'CM-CORE,CM-VISUAL',
   '{"cmEdition":"pro","addons":[],"limits":{"concurrency":100,"instances":2}}','ACTIVE',13),
 ('CMREPORT-ENT','CmReport 企业版','plan.cmreport.ent',200000,'>=1.0.0 <2.0.0',20,'CM-CORE,CM-VISUAL,CM-ENTERPRISE',
   '{"cmEdition":"enterprise","addons":[],"limits":{"concurrency":500,"nodes":4}}','ACTIVE',14),
 ('CMREPORT-ULT','CmReport 旗舰版','plan.cmreport.ult',600000,'>=1.0.0 <2.0.0',50,'CM-CORE,CM-VISUAL,CM-ENTERPRISE,CM-ULTIMATE',
   '{"cmEdition":"ultimate","addons":["ai.pack"],"limits":{"concurrency":2000,"nodes":16}}','ACTIVE',15);
