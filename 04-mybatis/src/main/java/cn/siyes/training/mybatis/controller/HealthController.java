package cn.siyes.training.mybatis.controller;

import cn.siyes.training.mybatis.mapper.TaskMapper;
import cn.siyes.training.mybatis.service.TaskService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class HealthController {

  private final TaskMapper taskMapper;

  public HealthController(TaskMapper taskMapper) {
    this.taskMapper = taskMapper;
  }

  @GetMapping("/health")
  public Map<String, String> health() {

    System.out.println(taskMapper.getClass());
    return Map.of("status", "up");
  }
}
