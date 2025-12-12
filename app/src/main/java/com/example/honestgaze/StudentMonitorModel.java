package com.example.honestgaze;

public class StudentMonitorModel {
    private String name;
    private int totalWarnings;
    private int eventsCount;
    private String status; // new field

    public StudentMonitorModel(String name, int totalWarnings, int eventsCount, String status) {
        this.name = name;
        this.totalWarnings = totalWarnings;
        this.eventsCount = eventsCount;
        this.status = status;
    }

    public String getName() { return name; }
    public int getTotalWarnings() { return totalWarnings; }
    public int getEventsCount() { return eventsCount; }
    public String getStatus() { return status; }
}
