package cn.siyes.training.mybatis.mapper;

import cn.siyes.training.mybatis.model.TaskDetail;

public interface TaskDetailMapper {
  TaskDetail findDetailById(Long taskId);
}
