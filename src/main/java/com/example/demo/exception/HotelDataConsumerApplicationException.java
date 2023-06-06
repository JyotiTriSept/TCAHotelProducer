package com.example.demo.exception;

public class HotelDataConsumerApplicationException extends Exception{
	public HotelDataConsumerApplicationException(Throwable ex) {
		super(ex);
		//System.out.println("Error code is: "+status.name()+" "+status.value());
	}

}
