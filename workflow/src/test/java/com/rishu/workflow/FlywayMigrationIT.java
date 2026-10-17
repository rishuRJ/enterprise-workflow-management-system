package com.rishu.workflow;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * The application context only starts if Flyway ran every migration on an empty
 * database and Hibernate's ddl-auto=validate found the schema matching the
 * entities. These tests check the results of that.
 */
class FlywayMigrationIT extends AbstractIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void appliesAllMigrationsInOrder() {
        List<String> versions = jdbcTemplate.queryForList(
                "select version from flyway_schema_history where success order by installed_rank",
                String.class);

        assertThat(versions).containsExactly("1", "2");
    }

    @Test
    void createsTheIndexesFromV2() {
        List<String> indexes = jdbcTemplate.queryForList(
                "select indexname from pg_indexes where schemaname = 'public'",
                String.class);

        assertThat(indexes).contains(
                "idx_requests_employee_created_at",
                "idx_requests_manager_created_at",
                "idx_request_history_request_occurred_at",
                "idx_outbox_event_unfinished");
    }
}
