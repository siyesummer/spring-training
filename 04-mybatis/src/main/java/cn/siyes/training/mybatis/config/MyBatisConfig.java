package cn.siyes.training.mybatis.config;

import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;

@Configuration
@EnableTransactionManagement
//mapper接口扫描
@MapperScan("cn.siyes.training.mybatis.mapper")
public class MyBatisConfig {

  @Bean
  public SqlSessionFactory sqlSessionFactory(DataSource dataSource) throws Exception {
    final SqlSessionFactoryBean factoryBean =
        new SqlSessionFactoryBean();
//    数据源
    factoryBean.setDataSource(dataSource);
//    数据类型
    factoryBean.setTypeAliasesPackage(
        "cn.siyes.training.mybatis.model");

    final PathMatchingResourcePatternResolver
        resolver = new PathMatchingResourcePatternResolver();
// mapper sql xml配置
    factoryBean.setMapperLocations(
        resolver.getResources(
            "classpath*:cn/siyes/training/mybatis/mapper/*.xml"
        )
    );

    final org.apache.ibatis.session.Configuration
        configuration = new org.apache.ibatis.session.Configuration();

//    配置数据库下划线间隔类型自动转JAVA类驼峰写法
    configuration.setMapUnderscoreToCamelCase(true);
//    设置数据库日志类型
    configuration.setLogImpl(org.apache.ibatis.logging.slf4j.Slf4jImpl.class);
    factoryBean.setConfiguration(configuration);

    return factoryBean.getObject();
  }

  @Bean
  public PlatformTransactionManager transactionManager(DataSource dataSource) {
    return new DataSourceTransactionManager(dataSource);
  }
}
