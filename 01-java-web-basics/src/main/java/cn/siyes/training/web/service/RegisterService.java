package cn.siyes.training.web.service;

import at.favre.lib.crypto.bcrypt.BCrypt;
import cn.siyes.training.web.dao.ConnectionFactory;
import cn.siyes.training.web.dao.UserDao;
import cn.siyes.training.web.exception.UsernameAlreadyExistsException;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;

public class RegisterService {

  public long register(String username, String password) throws SQLException {
    UserDao userDao = new UserDao();

    try(Connection connection = ConnectionFactory.getConnection()) {
      boolean exists = userDao.existsByUsername(connection, username);

//      重复用户名
      if (exists) {
        throw new UsernameAlreadyExistsException(username);
      }

      String passwordHash = BCrypt.withDefaults()
          .hashToString(12, password.toCharArray());

      long insert = userDao.insert(connection, username, passwordHash);

      return insert;
    } catch (SQLIntegrityConstraintViolationException e){
//      数据库唯一索引异常
      throw new UsernameAlreadyExistsException(username, e);
    } catch (SQLException e) {
      System.out.println("报错了e" + e);
      throw e;
    }
  }
}
