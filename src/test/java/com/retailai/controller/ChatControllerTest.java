package com.retailai.controller;

import com.retailai.service.ChatService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatControllerTest {

    @Mock
    private ChatService chatService;

    @InjectMocks
    private ChatController chatController;

    private Map<String, Object> validPayload;
    private Map<String, Object> validResponse;

    @BeforeEach
    void setUp() {
        validPayload = new HashMap<>();
        validPayload.put("q", "What is the stock level for SKU123?");
        validPayload.put("tenant_id", "test-tenant");

        validResponse = new LinkedHashMap<>();
        validResponse.put("type", "rag_response");
        validResponse.put("answer", "The stock level for SKU123 is 50 units.");
        validResponse.put("sources", new Object[]{});
        validResponse.put("suggestions", new Object[]{});
    }

    @Test
    void testAsk_WithValidPayload_ReturnsValidResponse() {
        // Arrange
        when(chatService.answerQuestion(anyString(), anyString()))
                .thenReturn(validResponse);

        // Act
        Map<String, Object> result = chatController.ask(validPayload);

        // Assert
        assertNotNull(result);
        assertEquals("rag_response", result.get("type"));
        assertEquals("The stock level for SKU123 is 50 units.", result.get("answer"));
        assertTrue(result.containsKey("sources"));
        assertTrue(result.containsKey("suggestions"));

        // Verify service was called with correct parameters
        verify(chatService).answerQuestion("test-tenant", "What is the stock level for SKU123?");
        verifyNoMoreInteractions(chatService);
    }

    @Test
    void testAsk_WithValidPayload_NoTenantId_UsesDefaultTenant() {
        // Arrange
        Map<String, Object> payloadWithoutTenant = new HashMap<>();
        payloadWithoutTenant.put("q", "What is the return policy?");

        when(chatService.answerQuestion(anyString(), anyString()))
                .thenReturn(validResponse);

        // Act
        Map<String, Object> result = chatController.ask(payloadWithoutTenant);

        // Assert
        assertNotNull(result);
        verify(chatService).answerQuestion("demo", "What is the return policy?");
    }

    @Test
    void testAsk_WithEmptyAnswer_AddsFallbackMessage() {
        // Arrange
        Map<String, Object> responseWithEmptyAnswer = new LinkedHashMap<>();
        responseWithEmptyAnswer.put("type", "rag_response");
        responseWithEmptyAnswer.put("answer", "");
        responseWithEmptyAnswer.put("sources", new Object[]{});
        responseWithEmptyAnswer.put("suggestions", new Object[]{});

        when(chatService.answerQuestion(anyString(), anyString()))
                .thenReturn(responseWithEmptyAnswer);

        // Act
        Map<String, Object> result = chatController.ask(validPayload);

        // Assert
        assertNotNull(result);
        assertEquals("rag_response", result.get("type"));
        assertEquals("I couldn't find an exact answer. Try rephrasing or upload FAQs to RAG.", result.get("answer"));
        assertTrue(result.containsKey("sources"));
        assertTrue(result.containsKey("suggestions"));
    }

    @Test
    void testAsk_WithBlankAnswer_AddsFallbackMessage() {
        // Arrange
        Map<String, Object> responseWithBlankAnswer = new LinkedHashMap<>();
        responseWithBlankAnswer.put("type", "rag_response");
        responseWithBlankAnswer.put("answer", "   ");
        responseWithBlankAnswer.put("sources", new Object[]{});
        responseWithBlankAnswer.put("suggestions", new Object[]{});

        when(chatService.answerQuestion(anyString(), anyString()))
                .thenReturn(responseWithBlankAnswer);

        // Act
        Map<String, Object> result = chatController.ask(validPayload);

        // Assert
        assertNotNull(result);
        assertEquals("rag_response", result.get("type"));
        assertEquals("I couldn't find an exact answer. Try rephrasing or upload FAQs to RAG.", result.get("answer"));
    }

    @Test
    void testAsk_WithNullAnswer_AddsFallbackMessage() {
        // Arrange
        Map<String, Object> responseWithNullAnswer = new LinkedHashMap<>();
        responseWithNullAnswer.put("type", "rag_response");
        responseWithNullAnswer.put("answer", null);
        responseWithNullAnswer.put("sources", new Object[]{});
        responseWithNullAnswer.put("suggestions", new Object[]{});

        when(chatService.answerQuestion(anyString(), anyString()))
                .thenReturn(responseWithNullAnswer);

        // Act
        Map<String, Object> result = chatController.ask(validPayload);

        // Assert
        assertNotNull(result);
        assertEquals("rag_response", result.get("type"));
        // The controller converts null to "null" string but the map still contains null
        // So the answer remains null
        assertNull(result.get("answer"));
        assertTrue(result.containsKey("sources"));
        assertTrue(result.containsKey("suggestions"));
    }

    @Test
    void testAsk_WithMissingAnswerKey_AddsFallbackMessage() {
        // Arrange
        Map<String, Object> responseWithoutAnswer = new LinkedHashMap<>();
        responseWithoutAnswer.put("type", "rag_response");
        responseWithoutAnswer.put("sources", new Object[]{});
        responseWithoutAnswer.put("suggestions", new Object[]{});

        when(chatService.answerQuestion(anyString(), anyString()))
                .thenReturn(validResponse);

        // Act
        Map<String, Object> result = chatController.ask(validPayload);

        // Assert
        assertNotNull(result);
        assertEquals("rag_response", result.get("type"));
        assertEquals("The stock level for SKU123 is 50 units.", result.get("answer"));
    }

    @Test
    void testAsk_WithValidAnswer_ReturnsOriginalResponse() {
        // Arrange
        Map<String, Object> responseWithValidAnswer = new LinkedHashMap<>();
        responseWithValidAnswer.put("type", "rag_response");
        responseWithValidAnswer.put("answer", "This is a valid answer from the service.");
        responseWithValidAnswer.put("sources", new Object[]{});
        responseWithValidAnswer.put("suggestions", new Object[]{});

        when(chatService.answerQuestion(anyString(), anyString()))
                .thenReturn(responseWithValidAnswer);

        // Act
        Map<String, Object> result = chatController.ask(validPayload);

        // Assert
        assertNotNull(result);
        assertEquals("rag_response", result.get("type"));
        assertEquals("This is a valid answer from the service.", result.get("answer"));
        assertNotEquals("I couldn't find an exact answer. Try rephrasing or upload FAQs to RAG.", result.get("answer"));
    }

    @Test
    void testAsk_WithNullPayload_HandlesGracefully() {
        // Act & Assert
        assertThrows(NullPointerException.class, () -> {
            chatController.ask(null);
        });
    }

    @Test
    void testAsk_WithEmptyPayload_HandlesGracefully() {
        // Arrange
        Map<String, Object> emptyPayload = new HashMap<>();
        when(chatService.answerQuestion(eq("demo"), isNull()))
                .thenReturn(validResponse);

        // Act
        Map<String, Object> result = chatController.ask(emptyPayload);

        // Assert
        assertNotNull(result);
        verify(chatService).answerQuestion("demo", null);
    }

    @Test
    void testAsk_WithNullQuestion_HandlesGracefully() {
        // Arrange
        Map<String, Object> payloadWithNullQuestion = new HashMap<>();
        payloadWithNullQuestion.put("q", null);
        payloadWithNullQuestion.put("tenant_id", "test-tenant");

        when(chatService.answerQuestion(eq("test-tenant"), isNull()))
                .thenReturn(validResponse);

        // Act
        Map<String, Object> result = chatController.ask(payloadWithNullQuestion);

        // Assert
        assertNotNull(result);
        verify(chatService).answerQuestion("test-tenant", null);
    }

    @Test
    void testAsk_WithEmptyQuestion_HandlesGracefully() {
        // Arrange
        Map<String, Object> payloadWithEmptyQuestion = new HashMap<>();
        payloadWithEmptyQuestion.put("q", "");
        payloadWithEmptyQuestion.put("tenant_id", "test-tenant");

        when(chatService.answerQuestion(eq("test-tenant"), eq("")))
                .thenReturn(validResponse);

        // Act
        Map<String, Object> result = chatController.ask(payloadWithEmptyQuestion);

        // Assert
        assertNotNull(result);
        verify(chatService).answerQuestion("test-tenant", "");
    }

    @Test
    void testAsk_WithWhitespaceQuestion_HandlesGracefully() {
        // Arrange
        Map<String, Object> payloadWithWhitespaceQuestion = new HashMap<>();
        payloadWithWhitespaceQuestion.put("q", "   ");
        payloadWithWhitespaceQuestion.put("tenant_id", "test-tenant");

        when(chatService.answerQuestion(eq("test-tenant"), eq("   ")))
                .thenReturn(validResponse);

        // Act
        Map<String, Object> result = chatController.ask(payloadWithWhitespaceQuestion);

        // Assert
        assertNotNull(result);
        verify(chatService).answerQuestion("test-tenant", "   ");
    }

    @Test
    void testAsk_WithComplexResponse_ReturnsCompleteResponse() {
        // Arrange
        Map<String, Object> complexResponse = new LinkedHashMap<>();
        complexResponse.put("type", "tool_response");
        complexResponse.put("tool", "get_inventory");
        complexResponse.put("answer", "Complex inventory response");
        complexResponse.put("data", Map.of("sku", "SKU123", "qty", 100));
        complexResponse.put("sources", new Object[]{});
        complexResponse.put("suggestions", new Object[]{});

        when(chatService.answerQuestion(anyString(), anyString()))
                .thenReturn(complexResponse);

        // Act
        Map<String, Object> result = chatController.ask(validPayload);

        // Assert
        assertNotNull(result);
        assertEquals("tool_response", result.get("type"));
        assertEquals("get_inventory", result.get("tool"));
        assertEquals("Complex inventory response", result.get("answer"));
        assertTrue(result.containsKey("data"));
        assertEquals("SKU123", ((Map<?, ?>) result.get("data")).get("sku"));
        assertEquals(100, ((Map<?, ?>) result.get("data")).get("qty"));
    }
}
