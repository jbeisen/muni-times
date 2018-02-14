package munitimes;

import java.util.HashSet;
import java.util.Set;

import com.amazon.speech.speechlet.lambda.SpeechletRequestStreamHandler;


public final class MuniTimesSpeechletRequestStreamHandler extends SpeechletRequestStreamHandler {

    private static final Set<String> supportedApplicationIds;

    static {
        supportedApplicationIds = new HashSet<>();
        supportedApplicationIds.add("amzn1.ask.skill.fc891ece-b28d-40fc-9270-2f2cbb3394b2");
    }

    public MuniTimesSpeechletRequestStreamHandler() {
        super(new MuniTimesSpeechlet(), supportedApplicationIds);
    }
}
