package com.jobhunter.dto.profile;

import java.util.ArrayList;
import java.util.List;

public class ProfileReadinessReport {

    private ProfileReadinessState state;
    private int score;
    private boolean canDiscover;
    private String headline;
    private String message;
    private List<ReadinessItem> items = new ArrayList<>();
    private List<String> missingItems = new ArrayList<>();

    public ProfileReadinessReport() {}

    public ProfileReadinessReport(ProfileReadinessState state, int score, boolean canDiscover, String headline, String message) {
        this.state = state;
        this.score = score;
        this.canDiscover = canDiscover;
        this.headline = headline;
        this.message = message;
    }

    public ProfileReadinessState getState() { return state; }
    public void setState(ProfileReadinessState state) { this.state = state; }

    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }

    public boolean isCanDiscover() { return canDiscover; }
    public void setCanDiscover(boolean canDiscover) { this.canDiscover = canDiscover; }

    public String getHeadline() { return headline; }
    public void setHeadline(String headline) { this.headline = headline; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public List<ReadinessItem> getItems() { return items; }
    public void setItems(List<ReadinessItem> items) { this.items = items; }

    public List<String> getMissingItems() { return missingItems; }
    public void setMissingItems(List<String> missingItems) { this.missingItems = missingItems; }
}
