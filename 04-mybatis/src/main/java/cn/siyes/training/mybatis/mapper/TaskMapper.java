package cn.siyes.training.mybatis.mapper;

import cn.siyes.training.mybatis.model.Task;
import cn.siyes.training.mybatis.model.TaskQuery;
import org.apache.ibatis.annotations.Param;

import java.util.List;

//使用 @MapperScan 后不必在每个接口上再写 @Mapper
public interface TaskMapper {
  Task findById(Long id);

  int insert(Task task);

  int update(Task task);

  int deleteById(Long id);

  List<Task> findPage(
      @Param("query") TaskQuery query,
      @Param("offset") int offset,
      @Param("size") int size
  );

  Long count(@Param("query") TaskQuery query);
}
