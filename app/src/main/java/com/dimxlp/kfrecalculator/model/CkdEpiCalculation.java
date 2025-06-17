package com.dimxlp.kfrecalculator.model;

public class CkdEpiCalculation {

    private String ckdEpiCalculationId;
    private String patientId;
    private String userId;
    private double creatinine;
    private String sex;
    private int age;
    private double result;
    private long createdAt;
    private long updatedAt;
    private String notes;

    public CkdEpiCalculation() {
    }

    public CkdEpiCalculation(String ckdEpiCalculationId, String patientId, String userId, double creatinine, String sex, int age, double result, long createdAt, long updatedAt, String notes) {
        this.ckdEpiCalculationId = ckdEpiCalculationId;
        this.patientId = patientId;
        this.userId = userId;
        this.creatinine = creatinine;
        this.sex = sex;
        this.age = age;
        this.result = result;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.notes = notes;
    }

    public String getCkdEpiCalculationId() {
        return ckdEpiCalculationId;
    }

    public void setCkdEpiCalculationId(String ckdEpiCalculationId) {
        this.ckdEpiCalculationId = ckdEpiCalculationId;
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

    public double getCreatinine() {
        return creatinine;
    }

    public void setCreatinine(double creatinine) {
        this.creatinine = creatinine;
    }

    public String getSex() {
        return sex;
    }

    public void setSex(String sex) {
        this.sex = sex;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public double getResult() {
        return result;
    }

    public void setResult(double result) {
        this.result = result;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
