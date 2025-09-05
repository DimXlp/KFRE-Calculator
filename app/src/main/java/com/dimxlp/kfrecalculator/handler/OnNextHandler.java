package com.dimxlp.kfrecalculator.handler;

/** Implement in wizard fragments to validate & persist before advancing. */
public interface OnNextHandler {
    /** @return true if it's OK to proceed to the next step. */
    boolean onNext();
}
