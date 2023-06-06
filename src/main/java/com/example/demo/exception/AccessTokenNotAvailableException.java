package com.example.demo.exception;

public class AccessTokenNotAvailableException extends RuntimeException{
	/**
	 * 
	 */
	private static final long serialVersionUID = 4764556866563569851L;
	private String message;
	 
    public AccessTokenNotAvailableException() {}
 
    public AccessTokenNotAvailableException(String msg)
    {
        super(msg);
        this.message = msg;
    }
}
