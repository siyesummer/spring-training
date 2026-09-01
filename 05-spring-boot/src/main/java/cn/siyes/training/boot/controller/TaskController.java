package cn.siyes.training.boot.controller;

import cn.siyes.training.boot.dto.*;
import cn.siyes.training.boot.model.Task;
import cn.siyes.training.boot.model.TaskDetail;
import cn.siyes.training.boot.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/tasks")
public class TaskController {

  private final TaskService taskService;

  public TaskController(TaskService taskService) {
    this.taskService = taskService;
  }

  @GetMapping("/{id}")
  public TaskResponse<Task> findTask(@PathVariable("id") Long id) {

    final Task byId = taskService.queryById(id);

    return new TaskResponse<Task>(TaskCode.OK, byId, "查询任务成功");
  }

  @PostMapping
  public TaskResponse<Task> createTask(@Valid  @RequestBody CreateTaskRequest createTaskRequest) {

    final Task task = taskService.create(createTaskRequest);

    return new TaskResponse<Task>(TaskCode.OK, task, "创建任务成功");
  }

  @DeleteMapping("/{id}")
  public TaskResponse<Integer> deleteTaskById(@PathVariable("id") Long id) {

    final Integer i = taskService.deleteById(id);

    return new TaskResponse<Integer>(TaskCode.OK, i, "删除任务成功");
  }

  @PutMapping("/{id}")
  public TaskResponse<Integer> updateTask(
      @PathVariable("id") Long id,
      @Valid @RequestBody UpdateTaskRequest updateTaskRequest) {

    final int i = taskService.updateTask(id, updateTaskRequest);

    return new TaskResponse<Integer>(TaskCode.OK, i, "修改任务成功");
  }

  @PatchMapping("/{id}")
  public TaskResponse<Integer> updateTaskStatus(
      @PathVariable("id") Long id,
      @Valid @RequestBody UpdateStatusRequest updateStatusRequest) {

    final int i = taskService.updateStatus(id, updateStatusRequest);

    return new TaskResponse<Integer>(TaskCode.OK, i, "修改任务状态成功");
  }

  @GetMapping
  public PageResponse<Task> findPage(@ModelAttribute QueryPageRequest queryPageRequest) {

    final List<Task> page = taskService.findPage(queryPageRequest);
    final Long count = taskService.getCount(queryPageRequest.getKeyword(), queryPageRequest.getStatus());

    final PageData<Task> taskPageData = new PageData<>(count, page);


    return new PageResponse<Task>(TaskCode.OK, taskPageData, "分页查询任务成功");
  }

  @PostMapping("/createAndUpdateStatus")
  public TaskResponse<Integer> createTaskAndUpdateStatus(@Valid @RequestBody CreateTaskRequest createTaskRequest) {

    final int count = taskService.createAndUpdateStatus(createTaskRequest);


    return new TaskResponse<Integer>(TaskCode.OK, count, "创建任务并修改状态-事务测试");
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

  @GetMapping("/{id}/detail")
//  需要申明为id 否则参数找不到
  public TaskResponse<TaskDetail> findDetailById(@PathVariable("id") Long id) {
    final TaskDetail detailById = taskService.findDetailById(id);

    return new TaskResponse<TaskDetail>(TaskCode.OK, detailById, "查询任务和评论成功");
  }

}
