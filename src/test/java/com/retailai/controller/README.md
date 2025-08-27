# ChatController Test Suite

This directory contains comprehensive unit tests for the `ChatController` class.

## Test Coverage

The `ChatControllerTest` class covers the following scenarios:

### ✅ **Valid Input Tests**
- **Valid payload with tenant ID**: Tests normal operation with complete payload
- **Valid payload without tenant ID**: Tests default tenant fallback to "demo"
- **Valid answer response**: Tests that valid responses are returned unchanged

### ✅ **Fallback Message Tests**
- **Empty answer**: Tests fallback when answer is empty string
- **Blank answer**: Tests fallback when answer contains only whitespace
- **Missing answer key**: Tests fallback when response doesn't contain answer key

### ✅ **Edge Case Tests**
- **Null payload**: Tests graceful handling of null input (throws NPE)
- **Empty payload**: Tests handling of empty payload map
- **Null question**: Tests handling of null question field
- **Empty question**: Tests handling of empty question string
- **Whitespace question**: Tests handling of whitespace-only question

### ✅ **Complex Response Tests**
- **Tool response**: Tests handling of complex responses with nested data
- **Inventory data**: Tests extraction and verification of structured data

## Test Structure

- **13 test methods** covering all major scenarios
- **Mockito framework** for dependency mocking
- **JUnit 5** for test execution
- **Comprehensive assertions** for response validation

## Key Testing Patterns

1. **Arrange-Act-Assert**: Clear test structure
2. **Mock verification**: Ensures service methods are called correctly
3. **Edge case coverage**: Tests boundary conditions and error scenarios
4. **Response validation**: Verifies both content and structure of responses

## Running Tests

```bash
# Run all ChatController tests
./gradlew test --tests ChatControllerTest

# Run specific test method
./gradlew test --tests ChatControllerTest.testAsk_WithValidPayload_ReturnsValidResponse
```

## Dependencies

- Spring Boot Test Starter
- Mockito 5.10.0+ (Java 24 compatible)
- JUnit Jupiter 5.10.2+
- ByteBuddy experimental mode enabled for Java 24 support
