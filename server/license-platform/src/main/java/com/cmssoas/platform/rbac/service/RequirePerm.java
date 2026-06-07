package com.cmssoas.platform.rbac.service;

import java.lang.annotation.*;

/** 标注接口所需权限点（按 permission.code）。SUPER_ADMIN 自动放行。 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequirePerm {
    String value();
}
