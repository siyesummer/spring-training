package cn.siyes.training.comprehensive.exception;

public class InsufficientStockException extends RuntimeException{
  public InsufficientStockException(String message) {
    super(message);
  }
}
