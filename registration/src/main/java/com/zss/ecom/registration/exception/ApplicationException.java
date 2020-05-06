package com.zss.ecom.registration.exception;

public class ApplicationException extends RuntimeException {

    public ApplicationException(String message, Throwable e){
        super(message,e);
    }
}
