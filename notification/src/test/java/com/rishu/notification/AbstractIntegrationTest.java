package com.rishu.notification;

import java.util.Map;
import java.util.TimeZone;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.kafka.KafkaContainer;

/**
 * Base class for integration tests: the full notification service, with its
 * Kafka listeners running, against a real PostgreSQL and Kafka in Docker.
 * The containers are started once and shared by every test class.
 */
@SpringBootTest
@TestPropertySource(properties = {
        // Only the local profile creates tables; tests start from an empty database
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
public abstract class AbstractIntegrationTest {

    // Same images as the workflow service's tests and docker-compose files
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:18");

    static final KafkaContainer KAFKA = new KafkaContainer("apache/kafka:4.0.0");

    static {
        // Windows can report the old zone id "Asia/Calcutta", which PostgreSQL 18 rejects
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        POSTGRES.start();
        KAFKA.start();
    }

    // Kafka is wired through the property rather than @ServiceConnection, because
    // KafkaErrorHandlerConfig reads spring.kafka.bootstrap-servers directly with
    // @Value to build the dead-letter publisher. @ServiceConnection would leave
    // that property at localhost:9092.
    @DynamicPropertySource
    static void kafkaProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
    }

    private static KafkaTemplate<String, String> testProducer;

    /**
     * Sends a message as plain text, the way the workflow service's JSON would
     * arrive on the topic. Waits until Kafka has accepted it.
     */
    protected static void send(String topic, String key, String value) {
        if (testProducer == null) {
            testProducer = new KafkaTemplate<>(new DefaultKafkaProducerFactory<String, String>(Map.of(
                    ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers(),
                    ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
                    ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class)));
        }
        testProducer.send(topic, key, value).join();
    }
}
