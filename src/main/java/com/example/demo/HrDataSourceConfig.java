package com.example.demo;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;


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
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName(hrDriver);
        dataSource.setUrl(hrUrl);
        dataSource.setUsername(hrUsername);
        dataSource.setPassword(hrPassword);
        return new JdbcTemplate(dataSource);
    }
}
