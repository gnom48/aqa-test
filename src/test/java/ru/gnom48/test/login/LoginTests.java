package ru.gnom48.test.login;

import io.qameta.allure.*;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import ru.gnom48.test.BaseTest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;
import static io.restassured.RestAssured.given;

@Epic("Аутентификация (вход в систему)")
@Feature("Login Tests")
@DisplayName("Тесты на вход в систему [LOGIN]")
public class LoginTests extends BaseTest {

    @Test
    @Story("Успешный вход")
    @DisplayName("Должен успешно логиниться с валидным токеном, если внешний сервис возвращает 200")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Проверка успешной аутентификации с валидным токеном")
    void testSuccessfulLogin() {
        // Arrange
        String token = generateValidToken();

        // Act
        Response response = sendLoginRequest(token);

        // Assert
        verifyExternalAuthServiceCalled(token);
        verifySuccessfulLoginResponse(response);
    }

    @Test
    @Story("Ошибка входа")
    @DisplayName("Должен вернуть корректную ошибку, если внешний сервис возвращает 401")
    @Severity(SeverityLevel.NORMAL)
    @Description("Проверка обработки ошибки аутентификации от внешнего сервиса")
    void testLoginWhenExternalServiceReturns401() {
        // Arrange
        stubExternalAuthServiceToReturnError(401);
        String token = generateValidToken();

        // Act
        Response response = sendLoginRequest(token);

        // Assert
        verifyExternalAuthServiceCalled(token);
        verifyInternalServerErrorResponse(response);
    }

    @Test
    @Story("Ошибка входа")
    @DisplayName("Должно возвращать корректную ошибку если внешний сервис пятисотнет")
    @Severity(SeverityLevel.NORMAL)
    @Description("Проверка обработки ошибки 500 от внешнего сервиса")
    void testLoginWhenExternalServiceReturns500() {
        // Arrange
        stubExternalAuthServiceToReturnError(500);
        String token = generateValidToken();

        // Act
        Response response = sendLoginRequest(token);

        // Assert
        verifyExternalAuthServiceCalled(token);
        verifyInternalServerErrorResponse(response);
    }

    @Test
    @Story("Ошибка входа")
    @DisplayName("Должно возвращать корректный ответ если внешний сервис будет долго думать")
    @Severity(SeverityLevel.NORMAL)
    @Description("Проверка таймаутов - внешний сервис отвечает медленно")
    void testLoginWhenExternalServiceTimesOut() {
        // Arrange
        stubExternalAuthServiceWithDelay(5000);
        String token = generateValidToken();

        // Act
        Response response = sendLoginRequest(token);

        // Assert
        verifyExternalAuthServiceCalled(token);
        verifySuccessfulLoginResponse(response);
    }

    @Test
    @Story("Ошибка входа")
    @DisplayName("Должен возвращать ошибку при невалидном токене")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Проверка валидации токена на стороне приложения")
    void testLoginWithInvalidToken() {
        // Arrange
        String invalidToken = generateInvalidToken();

        // Act
        Response response = sendLoginRequest(invalidToken);

        // Assert
        verifyExternalAuthServiceNotCalled();
        verifyBadRequestResponse(response);
    }

    @Test
    @Story("Ошибка входа")
    @DisplayName("Должен возвращать ошибку при отсутствии API ключа")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Проверка обязательности API ключа")
    void testLoginWithoutApiKey() {
        // Arrange
        String token = generateValidToken();

        // Act
        Response response = sendLoginRequestWithoutApiKey(token);

        // Assert
        verifyExternalAuthServiceNotCalled();
        verifyUnauthorizedResponse(response);
    }

    @Test
    @Story("Ошибка входа")
    @DisplayName("Должен возвращать ошибку при пустом токене")
    @Severity(SeverityLevel.NORMAL)
    @Description("Проверка обработки пустого токена")
    void testLoginWithEmptyToken() {
        // Act
        Response response = sendLoginRequest("");

        // Assert
        verifyExternalAuthServiceNotCalled();
        verifyBadRequestResponse(response);
    }

    @Test
    @Story("Ошибка входа")
    @DisplayName("Должен возвращать ошибку при слишком длинном токене")
    @Severity(SeverityLevel.MINOR)
    @Description("Проверка граничных значений - токен превышает допустимую длину")
    void testLoginWithTooLongToken() {
        // Arrange
        String tooLongToken = "A".repeat(33);

        // Act
        Response response = sendLoginRequest(tooLongToken);

        // Assert
        verifyExternalAuthServiceNotCalled();
        verifyBadRequestResponse(response);
    }

