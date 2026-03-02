package com.mealio.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class MonthAlreadyClosedException extends RuntimeException {
    public MonthAlreadyClosedException(String yearMonth) {
        super("Month " + yearMonth + " is already closed and cannot be modified.");
    }
}
