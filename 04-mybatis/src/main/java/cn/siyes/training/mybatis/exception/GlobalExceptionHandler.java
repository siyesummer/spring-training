package cn.siyes.training.mybatis.exception;

import cn.siyes.training.mybatis.dto.TaskResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

//这个类不是普通业务类，而是负责集中处理所有 Controller 异常的全局组件，
// 并且处理结果直接写入 HTTP 响应体。
@RestControllerAdvice
public class GlobalExceptionHandler {
  @ExceptionHandler(TaskNotFoundException.class)
  public ResponseEntity<TaskResponse<Void>>
  handleNotFound(TaskNotFoundException exception,
                 HttpServletRequest request) {
    System.out.println("全局拦截-任务找不到");
    final TaskResponse<Void> taskResponse = new TaskResponse<>(
        404,
        null,
        exception.getMessage()
    );
    return ResponseEntity
        .status(HttpStatus.NOT_FOUND)
        .body(taskResponse);
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<TaskResponse<Void>> handleIllArg(IllegalArgumentException exception) {
    System.out.println("全局拦截-参数异常");

    final TaskResponse<Void> taskResponse = new TaskResponse<>(
        HttpStatus.BAD_REQUEST.value(),
        null,
        exception.getMessage()
    );

    return ResponseEntity
        .status(HttpStatus.BAD_REQUEST)
        .body(taskResponse);
  }

  @ExceptionHandler(IllegalStateException.class)
  public ResponseEntity<TaskResponse<Void>> handleIllState(IllegalStateException exception) {
    System.out.println("全局拦截-(参数)非法状态异常");

    final TaskResponse<Void> taskResponse = new TaskResponse<>(
        HttpStatus.BAD_REQUEST.value(),
        null,
        exception.getMessage()
    );

    return ResponseEntity
        .status(HttpStatus.BAD_REQUEST)
        .body(taskResponse);
  }

  @ExceptionHandler(BindException.class)
  public ResponseEntity<TaskResponse<Void>> handleBind(BindException exception) {
    System.out.println("全局拦截-ModelAttribute异常");

    final TaskResponse<Void> taskResponse = new TaskResponse<>(
        HttpStatus.BAD_REQUEST.value(),
        null,
        exception.getMessage()
    );

    return ResponseEntity
        .status(HttpStatus.BAD_REQUEST)
        .body(taskResponse);
  }

//  Spring MVC 在完成 JSON 转 DTO 后执行校验，
//  校验失败时抛出 MethodArgumentNotValidException，再由 @RestControllerAdvice 处理。
//  @Valid开启后对应校验规则未通过时触发
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<TaskResponse<Void>> handleValidation(
      MethodArgumentNotValidException exception) {
    System.out.println("全局拦截-MethodArgumentNotValidException");
    String message = exception.getBindingResult()
        .getFieldErrors()
        .getFirst()
        .getDefaultMessage();

    TaskResponse<Void> body = new TaskResponse<>(
        HttpStatus.BAD_REQUEST.value(),
        message
    );

    return ResponseEntity
        .badRequest()
        .body(body);
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<TaskResponse<Void>> handleUnreadableBody(
      HttpMessageNotReadableException exception) {
    System.out.println("全局拦截-HttpMessageNotReadableException");
    return ResponseEntity
        .badRequest()
        .body(new TaskResponse<>(
            400,
            "请求体格式错误或任务状态无效"
        ));
  }

  @ExceptionHandler(Exception.class) // 500 兜底
  public ResponseEntity<TaskResponse<Void>> handleCommon(Exception exception) {
    System.out.println("全局拦截-兜底");

    final TaskResponse<Void> taskResponse = new TaskResponse<>(
        500,
        null,
        "服务器内部错误"
    );

    return ResponseEntity
        .status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(taskResponse);
  }
}
