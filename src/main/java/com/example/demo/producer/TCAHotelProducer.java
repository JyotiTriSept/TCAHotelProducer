package com.example.demo.producer;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.azure.messaging.eventhubs.EventData;
import com.azure.messaging.eventhubs.EventDataBatch;
import com.azure.messaging.eventhubs.EventHubClientBuilder;
import com.azure.messaging.eventhubs.EventHubProducerClient;

@Component
public class TCAHotelProducer {
private static final Logger LOGGER = LoggerFactory.getLogger(TCAHotelProducer.class);
	
	@Value("${tca.hotel.eventhubname}")
	private String EVENTHUBNAME;
	
	@Value("${tca.hotel.eventhubconnectionstring}")
	private String EVENTHUBCONNECTIONSTRING;
	
	public String publishEvents(List<EventData> eventDataList, UUID uuid) {
		
		LOGGER.info("Request id: "+uuid+" Publish Events method: START");
		
		// create a producer client
				EventHubProducerClient producer = new EventHubClientBuilder()
						.connectionString(EVENTHUBCONNECTIONSTRING, EVENTHUBNAME)
						.buildProducerClient();
				
				System.out.println(Thread.currentThread().getName()+" is sending event to EventHub: " + EVENTHUBNAME);
				// create a batch
		        EventDataBatch eventDataBatch = producer.createBatch();

		        for (EventData eventData : eventDataList) {
		            // try to add the event from the array to the batch
		            if (!eventDataBatch.tryAdd(eventData)) {
		                // if the batch is full, send it and then create a new batch
		                producer.send(eventDataBatch);
		                eventDataBatch = producer.createBatch();

		                // Try to add that event that couldn't fit before.
		                if (!eventDataBatch.tryAdd(eventData)) {
		                    throw new IllegalArgumentException("Event is too large for an empty batch. Max size: "
		                        + eventDataBatch.getMaxSizeInBytes());
		                }
		            }
		        }
		        // send the last batch of remaining events
		        if (eventDataBatch.getCount() > 0) {
		            producer.send(eventDataBatch);
		        }
		        producer.close();
		        LOGGER.info("Request id: "+uuid+" Publish Events method: END");
				return "Data sent to Event Hub by "+Thread.currentThread().getName();
	}
}
