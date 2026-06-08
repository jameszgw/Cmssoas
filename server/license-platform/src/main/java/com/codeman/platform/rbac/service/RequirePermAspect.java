package com.codeman.platform.rbac.service;

import com.codeman.platform.common.ApiException;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/** 在标注 @RequirePerm 的接口执行前做权限校验。 */
@Aspect
@Component
public class RequirePermAspect {

    private final PermissionChecker checker;

    public RequirePermAspect(PermissionChecker checker) {
        this.checker = checker;
    }

    @Before("@annotation(requirePerm)")
    public void check(RequirePerm requirePerm) {
        if (!checker.granted(requirePerm.value())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "无权限：" + requirePerm.value());
        }
    }
}
