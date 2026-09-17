package com.fintrack.transaction.service;

import com.fintrack.transaction.client.AccountServiceClient;
import com.fintrack.transaction.domain.Transaction;
import com.fintrack.transaction.domain.TransactionType;
import com.fintrack.transaction.dto.CreateTransactionRequest;
import com.fintrack.transaction.exception.AccountServiceUnavailableException;
import com.fintrack.transaction.exception.ResourceNotFoundException;
import com.fintrack.transaction.repository.TransactionRepository;
import com.fintrack.transaction.util.Money;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AccountServiceClient accountServiceClient;

    @InjectMocks
    private TransactionService transactionService;

    @Test
    void createTransaction_Success_PersistsAfterAccountCheck() {
        CreateTransactionRequest request = request(1L, TransactionType.DEPOSIT, "1000.00");
        doNothing().when(accountServiceClient).verifyAccountExists(1L);
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction saved = invocation.getArgument(0);
            saved.setId(10L);
            saved.setCreatedAt(Instant.parse("2026-09-17T10:05:00Z"));
            return saved;
        });

        var response = transactionService.createTransaction(request);

        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getAccountId()).isEqualTo(1L);
        assertThat(response.getType()).isEqualTo(TransactionType.DEPOSIT);
        assertThat(response.getAmount()).isEqualByComparingTo("1000.00");

        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(captor.capture());
        assertThat(captor.getValue().getAmount()).isEqualByComparingTo(Money.normalize(new BigDecimal("1000.00")));
    }

    @Test
    void createTransaction_MissingAccount_DoesNotPersist() {
        CreateTransactionRequest request = request(99L, TransactionType.DEPOSIT, "10.00");
        doThrow(new ResourceNotFoundException("Account not found: 99"))
                .when(accountServiceClient).verifyAccountExists(99L);

        assertThatThrownBy(() -> transactionService.createTransaction(request))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(transactionRepository, never()).save(any());
    }

    @Test
    void createTransaction_AccountServiceDown_DoesNotPersist() {
        CreateTransactionRequest request = request(1L, TransactionType.WITHDRAWAL, "10.00");
        doThrow(new AccountServiceUnavailableException("Account Service is unreachable"))
                .when(accountServiceClient).verifyAccountExists(1L);

        assertThatThrownBy(() -> transactionService.createTransaction(request))
                .isInstanceOf(AccountServiceUnavailableException.class);

        verify(transactionRepository, never()).save(any());
    }

    @Test
    void getTransactionsForAccount_ReturnsHistory() {
        Transaction tx = new Transaction();
        tx.setId(10L);
        tx.setAccountId(1L);
        tx.setType(TransactionType.DEPOSIT);
        tx.setAmount(new BigDecimal("100.50"));
        tx.setCreatedAt(Instant.parse("2026-09-17T10:05:00Z"));

        when(transactionRepository.findByAccountIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(tx));

        var history = transactionService.getTransactionsForAccount(1L);

        assertThat(history).hasSize(1);
        assertThat(history.get(0).getType()).isEqualTo(TransactionType.DEPOSIT);
    }

    @Test
    void getTransactionsForAccount_InvalidId_Throws() {
        assertThatThrownBy(() -> transactionService.getTransactionsForAccount(0L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("positive");
    }

    private CreateTransactionRequest request(Long accountId, TransactionType type, String amount) {
        CreateTransactionRequest request = new CreateTransactionRequest();
        request.setAccountId(accountId);
        request.setType(type);
        request.setAmount(new BigDecimal(amount));
        return request;
    }
}
