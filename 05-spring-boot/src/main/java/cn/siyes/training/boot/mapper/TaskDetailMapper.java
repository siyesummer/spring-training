package cn.siyes.training.boot.mapper;

import cn.siyes.training.boot.model.TaskDetail;

public interface TaskDetailMapper {
  TaskDetail findDetailById(Long taskId);
}
