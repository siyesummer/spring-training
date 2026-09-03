package cn.siyes.training.comprehensive.exception;

import cn.siyes.training.comprehensive.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

//这个类不是普通业务类，而是负责集中处理所有 Controller 异常的全局组件，
// 并且处理结果直接写入 HTTP 响应体。
@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler({OrderException.class, InsufficientStockException.class})
  public ResponseEntity<ApiResponse<Object>>
  handleOrderException(OrderException exception,
                 HttpServletRequest request) {
    System.out.println("全局拦截-订单异常: " + exception.getMessage());

    final ApiResponse<Object> resp = new ApiResponse<>(400, exception.getMessage());

    return ResponseEntity
        .status(HttpStatus.OK)
        .body(resp);
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<cn.siyes.training.comprehensive.dto.ApiResponse<Void>> handleIllArg(IllegalArgumentException exception) {
    System.out.println("全局拦截-参数异常");

    final ApiResponse<Void> ApiResponse = new ApiResponse<>(
        HttpStatus.BAD_REQUEST.value(),
        null,
        exception.getMessage()
    );

    return ResponseEntity
        .status(HttpStatus.BAD_REQUEST)
        .body(ApiResponse);
  }

//  Spring MVC 在完成 JSON 转 DTO 后执行校验，
//  校验失败时抛出 MethodArgumentNotValidException，再由 @RestControllerAdvice 处理。
//  @Valid开启后对应校验规则未通过时触发
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiResponse<Void>> handleValidation(
      MethodArgumentNotValidException exception) {
    System.out.println("全局拦截-MethodArgumentNotValidException");
    String message = exception.getBindingResult()
        .getFieldErrors()
        .getFirst()
        .getDefaultMessage();

    ApiResponse<Void> body = new ApiResponse<>(
        HttpStatus.BAD_REQUEST.value(),
        message
    );

    return ResponseEntity
        .badRequest()
        .body(body);
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ApiResponse<Void>> handleUnreadableBody(
      HttpMessageNotReadableException exception) {
    String message = exception.getMessage();

    System.out.println("全局拦截-HttpMessageNotReadableException" + message);
    return ResponseEntity
        .badRequest()
        .body(new ApiResponse<>(
            400,
            "请求体格式错误"
        ));
  }

  @ExceptionHandler(Exception.class) // 500 兜底
  public ResponseEntity<ApiResponse<Void>> handleCommon(Exception exception) {
    System.out.println("全局拦截-兜底");

    final ApiResponse<Void> ApiResponse = new ApiResponse<>(
        500,
        null,
        "服务器内部错误"
    );

    return ResponseEntity
        .status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(ApiResponse);
  }
}
