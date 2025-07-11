package com.dimxlp.kfrecalculator.model;

import com.dimxlp.kfrecalculator.enumeration.Risk;

import java.util.Date;
import java.util.Map;

public class Patient {
    private String patientId;
    private String userId;
    private String firstName;
    private String lastName;
    private String fullName;
    private String birthDate;
    private String gender;
    private boolean active;
    private Map<String, Disease> history;
    private String generalHistoryNote;
    private Date createdAt;
    private long lastUpdated;
    private double risk2Yr;
    private double risk5Yr;
    private Risk risk;
    private Date lastAssessment;
    private double egfrResult;

    public Patient() {}

    public Patient(String patientId, String userId, String firstName, String lastName, String birthDate, String gender, boolean active, Map<String, Disease> history, String generalHistoryNote, Date createdAt, long lastUpdated, double risk2Yr, double risk5Yr, Risk risk, Date lastAssessment, double egfrResult) {
        this.patientId = patientId;
        this.userId = userId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.fullName = firstName + " " + lastName;
        this.birthDate = birthDate;
        this.gender = gender;
        this.active = active;
        this.history = history;
        this.generalHistoryNote = generalHistoryNote;
        this.createdAt = createdAt;
        this.lastUpdated = lastUpdated;
        this.risk2Yr = risk2Yr;
        this.risk5Yr = risk5Yr;
        this.risk = risk;
        this.lastAssessment = lastAssessment;
        this.egfrResult = egfrResult;
    }

    public Patient(String fullName, String birthDate, long lastUpdated, Risk risk) {
        this.fullName = fullName;
        this.birthDate = birthDate;
        this.lastUpdated = lastUpdated;
        this.risk = risk;
    }

    public String getPatientId() {
        return patientId;
    }

    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(String birthDate) {
        this.birthDate = birthDate;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Map<String, Disease> getHistory() {
        return history;
    }

    public void setHistory(Map<String, Disease> history) {
        this.history = history;
    }

    public String getGeneralHistoryNote() {
        return generalHistoryNote;
    }

    public void setGeneralHistoryNote(String generalHistoryNote) {
        this.generalHistoryNote = generalHistoryNote;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public long getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(long lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public double getRisk2Yr() {
        return risk2Yr;
    }

    public void setRisk2Yr(double risk2Yr) {
        this.risk2Yr = risk2Yr;
    }

    public double getRisk5Yr() {
        return risk5Yr;
    }

    public void setRisk5Yr(double risk5Yr) {
        this.risk5Yr = risk5Yr;
    }

    public Risk getRisk() {
        return risk;
    }

    public void setRisk(Risk risk) {
        this.risk = risk;
    }

    public Date getLastAssessment() {
        return lastAssessment;
    }

    public void setLastAssessment(Date lastAssessment) {
        this.lastAssessment = lastAssessment;
    }

    public double getEgfrResult() {
        return egfrResult;
    }

    public void setEgfrResult(double egfrResult) {
        this.egfrResult = egfrResult;
    }
}
