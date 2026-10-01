package com.jobhunter.client.firecrawl;

import com.jobhunter.client.firecrawl.dto.*;
import com.jobhunter.client.firecrawl.exception.FirecrawlApiKeyMissingException;
import com.jobhunter.client.firecrawl.exception.FirecrawlClientException;
import com.jobhunter.client.firecrawl.exception.FirecrawlRateLimitException;
import com.jobhunter.config.FirecrawlProperties;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.*;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class FirecrawlClientTest {

    private FirecrawlProperties properties;
    private WebClient webClient;
    private WebClient.RequestBodyUriSpec uriSpec;
    private WebClient.RequestBodySpec bodySpec;
    private WebClient.RequestHeadersSpec headersSpec;
    private WebClient.ResponseSpec responseSpec;

    private RateLimiterRegistry rateLimiterRegistry;
    private CircuitBreakerRegistry circuitBreakerRegistry;
    private RetryRegistry retryRegistry;

    private FirecrawlClientImpl client;

    @BeforeEach
    void setUp() {
        properties = new FirecrawlProperties();
        properties.setApiKey("test_firecrawl_key_123");
        properties.setBaseUrl("https://api.firecrawl.dev");

        rateLimiterRegistry = RateLimiterRegistry.of(RateLimiterConfig.ofDefaults());
        circuitBreakerRegistry = CircuitBreakerRegistry.of(CircuitBreakerConfig.ofDefaults());
        retryRegistry = RetryRegistry.of(RetryConfig.ofDefaults());

        webClient = mock(WebClient.class);
        uriSpec = mock(WebClient.RequestBodyUriSpec.class);
        bodySpec = mock(WebClient.RequestBodySpec.class);
        headersSpec = mock(WebClient.RequestHeadersSpec.class);
        responseSpec = mock(WebClient.ResponseSpec.class);

        when(webClient.post()).thenReturn(uriSpec);
        when(uriSpec.uri(anyString())).thenReturn(bodySpec);
        when(bodySpec.header(eq(HttpHeaders.AUTHORIZATION), anyString())).thenReturn(bodySpec);
        when(bodySpec.bodyValue(any())).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);

        client = new FirecrawlClientImpl(webClient, properties, rateLimiterRegistry, circuitBreakerRegistry, retryRegistry);
    }

    @Test
    @DisplayName("Should execute search and return mapped documents successfully")
    void shouldExecuteSearchSuccessfully() {
        FirecrawlSearchResponse mockResponse = new FirecrawlSearchResponse();
        mockResponse.setSuccess(true);
        FirecrawlDocument doc = new FirecrawlDocument("https://boards.greenhouse.io/stripe/jobs/123", "Backend Engineer", "# Job");
        mockResponse.setData(List.of(doc));

        when(responseSpec.bodyToMono(eq(FirecrawlSearchResponse.class))).thenReturn(Mono.just(mockResponse));

        FirecrawlSearchRequest req = new FirecrawlSearchRequest("site:boards.greenhouse.io \"Java\"", 10);
        FirecrawlSearchResponse res = client.search(req);

        assertNotNull(res);
        assertTrue(res.isSuccess());
        assertEquals(1, res.getData().size());
        assertEquals("https://boards.greenhouse.io/stripe/jobs/123", res.getData().get(0).getUrl());
    }

    @Test
    @DisplayName("Should execute scrape and return document details")
    void shouldExecuteScrapeSuccessfully() {
        FirecrawlScrapeResponse mockResponse = new FirecrawlScrapeResponse();
        mockResponse.setSuccess(true);
        FirecrawlDocument doc = new FirecrawlDocument("https://boards.greenhouse.io/stripe/jobs/123", "Backend Engineer", "# Full Job Markdown");
        mockResponse.setData(doc);

        when(responseSpec.bodyToMono(eq(FirecrawlScrapeResponse.class))).thenReturn(Mono.just(mockResponse));

        FirecrawlScrapeRequest req = new FirecrawlScrapeRequest("https://boards.greenhouse.io/stripe/jobs/123");
        FirecrawlScrapeResponse res = client.scrape(req);

        assertNotNull(res);
        assertTrue(res.isSuccess());
        assertEquals("# Full Job Markdown", res.getData().getMarkdown());
    }

    @Test
    @DisplayName("Should throw FirecrawlApiKeyMissingException when API key is not configured")
    void shouldThrowWhenApiKeyMissing() {
        properties.setApiKey("");

        FirecrawlSearchRequest req = new FirecrawlSearchRequest("test", 10);
        assertThrows(FirecrawlApiKeyMissingException.class, () -> client.search(req));
    }

    @Test
    @DisplayName("Should throw FirecrawlRateLimitException on 429 status code")
    void shouldThrowRateLimitExceptionOn429() {
        when(responseSpec.bodyToMono(eq(FirecrawlSearchResponse.class)))
                .thenReturn(Mono.error(WebClientResponseException.create(HttpStatus.TOO_MANY_REQUESTS.value(), "Too Many Requests", HttpHeaders.EMPTY, new byte[0], null)));

        FirecrawlSearchRequest req = new FirecrawlSearchRequest("test", 10);
        assertThrows(FirecrawlRateLimitException.class, () -> client.search(req));
    }

    @Test
    @DisplayName("Should throw FirecrawlClientException on 500 server error")
    void shouldThrowClientExceptionOn500() {
        when(responseSpec.bodyToMono(eq(FirecrawlSearchResponse.class)))
                .thenReturn(Mono.error(WebClientResponseException.create(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Internal Server Error", HttpHeaders.EMPTY, new byte[0], null)));

        FirecrawlSearchRequest req = new FirecrawlSearchRequest("test", 10);
        assertThrows(FirecrawlClientException.class, () -> client.search(req));
    }
}
