package cn.siyes.training.web.model;

import java.sql.Timestamp;

public class Message {
  private final long id;
  private final long userId;
  private final String content;
  private final Timestamp createAt;

  public Message(long id, long userId, String content, Timestamp createAt) {
    this.id = id;
    this.userId = userId;
    this.content = content;
    this.createAt = createAt;
  }

  public long getId() {
    return id;
  }

  public long getUserId() {
    return userId;
  }

  public String getContent() {
    return content;
  }

  public Timestamp getCreateAt() {
    return createAt;
  }
}
