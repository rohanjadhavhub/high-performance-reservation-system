package com.bookmyshow.domain.statemachine;

/**
 * State Pattern interface for the Seat State Machine.
 * Each seat state implements this interface to enforce valid transitions.
 *
 * @see com.bookmyshow.domain.statemachine.SeatStatus
 */
public interface SeatState {

    /**
     * Attempt to transition this seat to HELD state.
     * @throws com.bookmyshow.exception.InvalidStateTransitionException if transition is illegal
     */
    SeatStatus hold();

    /**
     * Attempt to transition this seat to BOOKED state.
     * @throws com.bookmyshow.exception.InvalidStateTransitionException if transition is illegal
     */
    SeatStatus book();

    /**
     * Release this seat back to AVAILABLE state.
     * @throws com.bookmyshow.exception.InvalidStateTransitionException if transition is illegal
     */
    SeatStatus release();

    /**
     * @return the current status
     */
    SeatStatus getStatus();
}
