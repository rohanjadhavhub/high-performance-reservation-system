package com.bookmyshow.domain.statemachine;

import com.bookmyshow.exception.InvalidStateTransitionException;

/**
 * Seat State Machine implemented as a Java enum with the State Pattern.
 *
 * Valid transitions:
 *   AVAILABLE -> HELD    (via hold)
 *   HELD     -> BOOKED   (via book)
 *   HELD     -> AVAILABLE (via release)
 *   BOOKED is terminal - no further transitions allowed.
 */
public enum SeatStatus implements SeatState {

    AVAILABLE {
        @Override
        public SeatStatus hold() {
            return HELD;
        }

        @Override
        public SeatStatus book() {
            throw new InvalidStateTransitionException("AVAILABLE", "BOOKED",
                "Cannot book a seat directly. Must hold first.");
        }

        @Override
        public SeatStatus release() {
            // Already available, no-op
            return AVAILABLE;
        }

        @Override
        public SeatStatus getStatus() {
            return AVAILABLE;
        }
    },

    HELD {
        @Override
        public SeatStatus hold() {
            throw new InvalidStateTransitionException("HELD", "HELD",
                "Seat is already held.");
        }

        @Override
        public SeatStatus book() {
            return BOOKED;
        }

        @Override
        public SeatStatus release() {
            return AVAILABLE;
        }

        @Override
        public SeatStatus getStatus() {
            return HELD;
        }
    },

    BOOKED {
        @Override
        public SeatStatus hold() {
            throw new InvalidStateTransitionException("BOOKED", "HELD",
                "Cannot hold a booked seat.");
        }

        @Override
        public SeatStatus book() {
            throw new InvalidStateTransitionException("BOOKED", "BOOKED",
                "Seat is already booked.");
        }

        @Override
        public SeatStatus release() {
            throw new InvalidStateTransitionException("BOOKED", "AVAILABLE",
                "Cannot release a booked seat.");
        }

        @Override
        public SeatStatus getStatus() {
            return BOOKED;
        }
    };

    /**
     * Factory method to parse status from string (e.g. Redis value).
     */
    public static SeatStatus fromString(String value) {
        if (value == null) {
            return AVAILABLE;
        }
        try {
            return SeatStatus.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return AVAILABLE;
        }
    }
}
