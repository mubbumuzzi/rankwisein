package com.rankwise.common.audit;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
public class AuditAspect {

    @Around("@annotation(auditable)")
    public Object audit(ProceedingJoinPoint pjp, Auditable auditable) throws Throwable {
        String action = auditable.value().isBlank()
                ? ((MethodSignature) pjp.getSignature()).getMethod().getName()
                : auditable.value();
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String user = auth != null ? auth.getName() : "anonymous";
        long start = System.currentTimeMillis();
        try {
            Object result = pjp.proceed();
            log.info("AUDIT user={} action={} status=SUCCESS durationMs={}",
                    user, action, System.currentTimeMillis() - start);
            return result;
        } catch (Throwable ex) {
            log.warn("AUDIT user={} action={} status=FAILED error={}", user, action, ex.getMessage());
            throw ex;
        }
    }
}
