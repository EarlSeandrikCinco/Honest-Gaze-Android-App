package com.example.honestgaze;

public class Quiz {
    private String quizName;
    private String dateTime;       // formatted date/time string
    private int gracePeriod;
    private int numberOfWarnings;  // matches Firebase key in CreateQuiz
    private String roomId;

    // Empty constructor required for Firebase
    public Quiz() {}

    public Quiz(String quizName, String dateTime, int gracePeriod, int numberOfWarnings, String roomId) {
        this.quizName = quizName;
        this.dateTime = dateTime;
        this.gracePeriod = gracePeriod;
        this.numberOfWarnings = numberOfWarnings;
        this.roomId = roomId;
    }

    // Getters
    public String getQuizName() { return quizName; }
    public String getDateTime() { return dateTime; }
    public int getGracePeriod() { return gracePeriod; }
    public int getNumberOfWarnings() { return numberOfWarnings; }
    public String getRoomId() { return roomId; }

    // Setters
    public void setQuizName(String quizName) { this.quizName = quizName; }
    public void setDateTime(String dateTime) { this.dateTime = dateTime; }
    public void setGracePeriod(int gracePeriod) { this.gracePeriod = gracePeriod; }
    public void setNumberOfWarnings(int numberOfWarnings) { this.numberOfWarnings = numberOfWarnings; }
    public void setRoomId(String roomId) { this.roomId = roomId; }
}
