package cn.siyes.training.spring.annotation.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Component
@Aspect
public class TimingAspect {
  @Around("execution(* cn.siyes.training.spring.annotation.service..*(..))")
  public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
    final long start = System.nanoTime();

    try {
      return joinPoint.proceed();
    } finally {
      long cost = (System.nanoTime() - start) / 1000000;

      System.out.println(
          joinPoint.getSignature().getName() + " cost=" + cost + "ms"
      );
    }

  }
}
