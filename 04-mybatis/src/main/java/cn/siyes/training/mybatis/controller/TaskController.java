package cn.siyes.training.mybatis.controller;

import cn.siyes.training.mybatis.dto.*;
import cn.siyes.training.mybatis.model.Task;
import cn.siyes.training.mybatis.model.TaskDetail;
import cn.siyes.training.mybatis.model.TaskStatus;
import cn.siyes.training.mybatis.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

  private final TaskService taskService;

  public TaskController(TaskService taskService) {
    this.taskService = taskService;
  }

  @PostMapping
  public TaskResponse<TaskDetail> createTaskAndComments(
      @Valid
      @RequestBody CreateTaskRequest createTaskRequest) {
    final TaskDetail
        taskDetail = taskService.creatWithComment(createTaskRequest);

    return new TaskResponse<TaskDetail>(TaskCode.OK, taskDetail, "创建任务和评论成功");
  }

  @GetMapping("/{id}")
//  需要申明为id 否则参数找不到
  public TaskResponse<Task> findTaskById(@PathVariable("id") Long id) {
    final Task taskById = taskService.findTaskById(id);

    return new TaskResponse<Task>(TaskCode.OK, taskById, "查询任务成功");
  }

  @GetMapping
  public PageResponse<Task> findPage(
      @Valid @ModelAttribute QueryPageRequest queryPageRequest) {
    final Long count = taskService.getCount(queryPageRequest);

    final List<Task> pageList = taskService.findPage(queryPageRequest);

    final PageData<Task> taskPageData = new PageData<>(count, pageList);


    return new PageResponse<Task>(TaskCode.OK, taskPageData, "分页查询成功");
  }

  @PutMapping("/{id}")
//  需要申明为id 否则参数找不到
  public TaskResponse<Integer> updateTask(
      @PathVariable("id") Long id,
      @Valid @RequestBody UpdateTaskRequest updateTaskRequest) {
    final int count = taskService.updateTask(id, updateTaskRequest);

    return new TaskResponse<Integer>(TaskCode.OK, count, "修改任务成功");
  }

  @PatchMapping("/{id}")
//  需要申明为id 否则参数找不到
  public TaskResponse<Integer> updateStatus(
      @PathVariable("id") Long id,
      @Valid @RequestBody UpdateStatusRequest updateStatusRequest) {
    final int count = taskService.updateTaskStatus(id, updateStatusRequest);

    return new TaskResponse<Integer>(TaskCode.OK, count, "修改任务状态成功");
  }

  @DeleteMapping("/{id}")
//  需要申明为id 否则参数找不到
  public TaskResponse<Integer> deleteTaskById(
      @PathVariable("id") Long id
  ) {
    final int count = taskService.deleteById(id);

    return new TaskResponse<Integer>(TaskCode.OK, count, "删除任务成功");
  }

  @GetMapping("/{id}/detail")
//  需要申明为id 否则参数找不到
  public TaskResponse<TaskDetail> findDetailById(@PathVariable("id") Long id) {
    final TaskDetail detailById = taskService.findDetailById(id);

    return new TaskResponse<TaskDetail>(TaskCode.OK, detailById, "查询任务和评论成功");
  }

  @PostMapping("/{id}/comments")
//  需要申明为id 否则参数找不到
  public TaskResponse<Integer> createCommentsById(
      @PathVariable("id") Long id,
      @Valid @RequestBody CreateCommentsRequest createCommentsRequest
  ) {
    final int count = taskService.createCommentsById(id, createCommentsRequest);

    return new TaskResponse<Integer>(TaskCode.OK, count, "创建评论成功");
  }
}
