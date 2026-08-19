package cn.siyes.training.spring.xml.repository;

import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;

public class AccountRepository {
  private final JdbcTemplate jdbcTemplate;

  public AccountRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
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

//  余额查询
  public BigDecimal findBalance(long accountId) {
    return jdbcTemplate.queryForObject(
        "SELECT balance FROM accounts WHERE id = ?",
        BigDecimal.class,
        accountId
    );
  }
}
