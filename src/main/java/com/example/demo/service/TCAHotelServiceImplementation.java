package com.example.demo.service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.azure.messaging.eventhubs.EventData;
import com.example.demo.exception.InvalidTCAResponse;
import com.example.demo.exception.StoreExceptionToBlob;
import com.example.demo.exception.TCALoginApiException;

import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

@Component
public class TCAHotelServiceImplementation {
	private static final Logger LOGGER = LoggerFactory.getLogger(TCAHotelServiceImplementation.class);
	
	@Value("${tca.baseurl}")
	private String tcaBaseUrl;
	
	@Value("${tca.gethotelendpoint}")
	private String tcaHotelEndpoint;
	
	@Value("${hotels.groupcode}")
	private String hotelGroupCode;

    private static final String GROUPCODE="GroupCode";
    private static final String RETRY="Retried TCA Get Hotel Api call: ";
    private static final String AUTHHEADERNAME="Authorization";
    private static final String BEARER="Bearer ";
    private static final String EMPTYRESPONSE="Empty response from TCA get Hotel Api call";
	
	public static List<EventData> list = new ArrayList<EventData>();

	public String getHotelData(UUID uuid) throws InvalidTCAResponse, WebClientResponseException, Exception {
		
		LOGGER.info("Request id: "+uuid+" Get Hotel data method: START");
		
		WebClient webClient = WebClient.builder().baseUrl(tcaBaseUrl).build();
		
		long before = System.currentTimeMillis();
		
		try {
			
			ResponseEntity<String> responseEntity = webClient.get()
					.uri(uriBuilder -> uriBuilder.path(tcaHotelEndpoint)
							.queryParam(GROUPCODE, hotelGroupCode)
							.build())
					.header(AUTHHEADERNAME, BEARER + TCALoginServiceImplementation.loginAccessToken)
					.accept(MediaType.APPLICATION_JSON, MediaType.ALL)
					.retrieve()
					.onStatus(status -> (status.value() == 204), clientResponse -> Mono.empty())
					.toEntity(String.class)
					.retryWhen(Retry.backoff(3, Duration.ofSeconds(5)).doAfterRetry(retrySignal -> {
						System.out.println(RETRY + retrySignal.totalRetries());
					}))
					.block();

			JSONParser jparser = new JSONParser();

			if (responseEntity.getBody() != null) {

				JSONArray jrray = (JSONArray) jparser.parse(responseEntity.getBody());
				for (int i = 0; i < jrray.size(); i++) {
					JSONObject object = (JSONObject) jrray.get(i);
					String jsonString = object.toJSONString();

					list.add(new EventData(jsonString));
				}
			} else {
				//String issue  = "Empty response from TCA get Hotel Api call";
				LOGGER.info("Request id: "+uuid+" "+EMPTYRESPONSE);
				storeExceptionIntoBlob(EMPTYRESPONSE, uuid);
				throw new InvalidTCAResponse(EMPTYRESPONSE);
			}
		} catch (ParseException cause) {
			String issue = "Invalid JSON response from TCA Hotel Api call";
			LOGGER.error("Request id: "+uuid+" : "+issue);
			String errorMessage = "Issue: "+issue+"Error Message: " + cause.getMessage() + "Specific Cause: "
					+ cause.getCause();
			storeExceptionIntoBlob(errorMessage.strip(), uuid);
			long after = System.currentTimeMillis();
			String totalTimeTaken = "Total time taken after error occured while processing hotel data response from TCA: "
					+ (after - before) / 1000.0 + " seconds.\n";
			return totalTimeTaken;
			
		}catch (Exception e) {
			if (e.getCause() instanceof WebClientResponseException) {

				WebClientResponseException cause = (WebClientResponseException) e.getCause();
				LOGGER.error("Request id: "+uuid+" : "+cause.getResponseBodyAsString());
				LOGGER.error("Request id: "+uuid+" : "+cause.getMostSpecificCause());
				LOGGER.error("Request id: "+uuid+" : "+cause.getMessage());
				String errorMessage = "Error Message: " + cause.getMessage() + "Specific Cause: "
						+ cause.getMostSpecificCause() + "Response Body: " + cause.getResponseBodyAsString();
				storeExceptionIntoBlob(errorMessage.strip(), uuid);
				long after = System.currentTimeMillis();
				String totalTimeTaken = "Total time taken after error occured while fetching hotels from TCA: "
						+ (after - before) / 1000.0 + " seconds.\n";
				LOGGER.error("Request id: "+uuid+" : "+totalTimeTaken);    
				throw new TCALoginApiException(cause);
				//return totalTimeTaken;
			} else if (e.getCause() instanceof WebClientRequestException) {
				WebClientRequestException cause = (WebClientRequestException) e.getCause();
				LOGGER.error("Request id: "+uuid+" : "+cause.getRootCause());
				LOGGER.error("Request id: "+uuid+" : "+cause.getMostSpecificCause());
				LOGGER.error("Request id: "+uuid+" : "+cause.getMessage());
				String errorMessage = "Error Message: " + cause.getMessage() + "Specific Cause: "
						+ cause.getMostSpecificCause() + "Root Cause: " + cause.getRootCause();
				storeExceptionIntoBlob(errorMessage.strip(), uuid);
				long after = System.currentTimeMillis();
				String totalTimeTaken = "Total time taken after error occured while fetching hotels from TCA: "
						+ (after - before) / 1000.0 + " seconds.\n";
				LOGGER.error("Request id: "+uuid+" : "+totalTimeTaken);
				throw new TCALoginApiException(cause);
				//return totalTimeTaken;

			} else {

				LOGGER.error("Request id: "+uuid+" : "+e.getLocalizedMessage());
				LOGGER.error("Request id: "+uuid+" : "+e.getMessage());
				LOGGER.error("Request id: "+uuid+" : "+e.getCause());
				String errorMessage = "Error Message: " + e.getMessage() + "Cause: " + e.getCause() + "Message: "
						+ e.getLocalizedMessage();
				storeExceptionIntoBlob(errorMessage.strip(), uuid);
				long after = System.currentTimeMillis();
				String totalTimeTaken = "Total time taken after error occured while fetching hotels from TCA: "
						+ (after - before) / 1000.0 + " seconds.\n";
				LOGGER.error("Request id: "+uuid+" : "+totalTimeTaken);
				throw new TCALoginApiException(e);
				//return totalTimeTaken;
			}

		}
		long after = System.currentTimeMillis();
		LOGGER.info("Request id: "+uuid+" Get Hotel data method: END");
		return "Hotel data fetched in: "+ (after - before) / 1000.0 + " seconds.\n";
	}

	public String storeExceptionIntoBlob(String e,UUID uuid) {
		StoreExceptionToBlob.updateToLatestFolder(e.toString());
		StoreExceptionToBlob.storingExceptionInArchiveLocation(e.toString(), "Hotels", uuid);
		return null;
	}

}
