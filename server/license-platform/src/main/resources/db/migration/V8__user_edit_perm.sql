-- 新增「编辑用户」权限点（用户管理）
INSERT INTO permission(code,name,parent_code,type,sort) VALUES
 ('user:edit','编辑用户','system','ACTION',4);
