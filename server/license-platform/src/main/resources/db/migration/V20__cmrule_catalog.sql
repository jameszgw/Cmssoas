-- CmRuleEngine(规则引擎)商业授权集成:
-- 1) 产品目录登记 CMRULE 产品(模块=能力域,功能点=CmRuleEngine capabilities 能力键,
--    与 rule-engine-server/src/capabilities.js 的 CAPABILITY_KEYS 逐键同名);
-- 2) 四档套餐 社区/专业/企业/旗舰,features JSON 与 CmRuleEngine PRESETS 预设逐键一致
--    (License claims.features 原样喂给客户端 resolve(edition, overrides),显式键优先于内置预设,防两侧漂移);
-- 3) CmRuleEngine 授权菜单与操作权限点(签发/审计查询)。
-- 注:plan 的 product_code/edition 两列已由 V19 引入,本迁移直接使用。

-- ===== CMRULE 产品目录 =====
INSERT INTO product(code,name) VALUES ('CMRULE','CmRuleEngine 规则引擎');

INSERT INTO module(product_code,code,name,sort) VALUES
 ('CMRULE','CORE','基础设计',1),
 ('CMRULE','LOWCODE','低代码零代码',2),
 ('CMRULE','DECISION','决策建模',3),
 ('CMRULE','FLOW','规则流',4),
 ('CMRULE','VERSION','版本管理',5),
 ('CMRULE','ORCH','服务编排',6),
 ('CMRULE','AUDITLOG','审计',7),
 ('CMRULE','PLATFORM','平台增强',8),
 ('CMRULE','AI','智能',9);

-- 功能点 code = CmRuleEngine 能力键(rule-engine-server/src/capabilities.js CAPABILITY_KEYS),License features 与其同名
INSERT INTO feature(module_code,code,name,sort) VALUES
 ('CORE','ruleChainCore','基础规则链设计与运行',1),
 ('LOWCODE','zeroCodeWizard','零代码规则向导',2),
 ('LOWCODE','lintCheck','保存前体检',3),
 ('LOWCODE','nodeSearch','节点搜索',4),
 ('LOWCODE','templateLibrary','模板库',5),
 ('DECISION','decisionTable','决策表节点',6),
 ('FLOW','ruleFlow','规则流(子链串联)',7),
 ('FLOW','edgeChains','EDGE 规则链',8),
 ('VERSION','versionHistory','版本管理(快照/回滚)',9),
 ('VERSION','versionDiff','影响分析(版本对比)',10),
 ('ORCH','orchestrationBasic','服务编排基本节点(REST 外呼)',11),
 ('ORCH','orchestrationAdvanced','高级服务编排(消息/DB/补偿)',12),
 ('AUDITLOG','auditQuery','审计查询',13),
 ('AUDITLOG','auditExport','审计导出 CSV',14),
 ('PLATFORM','dbDialects','国产数据库适配',15),
 ('PLATFORM','haCluster','高可用集群',16),
 ('PLATFORM','distributedTx','分布式事务',17),
 ('PLATFORM','xinChuang','信创认证',18),
 ('AI','aiRuleGen','AI 生成规则链',19);

INSERT INTO app_version(product_code,version,sort) VALUES ('CMRULE','v1.0',1);

INSERT INTO feature_version(feature_code,version,available) VALUES
 ('ruleChainCore','v1.0',TRUE),('zeroCodeWizard','v1.0',TRUE),('lintCheck','v1.0',TRUE),('nodeSearch','v1.0',TRUE),
 ('templateLibrary','v1.0',TRUE),('decisionTable','v1.0',TRUE),('ruleFlow','v1.0',TRUE),('edgeChains','v1.0',TRUE),
 ('versionHistory','v1.0',TRUE),('versionDiff','v1.0',TRUE),('orchestrationBasic','v1.0',TRUE),
 ('orchestrationAdvanced','v1.0',TRUE),('auditQuery','v1.0',TRUE),('auditExport','v1.0',TRUE),
 ('dbDialects','v1.0',TRUE),('haCluster','v1.0',TRUE),('distributedTx','v1.0',TRUE),('xinChuang','v1.0',TRUE),
 ('aiRuleGen','v1.0',TRUE);

-- 四档套餐:features = CmRuleEngine PRESETS 预设(社区关 15 键 / 专业关 8 键 / 企业关 3 键 / 旗舰全开)
INSERT INTO plan(code,name,plan_key,price,version_range,seats,modules,features,status,sort,product_code,edition) VALUES
 ('CMRULE_COMMUNITY','CmRuleEngine 社区版','plan.cmr.com',0,'>=1.0.0 <2.0.0',1,'CORE,LOWCODE',
   '{"templateLibrary":false,"decisionTable":false,"ruleFlow":false,"edgeChains":false,"versionHistory":false,"orchestrationBasic":false,"auditQuery":false,"versionDiff":false,"auditExport":false,"orchestrationAdvanced":false,"dbDialects":false,"haCluster":false,"aiRuleGen":false,"distributedTx":false,"xinChuang":false}',
   'ACTIVE',14,'CMRULE','COMMUNITY'),
 ('CMRULE_PRO','CmRuleEngine 专业版','plan.cmr.pro',16800,'>=1.0.0 <2.0.0',5,'CORE,LOWCODE,DECISION,FLOW,VERSION,ORCH,AUDITLOG',
   '{"versionDiff":false,"auditExport":false,"orchestrationAdvanced":false,"dbDialects":false,"haCluster":false,"aiRuleGen":false,"distributedTx":false,"xinChuang":false}',
   'ACTIVE',15,'CMRULE','PROFESSIONAL'),
 ('CMRULE_ENT','CmRuleEngine 企业版','plan.cmr.ent',49800,'>=1.0.0 <2.0.0',20,'CORE,LOWCODE,DECISION,FLOW,VERSION,ORCH,AUDITLOG,PLATFORM',
   '{"aiRuleGen":false,"distributedTx":false,"xinChuang":false}','ACTIVE',16,'CMRULE','ENTERPRISE'),
 ('CMRULE_ULT','CmRuleEngine 旗舰版','plan.cmr.ult',158000,'>=1.0.0 <2.0.0',999,'CORE,LOWCODE,DECISION,FLOW,VERSION,ORCH,AUDITLOG,PLATFORM,AI',
   '{}','ACTIVE',17,'CMRULE','ULTIMATE');

-- ===== CmRuleEngine 授权权限点 =====
INSERT INTO permission(code,name,parent_code,type,sort) VALUES
 ('cmrule','CmRuleEngine 授权',NULL,'MENU',15),
 ('cmrule:view','查看 CmRuleEngine 授权','cmrule','ACTION',1),
 ('cmrule:issue','签发 CmRuleEngine 授权','cmrule','ACTION',2),
 ('cmrule:audit','CmRuleEngine 审计查询','cmrule','ACTION',3);
