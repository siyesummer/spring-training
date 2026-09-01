package cn.siyes.training.boot.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
public class HealthController {
  @GetMapping("/health")
  public Map<Object, Object> health() {
    final Map<Object, Object> map = new HashMap<>();
    map.put("status", "UP");
    return map;
  }
}
