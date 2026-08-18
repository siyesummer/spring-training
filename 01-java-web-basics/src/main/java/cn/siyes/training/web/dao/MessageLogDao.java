package cn.siyes.training.web.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class MessageLogDao {
  public long insert(Connection connection, long messageId) throws SQLException {
    String sql = """
        INSERT INTO
        message_logs(message_id, action)
        VALUES(?, ?)
        """;
    try(PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
      preparedStatement.setLong(1, messageId);
      preparedStatement.setString(2, "CREATE");

      return preparedStatement.executeUpdate();
    }
  }
}
