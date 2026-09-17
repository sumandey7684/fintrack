package com.fintrack.transaction.client;

import com.fintrack.transaction.exception.AccountServiceUnavailableException;
import com.fintrack.transaction.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class AccountServiceClientTest {

    private MockRestServiceServer server;
    private AccountServiceClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://localhost:8081");
        server = MockRestServiceServer.bindTo(builder).build();
        client = new AccountServiceClient(builder.build());
    }

    @Test
    void verifyAccountExists_WhenFound_Succeeds() {
        server.expect(requestTo("http://localhost:8081/api/accounts/1"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"id\":1}", MediaType.APPLICATION_JSON));

        assertThatCode(() -> client.verifyAccountExists(1L)).doesNotThrowAnyException();
        server.verify();
    }

    @Test
    void verifyAccountExists_WhenMissing_ThrowsNotFound() {
        server.expect(requestTo("http://localhost:8081/api/accounts/99"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThatThrownBy(() -> client.verifyAccountExists(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
        server.verify();
    }

    @Test
    void verifyAccountExists_WhenServerError_ThrowsUnavailable() {
        server.expect(requestTo("http://localhost:8081/api/accounts/1"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> client.verifyAccountExists(1L))
                .isInstanceOf(AccountServiceUnavailableException.class);
        server.verify();
    }
}
