package com.daf360.portal.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

@Configuration
@RequiredArgsConstructor
public class RhDataSourceConfig {

    private final AppProperties props;

    @Bean("rhDataSource")
    public DataSource rhDataSource() {
        return DataSourceBuilder.create()
                .url(props.getRhDb().getUrl())
                .username(props.getRhDb().getUsername())
                .password(props.getRhDb().getPassword())
                .build();
    }

    @Bean("rhJdbcTemplate")
    public JdbcTemplate rhJdbcTemplate(@Qualifier("rhDataSource") DataSource rhDataSource) {
        return new JdbcTemplate(rhDataSource);
    }
}
