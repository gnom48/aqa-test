package ru.gnom48.test.logout;

import io.qameta.allure.*;
import io.restassured.response.Response;
import ru.gnom48.test.BaseTest;

import org.junit.jupiter.api.*;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

@Epic("Выход из системы")
@Feature("Logout Tests")
@DisplayName("Тесты на выход из системы [LOGOUT]")
public class LogoutTests extends BaseTest {
    private String authenticatedToken;

    @BeforeEach
    @Step("Подготовка аутентифицированного токена для тестов выхода")
    void setUpLogoutTests() {
        // Arrange
        authenticatedToken = generateValidToken();
        
        // Act - логинимся перед каждым тестом
        performLogin(authenticatedToken);

        // Сбрасываем счетчики WireMock после логина перед остальными запросами
        resetWireMockRequests();
    }

    @Test
    @Story("Успешный выход")
    @DisplayName("Должен быть успешно отработан выход из системы для аутентифицированного токена")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Проверка успешного выхода из системы и последующего отказа в доступе к действиям")
    void testSuccessfulLogout() {
        // Act - выполняем выход
        Response logoutResponse = sendLogoutRequest(authenticatedToken);
        
        // Assert - проверяем выход
        verifySuccessfulLogoutResponse(logoutResponse);

        // Act - пробуем выполнить действие после выхода
        Response actionResponse = sendActionRequest(authenticatedToken);

        // Assert - действие должно быть недоступно
        verifyActionNotAvailableAfterLogout(actionResponse);
    }

    @Test
    @Story("Ошибка выхода")
    @DisplayName("Нельзя выйти из системы с неаутентифицированным токеном")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Проверка выхода с токеном, который не проходил аутентификацию")
    void testLogoutForUnauthenticatedToken() {
        // Arrange
        String unauthenticatedToken = generateValidToken();
        
        // Act
        Response response = sendLogoutRequest(unauthenticatedToken);

        // Assert
        verifyForbiddenResponse(response);
        verifyUserStillAuthenticated(authenticatedToken);
    }

    @Test
    @Story("Ошибка выхода")
    @DisplayName("Нельзя выйти из системы с невалидным токеном")
    @Severity(SeverityLevel.NORMAL)
    @Description("Проверка валидации токена при выходе")
    void testLogoutWithInvalidToken() {
        // Arrange
        String invalidToken = generateInvalidToken();
        
        // Act
        Response response = sendLogoutRequest(invalidToken);

        // Assert
        verifyBadRequestResponse(response);
        verifyUserStillAuthenticated(authenticatedToken);
    }

    @Test
    @Story("Ошибка выхода")
    @DisplayName("Нельзя выйти из системы с пустым токеном")
    @Severity(SeverityLevel.NORMAL)
    @Description("Проверка валидации - пустой токен при выходе")
    void testLogoutWithEmptyToken() {
        // Act
        Response response = sendLogoutRequest("");

        // Assert
        verifyBadRequestResponse(response);
        verifyUserStillAuthenticated(authenticatedToken);
    }

    @Test
    @Story("Успешный выход")
    @DisplayName("Должна быть возможность выйти и залогиниться снова с тем же токеном")
    @Severity(SeverityLevel.NORMAL)
    @Description("Проверка повторного использования токена после выхода")
    void testLogoutAndLoginAgain() {
        // Act 1 - выходим
        Response logoutResponse = sendLogoutRequest(authenticatedToken);
        verifySuccessfulLogoutResponse(logoutResponse);

        // Act 2 - логинимся снова
        Response loginResponse = sendLoginRequest(authenticatedToken);
        
        // Assert
        verifySuccessfulLoginResponse(loginResponse);

        // Проверяем, что действие снова доступно
        Response actionResponse = sendActionRequest(authenticatedToken);
        verifySuccessfulActionResponse(actionResponse);
    }

