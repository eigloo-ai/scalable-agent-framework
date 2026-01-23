package ai.eigloo.agentic.graphcomposer.config;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Aspect for monitoring database query performance.
 * Logs slow queries and provides performance metrics.
 */
@Aspect
@Component
@ConditionalOnProperty(name = "graph-composer.performance.enable-pool-monitoring", havingValue = "true")
public class DatabasePerformanceAspect {

    private static final Logger logger = LoggerFactory.getLogger(DatabasePerformanceAspect.class);
    private static final long SLOW_QUERY_THRESHOLD_MS = 1000; // 1 second

    /**
     * Monitor repository method execution times.
     */
    @Around("execution(* ai.eigloo.agentic.graph.repository.*.*(..))")
    public Object monitorRepositoryMethods(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        String methodName = joinPoint.getSignature().toShortString();
        
        try {
            Object result = joinPoint.proceed();
            long executionTime = System.currentTimeMillis() - startTime;
            
            if (executionTime > SLOW_QUERY_THRESHOLD_MS) {
                logger.warn("Slow query detected: {} took {}ms", methodName, executionTime);
            } else if (logger.isDebugEnabled()) {
                logger.debug("Query executed: {} took {}ms", methodName, executionTime);
            }
            
            return result;
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            logger.error("Query failed: {} took {}ms, error: {}", methodName, executionTime, e.getMessage());
            throw e;
        }
    }

    /**
     * Monitor service method execution times for database operations.
     */
    @Around("execution(* ai.eigloo.agentic.graphcomposer.service.*.*(..))")
    public Object monitorServiceMethods(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        String methodName = joinPoint.getSignature().toShortString();
        
        try {
            Object result = joinPoint.proceed();
            long executionTime = System.currentTimeMillis() - startTime;
            
            if (executionTime > SLOW_QUERY_THRESHOLD_MS) {
                logger.warn("Slow service operation: {} took {}ms", methodName, executionTime);
            } else if (logger.isDebugEnabled()) {
                logger.debug("Service operation: {} took {}ms", methodName, executionTime);
            }
            
            return result;
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            logger.error("Service operation failed: {} took {}ms, error: {}", methodName, executionTime, e.getMessage());
            throw e;
        }
    }
}