package com.example.demo.service;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.example.demo.exception.AccessTokenNotAvailableException;
import com.example.demo.exception.InvalidTCAResponse;
import com.example.demo.exception.StoreExceptionToBlob;
import com.example.demo.exception.TCALoginApiException;
import com.example.demo.model.LoginModel;

import reactor.util.retry.Retry;

@Component
public class TCALoginServiceImplementation {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(TCALoginServiceImplementation.class);
	
	@Value("${login.username}")
	private String userName;
	
	@Value("${login.password}")
	private String password;
	
	@Value("${tca.baseurl}")
	private String tcaBaseUrl;
	
	@Value("${tca.loginendpoint}")
	private String tcaLoginEndpoint;
	
	private static final String ACCESSTOKEN="access_token";
	private static final String LOGINMODULENAME="Login";
	private static final String RETRY=" Retried TCA Login API: ";
	private static final String TOTALTIMETAKEN=" Total time taken for successfully fetching access token from TCA: ";
	private static final String ACCESSTOKENNOTPRESENTMESSAGE=" Accces Token not present in TCA login response.";
	private static final String EMPTYRESPONSEMESSAGE=" Empty response recieved from TCA get Hotel Api.";
	

	public static String loginAccessToken = null;

	public String getLoginToken(UUID uuid) throws Exception {
		LOGGER.info("Request id: "+uuid+"Inside getLoginToken method");
		LoginModel loginReq = new LoginModel(userName, password);

		WebClient webConsumerClient = WebClient.builder().baseUrl(tcaBaseUrl).build();
		long before = System.currentTimeMillis();
		try {

			ResponseEntity<Object> responseEntity = webConsumerClient.post()
					                                .uri(uriBuilder -> uriBuilder.path(tcaLoginEndpoint).build())
					                                .contentType(MediaType.APPLICATION_JSON)
					                                .bodyValue(loginReq)
					                                .accept()
					                                .retrieve()
					                                .toEntity(Object.class)
					                                .retryWhen(Retry.backoff(3, Duration.ofSeconds(5)).doAfterRetry(retrySignal -> {
						                                System.out.println(RETRY + retrySignal.totalRetries());
					                                  }))
					                                .block();
			if(responseEntity.getBody() != null) {
				
				LinkedHashMap<String, Object> responseBody = (LinkedHashMap<String, Object>) responseEntity.getBody();
				
				if (responseBody.get(ACCESSTOKEN) != null) {
					
					loginAccessToken = (String) responseBody.get(ACCESSTOKEN);
					long after = System.currentTimeMillis();
					String totalTimeTaken = TOTALTIMETAKEN
							+ (after - before) / 1000.0 + " seconds.\n";
					return totalTimeTaken;
				} else {
					storeExceptionIntoBlob(ACCESSTOKENNOTPRESENTMESSAGE,uuid);
					throw new AccessTokenNotAvailableException(ACCESSTOKENNOTPRESENTMESSAGE);
				}
			} else {
				//String issue  = "Empty response from TCA get Hotel Api.";
				System.out.println(EMPTYRESPONSEMESSAGE);
				storeExceptionIntoBlob(EMPTYRESPONSEMESSAGE,uuid);
				throw new InvalidTCAResponse(EMPTYRESPONSEMESSAGE);
			}

		} catch (Exception e) {
			if (e.getCause() instanceof WebClientResponseException) {

				WebClientResponseException cause = (WebClientResponseException) e.getCause();
				LOGGER.error("Request id: "+uuid+" : "+cause.getResponseBodyAsString());
				LOGGER.error("Request id: "+uuid+" : "+cause.getMostSpecificCause().toString());
				LOGGER.error("Request id: "+uuid+" : "+cause.getMessage());
				
				String errorMessage = "Error Message: " + cause.getMessage() + "Specific Cause: "
						+ cause.getMostSpecificCause() + "Response Body: " + cause.getResponseBodyAsString();
				storeExceptionIntoBlob(errorMessage.strip(),uuid);
				long after = System.currentTimeMillis();
				String totalTimeTaken = TOTALTIMETAKEN + (after - before) / 1000.0 + " seconds.\n";
				
				LOGGER.error("Request id: "+uuid+totalTimeTaken);
				throw new TCALoginApiException(cause);
				//return totalTimeTaken;
			} else if (e.getCause() instanceof WebClientRequestException) {
				WebClientRequestException cause = (WebClientRequestException) e.getCause();
				LOGGER.error("Request id: "+uuid+" : "+cause.getRootCause().toString());
				LOGGER.error("Request id: "+uuid+" : "+cause.getMostSpecificCause().toString());
				LOGGER.error("Request id: "+uuid+" : "+cause.getMessage());
				String errorMessage = "Error Message: " + cause.getMessage() + "Specific Cause: "
						+ cause.getMostSpecificCause() + "Root Cause: " + cause.getRootCause();
				storeExceptionIntoBlob(errorMessage.strip(),uuid);
				long after = System.currentTimeMillis();
				String totalTimeTaken = TOTALTIMETAKEN + (after - before) / 1000.0 + " seconds.\n";
				LOGGER.error("Request id: "+uuid+totalTimeTaken);
				throw new TCALoginApiException(cause);
				//return totalTimeTaken;

			} else {

				LOGGER.error("Request id: "+uuid+" : "+e.getLocalizedMessage());
				LOGGER.error("Request id: "+uuid+" : "+e.getMessage());
				LOGGER.error("Request id: "+uuid+" : "+e.getCause().toString());
				String errorMessage = "Error Message: " + e.getMessage() + "Cause: " + e.getCause() + "Message: "
						+ e.getLocalizedMessage();
				storeExceptionIntoBlob(errorMessage.strip(),uuid);
				long after = System.currentTimeMillis();
				String totalTimeTaken = TOTALTIMETAKEN + (after - before) / 1000.0 + " seconds.\n";
				LOGGER.error("Request id: "+uuid+totalTimeTaken);
				//return totalTimeTaken;
				throw new TCALoginApiException(e);
			}

		}

	}

	public String storeExceptionIntoBlob(String e, UUID uuid) {
		StoreExceptionToBlob.updateToLatestFolder(e.toString());
		StoreExceptionToBlob.storingExceptionInArchiveLocation(e.toString(), LOGINMODULENAME, uuid);
		return null;
	}



}
