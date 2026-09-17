package com.fintrack.account.service;

import com.fintrack.account.domain.Account;
import com.fintrack.account.dto.CreateAccountRequest;
import com.fintrack.account.dto.DepositRequest;
import com.fintrack.account.exception.DuplicateAccountException;
import com.fintrack.account.exception.ResourceNotFoundException;
import com.fintrack.account.repository.AccountRepository;
import com.fintrack.account.util.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private AccountService accountService;

    private Account existing;

    @BeforeEach
    void setUp() {
        existing = new Account();
        existing.setId(1L);
        existing.setAccountNumber("ACC-1001");
        existing.setAccountHolder("Ada Lovelace");
        existing.setBalance(Money.zero());
        existing.setCreatedAt(Instant.parse("2026-09-17T10:00:00Z"));
    }

    @Test
    void createAccount_PersistsZeroBalance() {
        CreateAccountRequest request = new CreateAccountRequest();
        request.setAccountNumber("ACC-1001");
        request.setAccountHolder("Ada Lovelace");

        when(accountRepository.existsByAccountNumber("ACC-1001")).thenReturn(false);
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> {
            Account saved = invocation.getArgument(0);
            saved.setId(1L);
            saved.setCreatedAt(Instant.parse("2026-09-17T10:00:00Z"));
            return saved;
        });

        var response = accountService.createAccount(request);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getAccountNumber()).isEqualTo("ACC-1001");
        assertThat(response.getAccountHolder()).isEqualTo("Ada Lovelace");
        assertThat(response.getBalance()).isEqualByComparingTo("0.00");

        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository).save(captor.capture());
        assertThat(captor.getValue().getBalance()).isEqualByComparingTo("0.00");
    }

    @Test
    void createAccount_DuplicateNumber_ThrowsConflict() {
        CreateAccountRequest request = new CreateAccountRequest();
        request.setAccountNumber("ACC-1001");
        request.setAccountHolder("Ada Lovelace");

        when(accountRepository.existsByAccountNumber("ACC-1001")).thenReturn(true);

        assertThatThrownBy(() -> accountService.createAccount(request))
                .isInstanceOf(DuplicateAccountException.class)
                .hasMessageContaining("ACC-1001");
    }

    @Test
    void getAccount_Found_ReturnsResponse() {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(existing));

        var response = accountService.getAccount(1L);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getAccountHolder()).isEqualTo("Ada Lovelace");
    }

    @Test
    void getAccount_Missing_ThrowsNotFound() {
        when(accountRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.getAccount(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void deposit_PositiveAmount_IncreasesBalance() {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DepositRequest request = new DepositRequest();
        request.setAmount(new BigDecimal("100.50"));

        var response = accountService.deposit(1L, request);

        assertThat(response.getBalance()).isEqualByComparingTo("100.50");
    }

    @Test
    void deposit_MissingAccount_ThrowsNotFound() {
        when(accountRepository.findById(99L)).thenReturn(Optional.empty());

        DepositRequest request = new DepositRequest();
        request.setAmount(new BigDecimal("10.00"));

        assertThatThrownBy(() -> accountService.deposit(99L, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
