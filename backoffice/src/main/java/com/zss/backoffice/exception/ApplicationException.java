package com.zss.backoffice.exception;

public class ApplicationException extends RuntimeException {

    public ApplicationException(String message, Throwable e){
        super(message,e);
    }
    
}
