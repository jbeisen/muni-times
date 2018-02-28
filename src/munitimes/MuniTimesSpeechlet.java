package munitimes;

import com.amazon.speech.json.SpeechletRequestEnvelope;
import com.amazon.speech.slu.Intent;
import com.amazon.speech.slu.Slot;
import com.amazon.speech.speechlet.*;
import com.amazon.speech.ui.OutputSpeech;
import com.amazon.speech.ui.PlainTextOutputSpeech;
import com.amazon.speech.ui.Reprompt;
import com.amazon.speech.ui.SimpleCard;
import com.amazon.speech.ui.SsmlOutputSpeech;
import org.apache.log4j.BasicConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.NoSuchElementException;
import java.util.Properties;

public class MuniTimesSpeechlet implements SpeechletV2 {

    private static final Logger log = LoggerFactory.getLogger(MuniTimesSpeechlet.class);
    private static final String URL_FORMAT = "http://webservices.nextbus" +
            ".com/service/publicXMLFeed?command=predictions&a=sf-muni&stopId=%d";

    private Properties properties;

    public MuniTimesSpeechlet() {
        BasicConfigurator.configure();
        log.info("Starting app!");
        loadProperties();
    }

    private void loadProperties() {
        properties = new Properties();
        try {
            properties.load(new FileInputStream("munitimes.properties"));
        } catch (IOException e) {
            log.error("Error loading properties file", e);
        }
    }

    @Override
    public void onSessionStarted(SpeechletRequestEnvelope<SessionStartedRequest> requestEnvelope) {
        log.info("onSessionStarted requestId={}, sessionId={}", requestEnvelope.getRequest().getRequestId(),
                requestEnvelope.getSession().getSessionId());
    }

    @Override
    public SpeechletResponse onLaunch(SpeechletRequestEnvelope<LaunchRequest> requestEnvelope) {
        log.info("onLaunch requestId={}, sessionId={}", requestEnvelope.getRequest().getRequestId(),
                requestEnvelope.getSession().getSessionId());
        return getWelcomeResponse();
    }

    @Override
    public SpeechletResponse onIntent(SpeechletRequestEnvelope<IntentRequest> requestEnvelope) {
        IntentRequest request = requestEnvelope.getRequest();
        Session session = requestEnvelope.getSession();
        log.info("onIntent requestId={}, sessionId={}", request.getRequestId(),
                requestEnvelope.getSession().getSessionId());

        Intent intent = request.getIntent();
        String intentName = (intent != null) ? intent.getName() : null;

        if ("BusTime".equals(intentName)) {
            log.info("Received intent: BusTime");
            return getBusTimeResponse();
        }

        if ("SetFavoriteRoute".equals(intentName)) {
            log.info("Received intent: SetFavoriteRoute");
            return getSetFavoriteRouteResponse(intent, session);
        }


        if ("BusTimeForStop".equals(intentName)) {
            log.info("Received intent: BusTimeForStop");
            return getBusTimeForStopResponse(intent, session);
        }

        return getAskResponse("MUNI Times", "This is unsupported.  Please try something else.");
    }


    @Override
    public void onSessionEnded(SpeechletRequestEnvelope<SessionEndedRequest> requestEnvelope) {
        log.info("onSessionEnded requestId={}, sessionId={}", requestEnvelope.getRequest().getRequestId(),
                requestEnvelope.getSession().getSessionId());
    }

    private SpeechletResponse getWelcomeResponse() {
        String speechText = "Welcome to the MUNI times skill.";
        return getAskResponse("MUNI Times", speechText);
    }

    private SpeechletResponse getBusTimeResponse() {
        int stopId = Integer.parseInt(properties.getProperty("stopId.default"));
        String speechText;
        try {
            speechText = "The bus is arriving in " + getTimePhrase(1, stopId);
            speechText = speechText + "The next bus is in " + getTimePhrase(3, stopId);
        } catch (Exception e) {
            speechText = "Sorry, I had a problem getting the bus time.";
        }
        SimpleCard card = getSimpleCard("Bus time", speechText);
        SsmlOutputSpeech speech = getSSMLOutputSpeech(speechText);
        return SpeechletResponse.newTellResponse(speech, card);
    }

    private SpeechletResponse getSetFavoriteRouteResponse(Intent intent, Session session) {
        Slot routeSlot = intent.getSlot("route");
        String routeName = routeSlot.getValue();
        PlainTextOutputSpeech speech = getPlainTextOutputSpeech("Your favorite route is " + routeName);
        return SpeechletResponse.newTellResponse(speech);
    }

    private SpeechletResponse getBusTimeForStopResponse(Intent intent, Session session) {
        String route = intent.getSlot("route").getValue();
        String streetA = intent.getSlot("streetA").getValue();
        String streetB = intent.getSlot("streetB").getValue();

        log.info(String.format("Querying next %s bus at %s and %s", route, streetA, streetB));

        DynamoReader reader = new DynamoReader();
        int stopId = 0;

        SsmlOutputSpeech speech;
        try {
            stopId = reader.getStopId(route, streetA, streetB);
        } catch (NoSuchElementException e) {
            return SpeechletResponse.newTellResponse(getSSMLOutputSpeech("Sorry, I can't find that stop"));
        }

        try {
            speech = getSSMLOutputSpeech(String.format("The %s bus at %s and %s is coming " +
                    "in %s", route, streetA, streetB, getTimePhrase(1, stopId)));
        } catch (Exception e) {
            speech = getSSMLOutputSpeech("Sorry, I couldn't find the bus time.");
        }
        return SpeechletResponse.newTellResponse(speech);
    }


    private String getTimePhrase(int predictionNum, int stopId) throws ParserConfigurationException, IOException,
            SAXException {

        log.info("getting time phrase");

        String stopUrl = String.format(URL_FORMAT, stopId);
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.parse(new URL(stopUrl).openStream());

        Node firstPrediction = doc.getDocumentElement().getChildNodes().item(1).getChildNodes().item(1).getChildNodes()
                .item(predictionNum);

        int seconds =  Integer.valueOf(firstPrediction.getAttributes().getNamedItem("seconds").getTextContent());
        int minutes =  Integer.valueOf(firstPrediction.getAttributes().getNamedItem("minutes").getTextContent());

        return String.format("<emphasis level=\"moderate\">%s</emphasis> minutes and <emphasis " +
                "level=\"reduced\">%s</emphasis> seconds. ", minutes, seconds %
                60);
    }

    private SimpleCard getSimpleCard(String title, String content) {
        SimpleCard card = new SimpleCard();
        card.setTitle(title);
        card.setContent(content);

        return card;
    }

    private PlainTextOutputSpeech getPlainTextOutputSpeech(String speechText) {
        PlainTextOutputSpeech speech = new PlainTextOutputSpeech();
        speech.setText(speechText);

        return speech;
    }

    private SsmlOutputSpeech getSSMLOutputSpeech(String speechText) {
        SsmlOutputSpeech speech = new SsmlOutputSpeech();
        speech.setSsml("<speak>" + speechText + "</speak>");
        return speech;
    }

    private Reprompt getReprompt(OutputSpeech outputSpeech) {
        Reprompt reprompt = new Reprompt();
        reprompt.setOutputSpeech(outputSpeech);

        return reprompt;
    }

    private SpeechletResponse getAskResponse(String cardTitle, String speechText) {
        SimpleCard card = getSimpleCard(cardTitle, speechText);
        PlainTextOutputSpeech speech = getPlainTextOutputSpeech(speechText);
        Reprompt reprompt = getReprompt(speech);

        return SpeechletResponse.newAskResponse(speech, reprompt, card);
    }
}
