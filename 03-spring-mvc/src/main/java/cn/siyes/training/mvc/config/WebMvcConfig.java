package cn.siyes.training.mvc.config;

import cn.siyes.training.mvc.interceptor.RequestTraceInterceptor;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
@ComponentScan("cn.siyes.training.mvc")
//注册注解路由、参数解析、返回值处理、消息转换等 MVC 基础设施
@EnableWebMvc
public class WebMvcConfig implements WebMvcConfigurer {
  private final RequestTraceInterceptor requestTraceInterceptor;

  public WebMvcConfig(RequestTraceInterceptor requestTraceInterceptor) {
    this.requestTraceInterceptor = requestTraceInterceptor;
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(requestTraceInterceptor)
        .addPathPatterns("/api/**");
  }

  @Override
  public void addCorsMappings(CorsRegistry registry) {
//    浏览器的跨源访问约束，按需添加
    registry.addMapping("/api/**")
        .allowedOrigins("http://localhost:5173")
        .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
        .allowedHeaders("*")
        .allowCredentials(true)
        .maxAge(3600);
  }

  @Override
  public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
//    关闭时间戳形式
    converters.stream()
        .filter(MappingJackson2HttpMessageConverter.class::isInstance)
        .map(MappingJackson2HttpMessageConverter.class::cast)
        .forEach(converter ->
            converter.getObjectMapper().disable(
                SerializationFeature.WRITE_DATES_AS_TIMESTAMPS
            )
        );
  }
}
