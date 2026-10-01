package com.jobhunter.client.firecrawl;

import com.jobhunter.client.firecrawl.dto.*;

public interface FirecrawlClient {

    FirecrawlSearchResponse search(FirecrawlSearchRequest request);

    FirecrawlScrapeResponse scrape(FirecrawlScrapeRequest request);

    FirecrawlCrawlResponse crawl(FirecrawlCrawlRequest request);

    FirecrawlMapResponse map(FirecrawlMapRequest request);

    boolean isAvailable();
}
