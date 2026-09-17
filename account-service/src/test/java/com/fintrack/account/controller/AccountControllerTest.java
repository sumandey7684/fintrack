package com.fintrack.account.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintrack.account.dto.AccountResponse;
import com.fintrack.account.dto.CreateAccountRequest;
import com.fintrack.account.dto.DepositRequest;
import com.fintrack.account.exception.DuplicateAccountException;
import com.fintrack.account.exception.GlobalExceptionHandler;
import com.fintrack.account.exception.ResourceNotFoundException;
import com.fintrack.account.service.AccountService;
import com.fintrack.account.util.Money;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AccountController.class)
@Import(GlobalExceptionHandler.class)
class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AccountService accountService;

    @Test
    void createAccount_Returns201() throws Exception {
        CreateAccountRequest request = new CreateAccountRequest();
        request.setAccountNumber("ACC-1001");
        request.setAccountHolder("Ada Lovelace");

        when(accountService.createAccount(any(CreateAccountRequest.class)))
                .thenReturn(sampleResponse(Money.zero()));

        mockMvc.perform(post("/api/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.accountNumber").value("ACC-1001"))
                .andExpect(jsonPath("$.balance").value(0.00));
    }

    @Test
    void createAccount_BlankHolder_Returns400() throws Exception {
        String body = """
                {"accountNumber":"ACC-1001","accountHolder":"   "}
                """;

        mockMvc.perform(post("/api/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createAccount_Duplicate_Returns409() throws Exception {
        CreateAccountRequest request = new CreateAccountRequest();
        request.setAccountNumber("ACC-1001");
        request.setAccountHolder("Ada Lovelace");

        when(accountService.createAccount(any(CreateAccountRequest.class)))
                .thenThrow(new DuplicateAccountException("Account number already exists: ACC-1001"));

        mockMvc.perform(post("/api/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void getAccount_Returns200() throws Exception {
        when(accountService.getAccount(1L)).thenReturn(sampleResponse(Money.zero()));

        mockMvc.perform(get("/api/accounts/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountHolder").value("Ada Lovelace"));
    }

    @Test
    void getAccount_Missing_Returns404() throws Exception {
        when(accountService.getAccount(99L))
                .thenThrow(new ResourceNotFoundException("Account not found: 99"));

        mockMvc.perform(get("/api/accounts/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void deposit_Returns200() throws Exception {
        DepositRequest request = new DepositRequest();
        request.setAmount(new BigDecimal("50.00"));

        when(accountService.deposit(eq(1L), any(DepositRequest.class)))
                .thenReturn(sampleResponse(new BigDecimal("50.00")));

        mockMvc.perform(post("/api/accounts/1/deposit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(50.00));
    }

    @Test
    void deposit_NonPositiveAmount_Returns400() throws Exception {
        String body = """
                {"amount":0}
                """;

        mockMvc.perform(post("/api/accounts/1/deposit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    private AccountResponse sampleResponse(BigDecimal balance) {
        return new AccountResponse(
                1L,
                "ACC-1001",
                "Ada Lovelace",
                balance,
                Instant.parse("2026-09-17T10:00:00Z")
        );
    }
}
