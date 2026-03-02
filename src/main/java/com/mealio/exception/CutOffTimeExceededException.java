package com.mealio.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when a meal toggle is attempted after the mess cut-off time.
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class CutOffTimeExceededException extends RuntimeException {

    public CutOffTimeExceededException(String messName, String cutOffTime) {
        super("Meal toggle not allowed after cut-off time " + cutOffTime + " for mess: " + messName);
    }
}