    @Test
    @Story("Ошибка входа")
    @DisplayName("Должен возвращать ошибку при слишком коротком токене")
    @Severity(SeverityLevel.MINOR)
    @Description("Проверка граничных значений - токен меньше допустимой длины")
    void testLoginWithTooShortToken() {
        // Arrange
        String tooShortToken = "A".repeat(31);

        // Act
        Response response = sendLoginRequest(tooShortToken);

        // Assert
        verifyExternalAuthServiceNotCalled();
        verifyBadRequestResponse(response);
    }

    @Test
    @Story("Успешный вход")
    @DisplayName("Должен успешно логиниться с токеном содержащим только цифры")
    @Severity(SeverityLevel.NORMAL)
    @Description("Проверка формата токена - только цифры")
    void testLoginWithNumericToken() {
        // Arrange
        String numericToken = "1".repeat(32);

        // Act
        Response response = sendLoginRequest(numericToken);

        // Assert
        verifyExternalAuthServiceCalled(numericToken);
        verifySuccessfulLoginResponse(response);
    }

    @Test
    @Story("Успешный вход")
    @DisplayName("Должен успешно логиниться с токеном содержащим только буквы")
    @Severity(SeverityLevel.NORMAL)
    @Description("Проверка формата токена - только буквы")
    void testLoginWithAlphabeticToken() {
        // Arrange
        String alphabeticToken = "A".repeat(32);

        // Act
        Response response = sendLoginRequest(alphabeticToken);

        // Assert
        verifyExternalAuthServiceCalled(alphabeticToken);
        verifySuccessfulLoginResponse(response);
    }
    
    //#region assertions

    @Step("Проверка успешного ответа: статус 200 и result = OK")
    private void verifySuccessfulLoginResponse(Response response) {
        assertEquals(200, response.getStatusCode(), "Статус код должен быть 200");
        assertEquals("OK", response.jsonPath().getString("result"), "result должно быть 'OK'");
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

    @Step("Проверка ответа с неавторизованным доступом: статус 401")
    private void verifyUnauthorizedResponse(Response response) {
        assertEquals(401, response.getStatusCode(), "Статус код должен быть 401");
        assertNotNull(response.jsonPath().getString("message"), "Должно быть сообщение об ошибке");
    }

    @Step("Проверка вызова внешнего сервиса аутентификации с токеном: {token}")
    private void verifyExternalAuthServiceCalled(String token) {
        verify(postRequestedFor(urlEqualTo("/auth"))
                .withRequestBody(containing("token=" + token)));
    }

    @Step("Проверка, что внешний сервис аутентификации НЕ вызывался")
    private void verifyExternalAuthServiceNotCalled() {
        verify(0, postRequestedFor(urlEqualTo("/auth")));
    }

    //#endregion

    //#region requests

    @Step("Отправка запроса LOGIN с токеном: {token}")
    protected Response sendLoginRequest(String token) {
        return sendRequest(token, "LOGIN");
    }

    @Step("Отправка запроса LOGIN без API ключа, токен: {token}")
    protected Response sendLoginRequestWithoutApiKey(String token) {
        return given()
                .contentType(ContentType.URLENC)
                .formParam("token", token)
                .formParam("action", "LOGIN")
                .post("/endpoint")
                .then()
                .extract()
                .response();
    }

    //#endregion
    
    //#region responses

    @Step("Настройка заглушки: внешний сервис на аутентификацию возвращает ошибку {statusCode}")
    private void stubExternalAuthServiceToReturnError(int statusCode) {
        stubFor(post(urlEqualTo("/auth"))
                .willReturn(aResponse()
                        .withStatus(statusCode)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"result\":\"not ok\"}")));
    }

    @Step("Настройка заглушки: внешний сервис на аутентификацию висит с задержкой {delayMs} мс")
    private void stubExternalAuthServiceWithDelay(int delayMs) {
        stubFor(post(urlEqualTo("/auth"))
                .willReturn(aResponse()
                        .withFixedDelay(delayMs)
                        .withStatus(200)
                        .withBody("{\"status\":\"authenticated\"}")));
    }

    //#endregion
}