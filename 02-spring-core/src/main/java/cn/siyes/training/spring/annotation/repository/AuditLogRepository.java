package cn.siyes.training.spring.annotation.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;

@Repository
public class AuditLogRepository {
  private final JdbcTemplate jdbcTemplate;

  public AuditLogRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public int insert(long formAccountId,
                    long toAccountId,
                    BigDecimal amount) {
    String sql = """
        INSERT INTO transfer_logs
            (from_account_id, to_account_id, amount)
        VALUES (?, ?, ?)
        """;

    return jdbcTemplate.update(sql, formAccountId, toAccountId, amount);
  }
}
