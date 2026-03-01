package ru.gnom48.test;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import io.qameta.allure.Step;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;

public abstract class BaseTest {
    protected static final String API_KEY = "qazWSXedc";
    protected static final String TOKEN_PATTERN = "[A-Z0-9]{32}";

    protected static WireMockServer wireMockServer;
    protected static Process testApplication;
    protected static String appJarPath = "internal-0.0.1-SNAPSHOT.jar";
    protected static int mockPort = 8888;
    protected static int appPort = 8080;

    @BeforeAll
    static void setUpAll() throws IOException {
        startWireMock();
        startTestApplication();
    }

    @Step("Запуск WireMock сервера")
    private static void startWireMock() {
        wireMockServer = new WireMockServer(
                WireMockConfiguration.wireMockConfig()
                        .port(mockPort)
                        .usingFilesUnderClasspath("wiremock"));
        wireMockServer.start();
        WireMock.configureFor("localhost", mockPort);

        setupDefaultStubs();
    }

    @Step("Настройка заглушек по умолчанию")
    private static void setupDefaultStubs() {
        stubFor(post(urlEqualTo("/auth"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"status\":\"authenticated\"}")));

        stubFor(post(urlEqualTo("/doAction"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"result\":\"success\"}")));
    }

    @Step("Запуск тестируемого приложения")
    private static void startTestApplication() throws IOException {
        ProcessBuilder pb = new ProcessBuilder(
                "java", "-jar",
                "-Dsecret=" + API_KEY,
                "-Dmock=http://localhost:" + mockPort,
                appJarPath);
        pb.redirectErrorStream(true);
        pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);

        testApplication = pb.start();

        await().atMost(30, TimeUnit.SECONDS)
                .pollInterval(1, TimeUnit.SECONDS)
                .until(() -> {
                    try {
                        Response response = given()
                                .baseUri("http://localhost:" + appPort)
                                .header("X-Api-Key", API_KEY)
                                .contentType(ContentType.URLENC)
                                .formParam("token", "12345678901234567890123456789012")
                                .formParam("action", "LOGIN")
                                .post("/endpoint");

                        return response.getStatusCode() < 500;
                    } catch (Exception e) {
                        return false;
                    }
                });
    }

    @AfterAll
    static void downAll() {
        if (wireMockServer != null && wireMockServer.isRunning()) {
            wireMockServer.stop();
        }
        if (testApplication != null && testApplication.isAlive()) {
            testApplication.destroyForcibly();
        }
    }

    @BeforeEach
    void reSetUp() {
        wireMockServer.resetAll();
        setupDefaultStubs();
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = appPort;
    }

    @Step("Отправка запроса: token={token}, action={action}")
    protected Response sendRequest(String token, String action) {
        return given()
                .header("X-Api-Key", API_KEY)
                .contentType(ContentType.URLENC)
                .formParam("token", token)
                .formParam("action", action)
                .post("/endpoint")
                .then()
                .extract()
                .response();
    }

    @Step("Генерация валидного токена")
    protected String generateValidToken() {
        return java.util.UUID.randomUUID()
                .toString()
                .replace("-", "")
                .toUpperCase()
                .substring(0, 32);
    }

    @Step("Генерация НЕ валидного токена")
    protected String generateInvalidToken() {
        return "invalid-token-with-special-chars!@#";
    }
}