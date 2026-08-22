package cn.siyes.training.mvc.repository;

import cn.siyes.training.mvc.model.Task;
import cn.siyes.training.mvc.model.TaskStatus;

import java.util.List;
import java.util.Optional;

public interface TaskRepository {
  Task insert(Task task);
  Optional<Task> findById(Long id);
  List<Task> findPage(String keyword, TaskStatus status, int offset, int limit);
  long count(String keyword, TaskStatus status);
  int update(Task task);
  int updateStatus(Long id, TaskStatus status);
  int deleteById(Long id);
}
