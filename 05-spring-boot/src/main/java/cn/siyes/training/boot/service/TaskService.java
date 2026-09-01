package cn.siyes.training.boot.service;

import cn.siyes.training.boot.dto.*;
import cn.siyes.training.boot.exception.TaskNotFoundException;
import cn.siyes.training.boot.mapper.TaskCommentMapper;
import cn.siyes.training.boot.mapper.TaskDetailMapper;
import cn.siyes.training.boot.model.Task;
import cn.siyes.training.boot.model.TaskComment;
import cn.siyes.training.boot.model.TaskDetail;
import cn.siyes.training.boot.model.TaskStatus;
import cn.siyes.training.boot.repository.TaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class TaskService {
  private final TaskRepository taskRepository;
  private final TaskCommentMapper taskCommentMapper;
  private final TaskDetailMapper taskDetailMapper;

  public TaskService(TaskRepository taskRepository, TaskCommentMapper taskCommentMapper, TaskDetailMapper taskDetailMapper) {
    this.taskRepository = taskRepository;
    this.taskCommentMapper = taskCommentMapper;
    this.taskDetailMapper = taskDetailMapper;
  }

  @Transactional
  public Task create(CreateTaskRequest taskRequest) {
    final Task task = new Task();
    task.setTitle(taskRequest.getTitle());
    task.setDescription(taskRequest.getDescription());
    task.setDueDate(taskRequest.getDueDate());
    System.out.println("mapper里创建");

    return taskRepository.insert(task);
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

  @Transactional
  public int updateTask(Long id, UpdateTaskRequest updateTaskRequest) {
    final Task task = new Task();
    task.setId(id);
    task.setTitle(updateTaskRequest.getTitle());
    task.setDueDate(updateTaskRequest.getDueDate());
    task.setDescription(updateTaskRequest.getDescription());

    return taskRepository.update(task);
  }

  @Transactional
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

  @Transactional
  public int createAndUpdateStatus(CreateTaskRequest taskRequest) {
    final Task task = new Task();
    task.setTitle(taskRequest.getTitle());
    task.setDescription(taskRequest.getDescription());
    task.setDueDate(taskRequest.getDueDate());

    final Task insert = taskRepository.insert(task);

    if (insert != null) {
//      throw new IllegalArgumentException("测试事务");
    }

    final UpdateStatusRequest updateStatusRequest = new UpdateStatusRequest();
    updateStatusRequest.setStatus(TaskStatus.DONE);

    return taskRepository.updateStatus(insert.getId(), updateStatusRequest.getStatus());
  }

  @Transactional
  public int createCommentsById(Long taskId, CreateCommentsRequest createCommentsRequest) {
    queryById(taskId);

    final List<String> comments = createCommentsRequest.getComments();

    final ArrayList<TaskComment> taskComments = new ArrayList<>();

    comments.forEach(comment -> {
      final TaskComment taskComment = new TaskComment();
      taskComment.setTaskId(taskId);
      taskComment.setContent(comment);
      taskComments.add(taskComment);
    });

    return taskCommentMapper.insertBatch(taskComments);

  }

  @Transactional(readOnly = true)
  public TaskDetail findDetailById(Long id) {

    final TaskDetail detailById = taskDetailMapper.findDetailById(id);

    if (detailById == null) {
      throw new TaskNotFoundException(id);
    }

    return detailById;
  }
}
