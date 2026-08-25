import cn.siyes.training.mybatis.mapper.TaskCommentMapper;
import cn.siyes.training.mybatis.mapper.TaskDetailMapper;
import cn.siyes.training.mybatis.mapper.TaskMapper;
import cn.siyes.training.mybatis.model.*;
import cn.siyes.training.mybatis.standalone.MyBatisFactory;
import org.apache.ibatis.session.SqlSession;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class NewTaskTest {
  @Test
  public void insert() {
    try(final SqlSession sqlSession =
            MyBatisFactory.getSqlSessionFactory().openSession()) {
      final TaskMapper mapper = sqlSession.getMapper(TaskMapper.class);

      final Task task = new Task();
      task.setTitle("早上起来第一件事");
      task.setDescription("大喊一声旺旺");
      task.setStatus(TaskStatus.DONE);
      task.setDueDate(LocalDate.parse("2026-08-24"));
      final int insert = mapper.insert(task);
      sqlSession.commit();

      System.out.println("掺入几条: " + insert + ",新数据id为：" + task.getId());
    }
  }

  @Test
  public void findTask() {
    try(final SqlSession sqlSession =
            MyBatisFactory.getSqlSessionFactory().openSession()) {
      final TaskMapper taskMapper = sqlSession.getMapper(TaskMapper.class);

      final Task byId = taskMapper.findById(10L);
      System.out.println(byId.toString());

      System.out.println("第二次");
//      sqlSession.clearCache();
      final Task byId1 = taskMapper.findById(10L);
      System.out.println(byId1.toString());
    }
  }

  @Test
  public void update() {
    try(final SqlSession sqlSession =
            MyBatisFactory.getSqlSessionFactory().openSession()) {
      final TaskMapper mapper = sqlSession.getMapper(TaskMapper.class);

      final Task task = new Task();
      task.setId(4L);
      task.setTitle("9早上起来第一件事");
      task.setDescription("8大喊一声旺旺");
      task.setStatus(TaskStatus.TODO);
      task.setDueDate(LocalDate.parse("2027-08-24"));
      final int insert = mapper.update(task);
      sqlSession.commit();

      System.out.println("修改后task: " + task);
    }
  }

  @Test
  public void delete() {
    try(final SqlSession sqlSession =
            MyBatisFactory.getSqlSessionFactory().openSession()) {
      final TaskMapper mapper = sqlSession.getMapper(TaskMapper.class);

      final int count = mapper.deleteById(3L);
      sqlSession.commit();

      System.out.println("删除成功: " + count);
    }
  }

  @Test
  public void count() {
    try(final SqlSession sqlSession =
            MyBatisFactory.getSqlSessionFactory().openSession()) {
      final TaskMapper mapper = sqlSession.getMapper(TaskMapper.class);

      final TaskQuery taskQuery = new TaskQuery();
      taskQuery.setKeyword("8大");

      final Long count = mapper.count(taskQuery);

      System.out.println("查询成功(几条): " + count);
    }
  }

  @Test
  public void findPage() {
    try(final SqlSession sqlSession =
            MyBatisFactory.getSqlSessionFactory().openSession()) {
      final TaskMapper mapper = sqlSession.getMapper(TaskMapper.class);

      final TaskQuery taskQuery = new TaskQuery();

      taskQuery.setKeyword("");
      taskQuery.setPage(1);
      taskQuery.setSize(20);
      taskQuery.setKeyword("早上");
      taskQuery.setStatus(TaskStatus.DONE);
      taskQuery.setSortBy("dueDate");
      taskQuery.setDirection("asc");
      int offset = (taskQuery.getPage() - 1) * taskQuery.getSize();
      final List<Task>
          page = mapper.findPage(taskQuery, offset, taskQuery.getSize());

      System.out.println("分页查询");
      page.forEach(System.out::println);

//      恶意排序值
      taskQuery.setSortBy("id desc; delete from tasks");
      final List<Task>
          page1 = mapper.findPage(taskQuery, offset, taskQuery.getSize());

      System.out.println("恶意排序值");
      page1.forEach(System.out::println);

    }
  }

  @Test
  public void insertComment() {
    try(final SqlSession sqlSession =
            MyBatisFactory.getSqlSessionFactory().openSession()) {
      final TaskCommentMapper mapper = sqlSession.getMapper(TaskCommentMapper.class);

      final TaskComment taskComment = new TaskComment();
      taskComment.setTaskId(4L);
      taskComment.setContent("第二条评论");

      final ArrayList<TaskComment> taskComments = new ArrayList<>();
      taskComments.add(taskComment);

      final int count = mapper.insertBatch(taskComments);
      sqlSession.commit();

      System.out.println("评论插入(几条): " + count);
    }
  }

  @Test
  public void findByTaskId() {
    try(final SqlSession sqlSession =
            MyBatisFactory.getSqlSessionFactory().openSession()) {
      final TaskCommentMapper mapper = sqlSession.getMapper(TaskCommentMapper.class);

      final List<TaskComment> byTaskId = mapper.findByTaskId(4L);

      byTaskId.forEach(System.out::println);
    }
  }

  @Test
  public void findDetailById() {
    try(final SqlSession sqlSession =
            MyBatisFactory.getSqlSessionFactory().openSession()) {
      final TaskDetailMapper mapper = sqlSession.getMapper(TaskDetailMapper.class);

      final TaskDetail detailById = mapper.findDetailById(4L);

      System.out.println(detailById);
    }
  }

  @Test
  public void insertTaskAndComment() {
    try(final SqlSession sqlSession =
            MyBatisFactory.getSqlSessionFactory().openSession()) {
      try {
        final TaskMapper taskMapper =
            sqlSession.getMapper(TaskMapper.class);
        final TaskCommentMapper commentMapper =
            sqlSession.getMapper(TaskCommentMapper.class);

        final Task task = new Task();
        task.setTitle("早2上起来第一件事");
        task.setDescription("大喊一声旺旺");
        task.setStatus(TaskStatus.DONE);
        task.setDueDate(LocalDate.parse("2026-08-24"));
//        插入任务
        final int insert = taskMapper.insert(task);

        if (insert == 1) {
//          throw new RuntimeException("主动制造错误");
        }

        final TaskComment taskComment = new TaskComment();
//        task.getId()是新增数据的id
        taskComment.setTaskId(task.getId());
        taskComment.setContent("插入任务和评论");

        final ArrayList<TaskComment> taskComments = new ArrayList<>();
        taskComments.add(taskComment);


//        插入评论
        final int count = commentMapper.insertBatch(taskComments);

        sqlSession.commit();

        final TaskDetailMapper detailMapper =
            sqlSession.getMapper(TaskDetailMapper.class);

        final TaskDetail detailById = detailMapper.findDetailById(task.getId());
//        显示新创建的任务和评论
        System.out.println(detailById);

      } catch (RuntimeException e) {
        sqlSession.rollback();
        throw new RuntimeException(e);
      }
    }
  }

  @Test
  public void countCommentByTaskId() {
    try(final SqlSession sqlSession =
            MyBatisFactory.getSqlSessionFactory().openSession()) {
      final TaskCommentMapper commentMapper = sqlSession.getMapper(TaskCommentMapper.class);
      final long count = commentMapper.countByTaskId(4L);

      System.out.println("这个任务有几条评论: " + count);

    }
  }
}
