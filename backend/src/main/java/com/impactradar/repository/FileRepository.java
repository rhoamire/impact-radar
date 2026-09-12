package com.impactradar.repository;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class FileRepository {

    private final JdbcClient jdbcClient;

    public FileRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public int countFiles() {
        return jdbcClient
                .sql("SELECT COUNT(*) FROM files")
                .query(Integer.class)
                .single();
    }
}