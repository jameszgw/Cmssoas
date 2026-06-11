-- [MySQL 方言] 由 db/migration/V19__cmprint_catalog.sql 机械转换:IDENTITY→AUTO_INCREMENT、TIMESTAMP→DATETIME、布尔默认 1/0、建表 utf8mb4。
-- 本迁移无建表,仅 ALTER + 种子数据;TRUE 改 1 以兼容 MySQL 5.7。
-- CmPrint(打印模板设计器)商业授权集成:
-- 1) plan 表补「产品/版本档位」两列——订阅自动签发时按套餐落 productCode 与 edition(此前写死 CODEMAN/套餐码);
-- 2) 产品目录登记 CMPRINT 产品(模块=能力域,功能点=CmPrint capabilities 能力键,与前端 resolveEdition 同名);
-- 3) 三档套餐 社区/专业/企业,features JSON 与 CmPrint EDITIONS 预设逐键一致(License claims.features 原样
--    喂给 CmPrint 的 resolveEdition(edition, overrides),显式键优先于前端预设,防两侧漂移);
-- 4) CmPrint 授权菜单与操作权限点(签发/审计查询)。

ALTER TABLE plan ADD COLUMN product_code VARCHAR(32) NOT NULL DEFAULT 'CODEMAN';
ALTER TABLE plan ADD COLUMN edition VARCHAR(32);
-- 存量 CODEMAN 套餐:套餐码即版本档位(BASIC/PROFESSIONAL/ENTERPRISE/FLAGSHIP),与原签发行为一致
UPDATE plan SET edition = code WHERE product_code = 'CODEMAN';

-- ===== CMPRINT 产品目录 =====
INSERT INTO product(code,name) VALUES ('CMPRINT','CmPrint 打印模板设计器');

INSERT INTO module(product_code,code,name,sort) VALUES
 ('CMPRINT','DESIGN','设计器',1),
 ('CMPRINT','DATA','数据',2),
 ('CMPRINT','OUTPUT','预览打印',3),
 ('CMPRINT','EXPORT','导出',4),
 ('CMPRINT','TEMPLATE','模板',5);

-- 功能点 code = CmPrint 能力键(src/core/capabilities.js CAPABILITY_KEYS),License features 与其同名
INSERT INTO feature(module_code,code,name,sort) VALUES
 ('DESIGN','theme','设计器换肤',1),
 ('DATA','dataSource','数据源编辑器',2),
 ('OUTPUT','preview','测量分页预览',3),
 ('OUTPUT','print','浏览器打印',4),
 ('OUTPUT','directPrint','直连静默打印',5),
 ('OUTPUT','watermark','水印',6),
 ('OUTPUT','overprint','套打底图',7),
 ('OUTPUT','calibration','打印对位校准',8),
 ('EXPORT','exportJson','模板 JSON 导出',9),
 ('EXPORT','exportPdf','PDF 导出(图片型)',10),
 ('EXPORT','exportPdfText','PDF 导出(文本型)',11),
 ('EXPORT','sharePdf','PDF 系统分享',12),
 ('EXPORT','exportWord','Word 导出',13),
 ('EXPORT','exportExcel','Excel/CSV 导出',14),
 ('EXPORT','exportImage','图片导出',15),
 ('TEMPLATE','subTemplate','子模板',16),
 ('TEMPLATE','templateGallery','云端模板库',17);

INSERT INTO app_version(product_code,version,sort) VALUES ('CMPRINT','v0.5',1);

INSERT INTO feature_version(feature_code,version,available) VALUES
 ('theme','v0.5',1),('dataSource','v0.5',1),('preview','v0.5',1),('print','v0.5',1),
 ('directPrint','v0.5',1),('watermark','v0.5',1),('overprint','v0.5',1),('calibration','v0.5',1),
 ('exportJson','v0.5',1),('exportPdf','v0.5',1),('exportPdfText','v0.5',1),('sharePdf','v0.5',1),
 ('exportWord','v0.5',1),('exportExcel','v0.5',1),('exportImage','v0.5',1),
 ('subTemplate','v0.5',1),('templateGallery','v0.5',1);

-- 三档套餐:features = CmPrint EDITIONS 预设(社区关 13 键 / 专业关 2 键 / 企业全开)
INSERT INTO plan(code,name,plan_key,price,version_range,seats,modules,features,status,sort,product_code,edition) VALUES
 ('CMPRINT_COMMUNITY','CmPrint 社区版','plan.cmp.com',0,'>=0.5.0 <1.0.0',1,'DESIGN,DATA,OUTPUT,TEMPLATE',
   '{"exportPdf":false,"exportPdfText":false,"sharePdf":false,"exportWord":false,"exportExcel":false,"exportImage":false,"directPrint":false,"overprint":false,"calibration":false,"watermark":false,"subTemplate":false,"templateGallery":false,"theme":false}',
   'ACTIVE',11,'CMPRINT','COMMUNITY'),
 ('CMPRINT_PRO','CmPrint 专业版','plan.cmp.pro',12800,'>=0.5.0 <1.0.0',5,'DESIGN,DATA,OUTPUT,EXPORT,TEMPLATE',
   '{"directPrint":false,"templateGallery":false}','ACTIVE',12,'CMPRINT','PROFESSIONAL'),
 ('CMPRINT_ENT','CmPrint 企业版','plan.cmp.ent',39800,'>=0.5.0 <1.0.0',20,'DESIGN,DATA,OUTPUT,EXPORT,TEMPLATE',
   '{}','ACTIVE',13,'CMPRINT','ENTERPRISE');

-- ===== CmPrint 授权权限点 =====
INSERT INTO permission(code,name,parent_code,type,sort) VALUES
 ('cmprint','CmPrint 授权',NULL,'MENU',14),
 ('cmprint:view','查看 CmPrint 授权','cmprint','ACTION',1),
 ('cmprint:issue','签发 CmPrint 授权','cmprint','ACTION',2),
 ('cmprint:audit','CmPrint 审计查询','cmprint','ACTION',3);
