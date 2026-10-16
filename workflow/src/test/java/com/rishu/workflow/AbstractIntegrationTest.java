package com.rishu.workflow;

import java.util.TimeZone;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.kafka.KafkaContainer;

/**
 * Base class for integration tests: the full Spring context against a real
 * PostgreSQL and Kafka running in Docker.
 *
 * The containers are started once and shared by every test class (singleton
 * containers). That keeps the run fast and lets Spring reuse one cached
 * application context. @ServiceConnection points the datasource and Kafka
 * settings at the containers, so no URLs or passwords are needed.
 */
@SpringBootTest
@TestPropertySource(properties = "app.scheduling.enabled=false")
public abstract class AbstractIntegrationTest {

    // Same images as docker-compose.yml
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:18");

    @ServiceConnection
    static final KafkaContainer KAFKA = new KafkaContainer("apache/kafka:4.0.0");

    static {
        // Windows can report the old zone id "Asia/Calcutta", which PostgreSQL 18
        // rejects on connect. UTC also makes time-based queries predictable.
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        POSTGRES.start();
        KAFKA.start();
    }
}
