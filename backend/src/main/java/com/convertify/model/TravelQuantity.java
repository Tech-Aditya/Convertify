package com.convertify.model;

/** The quantity the time/distance/speed calculator solves for, with the formula it applies. */
public enum TravelQuantity {

    DISTANCE("Distance = Speed × Time"),
    SPEED("Speed = Distance ÷ Time"),
    TIME("Time = Distance ÷ Speed");

    private final String formula;

    TravelQuantity(String formula) {
        this.formula = formula;
    }

    public String formula() {
        return formula;
    }
}
