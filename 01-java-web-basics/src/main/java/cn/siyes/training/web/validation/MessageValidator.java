package cn.siyes.training.web.validation;

public class MessageValidator {
  private static final int MAX_CONTENT_LENGTH = 500;

  public MessageValidator() {
  }

  public static boolean isValidContent(String content) {
    return content != null
        && !content.isBlank()
        && content.length() <= MAX_CONTENT_LENGTH;
  }
}