    @Test
    @Story("Успешный выход")
    @DisplayName("Должна быть возможность выйти несколько раз подряд")
    @Severity(SeverityLevel.MINOR)
    @Description("Проверка идемпотентности операции выхода")
    void testMultipleLogouts() {
        // Act 1 - первый выход
        Response firstLogout = sendLogoutRequest(authenticatedToken);
        verifySuccessfulLogoutResponse(firstLogout);

        // Act 2 - второй выход
        Response secondLogout = sendLogoutRequest(authenticatedToken);

        // Assert - второй выход должен вернуть ошибку, так как токен уже не аутентифицирован
        verifyForbiddenResponse(secondLogout);
    }

    //#region вспомогательные шаги
    
    @Step("Выполнение логина с токеном: {token}")
    private void performLogin(String token) {
        Response loginResponse = sendRequest(token, "LOGIN");
        assertEquals("OK", loginResponse.jsonPath().getString("result"), 
                "Логин должен быть успешным");
    }

    @Step("Сброс счетчиков запросов WireMock")
    private void resetWireMockRequests() {
        wireMockServer.resetRequests();
    }

    //#endregion

    //#region requests

    @Step("Отправка запроса LOGOUT с токеном: {token}")
    private Response sendLogoutRequest(String token) {
        return sendRequest(token, "LOGOUT");
    }

    @Step("Отправка запроса LOGIN с токеном: {token}")
    private Response sendLoginRequest(String token) {
        return sendRequest(token, "LOGIN");
    }

    @Step("Отправка запроса ACTION с токеном: {token}")
    private Response sendActionRequest(String token) {
        return sendRequest(token, "ACTION");
    }

    //#endregion

    //#region assertions

    @Step("Проверка успешного ответа на выход: статус 200 и result = OK")
    private void verifySuccessfulLogoutResponse(Response response) {
        assertEquals(200, response.getStatusCode(), "Статус код должен быть 200");
        assertEquals("OK", response.jsonPath().getString("result"), "result должно быть 'OK'");
    }

    @Step("Проверка успешного ответа на логин: статус 200 и result = OK")
    private void verifySuccessfulLoginResponse(Response response) {
        assertEquals(200, response.getStatusCode(), "Статус код должен быть 200");
        assertEquals("OK", response.jsonPath().getString("result"), "result должно быть 'OK'");
    }

    @Step("Проверка успешного ответа на действие: статус 200 и result = OK")
    private void verifySuccessfulActionResponse(Response response) {
        assertEquals(200, response.getStatusCode(), "Статус код должен быть 200");
        assertEquals("OK", response.jsonPath().getString("result"), "result должно быть 'OK'");
    }

    @Step("Проверка ответа с ошибкой доступа: статус 403")
    private void verifyForbiddenResponse(Response response) {
        assertEquals(403, response.getStatusCode(), "Статус код должен быть 403");
        assertEquals("ERROR", response.jsonPath().getString("result"), "result должно быть 'ERROR'");
        assertNotNull(response.jsonPath().getString("message"), "Должно быть сообщение об ошибке");
    }

    @Step("Проверка ответа с плохим запросом: статус 400")
    private void verifyBadRequestResponse(Response response) {
        assertEquals(400, response.getStatusCode(), "Статус код должен быть 400");
        assertNotNull(response.jsonPath().getString("message"), "Должно быть сообщение об ошибке");
    }

    @Step("Проверка, что действие недоступно после выхода")
    private void verifyActionNotAvailableAfterLogout(Response actionResponse) {
        verify(0, postRequestedFor(urlEqualTo("/doAction")));
        assertEquals(403, actionResponse.getStatusCode(), "Статус код должен быть 403");
        assertEquals("ERROR", actionResponse.jsonPath().getString("result"), 
                "result должно быть 'ERROR'");
    }

    @Step("Проверка, что пользователь с токеном {token} все еще аутентифицирован")
    private void verifyUserStillAuthenticated(String token) {
        Response actionResponse = sendActionRequest(token);
        verifyExternalActionServiceCalled(token);
        verifySuccessfulActionResponse(actionResponse);
    }

    @Step("Проверка вызова внешнего сервиса действия с токеном: {token}")
    private void verifyExternalActionServiceCalled(String token) {
        verify(postRequestedFor(urlEqualTo("/doAction"))
                .withRequestBody(containing("token=" + token)));
    }

    //#endregion
}