package cn.siyes.training.mybatis.service;

import cn.siyes.training.mybatis.dto.*;
import cn.siyes.training.mybatis.exception.TaskNotFoundException;
import cn.siyes.training.mybatis.mapper.TaskCommentMapper;
import cn.siyes.training.mybatis.mapper.TaskDetailMapper;
import cn.siyes.training.mybatis.mapper.TaskMapper;
import cn.siyes.training.mybatis.model.Task;
import cn.siyes.training.mybatis.model.TaskComment;
import cn.siyes.training.mybatis.model.TaskDetail;

import cn.siyes.training.mybatis.model.TaskQuery;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class TaskService {
  private final TaskMapper taskMapper;
  private final TaskCommentMapper taskCommentMapper;
  private final TaskDetailMapper taskDetailMapper;

  public TaskService(TaskMapper taskMapper, TaskCommentMapper taskCommentMapper, TaskDetailMapper taskDetailMapper) {
    this.taskMapper = taskMapper;
    this.taskCommentMapper = taskCommentMapper;
    this.taskDetailMapper = taskDetailMapper;
  }

//  创建任务和评论
  @Transactional
  public TaskDetail creatWithComment(CreateTaskRequest createTaskRequest) {
    final Task task = new Task();
    task.setTitle(createTaskRequest.getTitle());
    task.setDescription(createTaskRequest.getDescription());
    task.setDueDate(createTaskRequest.getDueDate());
//    添加任务
    final int insert = taskMapper.insert(task);

    if (insert == 1) {
//      throw new IllegalStateException("事务回滚练习");
    }

    final ArrayList<TaskComment> taskComments = new ArrayList<>();
    final List<String> initialComments =
        createTaskRequest.getInitialComments();
    if (initialComments != null) {
//      插入评论
      initialComments.forEach(comment -> {
        final TaskComment taskComment = new TaskComment();
        //    task.getId()存储着新建任务的id
        taskComment.setTaskId(task.getId());
        taskComment.setContent(comment);
        taskComments.add(taskComment);
      });
    }

    if (!taskComments.isEmpty()) {
      //    添加新任务额度评论
      taskCommentMapper.insertBatch(taskComments);
    }

//    查询最新创建的任务和评论
    return findDetailById(task.getId());
  }

  @Transactional(readOnly = true)
  public Long getCount(QueryPageRequest queryPageRequest) {
    final TaskQuery taskQuery = getTaskQuery(queryPageRequest);

    return taskMapper.count(taskQuery);
  }

  @Transactional(readOnly = true)
  public List<Task> findPage(QueryPageRequest queryPageRequest) {
    final TaskQuery taskQuery = getTaskQuery(queryPageRequest);

    int offset = (taskQuery.getPage() - 1) * taskQuery.getSize();

    return taskMapper.findPage(taskQuery, offset, taskQuery.getSize());
  }

  @Transactional
  public int updateTask(Long taskId, UpdateTaskRequest updateTaskRequest) {
    findTaskById(taskId);

    final Task task = new Task();
    task.setId(taskId);
    task.setTitle(updateTaskRequest.getTitle());
    task.setDescription(updateTaskRequest.getDescription());
    task.setDueDate(updateTaskRequest.getDueDate());
    return taskMapper.update(task);
  }

  @Transactional
  public int updateTaskStatus(Long taskId, UpdateStatusRequest updateStatusRequest) {
    findTaskById(taskId);

    final Task task = new Task();
    task.setId(taskId);
    task.setStatus(updateStatusRequest.getStatus());
    return taskMapper.update(task);
  }

  @Transactional
  public int deleteById(Long taskId) {
    findTaskById(taskId);

    return taskMapper.deleteById(taskId);
  }

  @Transactional(readOnly = true)
  public Task findTaskById(Long taskId) {
    final Task byId = taskMapper.findById(taskId);
    if (byId == null) {
      throw new TaskNotFoundException(taskId);
    }
    return byId;
  }

  @Transactional(readOnly = true)
  public TaskDetail findDetailById(Long id) {

    final TaskDetail detailById = taskDetailMapper.findDetailById(id);

    if (detailById == null) {
      throw new TaskNotFoundException(id);
    }

    return detailById;
  }

  @Transactional
  public int createCommentsById(Long taskId, CreateCommentsRequest createCommentsRequest) {
    findTaskById(taskId);

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

  public TaskQuery getTaskQuery(QueryPageRequest queryPageRequest) {
    final TaskQuery taskQuery = new TaskQuery();
    taskQuery.setKeyword(queryPageRequest.getKeyword());
    taskQuery.setStatus(queryPageRequest.getStatus());
    taskQuery.setPage(queryPageRequest.getPage());
    taskQuery.setSize(queryPageRequest.getSize());
    taskQuery.setSortBy(queryPageRequest.getSortBy());
    taskQuery.setDirection(queryPageRequest.getDirection());

    return taskQuery;
  }
}
