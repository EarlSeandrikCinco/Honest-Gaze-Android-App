package com.example.honestgaze;

public class Quiz {
    private String quizName;
    private String dateTime;       // formatted date/time string
    private int gracePeriod;
    private int numberOfWarnings;  // matches Firebase key in CreateQuiz
    private String roomId;
    private boolean isActive = true;  // Default to true when created
    private Long startTime;  // Timestamp when quiz started
    private Long endTime;    // Timestamp when quiz ended
    private String date;     // Date string for display

    // Empty constructor required for Firebase
    public Quiz() {}

    public Quiz(String quizName, String dateTime, int gracePeriod, int numberOfWarnings, String roomId) {
        this.quizName = quizName;
        this.dateTime = dateTime;
        this.gracePeriod = gracePeriod;
        this.numberOfWarnings = numberOfWarnings;
        this.roomId = roomId;
        this.isActive = true;
    }

    // Getters
    public String getQuizName() { return quizName; }
    public String getDateTime() { return dateTime; }
    public int getGracePeriod() { return gracePeriod; }
    public int getNumberOfWarnings() { return numberOfWarnings; }
    public String getRoomId() { return roomId; }
    public boolean getIsActive() { return isActive; }
    public Long getStartTime() { return startTime; }
    public Long getEndTime() { return endTime; }
    public String getDate() { return date; }

    // Setters
    public void setQuizName(String quizName) { this.quizName = quizName; }
    public void setDateTime(String dateTime) { this.dateTime = dateTime; }
    public void setGracePeriod(int gracePeriod) { this.gracePeriod = gracePeriod; }
    public void setNumberOfWarnings(int numberOfWarnings) { this.numberOfWarnings = numberOfWarnings; }
    public void setRoomId(String roomId) { this.roomId = roomId; }
    public void setIsActive(boolean isActive) { this.isActive = isActive; }
    public void setStartTime(Long startTime) { this.startTime = startTime; }
    public void setEndTime(Long endTime) { this.endTime = endTime; }
    public void setDate(String date) { this.date = date; }
}
