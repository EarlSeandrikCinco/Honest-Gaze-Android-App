package com.example.honestgaze;

public class Quiz {

    private String quizName;
    private String dateTime;
    private int gracePeriod;
    private int warningCooldown;
    private int numberOfWarnings;

    // Default constructor required for Firebase
    public Quiz() {
    }

    public Quiz(String quizName, String dateTime, int gracePeriod, int warningCooldown, int numberOfWarnings) {
        this.quizName = quizName;
        this.dateTime = dateTime;
        this.gracePeriod = gracePeriod;
        this.warningCooldown = warningCooldown;
        this.numberOfWarnings = numberOfWarnings;
    }

    // Public getters required for Firebase
    public String getQuizName() {
        return quizName;
    }

    public String getDateTime() {
        return dateTime;
    }

    public int getGracePeriod() {
        return gracePeriod;
    }

    public int getWarningCooldown() {
        return warningCooldown;
    }

    public int getNumberOfWarnings() {
        return numberOfWarnings;
    }

    // Optional: setters if you need to update fields later
    public void setQuizName(String quizName) {
        this.quizName = quizName;
    }

    public void setDateTime(String dateTime) {
        this.dateTime = dateTime;
    }

    public void setGracePeriod(int gracePeriod) {
        this.gracePeriod = gracePeriod;
    }

    public void setWarningCooldown(int warningCooldown) {
        this.warningCooldown = warningCooldown;
    }

    public void setNumberOfWarnings(int numberOfWarnings) {
        this.numberOfWarnings = numberOfWarnings;
    }
}
