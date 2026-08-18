package cn.siyes.training.web.exception;

public class InvalidCredentialsException extends RuntimeException {
  public InvalidCredentialsException() {
    super("用户名或密码错误");
  }
}
