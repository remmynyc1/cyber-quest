package com.example.demo;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import com.zaxxer.hikari.HikariDataSource;


@Configuration 
public class HrDataSourceConfig {
    @Value("${hr.datasource.url}")
    private String hrUrl;

    @Value("${hr.datasource.username}")
    private String hrUsername;

    @Value("${hr.datasource.password}")
    private String hrPassword;

    @Value("${hr.datasource.driver-class-name}")
    private String hrDriver;
    
    @Bean(name = "hrJdbcTemplate")
    public JdbcTemplate hrJdbcTemplate() {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setDriverClassName(hrDriver);
        dataSource.setJdbcUrl(hrUrl);
        dataSource.setUsername(hrUsername);
        dataSource.setPassword(hrPassword);
        dataSource.setMaximumPoolSize(3);
        return new JdbcTemplate(dataSource);
    }
}
