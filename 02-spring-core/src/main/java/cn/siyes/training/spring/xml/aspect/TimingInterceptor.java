package cn.siyes.training.spring.xml.aspect;


import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

public class TimingInterceptor implements MethodInterceptor {

  @Override
  public Object invoke(MethodInvocation invocation) throws Throwable {
    final long start = System.nanoTime();

    try{
      return invocation.proceed();
    } finally {
      final long end = System.nanoTime();

      long cost = (end - start) / 1000000;

      System.out.println(invocation.getMethod().getName() + " cost" + cost + "ms");
    }
  }
}
