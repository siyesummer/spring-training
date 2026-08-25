package cn.siyes.training.mybatis.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
public class DatabaseConfig {

  @Bean(destroyMethod = "close")
  public HikariDataSource dataSource(Environment env) {
    final HikariConfig config = new HikariConfig();
    config.setJdbcUrl(env.getRequiredProperty("MYBATIS_DB_URL"));
    config.setUsername(env.getRequiredProperty("MYBATIS_DB_USERNAME"));
    config.setPassword(env.getRequiredProperty("MYBATIS_DB_PASSWORD"));
    config.setDriverClassName("com.mysql.cj.jdbc.Driver");
    config.setPoolName("mybatis-training-pool");
    config.setMaximumPoolSize(10);
    config.setMinimumIdle(2);
    config.setConnectionTimeout(5000);
    config.setInitializationFailTimeout(5000);
    return new HikariDataSource(config);
  }
}
