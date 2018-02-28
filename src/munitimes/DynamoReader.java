package munitimes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.GetItemRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DynamoReader {


    private static final Logger log = LoggerFactory.getLogger(MuniTimesSpeechlet.class);

    public int getStopId(String route, String streetA, String streetB) throws NoSuchElementException {
        log.info("getting stop ID");
        AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard()
                .withRegion("us-east-1")
                .build();


        HashMap<String, AttributeValue> keyToGet = new HashMap<>();
        keyToGet.put("tag", new AttributeValue(route));

        GetItemRequest request = new GetItemRequest()
                .withKey(keyToGet)
                .withTableName("routes");


        Map<String, AttributeValue> returnedItem = client.getItem(request).getItem();

        log.info("queried dynamodb");

        List<AttributeValue> stops = returnedItem.get("stops").getL();

        Optional<AttributeValue> matchingStop = stops.stream().filter(stop ->
                stop.getM().get("title").getS().contains(streetA) &&
                        stop.getM().get("title").getS().contains(streetB))
                .findAny();

        AttributeValue stop = matchingStop.get();

        int stopId = Integer.parseInt(stop.getM().get("stopId").getS());

        log.info("Stop id: {}", stopId);

        return stopId;
    }

}
