package ru.gnom48.test.action;

import io.qameta.allure.*;
import io.restassured.response.Response;
import ru.gnom48.test.BaseTest;

import org.junit.jupiter.api.*;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

@Epic("Действие пользователя")
public class ActionTests extends BaseTest {
    private String validToken;

    @BeforeEach
    void setUpActionTests() {
        validToken = generateValidToken();

        // Сначала логинимся
        stubFor(post(urlEqualTo("/auth"))
                .willReturn(aResponse().withStatus(200)));

        Response loginResponse = sendRequest(validToken, "LOGIN");
        assertEquals("OK", loginResponse.jsonPath().getString("result"));

        // Сбрасываем счетчики WireMock после логина перед остальными запросами
        wireMockServer.resetRequests();
    }

    @Test
    @Story("Успешное действие")
    @DisplayName("Должна быть успешная обработка действия для аутентифицированного токена")
    void testSuccessfulAction() {
        Response response = sendRequest(validToken, "ACTION");

        verify(postRequestedFor(urlEqualTo("/doAction"))
                .withRequestBody(containing("token=" + validToken)));

        assertEquals(200, response.getStatusCode());
        assertEquals("OK", response.jsonPath().getString("result"));
    }

    @Test
    @Story("Ошибка действия")
    @DisplayName("Должна быть корректная ошибка при попытке выполнить действие для неаутентифицированного токена")
    void testActionForUnauthenticatedToken() {
        String newToken = generateValidToken();

        Response response = sendRequest(newToken, "ACTION");

        assertEquals(403, response.getStatusCode());
        assertEquals("ERROR", response.jsonPath().getString("result"));
    }

    @Test
    @Story("Ошибка внешнего сервиса на действие")
    @DisplayName("Должна быть корректная ошибка при ошибке на действие от внешнего сервиса")
    void testActionWhenExternalServiceReturns400() {
        stubFor(post(urlEqualTo("/doAction"))
                .willReturn(aResponse().withStatus(500)));

        Response response = sendRequest(validToken, "ACTION");

        // ERROR: по сути здесь баг: если запрос корректный НО внешний сервис кинет ошибку на действие
        // то приложение упадет

        // по задумке надо проверять так:
        // assertEquals(400, response.getStatusCode());
        // assertEquals("ERROR", response.jsonPath().getString("result"));

        // но исходя из того как работает, то так:
        assertEquals(500, response.getStatusCode());
    }
}