package com.example.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class TcaHotelProducerAppApplication {

	private static final Logger LOGGER = LoggerFactory.getLogger(TcaHotelProducerAppApplication.class);
			
	public static void main(String[] args) {
		LOGGER.info("Before Starting application name: "+TcaHotelProducerAppApplication.class.getName());
		SpringApplication.run(TcaHotelProducerAppApplication.class, args);
		LOGGER.debug("Starting TcaHotelProducerAppApplication in debug with {} args", args.length);
		LOGGER.info("Starting TcaHotelProducerAppApplication with {} args.", args.length); 
	}

}
