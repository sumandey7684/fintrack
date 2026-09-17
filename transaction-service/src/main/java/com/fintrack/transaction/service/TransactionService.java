package com.fintrack.transaction.service;

import com.fintrack.transaction.client.AccountServiceClient;
import com.fintrack.transaction.domain.Transaction;
import com.fintrack.transaction.dto.CreateTransactionRequest;
import com.fintrack.transaction.dto.TransactionResponse;
import com.fintrack.transaction.repository.TransactionRepository;
import com.fintrack.transaction.util.Money;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Records transaction history only.
 * <p>
 * Consistency limitation (ADR-005): balance updates happen in Account Service
 * ({@code POST /api/accounts/{id}/deposit}); this service does not change balances.
 * A deposit recorded here without a matching Account Service deposit (or vice versa)
 * can leave balance and history out of sync. The MVP intentionally does not use
 * distributed transactions.
 */
@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountServiceClient accountServiceClient;

    public TransactionService(
            TransactionRepository transactionRepository,
            AccountServiceClient accountServiceClient) {
        this.transactionRepository = transactionRepository;
        this.accountServiceClient = accountServiceClient;
    }

    @Transactional
    public TransactionResponse createTransaction(CreateTransactionRequest request) {
        BigDecimal amount = Money.normalize(request.getAmount());
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("amount must be greater than zero");
        }

        accountServiceClient.verifyAccountExists(request.getAccountId());

        Transaction transaction = new Transaction();
        transaction.setAccountId(request.getAccountId());
        transaction.setType(request.getType());
        transaction.setAmount(amount);

        return toResponse(transactionRepository.save(transaction));
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> getTransactionsForAccount(Long accountId) {
        if (accountId == null || accountId <= 0) {
            throw new IllegalArgumentException("accountId must be positive");
        }
        return transactionRepository.findByAccountIdOrderByCreatedAtDesc(accountId).stream()
                .map(this::toResponse)
                .toList();
    }

    private TransactionResponse toResponse(Transaction transaction) {
        return new TransactionResponse(
                transaction.getId(),
                transaction.getAccountId(),
                transaction.getType(),
                transaction.getAmount(),
                transaction.getCreatedAt()
        );
    }
}
