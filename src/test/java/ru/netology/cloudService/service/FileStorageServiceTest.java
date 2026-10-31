package ru.netology.cloudService.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import ru.netology.cloudService.repository.UserRepository;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CloudServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("cloud_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("cloud.storage.path", () -> "./target/test-storage");
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    @Test
    void loginAndListFlow() {
        ResponseEntity<Map> loginResp = restTemplate.postForEntity(
                "/login",
                Map.of("login", "user", "password", "user"),
                Map.class);

        assertThat(loginResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        String token = (String) loginResp.getBody().get("auth-token");
        assertThat(token).isNotBlank();

        HttpHeaders headers = new HttpHeaders();
        headers.set("auth-token", token);
        ResponseEntity<Object[]> listResp = restTemplate.exchange(
                "/list", HttpMethod.GET, new HttpEntity<>(headers), Object[].class);

        assertThat(listResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(listResp.getBody()).isNotNull();
    }
}
