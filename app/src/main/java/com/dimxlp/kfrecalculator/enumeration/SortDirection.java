package com.dimxlp.kfrecalculator.enumeration;

public enum SortDirection {

    NONE("None"),
    ASCENDING("Ascending"),
    DESCENDING("Descending");

    String sortDirection;

    SortDirection(String sortDirection) {
        this.sortDirection = sortDirection;
    }

    public String getSortDirection() {
        return sortDirection;
    }

    public void setSortDirection(String sortDirection) {
        this.sortDirection = sortDirection;
    }
}
