package com.example.demo.controller;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.producer.TCAHotelProducer;
import com.example.demo.service.HotelConsumerEHServiceImpl;
import com.example.demo.service.TCAHotelServiceImplementation;
import com.example.demo.service.TCALoginServiceImplementation;

@RestController
public class ProducerController {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(ProducerController.class);
			
	@Autowired
	TCALoginServiceImplementation loginService;
	
	
	@Autowired
	TCAHotelServiceImplementation hotelService;
	
	@Autowired
	TCAHotelProducer producer;
	
	@Autowired
	HotelConsumerEHServiceImpl tcaHotelConsumer;
	
	
	@GetMapping("/tca/hotel/publish")
	public String publishHotelDataToEh() throws Exception {
		UUID uuid = UUID. randomUUID();
		
		long before = System.currentTimeMillis();
		LOGGER.info("Request id: "+uuid+" Fetching the bearer token from TCA");
		
		String loginToken = loginService.getLoginToken(uuid);
		LOGGER.info("Request id: "+uuid+" Fetching Hotel Data");
		String hotelData = hotelService.getHotelData(uuid);
		LOGGER.info("Request id: "+uuid+" "+hotelData);
		LOGGER.info("Request id: "+uuid+" Sending data to event hub");
		
		String publishEvents = producer.publishEvents(TCAHotelServiceImplementation.list, uuid);
		LOGGER.info("Request id: "+uuid+" "+publishEvents);
		
		System.out.println(tcaHotelConsumer.consumeHotelData(uuid));

		long after = System.currentTimeMillis();
		String totalTimeTaken = "Time it took for all Hotel Data to be stored in RAW layer: "
				+ (after - before) / 1000.0 + " seconds.\n";
		System.out.println(totalTimeTaken);
		return totalTimeTaken;
		
		
	}

}
