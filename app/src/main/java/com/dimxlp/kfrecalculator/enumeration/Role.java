package com.dimxlp.kfrecalculator.enumeration;

public enum Role {
    DOCTOR, INDIVIDUAL;

    public static Role fromString(String value) {
        if (value == null) return null;
        switch (value.toLowerCase()) {
            case "doctor": return DOCTOR;
            case "individual": return INDIVIDUAL;
            default: return null;
        }
    }

    @Override
    public String toString() {
        return switch (this) {
            case DOCTOR -> "Doctor";
            case INDIVIDUAL -> "Individual";
            default -> super.toString();
        };
    }
}
