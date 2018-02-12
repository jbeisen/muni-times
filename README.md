# MUNI Times

Welcome! This is an Alexa skill for getting San Francisco MUNI Times.

So far this skill is very simple, but I'm working on adding some more features. Right now it's very
closely based off the sample HelloWorld skill provided by Amazon.

### Usage

The skill currently uses a single hardcoded MUNI stop. To set the stop, change the field `STOP_URL` in the
`MuniTimesSpeechlet` class. This skill uses the [Next Bus API](https://www.nextbus.com/xmlFeedDocs/NextBusXMLFeed
.pdf), so check that out to get the correct format for the prediction URL.

- Get the bus time:

  > _You:_ "Alexa, ask MUNI for the bus times."

  > _Alexa:_ "The bus is arriving in 5 minutes and 34 seconds. The next bus is in 12 minutes and 3 seconds."

### Future Plans

- Get the bus time for any stop:

  > _You:_ "Alexa, when's the next 14 bus at 1st and Mision?"

- Setting a favorite stop:

  > _You:_ "Alexa, tell MUNI to set my favorite stop to route 14 at 1st and Mission."

