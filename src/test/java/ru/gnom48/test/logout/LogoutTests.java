package ru.gnom48.test.logout;

import io.qameta.allure.*;
import io.restassured.response.Response;
import ru.gnom48.test.BaseTest;

import org.junit.jupiter.api.*;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

@Epic("Выход из системы")
public class LogoutTests extends BaseTest {
    private String validToken;

    @BeforeEach
    void setUpLogoutTests() {
        validToken = generateValidToken();
        
        // Сначала логинимся
        stubFor(post(urlEqualTo("/auth"))
            .willReturn(aResponse().withStatus(200)));
        
        sendRequest(validToken, "LOGIN");

        // Сбрасываем счетчики WireMock после логина перед остальными запросами
        wireMockServer.resetRequests();
    }

    @Test
    @Story("Успешный выход")
    @DisplayName("Должен быть успешно отработан выход из системы для аутентифицированного токена")
    void testSuccessfulLogout() {
        Response logoutResponse = sendRequest(validToken, "LOGOUT");

        assertEquals(200, logoutResponse.getStatusCode());
        assertEquals("OK", logoutResponse.jsonPath().getString("result"));

        // После логаута ACTION должен быть недоступен
        Response actionResponse = sendRequest(validToken, "ACTION");

        verify(0, postRequestedFor(urlEqualTo("/doAction")));

        assertEquals(403, actionResponse.getStatusCode());
        assertEquals("ERROR", actionResponse.jsonPath().getString("result"));
    }

    @Test
    @Story("Ошибка выхода")
    @DisplayName("Нельзя выйти из системы с неаутентифицированным токеном")
    void testLogoutForNonExistentToken() {
        String invalidToken = generateValidToken(); // Новый токен - не логинился
        
        Response response = sendRequest(invalidToken, "LOGOUT");

        assertEquals(403, response.getStatusCode());
        assertEquals("ERROR", response.jsonPath().getString("result"));
    }
}