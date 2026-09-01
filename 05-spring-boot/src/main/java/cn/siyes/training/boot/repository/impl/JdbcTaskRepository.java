package cn.siyes.training.boot.repository.impl;

import cn.siyes.training.boot.exception.TaskNotFoundException;
import cn.siyes.training.boot.model.Task;
import cn.siyes.training.boot.model.TaskStatus;
import cn.siyes.training.boot.repository.TaskRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

//@Repository
public class JdbcTaskRepository implements TaskRepository {
  private final JdbcTemplate jdbcTemplate;

  private final RowMapper<Task> taskRowMapper = (resultSet, rowNum) -> {
    final Task task = new Task();
    task.setId(resultSet.getLong("id"));
    task.setTitle(resultSet.getString("title"));
    task.setDescription(resultSet.getString("description"));
    task.setStatus(TaskStatus.valueOf(resultSet.getString("status")));
    task.setDueDate(resultSet.getObject("due_date", LocalDate.class));
    task.setCreatedAt(resultSet.getObject("created_at", LocalDateTime.class));
    task.setUpdatedAt(resultSet.getObject("updated_at", LocalDateTime.class));
    return task;
  };

  public JdbcTaskRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public Task insert(Task task) {
    String sql = """
        INSERT INTO tasks (title, description, status, due_date)
        VALUES (?, ?, ?, ?)
        """;
    final GeneratedKeyHolder generatedKeyHolder = new GeneratedKeyHolder();
    final int rows = jdbcTemplate.update(connection -> {
      final PreparedStatement statement = connection.prepareStatement(
          sql,
          Statement.RETURN_GENERATED_KEYS
      );
      statement.setString(1, task.getTitle());
      statement.setString(2, task.getDescription());
      statement.setString(3, task.getStatus().name());
      statement.setObject(4 ,task.getDueDate());

      return statement;
    }, generatedKeyHolder);

    if (rows != 1 || generatedKeyHolder.getKey() == null) {
      throw new IllegalStateException("任务创建失败");
    }

    final long id = generatedKeyHolder.getKey().longValue();

    System.out.println("创建成功");
    return findById(id).orElseThrow(
        () -> new IllegalStateException("任务创建后无法查询")
    );
  }

  @Override
  public Optional<Task> findById(Long id) {
    String sql = """
        SELECT * FROM tasks
        WHERE id = ?
        """;

    return jdbcTemplate.query(sql, taskRowMapper, id)
        .stream()
        .findFirst();
  }

  @Override
  public List<Task> findPage(String keyword, TaskStatus status, int offset, int limit) {
    final QueryParts queryParts = buildWhere(keyword, status);
    String sql = """
        SELECT * FROM tasks
        """ + queryParts.whereSql()
        + "ORDER BY created_at DESC, id DESC LIMIT ? OFFSET ?";

    final ArrayList<Object> parameters = new ArrayList<>(queryParts.parameters());

    parameters.add(limit);
    parameters.add(offset);

    System.out.println("查看whereSql:" + queryParts.whereSql());
    System.out.println("查看parameters:" + Arrays.toString(parameters.toArray()));

    return jdbcTemplate.query(sql, taskRowMapper, parameters.toArray());
  }

  @Override
  public long count(String keyword, TaskStatus status) {
    final QueryParts queryParts = buildWhere(keyword, status);
    String sql = "SELECT COUNT(*) FROM tasks" + queryParts.whereSql();
    final Long total = jdbcTemplate.queryForObject(
        sql,
        Long.class,
        queryParts.parameters().toArray()
    );
    return total == null ? 0 : total;
  }

  @Override
  public int update(Task task) {
    String sql = """
        UPDATE tasks
        SET title = ?, description = ?, due_date = ?
        WHERE id = ?
        """;

    final Optional<Task> byId = findById(task.getId());
    if (byId.isEmpty()) {
      throw new TaskNotFoundException(task.getId());
    }

    return jdbcTemplate.update(sql,
        task.getTitle(),
        task.getDescription(),
        task.getDueDate(),
        task.getId());
  }

  @Override
  public int updateStatus(Long id, TaskStatus status) {
    String sql = """
        UPDATE tasks
        SET status = ?
        WHERE id = ?
        """;

    final Optional<Task> byId = findById(id);
    if (byId.isEmpty()) {
      throw new TaskNotFoundException(id);
    }

    return jdbcTemplate.update(sql, status.name(), id);
  }

  @Override
  public int deleteById(Long id) {
    String sql = """
        DELETE FROM tasks
        WHERE id = ?
        """;

    final Optional<Task> byId = findById(id);
    if (byId.isEmpty()) {
      throw new TaskNotFoundException(id);
    }

    return jdbcTemplate.update(sql, id);
  }

  private QueryParts buildWhere(String keyword, TaskStatus status) {

    final StringBuilder where = new StringBuilder(" WHERE 1 = 1");
    final List<Object> parameters = new ArrayList<>();

    if (keyword != null && !keyword.isBlank()) {
      where.append(" AND title LIKE ?");
      parameters.add("%" + keyword.trim() + "%");
    }

    if (status != null) {
      where.append(" AND status = ?");
      parameters.add(status.name());
    }

    return new QueryParts(where.toString(), parameters);
  }

  private record QueryParts(String whereSql, List<Object> parameters) {}
}
