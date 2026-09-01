package cn.siyes.training.boot.mapper;

import cn.siyes.training.boot.model.TaskComment;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface TaskCommentMapper {
  int insertBatch(@Param("comments") List<TaskComment> comments);

  List<TaskComment> findByTaskId(Long taskId);

//  注解方式
  @Select("SELECT COUNT(*) FROM task_comments WHERE task_id = #{taskId}")
  long countByTaskId(Long taskId);
}
