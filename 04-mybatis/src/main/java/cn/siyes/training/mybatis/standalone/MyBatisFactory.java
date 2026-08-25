package cn.siyes.training.mybatis.standalone;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class MyBatisFactory {
  private static final SqlSessionFactory SQL_SESSION_FACTORY = build();

  private MyBatisFactory() {

  }

  public static SqlSessionFactory getSqlSessionFactory() {
    return SQL_SESSION_FACTORY;
  }

  private static SqlSessionFactory build() {
    final Properties properties = new Properties();
    properties.setProperty("db.url", requireEnv("MYBATIS_DB_URL"));
    properties.setProperty("db.username", requireEnv("MYBATIS_DB_USERNAME"));
    properties.setProperty("db.password", requireEnv("MYBATIS_DB_PASSWORD"));

    try(final InputStream resourceAsStream =
            Resources.getResourceAsStream("mybatis-config.xml")) {
      return new SqlSessionFactoryBuilder()
          .build(
              resourceAsStream,
              "development",
              properties
          );
    } catch (IOException e) {
      throw new IllegalStateException("读取mybatis配置失败", e);
    }
  }

  private static String requireEnv(String name) {
    final String val = System.getenv(name);
    if (val == null || val.isBlank()) {
      throw new IllegalStateException("环境变量缺失: " + name);
    }

    return val;
  }
}
