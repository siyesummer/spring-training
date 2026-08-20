package cn.siyes.training.spring.annotation.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;

@Configuration
@ComponentScan("cn.siyes.training.spring.annotation") // 开启组件扫描
//会向容器注册事务基础设施，使 Spring 查找 @Transactional 并创建事务代理。
// 它不会自己创建数据库连接；仍然需要 @Bean transactionManager
@EnableTransactionManagement
//proxyTargetClass = true 对应 XML 的 proxy-target-class="true"，使用 CGLIB 类代理。
// 当前 AccountService 没有实现接口，所以仍按 AccountService.class 获取更直观。若改用接口，则可以比较 JDK 动态代理和 CGLIB 的差异。
@EnableAspectJAutoProxy(proxyTargetClass = true)
public class AnnotationConfig {

//  第三方类，不能在它们的源码上添加 @Component。它们由 @Bean 方法注册。
  @Bean
  public DataSource dataSource(Environment env) {
    final DriverManagerDataSource driverManagerDataSource =
        new DriverManagerDataSource();
    driverManagerDataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
    driverManagerDataSource.setUrl(env.getRequiredProperty("DB_URL"));
    driverManagerDataSource.setUsername(env.getRequiredProperty("DB_USERNAME"));
    driverManagerDataSource.setPassword(env.getRequiredProperty("DB_PASSWORD"));

    return driverManagerDataSource;
  }

//  @Bean 方法的参数不是普通方法调用参数，而是 Spring 解析依赖后传入的 Bean 引用。
  @Bean
  public JdbcTemplate jdbcTemplate(DataSource dataSource) {
    return new JdbcTemplate(dataSource);
  }

  @Bean
  public PlatformTransactionManager transactionManager(DataSource dataSource) {
    return new DataSourceTransactionManager(dataSource);
  }
}
