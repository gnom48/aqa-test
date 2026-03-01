package ru.gnom48.test;

import io.qameta.allure.*;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static io.restassured.RestAssured.given;

@Epic("Неготивные сценарии")
@Feature("Корректная обработка ошибок пользователя")
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

    @ParameterizedTest
    @MethodSource("invalidTokensProvider")
    @Story("Валидация некорректных токенов")
    @DisplayName("Надо отбрасывать некорректные токены на этапе валидации с соответствующим сообщением об ошибке")
    void testInvalidTokens(String token, String description) {
        Response response = sendRequest(token, "LOGIN");

        assertEquals(400, response.getStatusCode());
        assertEquals("ERROR", response.jsonPath().getString("result"));
        assertNotNull(response.jsonPath().getString("message"));
    }

    @Test
    @Story("Проверка некорректного API key")
    @DisplayName("Должна быть корректно обработана ошибка API key")
    void testInvalidApiKey() {
        Response response = given()
                .header("X-Api-Key", "invalid-key")
                .contentType(ContentType.URLENC)
                .formParam("token", generateValidToken())
                .formParam("action", "LOGIN")
                .post("/endpoint")
                .then()
                .extract()
                .response();

        assertEquals(401, response.getStatusCode());
    }

    @Test
    @Story("Проверка отсутствия API key")
    @DisplayName("Должна быть корректно обработана ошибка отсутствия API key")
    void testNoApiKey() {
        Response response = given()
                // не передаем заголовок X-Api-Key
                .contentType(ContentType.URLENC)
                .formParam("token", generateValidToken())
                .formParam("action", "LOGIN")
                .post("/endpoint")
                .then()
                .extract()
                .response();

        assertEquals(401, response.getStatusCode());
    }

    @Test
    @Story("Некорректный тип действия на /endpoint")
    @DisplayName("Должна быть обработка некорректного типа действия")
    void testInvalidAction() {
        Response response = sendRequest(generateValidToken(), "INVALID_ACTION");

        assertEquals(400, response.getStatusCode());
        assertEquals("ERROR", response.jsonPath().getString("result"));
    }

    @Nested
    @Story("Отсутствие параметра в запросе")
    public class MissingParameterTests {
        @Test
        @DisplayName("Должна быть проерка на наличие параметра token")
        void testMissingToken() {
            Response response = given()
                    .header("X-Api-Key", API_KEY)
                    .contentType(ContentType.URLENC)
                    .formParam("action", "LOGIN")
                    .post("/endpoint")
                    .then()
                    .extract()
                    .response();

            assertEquals(400, response.getStatusCode());
        }

        @Test
        @DisplayName("Должна быть проерка на наличие параметра action")
        void testMissingAction() {
            Response response = given()
                    .header("X-Api-Key", API_KEY)
                    .contentType(ContentType.URLENC)
                    .formParam("token", generateValidToken())
                    .post("/endpoint")
                    .then()
                    .extract()
                    .response();

            assertEquals(400, response.getStatusCode()); 
        }
    }
}