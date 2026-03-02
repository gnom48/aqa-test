package ru.gnom48.test.action;

import io.qameta.allure.*;
import io.restassured.response.Response;
import ru.gnom48.test.BaseTest;

import org.junit.jupiter.api.*;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

@Epic("Действие пользователя")
@Feature("Action Tests")
@DisplayName("Тесты на действия [ACTION]")
public class ActionTests extends BaseTest {
    private String authenticatedToken;

    @BeforeEach
    @Step("Подготовка аутентифицированного токена для тестов действий")
    void setUpActionTests() {
        // Arrange
        authenticatedToken = generateValidToken();
        
        // Act - логинимся перед каждым тестом
        performLogin(authenticatedToken);
        
        // Сбрасываем счетчики WireMock после логина перед остальными запросами
        resetWireMockRequests();
    }

    @Test
    @Story("Успешное действие")
    @DisplayName("Должна быть успешная обработка действия для аутентифицированного токена")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Проверка выполнения действия после успешной аутентификации")
    void testSuccessfulAction() {
        // Act
        Response response = sendActionRequest(authenticatedToken);
        
        // Assert
        verifyExternalActionServiceCalled(authenticatedToken);
        verifySuccessfulActionResponse(response);
    }

    @Test
    @Story("Ошибка действия")
    @DisplayName("Должна быть корректная ошибка при попытке выполнить действие для неаутентифицированного токена")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Проверка доступа к действию без предварительной аутентификации")
    void testActionForUnauthenticatedToken() {
        // Arrange
        String unauthenticatedToken = generateValidToken();
        
        // Act
        Response response = sendActionRequest(unauthenticatedToken);
        
        // Assert
        verifyExternalActionServiceNotCalled();
        verifyForbiddenResponse(response);
    }

    @Test
    @Story("Ошибка внешнего сервиса на действие")
    @DisplayName("Должна быть корректная ошибка при ошибке на действие от внешнего сервиса")
    @Severity(SeverityLevel.NORMAL)
    @Description("Проверка обработки ошибки 500 от внешнего сервиса при выполнении действия")
    void testActionWhenExternalServiceReturns500() {
        // Arrange
        stubExternalActionServiceToReturnError(500);
        
        // Act
        Response response = sendActionRequest(authenticatedToken);
        
        // Assert
        verifyExternalActionServiceCalled(authenticatedToken);
        verifyInternalServerErrorResponse(response);
    }

    @Test
    @Story("Ошибка внешнего сервиса на действие")
    @DisplayName("Должна быть корректная ошибка при 400 ошибке от внешнего сервиса")
    @Severity(SeverityLevel.NORMAL)
    @Description("Проверка обработки ошибки 400 от внешнего сервиса при выполнении действия")
    void testActionWhenExternalServiceReturns400() {
        // Arrange
        stubExternalActionServiceToReturnError(400);
        
        // Act
        Response response = sendActionRequest(authenticatedToken);
        
        // Assert
        verifyExternalActionServiceCalled(authenticatedToken);
        verifyInternalServerErrorResponse(response);
    }

    @Test
    @Story("Ошибка действия")
    @DisplayName("Должна быть ошибка при выполнении действия с пустым токеном")
    @Severity(SeverityLevel.NORMAL)
    @Description("Проверка валидации - пустой токен при выполнении действия")
    void testActionWithEmptyToken() {
        // Act
        Response response = sendActionRequest("");
        
        // Assert
        verifyExternalActionServiceNotCalled();
        verifyBadRequestResponse(response);
    }

    @Test
    @Story("Ошибка действия")
    @DisplayName("Должна быть ошибка при выполнении действия с невалидным форматом токена")
    @Severity(SeverityLevel.NORMAL)
    @Description("Проверка валидации - невалидный формат токена при выполнении действия")
    void testActionWithInvalidTokenFormat() {
        // Arrange
        String invalidToken = generateInvalidToken();
        
        // Act
        Response response = sendActionRequest(invalidToken);
        
        // Assert
        verifyExternalActionServiceNotCalled();
        verifyBadRequestResponse(response);
    }

    @Test
    @Story("Успешное действие")
    @DisplayName("Должна быть успешная обработка нескольких действий подряд")
    @Severity(SeverityLevel.NORMAL)
    @Description("Проверка выполнения нескольких последовательных действий")
    void testMultipleActions() {
        // Act & Assert - первое действие
        Response firstResponse = sendActionRequest(authenticatedToken);
        verifySuccessfulActionResponse(firstResponse);
        verifyExternalActionServiceCalled(authenticatedToken, 1);
        
        // Сбрасываем счетчик для второго действия
        resetWireMockRequests();
        
        // Act & Assert - второе действие
        Response secondResponse = sendActionRequest(authenticatedToken);
        verifySuccessfulActionResponse(secondResponse);
        verifyExternalActionServiceCalled(authenticatedToken, 1);
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

    @Step("Отправка запроса ACTION с токеном: {token}")
    private Response sendActionRequest(String token) {
        return sendRequest(token, "ACTION");
    }

    //#endregion

    //#region responses

    @Step("Настройка заглушки: внешний сервис на действие возвращает ошибку {statusCode}")
    private void stubExternalActionServiceToReturnError(int statusCode) {
        stubFor(post(urlEqualTo("/doAction"))
                .willReturn(aResponse()
                        .withStatus(statusCode)
                        .withHeader("Content-Type", "application/json")));
    }

    //#endregion

    //#region assertions

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

    @Step("Проверка ответа с внутренней ошибкой сервера: статус 500")
    private void verifyInternalServerErrorResponse(Response response) {
        assertEquals(500, response.getStatusCode(), "Статус код должен быть 500");
        assertNotNull(response.jsonPath().getString("message"), "Должно быть сообщение об ошибке");
        assertEquals("ERROR", response.jsonPath().getString("result"), "result должно быть 'ERROR'");
    }

    @Step("Проверка ответа с плохим запросом: статус 400")
    private void verifyBadRequestResponse(Response response) {
        assertEquals(400, response.getStatusCode(), "Статус код должен быть 400");
        assertNotNull(response.jsonPath().getString("message"), "Должно быть сообщение об ошибке");
    }

    @Step("Проверка вызова внешнего сервиса действия с токеном: {token}")
    private void verifyExternalActionServiceCalled(String token) {
        verify(postRequestedFor(urlEqualTo("/doAction"))
                .withRequestBody(containing("token=" + token)));
    }

    @Step("Проверка вызова внешнего сервиса действия {expectedCount} раз с токеном: {token}")
    private void verifyExternalActionServiceCalled(String token, int expectedCount) {
        verify(expectedCount, postRequestedFor(urlEqualTo("/doAction"))
                .withRequestBody(containing("token=" + token)));
    }

    @Step("Проверка, что внешний сервис действия НЕ вызывался")
    private void verifyExternalActionServiceNotCalled() {
        verify(0, postRequestedFor(urlEqualTo("/doAction")));
    }

    //#endregion
}