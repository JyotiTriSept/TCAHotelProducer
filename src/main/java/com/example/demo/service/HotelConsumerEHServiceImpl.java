package com.example.demo.service;

import java.time.Duration;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.example.demo.exception.HotelDataConsumerApplicationException;
import com.example.demo.exception.StoreExceptionToBlob;

import reactor.util.retry.Retry;

@Component
public class HotelConsumerEHServiceImpl {
	private static final Logger LOGGER = LoggerFactory.getLogger(HotelConsumerEHServiceImpl.class);
	
	@Value("${tca.hotel.consumerapp.baseurl}")
	private String hotelConsumerBaseUrl;
	
	@Value("${tca.hotel.consumerapp.endpoint}")
	private String hotelConsumerEndpoint;
	
	public String consumeHotelData(UUID uuid) throws Exception{
		
		LOGGER.info("Request id: "+uuid+" Consume Events Application calling: START");
		
		long before = System.currentTimeMillis();
		 //WebClient webConsumerClient = WebClient.builder().baseUrl("https://ehub-sb-poc-ehhotelconsumer.azuremicroservices.io").build();
		 WebClient webConsumerClient = WebClient.builder().baseUrl(hotelConsumerBaseUrl).build();
		 try {
			 
			 ResponseEntity<String> response = webConsumerClient.get()
					 .uri(uriBuilder -> uriBuilder.path(hotelConsumerEndpoint)
							 .queryParam("totalEvent", TCAHotelServiceImplementation.list.size())
							 .queryParam("uuid", uuid)
							 .build())
					 .accept()
					 .retrieve()
					 .toEntity(String.class)
					 //.timeout(Duration.ofSeconds(5))
					 .retryWhen(Retry.backoff(3, Duration.ofSeconds(5))
							 .doAfterRetry(retrySignal->{ System.out.println("Retried to hotel consumer application: " +
									 retrySignal.totalRetries()); }))
					 .block();
			 String res = response.getBody();
			 long after = System.currentTimeMillis(); 
			 String totalTimeTaken = "Total time taken at consumer application: " + (after - before) / 1000.0 + " seconds.\n";
			 LOGGER.info("Request id: "+uuid+" Consume Events Application calling: END");
			 return res+totalTimeTaken;
			 
		 } catch(Exception e) {
				if(e.getCause() instanceof WebClientResponseException) {
					
					WebClientResponseException cause =(WebClientResponseException) e.getCause();
					LOGGER.error("Request id: "+uuid+" : "+cause.getResponseBodyAsString());
					LOGGER.error("Request id: "+uuid+" : "+cause.getMostSpecificCause());
					LOGGER.error("Request id: "+uuid+" : "+cause.getMessage());
					String errorMessage = "Error Message: " + cause.getMessage() + "Specific Cause: "
							+ cause.getMostSpecificCause() + "Response Body: " + cause.getResponseBodyAsString();
					storeExceptionIntoBlob(errorMessage.strip(), uuid);
					long after = System.currentTimeMillis(); 
					String totalTimeTaken = "Total time taken at consumer application: " + (after - before) / 1000.0 + " seconds.\n";
					LOGGER.error("Request id: "+uuid+" : "+totalTimeTaken);
					throw new HotelDataConsumerApplicationException(cause);
					//return totalTimeTaken;
				} else if(e.getCause() instanceof WebClientRequestException) {
					WebClientRequestException cause =(WebClientRequestException) e.getCause();
					LOGGER.error("Request id: "+uuid+" : "+cause.getRootCause());
					LOGGER.error("Request id: "+uuid+" : "+cause.getMostSpecificCause());
					LOGGER.error("Request id: "+uuid+" : "+cause.getMessage());
					String errorMessage = "Error Message: " + cause.getMessage() + "Specific Cause: "
							+ cause.getMostSpecificCause() + "Root Cause: " + cause.getRootCause();
					storeExceptionIntoBlob(errorMessage.strip(), uuid);
					long after = System.currentTimeMillis(); 
					String totalTimeTaken = "Total time taken at consumer application: " + (after - before) / 1000.0 + " seconds.\n";
					LOGGER.error("Request id: "+uuid+" : "+totalTimeTaken);
					throw new HotelDataConsumerApplicationException(cause);
					//return totalTimeTaken;
					
				} else {
					
					LOGGER.error("Request id: "+uuid+" : "+e.getLocalizedMessage());
					LOGGER.error("Request id: "+uuid+" : "+e.getMessage());
					LOGGER.error("Request id: "+uuid+" : "+e.getCause());
					String errorMessage = "Error Message: " + e.getMessage() + "Cause: " + e.getCause() + "Message: "
							+ e.getLocalizedMessage();
					storeExceptionIntoBlob(errorMessage.strip(), uuid);
					long after = System.currentTimeMillis(); 
					String totalTimeTaken = "Total time taken at consumer application: " + (after - before) / 1000.0 + " seconds.\n";
					LOGGER.error("Request id: "+uuid+" : "+totalTimeTaken);
					throw new HotelDataConsumerApplicationException(e);
					//return totalTimeTaken;
				}
				
			}
		 
	}

	
	public String storeExceptionIntoBlob(String e, UUID uuid) {
		StoreExceptionToBlob.updateToLatestFolder(e.toString());
		StoreExceptionToBlob.storingExceptionInArchiveLocation(e.toString(), "Hotels", uuid);
		return null;
	}
}
