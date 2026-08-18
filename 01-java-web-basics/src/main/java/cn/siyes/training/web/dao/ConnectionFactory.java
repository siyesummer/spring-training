package cn.siyes.training.web.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConnectionFactory {
  static {
    try {
      Class.forName("com.mysql.cj.jdbc.Driver");
      System.out.println("MySQL 驱动加载成功");
    } catch (ClassNotFoundException e) {
      throw new IllegalStateException("Tomcat 运行时找不到 MySQL JDBC 驱动", e);
    }
  }

  public static Connection getConnection() throws SQLException {
    return DriverManager.getConnection(
        requireEnv("TRAINING_DB_URL"),
        requireEnv("TRAINING_DB_USER"),
        requireEnv("TRAINING_DB_PASSWORD")
    );
  }

  private static String requireEnv(String name) {
    String value = System.getenv(name);
    if (value == null || value.isBlank()) {
      throw new IllegalStateException("缺少环境变量：" + name);
    }
    return value;
  }
}
