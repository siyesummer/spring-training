package cn.siyes.training.web.service;

import cn.siyes.training.web.dao.ConnectionFactory;
import cn.siyes.training.web.dao.MessageDao;
import cn.siyes.training.web.dao.MessageLogDao;
import cn.siyes.training.web.model.Message;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;

public class MessageService {
  public long insert(long userId, String content) throws SQLException {
    MessageDao messageDao = new MessageDao();

    try(Connection connection = ConnectionFactory.getConnection()) {
      long insert = messageDao.insert(connection, userId, content);

      return insert;
    }
  }

  public void publish(long userId, String content) throws SQLException {
    MessageDao messageDao = new MessageDao();
    MessageLogDao messageLogDao = new MessageLogDao();

    try(Connection connection = ConnectionFactory.getConnection()) {
      connection.setAutoCommit(false);

      try {
//        插入信息
        long msgId = messageDao.insertAndReturnId(connection, userId, content);
//        插入信息日志
        messageLogDao.insert(connection, msgId);
        connection.commit();
      } catch (SQLException e) {
        connection.rollback();

        throw e;
      }

    }
  }

  public ArrayList<Message> finAll() throws SQLException {
    try(Connection connection = ConnectionFactory.getConnection()) {
      MessageDao messageDao = new MessageDao();

      return messageDao.findALL(connection);
    }
  }
}
