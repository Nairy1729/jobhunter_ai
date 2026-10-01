package com.jobhunter.model.tailoring;

public enum ResumeTailoringStatus {
    DRAFT,
    GENERATED,
    VALIDATING,
    VALIDATED,
    PDF_GENERATED,
    READY_FOR_DOWNLOAD,
    VALIDATION_FAILED,
    PDF_GENERATION_FAILED;

    public boolean isDownloadable() {
        return this == READY_FOR_DOWNLOAD;
    }
}
