package com.example.demo.exception;

public class InvalidTCAResponse extends Exception{
	private String message;
	 
    public InvalidTCAResponse() {}
 
    public InvalidTCAResponse(String msg)
    {
        super(msg);
        this.message = msg;
    }
}
