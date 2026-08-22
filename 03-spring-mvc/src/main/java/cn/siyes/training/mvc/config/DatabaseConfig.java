package cn.siyes.training.mvc.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;

@Configuration
@EnableTransactionManagement
public class DatabaseConfig {

  @Bean(destroyMethod = "close")
  public HikariDataSource dataSource(Environment env) {
    final HikariConfig config = new HikariConfig();
    config.setJdbcUrl(env.getRequiredProperty("MVC_DB_URL"));
    config.setUsername(env.getRequiredProperty("MVC_DB_USERNAME"));
    config.setPassword(env.getRequiredProperty("MVC_DB_PASSWORD"));
    config.setDriverClassName("com.mysql.cj.jdbc.Driver");
    config.setPoolName("spring-mvc-training-pool");
    config.setMaximumPoolSize(10);
    config.setMinimumIdle(2);
    config.setConnectionTimeout(5000);
    config.setInitializationFailTimeout(5000);
    return new HikariDataSource(config);
  }

  @Bean
  public JdbcTemplate jdbcTemplate(DataSource dataSource) {
    return new JdbcTemplate(dataSource);
  }

  @Bean
  public PlatformTransactionManager transactionManager(DataSource dataSource) {
    return new DataSourceTransactionManager(dataSource);
  }
}
