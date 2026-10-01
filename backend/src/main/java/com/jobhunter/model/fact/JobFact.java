package com.jobhunter.model.fact;

import java.time.Instant;
import java.util.UUID;

/**
 * Encapsulates an external requirement or expectation extracted from a Job Description.
 * STRICT ARCHITECTURAL INVARIANT:
 * JobFact instances represent external, untrusted demand.
 * They CANNOT be converted, cast, or assigned into CandidateFact instances.
 * Job requirements never become candidate evidence.
 */
public record JobFact(
        UUID id,
        UUID jobId,
        FactCategory category,
        String requirementText,
        boolean mandatory,
        String sourceContext,
        Instant extractedAt
) {
    public JobFact {
        if (id == null) id = UUID.randomUUID();
        if (extractedAt == null) extractedAt = Instant.now();
    }
}
