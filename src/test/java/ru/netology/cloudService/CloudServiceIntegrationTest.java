package ru.netology.cloudService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import ru.netology.cloudService.service.TokenBlacklistService;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
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

    private String login() {
        ResponseEntity<Map> resp = restTemplate.postForEntity(
                "/login", Map.of("login", "user", "password", "user"), Map.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        return (String) resp.getBody().get("auth-token");
    }

    private HttpHeaders auth(String token) {
        HttpHeaders h = new HttpHeaders();
        h.set("auth-token", token);
        return h;
    }

    private ResponseEntity<Void> upload(String token, String name) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new ByteArrayResource("data".getBytes()) {
            @Override public String getFilename() { return name; }
        });
        return restTemplate.exchange(
                "/file?filename=" + name, HttpMethod.POST,
                new HttpEntity<>(body, auth(token)), Void.class);
    }

    @Test
    void fullLifecycle() {
        String token = login();

        // upload
        assertThat(upload(token, "lifecycle.txt").getStatusCode()).isEqualTo(HttpStatus.OK);

        // list contains
        ResponseEntity<String> listRaw = restTemplate.exchange(
                "/list", HttpMethod.GET, new HttpEntity<>(auth(token)), String.class);
        assertThat(listRaw.getBody()).startsWith("[");

        // download
        ResponseEntity<byte[]> dl = restTemplate.exchange(
                "/file?filename=lifecycle.txt", HttpMethod.GET,
                new HttpEntity<>(auth(token)), byte[].class);
        assertThat(dl.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(new String(dl.getBody())).isEqualTo("data");

        // rename
        HttpHeaders json = auth(token);
        json.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<Void> ren = restTemplate.exchange(
                "/file?filename=lifecycle.txt", HttpMethod.PUT,
                new HttpEntity<>(Map.of("name", "renamed.txt"), json), Void.class);
        assertThat(ren.getStatusCode()).isEqualTo(HttpStatus.OK);

        // delete
        ResponseEntity<Void> del = restTemplate.exchange(
                "/file?filename=renamed.txt", HttpMethod.DELETE,
                new HttpEntity<>(auth(token)), Void.class);
        assertThat(del.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void login_withWrongPassword_returns400() {
        ResponseEntity<String> resp = restTemplate.postForEntity(
                "/login", Map.of("login", "user", "password", "wrong"), String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void request_withoutToken_returns401() {
        ResponseEntity<String> resp = restTemplate.getForEntity("/list", String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void upload_withPathTraversal_returns400() {
        String token = login();

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new ByteArrayResource("x".getBytes()) {
            @Override public String getFilename() { return "x.txt"; }
        });

        ResponseEntity<String> resp = restTemplate.exchange(
                "/file?filename=../2/evil.txt", HttpMethod.POST,
                new HttpEntity<>(body, auth(token)), String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void logout_withBearerToken_invalidatesToken() {
        String token = login();
        HttpHeaders bearer = new HttpHeaders();
        bearer.set("auth-token", "Bearer " + token);

        ResponseEntity<Void> logout = restTemplate.exchange(
                "/logout", HttpMethod.POST, new HttpEntity<>(bearer), Void.class);
        assertThat(logout.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<String> after = restTemplate.exchange(
                "/list", HttpMethod.GET, new HttpEntity<>(bearer), String.class);
        assertThat(after.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Autowired
    private TokenBlacklistService tokenBlacklistService;

    @BeforeEach
    void resetBlacklist() {
        tokenBlacklistService.clear();
    }
}