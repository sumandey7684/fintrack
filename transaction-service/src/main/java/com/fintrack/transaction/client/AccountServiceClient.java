package com.fintrack.transaction.client;

import com.fintrack.transaction.exception.AccountServiceUnavailableException;
import com.fintrack.transaction.exception.ResourceNotFoundException;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
public class AccountServiceClient {

    private final RestClient accountServiceRestClient;

    public AccountServiceClient(RestClient accountServiceRestClient) {
        this.accountServiceRestClient = accountServiceRestClient;
    }

    /**
     * Verifies that an account exists in Account Service.
     * Does not read or cache balances — Transaction Service never owns account balances (ADR-005).
     */
    public void verifyAccountExists(Long accountId) {
        try {
            accountServiceRestClient.get()
                    .uri("/api/accounts/{id}", accountId)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (request, response) -> {
                        if (response.getStatusCode().value() == 404) {
                            throw new ResourceNotFoundException("Account not found: " + accountId);
                        }
                        throw new AccountServiceUnavailableException(
                                "Account Service returned client error: " + response.getStatusCode().value());
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, (request, response) -> {
                        throw new AccountServiceUnavailableException(
                                "Account Service returned server error: " + response.getStatusCode().value());
                    })
                    .toBodilessEntity();
        } catch (ResourceNotFoundException | AccountServiceUnavailableException ex) {
            throw ex;
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 404) {
                throw new ResourceNotFoundException("Account not found: " + accountId);
            }
            throw new AccountServiceUnavailableException(
                    "Account Service communication failed: " + ex.getStatusCode().value(), ex);
        } catch (RestClientException ex) {
            throw new AccountServiceUnavailableException(
                    "Account Service is unreachable", ex);
        }
    }
}
