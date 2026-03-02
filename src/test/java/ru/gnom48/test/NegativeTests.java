package ru.gnom48.test;

import io.qameta.allure.*;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static io.restassured.RestAssured.given;

@Epic("Негативные сценарии")
@Feature("Корректная обработка ошибок пользователя")
@DisplayName("Тесты на корректную обработку ошибок пользователя")
public class NegativeTests extends BaseTest {

    private static Stream<Arguments> invalidTokensProvider() {
        return Stream.of(
                Arguments.of("", "empty token"),
                Arguments.of("123", "too short"),
                Arguments.of("123456789012345678901234567890123", "too long"),
                Arguments.of("abcdefghijklmnopqrstuvwxyz123456", "lowercase letters"),
                Arguments.of("123456789012345678901234567890!@", "special characters"),
                Arguments.of(null, "null token"));
    }

    @ParameterizedTest(name = "Токен: {1}")
    @MethodSource("invalidTokensProvider")
    @Story("Валидация некорректных токенов")
    @DisplayName("Надо отбрасывать некорректные токены на этапе валидации с соответствующим сообщением об ошибке")
    @Severity(SeverityLevel.CRITICAL)
    void testInvalidTokens(String token, String description) {
        // Act
        Response response = sendRequest(token, "LOGIN");

        // Assert
        verifyInvalidTokenErrorResponse(response);
    }

    @Test
    @Story("Проверка некорректного API key")
    @DisplayName("Должна быть корректно обработана ошибка API key")
    @Severity(SeverityLevel.CRITICAL)
    void testInvalidApiKey() {
        // Act
        Response response = sendRequestWithInvalidApiKey();

        // Assert
        verifyUnauthorizedResponse(response);
    }

    @Test
    @Story("Проверка отсутствия API key")
    @DisplayName("Должна быть корректно обработана ошибка отсутствия API key")
    @Severity(SeverityLevel.CRITICAL)
    void testNoApiKey() {
        // Act
        Response response = sendRequestWithoutApiKey();

        // Assert
        verifyUnauthorizedResponse(response);
    }

    @Test
    @Story("Некорректный тип действия на /endpoint")
    @DisplayName("Должна быть обработка некорректного типа действия")
    @Severity(SeverityLevel.NORMAL)
    void testInvalidAction() {
        // Act
        Response response = sendRequest(generateValidToken(), "INVALID_ACTION");

        // Assert
        verifyInvalidActionErrorResponse(response);
    }

    @Nested
    @Story("Отсутствие параметра в запросе")
    @DisplayName("Тесты на отсутствующие параметры")
    public class MissingParameterTests {
        
        @Test
        @DisplayName("Должна быть проверка на наличие параметра token")
        @Severity(SeverityLevel.CRITICAL)
        void testMissingToken() {
            // Act
            Response response = sendRequestWithoutToken();

            // Assert
            verifyMissingTokenResponse(response);
        }

        @Test
        @DisplayName("Должна быть проверка на наличие параметра action")
        @Severity(SeverityLevel.CRITICAL)
        void testMissingAction() {
            // Act
            Response response = sendRequestWithoutAction();

            // Assert
            verifyMissingActionResponse(response);
        }
    }

    //#region requests
    
    @Step("Отправка запроса с некорректным API key")
    private Response sendRequestWithInvalidApiKey() {
        return given()
                .header("X-Api-Key", "invalid-key")
                .contentType(ContentType.URLENC)
                .formParam("token", generateValidToken())
                .formParam("action", "LOGIN")
                .post("/endpoint")
                .then()
                .extract()
                .response();
    }

    @Step("Отправка запроса без API key")
    private Response sendRequestWithoutApiKey() {
        return given()
                .contentType(ContentType.URLENC)
                .formParam("token", generateValidToken())
                .formParam("action", "LOGIN")
                .post("/endpoint")
                .then()
                .extract()
                .response();
    }

    @Step("Отправка запроса без параметра token")
    private Response sendRequestWithoutToken() {
        return given()
                .header("X-Api-Key", API_KEY)
                .contentType(ContentType.URLENC)
                .formParam("action", "LOGIN")
                .post("/endpoint")
                .then()
                .extract()
                .response();
    }

    @Step("Отправка запроса без параметра action")
    private Response sendRequestWithoutAction() {
        return given()
                .header("X-Api-Key", API_KEY)
                .contentType(ContentType.URLENC)
                .formParam("token", generateValidToken())
                .post("/endpoint")
                .then()
                .extract()
                .response();
    }

    //#endregion

    //#region assertions

    @Step("Проверка ответа с некорректным токеном: статус 400, result = ERROR, есть message")
    private void verifyInvalidTokenErrorResponse(Response response) {
        assertEquals(400, response.getStatusCode(), "Статус код должен быть 400");
        assertEquals("ERROR", response.jsonPath().getString("result"), "result должно быть 'ERROR'");
        assertNotNull(response.jsonPath().getString("message"), "Должно быть сообщение об ошибке");
    }

    @Step("Проверка ответа с некорректным action: статус 400, result = ERROR")
    private void verifyInvalidActionErrorResponse(Response response) {
        assertEquals(400, response.getStatusCode(), "Статус код должен быть 400");
        assertEquals("ERROR", response.jsonPath().getString("result"), "result должно быть 'ERROR'");
        assertNotNull(response.jsonPath().getString("message"), "Должно быть сообщение об ошибке");
    }

    @Step("Проверка неавторизованного доступа: статус 401")
    private void verifyUnauthorizedResponse(Response response) {
        assertEquals(401, response.getStatusCode(), "Статус код должен быть 401");
    }

    @Step("Проверка ответа при отсутствии токена: статус 400")
    private void verifyMissingTokenResponse(Response response) {
        assertEquals(400, response.getStatusCode(), "Статус код должен быть 400");
        assertNotNull(response.jsonPath().getString("message"), "Должно быть сообщение об ошибке");
    }

    @Step("Проверка ответа при отсутствии action: статус 400")
    private void verifyMissingActionResponse(Response response) {
        assertEquals(400, response.getStatusCode(), "Статус код должен быть 400");
        assertNotNull(response.jsonPath().getString("message"), "Должно быть сообщение об ошибке");
    }

    //#endregion
}