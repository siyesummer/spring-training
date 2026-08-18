package cn.siyes.training.web.validation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MessageValidatorTest {
  @Test
  void acceptsNormalContent() {
    assertTrue(MessageValidator.isValidContent("第一条留言"));
  }

  @Test
  void rejectsNullEmptyAndBlankContent() {
    assertFalse(MessageValidator.isValidContent(null));
    assertFalse(MessageValidator.isValidContent(""));
    assertFalse(MessageValidator.isValidContent("   "));
  }

  @Test
  void acceptsExactly500Characters() {
    assertTrue(MessageValidator.isValidContent("a".repeat(500)));
  }

  @Test
  void rejectsMoreThan500Characters() {
    assertFalse(MessageValidator.isValidContent("a".repeat(501)));
  }
}
