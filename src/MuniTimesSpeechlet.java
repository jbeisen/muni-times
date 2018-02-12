import com.amazon.speech.json.SpeechletRequestEnvelope;
import com.amazon.speech.slu.Intent;
import com.amazon.speech.speechlet.*;
import com.amazon.speech.ui.OutputSpeech;
import com.amazon.speech.ui.PlainTextOutputSpeech;
import com.amazon.speech.ui.Reprompt;
import com.amazon.speech.ui.SimpleCard;
import com.amazon.speech.ui.SsmlOutputSpeech;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.net.URL;

public class MuniTimesSpeechlet implements SpeechletV2 {

    private static final Logger log = LoggerFactory.getLogger(MuniTimesSpeechlet.class);
    private final static String STOP_URL = "NEXT BUS PREDICTIONS URL GOES HERE";

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
        log.info("onIntent requestId={}, sessionId={}", request.getRequestId(),
                requestEnvelope.getSession().getSessionId());

        Intent intent = request.getIntent();
        String intentName = (intent != null) ? intent.getName() : null;

        if ("BusTime".equals(intentName)) {
            return getBusTimeResponse();
        } else {
            return getAskResponse("MUNI Times", "This is unsupported.  Please try something else.");
        }
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
        try {
            String speechText = "The bus is arriving in " + getTimePhrase(1);
            speechText = speechText + "The next bus is in " + getTimePhrase(3);
            SimpleCard card = getSimpleCard("Bus time", speechText);
            SsmlOutputSpeech speech = getSSMLOutputSpeech(speechText);
            return SpeechletResponse.newTellResponse(speech, card);
        } catch (Exception e) {

        }
        return null;
    }

    private String getTimePhrase(int predictionNum) throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.parse(new URL(STOP_URL).openStream());


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
