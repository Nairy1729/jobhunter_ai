package com.jobhunter.service.government;

import com.jobhunter.model.entity.government.CrawlFrequency;
import com.jobhunter.model.entity.government.GovernmentSource;
import com.jobhunter.repository.government.GovernmentSourceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
public class GovernmentSourceSchedulerService {

    private static final Logger log = LoggerFactory.getLogger(GovernmentSourceSchedulerService.class);

    private final GovernmentSourceRepository sourceRepository;
    private final GovernmentDiscoveryService discoveryService;

    public GovernmentSourceSchedulerService(
            GovernmentSourceRepository sourceRepository,
            GovernmentDiscoveryService discoveryService) {
        this.sourceRepository = sourceRepository;
        this.discoveryService = discoveryService;
    }

    /**
     * Periodic scheduler running every hour to check sources that are due for crawling.
     */
    @Scheduled(fixedDelay = 3600000) // 1 hour
    public void runScheduledCrawl() {
        log.info("Running scheduled Government Source crawl check...");
        List<GovernmentSource> sources = sourceRepository.findByActiveTrue();
        Instant now = Instant.now();

        int checkedCount = 0;
        for (GovernmentSource source : sources) {
            if (isDueForCrawl(source, now)) {
                checkedCount++;
                try {
                    discoveryService.executeDiscovery(source.getState(), source.getDistrict(), null, 2);
                } catch (Exception e) {
                    log.error("Scheduled crawl error for source [{}]: {}", source.getName(), e.getMessage());
                }
            }
        }
        log.info("Scheduled Government Source crawl check finished. Processed {} due sources.", checkedCount);
    }

    public boolean isDueForCrawl(GovernmentSource source, Instant now) {
        if (source.getLastChecked() == null) return true;

        Duration elapsed = Duration.between(source.getLastChecked(), now);
        CrawlFrequency freq = source.getCrawlFrequency() != null ? source.getCrawlFrequency() : CrawlFrequency.DAILY;

        return switch (freq) {
            case HIGH_HOURLY -> elapsed.toHours() >= 1;
            case DAILY -> elapsed.toHours() >= 24;
            case BIWEEKLY -> elapsed.toDays() >= 3;
            case WEEKLY -> elapsed.toDays() >= 7;
        };
    }
}
