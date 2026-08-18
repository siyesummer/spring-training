package cn.siyes.training.web.exception;

public class UsernameAlreadyExistsException extends RuntimeException {
  public UsernameAlreadyExistsException(String username) {
    super("用户名已存在: " + username);
  }

  public UsernameAlreadyExistsException(String username, Throwable e) {
    super("用户名已存在: " + username, e);
  }
}
