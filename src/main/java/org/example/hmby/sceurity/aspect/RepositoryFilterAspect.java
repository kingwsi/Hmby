package org.example.hmby.sceurity.aspect;

import jakarta.persistence.EntityManager;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.example.hmby.sceurity.SecurityUtils;
import org.hibernate.Session;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;


@Component
@Aspect
public class RepositoryFilterAspect {

    private final EntityManager entityManager;

    public RepositoryFilterAspect(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Pointcut("execution(* org.example.hmby.repository..*(..))")
    public void transactionalMethods() {}

    // 在目标方法执行前启用 Hibernate 过滤器
    @Before("transactionalMethods()")
    @Transactional
    public void enableUserFilter() {
        String userId = SecurityUtils.getUserId(); // 获取当前用户 ID

        Session session = entityManager.unwrap(Session.class);
        // 启用过滤器
        session.enableFilter("userFilter").setParameter("userId", userId);
    }
}
