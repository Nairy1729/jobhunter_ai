package com.jobhunter.service.discovery.adapter;

import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Component
public class JobSourceAdapterRegistry {

    private final List<JobSourceAdapter> adapters;
    private final GenericCareerAdapter fallbackAdapter;

    public JobSourceAdapterRegistry(List<JobSourceAdapter> adapterList, GenericCareerAdapter fallbackAdapter) {
        // Specific adapters first, generic fallback last
        this.adapters = adapterList.stream()
                .filter(a -> !(a instanceof GenericCareerAdapter))
                .sorted(Comparator.comparing(JobSourceAdapter::getSourceIdentifier))
                .toList();
        this.fallbackAdapter = fallbackAdapter;
    }

    public JobSourceAdapter findAdapter(String url) {
        if (url == null || url.isBlank()) {
            return fallbackAdapter;
        }

        for (JobSourceAdapter adapter : adapters) {
            if (adapter.supports(url)) {
                return adapter;
            }
        }
        return fallbackAdapter;
    }
}
