package cn.siyes.training.boot.repository.impl;

import cn.siyes.training.boot.mapper.TaskMapper;
import cn.siyes.training.boot.model.Task;
import cn.siyes.training.boot.model.TaskStatus;
import cn.siyes.training.boot.repository.TaskRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class MyBatisTaskRepository implements TaskRepository {
  private final TaskMapper taskMapper;

  public MyBatisTaskRepository(TaskMapper taskMapper) {
    this.taskMapper = taskMapper;
  }

  @Override
  public Task insert(Task task) {
    taskMapper.insert(task);

    return taskMapper.findById(task.getId());
  }

  @Override
  public Optional<Task> findById(Long id) {
    return Optional.ofNullable(taskMapper.findById(id));
  }

  @Override
  public List<Task> findPage(String keyword, TaskStatus status, int offset, int limit) {
    return taskMapper.findPage(keyword, status.name(), offset, limit);
  }

  @Override
  public long count(String keyword, TaskStatus status) {
    return taskMapper.count(keyword, status.name());
  }

  @Override
  public int update(Task task) {
    return taskMapper.update(task);
  }

  @Override
  public int updateStatus(Long id, TaskStatus status) {
    return taskMapper.updateStatus(id, status.name());
  }

  @Override
  public int deleteById(Long id) {
    return taskMapper.deleteById(id);
  }
}
