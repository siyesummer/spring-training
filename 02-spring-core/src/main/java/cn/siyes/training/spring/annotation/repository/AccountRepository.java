package cn.siyes.training.spring.annotation.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;

//它是组件扫描识别的 Bean 注册标记。
//它表达类属于持久化层，后续可以参与 Spring 数据访问异常转换等基础设施。
@Repository
public class AccountRepository {
  private final JdbcTemplate jdbcTemplate;

  public AccountRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public BigDecimal findBalance(long accountId) {
    return jdbcTemplate.queryForObject(
        "SELECT balance FROM accounts WHERE id = ?",
        BigDecimal.class,
        accountId
    );
  }

  public int debit(long accountId, BigDecimal amount) {
    String sql = """
        UPDATE accounts
        SET balance = balance - ?
        WHERE id = ? AND balance >= ?
        """;

    return jdbcTemplate.update(sql, amount, accountId, amount);
  }

  public int credit(long accountId, BigDecimal amount) {
    String sql = """
        UPDATE accounts
        SET balance = balance + ?
        WHERE id = ?
        """;

    return jdbcTemplate.update(sql, amount, accountId);
  }
}
