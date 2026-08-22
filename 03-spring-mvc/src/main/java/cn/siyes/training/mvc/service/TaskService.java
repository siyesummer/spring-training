package cn.siyes.training.mvc.service;

import cn.siyes.training.mvc.dto.CreateTaskRequest;
import cn.siyes.training.mvc.dto.QueryPageRequest;
import cn.siyes.training.mvc.dto.UpdateStatusRequest;
import cn.siyes.training.mvc.dto.UpdateTaskRequest;
import cn.siyes.training.mvc.exception.TaskNotFoundException;
import cn.siyes.training.mvc.model.Task;
import cn.siyes.training.mvc.model.TaskStatus;
import cn.siyes.training.mvc.repository.TaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class TaskService {
  private final TaskRepository taskRepository;

  public TaskService(TaskRepository taskRepository) {
    this.taskRepository = taskRepository;
  }

  public Task create(CreateTaskRequest taskRequest) {
    final Task task = new Task();
    task.setTitle(taskRequest.getTitle());
    task.setDescription(taskRequest.getDescription());
    task.setDueDate(taskRequest.getDueDate());
    final Task insert = taskRepository.insert(task);

    return insert;
  }

  public Task queryById(Long id) {
    return taskRepository.findById(id)
        .orElseThrow(() ->
            new TaskNotFoundException(id)
        );
  }

  public List<Task> findPage(QueryPageRequest queryPageRequest) {
    int offset = (queryPageRequest.getPage() - 1) * queryPageRequest.getSize();
    return taskRepository.findPage(
        queryPageRequest.getKeyword(),
        queryPageRequest.getStatus(),
        offset,
        queryPageRequest.getSize());
  }

  public Long getCount(String keyword, TaskStatus status) {
    return taskRepository.count(keyword, status);
  }

  public int updateTask(Long id, UpdateTaskRequest updateTaskRequest) {
    final Task task = new Task();
    task.setId(id);
    task.setTitle(updateTaskRequest.getTitle());
    task.setDueDate(updateTaskRequest.getDueDate());
    task.setDescription(updateTaskRequest.getDescription());

    return taskRepository.update(task);
  }

  public int updateStatus(Long id, UpdateStatusRequest updateStatusRequest) {
    if (updateStatusRequest.getStatus() == null) {
      throw new IllegalArgumentException("任务状态不能为空");
    }
    return taskRepository.updateStatus(id, updateStatusRequest.getStatus());
  }

  public Integer deleteById(Long id) {
    if (id == null) {
      throw new IllegalArgumentException("任务id不能为空");
    }

    return taskRepository.deleteById(id);
  }
}
