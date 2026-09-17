package com.fintrack.account.service;

import com.fintrack.account.domain.Account;
import com.fintrack.account.dto.AccountResponse;
import com.fintrack.account.dto.CreateAccountRequest;
import com.fintrack.account.dto.DepositRequest;
import com.fintrack.account.exception.DuplicateAccountException;
import com.fintrack.account.exception.ResourceNotFoundException;
import com.fintrack.account.repository.AccountRepository;
import com.fintrack.account.util.Money;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class AccountService {

    private final AccountRepository accountRepository;

    public AccountService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Transactional
    public AccountResponse createAccount(CreateAccountRequest request) {
        String accountNumber = request.getAccountNumber().trim();
        String accountHolder = request.getAccountHolder().trim();

        if (accountRepository.existsByAccountNumber(accountNumber)) {
            throw new DuplicateAccountException("Account number already exists: " + accountNumber);
        }

        Account account = new Account();
        account.setAccountNumber(accountNumber);
        account.setAccountHolder(accountHolder);
        account.setBalance(Money.zero());

        return toResponse(accountRepository.save(account));
    }

    @Transactional(readOnly = true)
    public AccountResponse getAccount(Long id) {
        return toResponse(findAccountOrThrow(id));
    }

    @Transactional
    public AccountResponse deposit(Long id, DepositRequest request) {
        Account account = findAccountOrThrow(id);
        BigDecimal amount = Money.normalize(request.getAmount());

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("amount must be greater than zero");
        }

        account.setBalance(Money.normalize(account.getBalance().add(amount)));
        return toResponse(accountRepository.save(account));
    }

    private Account findAccountOrThrow(Long id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + id));
    }

    private AccountResponse toResponse(Account account) {
        return new AccountResponse(
                account.getId(),
                account.getAccountNumber(),
                account.getAccountHolder(),
                account.getBalance(),
                account.getCreatedAt()
        );
    }
}
