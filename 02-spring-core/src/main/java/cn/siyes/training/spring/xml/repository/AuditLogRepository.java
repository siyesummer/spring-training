package cn.siyes.training.spring.xml.repository;

import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;

public class AuditLogRepository {
  private final JdbcTemplate jdbcTemplate;

  public AuditLogRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public int insert(long fromAccountId, long toAccountId, BigDecimal amount) {
    String sql = """
        INSERT INTO transfer_logs (from_account_id, to_account_id, amount)
        VALUES (?, ?, ?)
        """;

    return jdbcTemplate.update(sql, fromAccountId, toAccountId, amount);
  }
}
