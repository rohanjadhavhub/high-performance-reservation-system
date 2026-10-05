package com.bookmyshow.exception;

/**
 * Thrown when an illegal seat state transition is attempted.
 */
public class InvalidStateTransitionException extends RuntimeException {

    private final String fromState;
    private final String toState;

    public InvalidStateTransitionException(String fromState, String toState, String message) {
        super(String.format("Invalid transition from %s to %s: %s", fromState, toState, message));
        this.fromState = fromState;
        this.toState = toState;
    }

    public String getFromState() {
        return fromState;
    }

    public String getToState() {
        return toState;
    }
}
