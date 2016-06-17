package com.rei.trip.exception;

/**
 * This class represents an exception in the Trip Data Builder,
 *
 * @author sahan
 */
public class DataBuilderException extends Exception {
    /**
     * Constructs a DataBuilderException with the given detail message.
     *
     * @param message The detail message of the DataBuilderException.
     */
    public DataBuilderException(String message) {
        super(message);
    }

    /**
     * Constructs a DataBuilderException with the given root cause.
     *
     * @param cause The root cause of the DataBuilderException.
     */
    public DataBuilderException(Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a DataBuilderException with the given detail message and root cause.
     *
     * @param message The detail message of the DataBuilderException.
     * @param cause The root cause of the DataBuilderException.
     */
    public DataBuilderException(String message, Throwable cause) {
        super(message, cause);
    }
}
