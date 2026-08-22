package cn.siyes.training.mvc.controller;

import cn.siyes.training.mvc.dto.*;
import cn.siyes.training.mvc.model.Task;
import cn.siyes.training.mvc.model.TaskStatus;
import cn.siyes.training.mvc.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

  private final TaskService taskService;

  public TaskController(TaskService taskService) {
    this.taskService = taskService;
  }

  @GetMapping("/test")
  public Map<String, String> test() {
    System.out.println("测试");
    return Map.of("res", "测试");
  }

  @PostMapping
  public ResponseEntity<TaskResponse<Task>>
    insert(@Valid @RequestBody CreateTaskRequest createTaskRequest) {
    System.out.println("参数是什么" + createTaskRequest);
    final Task insert = taskService.create(createTaskRequest);
    final TaskResponse<Task> body = new TaskResponse<>(TaskCode.CREATED, insert, "创建成功");

    return ResponseEntity
        .status(HttpStatus.CREATED)
        .body(body);
  }

  @GetMapping("/{id}")
  public TaskResponse<Task> queryById(@PathVariable("id") Long id) {
    final Task task = taskService.queryById(id);
    return new TaskResponse<Task>(TaskCode.OK, task, "查询成功");
  }

  @GetMapping
  public PageResponse<Task> findPage(@Valid @ModelAttribute QueryPageRequest queryPageRequest) {
    final List<Task> pageList = taskService.findPage(queryPageRequest);

//    总数目
    final Long count = taskService.getCount(
        queryPageRequest.getKeyword(),
        queryPageRequest.getStatus());

    final PageData<Task> taskPageData =
        new PageData<Task>(count, pageList);

    return new PageResponse<Task>(TaskCode.OK, taskPageData, "查询分页成功");
  }

  @GetMapping("/count")
  public TaskResponse<Long> getCount(@RequestParam("keyword") String keyword, @RequestParam("status") TaskStatus status) {
    System.out.println("参数:" + keyword + " - " + status);
    final Long count = taskService.getCount(keyword, status);

    return new TaskResponse<Long>(TaskCode.OK, count, "查询数目成功");
  }

  @PutMapping("/{id}")
  public TaskResponse<Integer>
    updateTask(@PathVariable("id") Long id, @Valid @RequestBody UpdateTaskRequest updateTaskRequest) {
    final int count = taskService.updateTask(id, updateTaskRequest);

    return new TaskResponse<Integer>(TaskCode.OK, count, "更新完成");
  }

  @PatchMapping("/{id}/status")
  public TaskResponse<Integer> updateStatus(@PathVariable("id") Long id, @Valid @RequestBody UpdateStatusRequest updateStatusRequest) {
    System.out.println("参数---" + updateStatusRequest);

    final int count = taskService.updateStatus(id, updateStatusRequest);
    return new TaskResponse<Integer>(TaskCode.OK, count, "更新状态成功");
  }

  @DeleteMapping("/{id}")
  public TaskResponse<Integer> deleteTask(@PathVariable("id") Long id) {
    final int count = taskService.deleteById(id);
    return new TaskResponse<Integer>(TaskCode.OK, count, "删除任务完成");
  }
}
