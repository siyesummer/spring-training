package cn.siyes.training.web.dao;

import cn.siyes.training.web.model.Message;

import java.sql.*;
import java.util.ArrayList;

public class MessageDao {

  public long insert(Connection connection, long userId, String content) throws SQLException {
    String sql = """
        INSERT INTO
        messages(user_id, content)
        VALUES(?, ?)
        """;
    try(PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
      preparedStatement.setLong(1, userId);
      preparedStatement.setString(2, content);

      return preparedStatement.executeUpdate();
    }
  }

  public long insertAndReturnId(Connection connection, long userId, String content) throws SQLException {
    String sql = """
        INSERT INTO
        messages(user_id, content)
        VALUES(?, ?)
        """;
    try(PreparedStatement preparedStatement = connection.prepareStatement(
        sql, Statement.RETURN_GENERATED_KEYS)) {
      preparedStatement.setLong(1, userId);
      preparedStatement.setString(2, content);

      int affecteRows = preparedStatement.executeUpdate();
      if (affecteRows != 1) {
        throw new SQLException("新增留言失败，受影响行数不为1");
      }

      try(ResultSet generatedKeys = preparedStatement.getGeneratedKeys()){
        if (generatedKeys.next()) {
          return generatedKeys.getLong(1);
        }
      }
    }

    throw new SQLException("新增留言成功但未取得生成的主键");
  }

  public ArrayList<Message> findALL(Connection connection) throws SQLException {
    String sql = """
        SELECT id, user_id, content, created_at
        FROM messages
        ORDER BY created_at DESC, id DESC
        """;

    try(PreparedStatement preparedStatement = connection.prepareStatement(sql);) {
      try(ResultSet resultSet = preparedStatement.executeQuery();){

        ArrayList<Message> messageArrayList = new ArrayList<>();

        while (resultSet.next()) {
          long id = resultSet.getLong("id");
          long userId = resultSet.getLong("user_id");
          String content = resultSet.getString("content");
          Timestamp createdAt = resultSet.getTimestamp("created_at");

          Message message = new Message(id, userId, content, createdAt);
          messageArrayList.add(message);
        }

        return messageArrayList;
      }
    }
  }
}
