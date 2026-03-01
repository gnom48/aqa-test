# Тестовый проект для api сервиса

## Описание проекта

Проект содержит автоматизированные тесты для приложения `internal-0.0.1-SNAPSHOT.jar`.

### Что тестируется

Тестируемое приложение имеет один endpoint - его и тестируем:
- `POST http://localhost:8080/endpoint` с параметрами `token` и `action`

## Стек:

- **Java 25** - язык программирования
- **JUnit 5** - фреймворк для тестирования
- **WireMock** - эмуляция внешнего сервиса
- **REST Assured** - HTTP клиент для тестов
- **Allure** - генерация отчетов
- **Maven** - сборка проекта
- **Awaitility** - ожидание асинхронных операций

## Запуск тестов

### Предварительные требования

- Java 17 или выше
- Maven
- Тестируемое приложение `internal-0.0.1-SNAPSHOT.jar` (положить в корень проекта)

### Быстрый запуск

```bash
# Очистка предыдущих результатов и запуск всех тестов
mvn clean test

# Запуск конкретного тестового класса
mvn test -Dtest=AuthenticationTests

# Запуск конкретного теста
mvn test -Dtest=AuthenticationTests#testSuccessfulLogin
```

### Запуск с отчетом Allure

```bash
mvn clean test
mvn allure:serve
```

## Postman коллекция
Все завпросы к тестируемому приложению описаны в Postman коллекции в файле `AQA.postman_collection.json`.