package cn.siyes.training.boot.mapper;

import cn.siyes.training.boot.model.Task;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface TaskMapper {
  Task findById(Long id);

  int insert(Task task);

  int update(Task task);

  int updateStatus(@Param("id") Long id, @Param("status") String status);

  int deleteById(Long id);

  List<Task> findPage(
      @Param("keyword") String keyword,
      @Param("status") String status,
      @Param("offset") int offset,
      @Param("size") int size
  );

  long count(
      @Param("keyword") String keyword,
      @Param("status") String status
  );
}
