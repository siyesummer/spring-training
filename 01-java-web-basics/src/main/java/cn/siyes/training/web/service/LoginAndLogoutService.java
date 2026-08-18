package cn.siyes.training.web.service;

import at.favre.lib.crypto.bcrypt.BCrypt;
import cn.siyes.training.web.dao.ConnectionFactory;
import cn.siyes.training.web.dao.UserDao;
import cn.siyes.training.web.exception.InvalidCredentialsException;
import cn.siyes.training.web.model.User;

import java.sql.Connection;
import java.sql.SQLException;

public class LoginAndLogoutService {
  public User getUser(String username, String password) throws SQLException {
    UserDao userDao = new UserDao();

    try(Connection connection = ConnectionFactory.getConnection()) {
      User user = userDao.getUser(connection, username);

      if (user == null) {
        throw new InvalidCredentialsException();
      }

      BCrypt.Result verify = BCrypt.verifyer()
          .verify(password.toCharArray(), user.getPasswordHash());

      if (!verify.verified) {
        throw new InvalidCredentialsException();
      }

      return user;
    }
  }
}
