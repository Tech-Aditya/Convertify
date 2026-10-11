package com.convertify.model;

/** Direction of a date shift. */
public enum DateOperation {

    ADD(1),
    SUBTRACT(-1);

    private final int sign;

    DateOperation(int sign) {
        this.sign = sign;
    }

    public long apply(long amount) {
        return sign * amount;
    }
}
