package com.fintrack.transaction.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintrack.transaction.domain.TransactionType;
import com.fintrack.transaction.dto.CreateTransactionRequest;
import com.fintrack.transaction.dto.TransactionResponse;
import com.fintrack.transaction.exception.AccountServiceUnavailableException;
import com.fintrack.transaction.exception.GlobalExceptionHandler;
import com.fintrack.transaction.exception.ResourceNotFoundException;
import com.fintrack.transaction.service.TransactionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TransactionController.class)
@Import(GlobalExceptionHandler.class)
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TransactionService transactionService;

    @Test
    void createTransaction_Returns201() throws Exception {
        CreateTransactionRequest request = validRequest();
        when(transactionService.createTransaction(any(CreateTransactionRequest.class)))
                .thenReturn(sampleResponse(TransactionType.DEPOSIT));

        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.type").value("DEPOSIT"))
                .andExpect(jsonPath("$.amount").value(1000.00));
    }

    @Test
    void createTransaction_InvalidAmount_Returns400() throws Exception {
        String body = """
                {"accountId":1,"type":"DEPOSIT","amount":0}
                """;

        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createTransaction_InvalidAccountId_Returns400() throws Exception {
        String body = """
                {"accountId":0,"type":"DEPOSIT","amount":10.00}
                """;

        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createTransaction_InvalidType_Returns400() throws Exception {
        String body = """
                {"accountId":1,"type":"TRANSFER","amount":10.00}
                """;

        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("type must be DEPOSIT or WITHDRAWAL"));
    }

    @Test
    void createTransaction_MissingAccount_Returns404() throws Exception {
        when(transactionService.createTransaction(any(CreateTransactionRequest.class)))
                .thenThrow(new ResourceNotFoundException("Account not found: 99"));

        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void createTransaction_AccountServiceDown_Returns503() throws Exception {
        when(transactionService.createTransaction(any(CreateTransactionRequest.class)))
                .thenThrow(new AccountServiceUnavailableException("Account Service is unreachable"));

        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503));
    }

    @Test
    void getHistory_Returns200() throws Exception {
        when(transactionService.getTransactionsForAccount(1L))
                .thenReturn(List.of(sampleResponse(TransactionType.DEPOSIT)));

        mockMvc.perform(get("/api/transactions/account/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].accountId").value(1))
                .andExpect(jsonPath("$[0].type").value("DEPOSIT"));
    }

    private CreateTransactionRequest validRequest() {
        CreateTransactionRequest request = new CreateTransactionRequest();
        request.setAccountId(1L);
        request.setType(TransactionType.DEPOSIT);
        request.setAmount(new BigDecimal("1000.00"));
        return request;
    }

    private TransactionResponse sampleResponse(TransactionType type) {
        return new TransactionResponse(
                10L,
                1L,
                type,
                new BigDecimal("1000.00"),
                Instant.parse("2026-09-17T10:05:00Z")
        );
    }
}
