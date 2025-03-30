package com.dimxlp.kfrecalculator.enumeration;

public enum Risk {
    HIGH("High"),
    MEDIUM("Medium"),
    LOW("Low");

    String risk;

    Risk(String risk) {
        this.risk = risk;
    }

    public String getRisk() {
        return risk;
    }

    public void setRisk(String risk) {
        this.risk = risk;
    }
}
