package com.jobhunter.client.firecrawl;

import com.jobhunter.client.firecrawl.dto.*;
import com.jobhunter.client.firecrawl.exception.FirecrawlApiKeyMissingException;
import com.jobhunter.client.firecrawl.exception.FirecrawlClientException;
import com.jobhunter.client.firecrawl.exception.FirecrawlRateLimitException;
import com.jobhunter.config.FirecrawlProperties;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Service
public class FirecrawlClientImpl implements FirecrawlClient {

    private static final Logger log = LoggerFactory.getLogger(FirecrawlClientImpl.class);

    private final WebClient webClient;
    private final FirecrawlProperties properties;
    private final RateLimiter rateLimiter;
    private final CircuitBreaker circuitBreaker;
    private final Retry retry;

    @Autowired
    public FirecrawlClientImpl(
            FirecrawlProperties properties,
            RateLimiterRegistry rateLimiterRegistry,
            CircuitBreakerRegistry circuitBreakerRegistry,
            RetryRegistry retryRegistry) {
        this.properties = properties;
        this.rateLimiter = rateLimiterRegistry.rateLimiter("firecrawl");
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker("firecrawl");
        this.retry = retryRegistry.retry("firecrawl");

        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, properties.getConnectionTimeoutSeconds() * 1000)
                .responseTimeout(Duration.ofSeconds(properties.getReadTimeoutSeconds()))
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler(properties.getReadTimeoutSeconds(), TimeUnit.SECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(properties.getConnectionTimeoutSeconds(), TimeUnit.SECONDS)));

        this.webClient = WebClient.builder()
                .baseUrl(properties.getBaseUrl())
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    // Constructor for testing with mock WebClient
    public FirecrawlClientImpl(
            WebClient webClient,
            FirecrawlProperties properties,
            RateLimiterRegistry rateLimiterRegistry,
            CircuitBreakerRegistry circuitBreakerRegistry,
            RetryRegistry retryRegistry) {
        this.webClient = webClient;
        this.properties = properties;
        this.rateLimiter = rateLimiterRegistry.rateLimiter("firecrawl");
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker("firecrawl");
        this.retry = retryRegistry.retry("firecrawl");
    }

    @Override
    public boolean isAvailable() {
        return properties.isConfigured();
    }

    @Override
    public FirecrawlSearchResponse search(FirecrawlSearchRequest request) {
        return executeWithResilience(() -> executeSearch(request));
    }

    @Override
    public FirecrawlScrapeResponse scrape(FirecrawlScrapeRequest request) {
        return executeWithResilience(() -> executeScrape(request));
    }

    @Override
    public FirecrawlCrawlResponse crawl(FirecrawlCrawlRequest request) {
        return executeWithResilience(() -> executeCrawl(request));
    }

    @Override
    public FirecrawlMapResponse map(FirecrawlMapRequest request) {
        return executeWithResilience(() -> executeMap(request));
    }

    private <T> T executeWithResilience(Supplier<T> supplier) {
        Supplier<T> retrySupplier = Retry.decorateSupplier(retry, supplier);
        Supplier<T> circuitBreakerSupplier = CircuitBreaker.decorateSupplier(circuitBreaker, retrySupplier);
        Supplier<T> rateLimiterSupplier = RateLimiter.decorateSupplier(rateLimiter, circuitBreakerSupplier);
        return rateLimiterSupplier.get();
    }

    private void ensureApiKeyConfigured() {
        if (!properties.isConfigured()) {
            throw new FirecrawlApiKeyMissingException();
        }
    }

    private FirecrawlSearchResponse executeSearch(FirecrawlSearchRequest request) {
        ensureApiKeyConfigured();
        log.info("FIRECRAWL_SEARCH - Executing query: [{}], Limit: [{}]", request.getQuery(), request.getLimit());

        try {
            FirecrawlSearchResponse response = webClient.post()
                    .uri("/v1/search")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getApiKey())
                    .bodyValue(request)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, clientResponse -> {
                        int status = clientResponse.statusCode().value();
                        if (status == 429) {
                            return clientResponse.bodyToMono(String.class)
                                    .flatMap(body -> Mono.error(new FirecrawlRateLimitException("Firecrawl rate limit exceeded (429): " + body)));
                        } else if (status == 401 || status == 403) {
                            return clientResponse.bodyToMono(String.class)
                                    .flatMap(body -> Mono.error(new FirecrawlClientException("Firecrawl authentication/authorization failed (" + status + "): " + body, status)));
                        } else {
                            return clientResponse.bodyToMono(String.class)
                                    .flatMap(body -> Mono.error(new FirecrawlClientException("Firecrawl client error (" + status + "): " + body, status)));
                        }
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, clientResponse -> {
                        int status = clientResponse.statusCode().value();
                        return clientResponse.bodyToMono(String.class)
                                .flatMap(body -> Mono.error(new FirecrawlClientException("Firecrawl server error (" + status + "): " + body, status)));
                    })
                    .bodyToMono(FirecrawlSearchResponse.class)
                    .block(Duration.ofSeconds(properties.getReadTimeoutSeconds() + 5));

            int count = (response != null && response.getData() != null) ? response.getData().size() : 0;
            log.info("SEARCH_RESULTS_RECEIVED - Query: [{}], Found: [{}] items", request.getQuery(), count);
            return response != null ? response : new FirecrawlSearchResponse(false, List.of());
        } catch (WebClientResponseException e) {
            if (e.getStatusCode().value() == 429) {
                throw new FirecrawlRateLimitException("Firecrawl rate limit exceeded (429)", e);
            }
            throw new FirecrawlClientException("Firecrawl HTTP error: " + e.getStatusCode().value() + " " + e.getResponseBodyAsString(), e.getStatusCode().value(), e);
        } catch (FirecrawlClientException e) {
            throw e;
        } catch (Exception e) {
            log.error("Firecrawl search failed for query [{}]: {}", request.getQuery(), e.getMessage());
            throw new FirecrawlClientException("Firecrawl search failed: " + e.getMessage(), e);
        }
    }

    private FirecrawlScrapeResponse executeScrape(FirecrawlScrapeRequest request) {
        ensureApiKeyConfigured();
        log.info("JOB_PAGE_SCRAPE_REQUESTED - URL: [{}]", request.getUrl());

        try {
            FirecrawlScrapeResponse response = webClient.post()
                    .uri("/v1/scrape")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getApiKey())
                    .bodyValue(request)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, clientResponse -> {
                        int status = clientResponse.statusCode().value();
                        if (status == 429) {
                            return clientResponse.bodyToMono(String.class)
                                    .flatMap(body -> Mono.error(new FirecrawlRateLimitException("Firecrawl rate limit exceeded (429): " + body)));
                        } else if (status == 401 || status == 403) {
                            return clientResponse.bodyToMono(String.class)
                                    .flatMap(body -> Mono.error(new FirecrawlClientException("Firecrawl authentication/authorization failed (" + status + "): " + body, status)));
                        } else {
                            return clientResponse.bodyToMono(String.class)
                                    .flatMap(body -> Mono.error(new FirecrawlClientException("Firecrawl client error (" + status + "): " + body, status)));
                        }
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, clientResponse -> {
                        int status = clientResponse.statusCode().value();
                        return clientResponse.bodyToMono(String.class)
                                .flatMap(body -> Mono.error(new FirecrawlClientException("Firecrawl server error (" + status + "): " + body, status)));
                    })
                    .bodyToMono(FirecrawlScrapeResponse.class)
                    .block(Duration.ofSeconds(properties.getReadTimeoutSeconds() + 5));

            boolean success = response != null && response.isSuccess();
            log.info("JOB_PAGE_SCRAPED - URL: [{}], Success: [{}]", request.getUrl(), success);
            return response != null ? response : new FirecrawlScrapeResponse(false, null);
        } catch (WebClientResponseException e) {
            if (e.getStatusCode().value() == 429) {
                throw new FirecrawlRateLimitException("Firecrawl rate limit exceeded (429)", e);
            }
            throw new FirecrawlClientException("Firecrawl HTTP error: " + e.getStatusCode().value() + " " + e.getResponseBodyAsString(), e.getStatusCode().value(), e);
        } catch (FirecrawlClientException e) {
            throw e;
        } catch (Exception e) {
            log.error("Firecrawl scrape failed for URL [{}]: {}", request.getUrl(), e.getMessage());
            throw new FirecrawlClientException("Firecrawl scrape failed: " + e.getMessage(), e);
        }
    }

    private FirecrawlCrawlResponse executeCrawl(FirecrawlCrawlRequest request) {
        ensureApiKeyConfigured();
        log.info("FIRECRAWL_CRAWL - URL: [{}], MaxDepth: [{}]", request.getUrl(), request.getMaxDepth());

        try {
            return webClient.post()
                    .uri("/v1/crawl")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getApiKey())
                    .bodyValue(request)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, clientResponse -> {
                        int status = clientResponse.statusCode().value();
                        if (status == 429) {
                            return clientResponse.bodyToMono(String.class)
                                    .flatMap(body -> Mono.error(new FirecrawlRateLimitException("Firecrawl rate limit exceeded (429): " + body)));
                        }
                        return clientResponse.bodyToMono(String.class)
                                .flatMap(body -> Mono.error(new FirecrawlClientException("Firecrawl client error (" + status + "): " + body, status)));
                    })
                    .bodyToMono(FirecrawlCrawlResponse.class)
                    .block(Duration.ofSeconds(properties.getReadTimeoutSeconds() + 5));
        } catch (FirecrawlClientException e) {
            throw e;
        } catch (Exception e) {
            log.error("Firecrawl crawl failed for URL [{}]: {}", request.getUrl(), e.getMessage());
            throw new FirecrawlClientException("Firecrawl crawl failed: " + e.getMessage(), e);
        }
    }

    private FirecrawlMapResponse executeMap(FirecrawlMapRequest request) {
        ensureApiKeyConfigured();
        log.info("FIRECRAWL_MAP - URL: [{}]", request.getUrl());

        try {
            return webClient.post()
                    .uri("/v1/map")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getApiKey())
                    .bodyValue(request)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, clientResponse -> {
                        int status = clientResponse.statusCode().value();
                        if (status == 429) {
                            return clientResponse.bodyToMono(String.class)
                                    .flatMap(body -> Mono.error(new FirecrawlRateLimitException("Firecrawl rate limit exceeded (429): " + body)));
                        }
                        return clientResponse.bodyToMono(String.class)
                                .flatMap(body -> Mono.error(new FirecrawlClientException("Firecrawl client error (" + status + "): " + body, status)));
                    })
                    .bodyToMono(FirecrawlMapResponse.class)
                    .block(Duration.ofSeconds(properties.getReadTimeoutSeconds() + 5));
        } catch (FirecrawlClientException e) {
            throw e;
        } catch (Exception e) {
            log.error("Firecrawl map failed for URL [{}]: {}", request.getUrl(), e.getMessage());
            throw new FirecrawlClientException("Firecrawl map failed: " + e.getMessage(), e);
        }
    }
}
