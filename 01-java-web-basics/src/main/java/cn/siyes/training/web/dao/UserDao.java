package cn.siyes.training.web.dao;

import cn.siyes.training.web.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UserDao {

  public boolean existsByUsername(Connection connection, String username) throws SQLException {

    String sql = """
        SELECT 1
        FROM users
        WHERE username = ?
        LIMIT 1
        """;

      try(PreparedStatement preparedStatement = connection.prepareStatement(sql);) {
        preparedStatement.setString(1, username);

        ResultSet resultSet = preparedStatement.executeQuery();

        return resultSet.next();
      }

  }

  public User getUser(Connection connection, String username) throws SQLException {
    String sql = """
        SELECT id, username, password_hash
        FROM users
        WHERE username = ?
        LIMIT 1
        """;

    try(PreparedStatement preparedStatement = connection.prepareStatement(sql);) {
      preparedStatement.setString(1, username);

      try(ResultSet resultSet = preparedStatement.executeQuery();){
        if (!resultSet.next()) {
          return null;
        }
        return new User(
            resultSet.getLong("id"),
            resultSet.getString("username"),
            resultSet.getString("password_hash")
        );
      }
    }
  }

  public long insert(Connection connection, String username, String passwordHash) throws SQLException {
    String sql = """
        INSERT INTO
        users(username, password_hash)
        VALUES(?, ?)
        """;

    try(PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
      preparedStatement.setString(1, username);
      preparedStatement.setString(2, passwordHash);

      return preparedStatement.executeUpdate();
    }
  }
}
