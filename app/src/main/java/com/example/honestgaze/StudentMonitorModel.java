package com.example.honestgaze;

public class StudentMonitorModel {
    private String name;
    private int totalWarnings;
    private int eventsCount;

    public StudentMonitorModel() {}

    public StudentMonitorModel(String name, int totalWarnings, int eventsCount) {
        this.name = name;
        this.totalWarnings = totalWarnings;
        this.eventsCount = eventsCount;
    }

    public String getName() { return name; }
    public int getTotalWarnings() { return totalWarnings; }
    public int getEventsCount() { return eventsCount; }

    public void setName(String name) { this.name = name; }
    public void setTotalWarnings(int totalWarnings) { this.totalWarnings = totalWarnings; }
    public void setEventsCount(int eventsCount) { this.eventsCount = eventsCount; }
}
