package com.jobhunter.model.dto.government;

import com.jobhunter.model.entity.government.EligibilityStatus;
import java.util.ArrayList;
import java.util.List;

public class EligibilityEvaluationResultDto {
    private EligibilityStatus status = EligibilityStatus.UNKNOWN;
    private String summaryMessage;
    private List<EligibilityReasonDto> reasons = new ArrayList<>();
    private List<String> missingProfileFields = new ArrayList<>();

    public EligibilityEvaluationResultDto() {}

    public EligibilityEvaluationResultDto(EligibilityStatus status, String summaryMessage, List<EligibilityReasonDto> reasons, List<String> missingProfileFields) {
        this.status = status;
        this.summaryMessage = summaryMessage;
        this.reasons = reasons != null ? reasons : new ArrayList<>();
        this.missingProfileFields = missingProfileFields != null ? missingProfileFields : new ArrayList<>();
    }

    public EligibilityStatus getStatus() { return status; }
    public void setStatus(EligibilityStatus status) { this.status = status; }

    public String getSummaryMessage() { return summaryMessage; }
    public void setSummaryMessage(String summaryMessage) { this.summaryMessage = summaryMessage; }

    public List<EligibilityReasonDto> getReasons() { return reasons; }
    public void setReasons(List<EligibilityReasonDto> reasons) { this.reasons = reasons; }

    public List<String> getMissingProfileFields() { return missingProfileFields; }
    public void setMissingProfileFields(List<String> missingProfileFields) { this.missingProfileFields = missingProfileFields; }
}
