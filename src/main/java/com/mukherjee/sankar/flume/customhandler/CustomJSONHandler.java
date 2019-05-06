package com.mukherjee.sankar.flume.customhandler;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import java.io.BufferedReader;
import java.lang.reflect.Type;
import java.nio.charset.UnsupportedCharsetException;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.apache.flume.Context;
import org.apache.flume.Event;
import org.apache.flume.event.EventBuilder;
import org.apache.flume.event.JSONEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.flume.source.http.HTTPSourceHandler;
import org.apache.flume.source.http.HTTPBadRequestException;

/**
 *
 * Custom JSONHandler for HTTPSource that accepts an array of events.
 * This accepts an token in header with name auth and validates request based on it.
 * Thus allows you to accept only Authorised event.
 *
 * This handler throws exception if the deserialization fails because of bad
 * format or any other reason. It also throws exception if there is no header with name auth or
 * the auth header is not matching correct token.
 *
 * Request Example
 * [{"headers" : {"auth":"YOUR_TOKEN", "some-header": "some-value"},"body": "random_body1"},
 * {"headers" : {"auth": "YOUR_TOKEN"},"body": "random_body2"}]
 *
 */

public class CustomJSONHandler implements HTTPSourceHandler {

    private final String CONF_INSERT_AUTHTOKEN = "authtoken";
    private static final Logger LOG = LoggerFactory.getLogger(CustomJSONHandler.class);
    private final Type listType =
            new TypeToken<List<JSONEvent>>() {
            }.getType();
    private final Gson gson;
    private String authToken ;

    public CustomJSONHandler() {
        gson = new GsonBuilder().disableHtmlEscaping().create();
    }

    @Override
    public List<Event> getEvents(HttpServletRequest request) throws Exception {
        BufferedReader reader = request.getReader();
        String charset = request.getCharacterEncoding();
        //UTF-8 is default for JSON. If no charset is specified, UTF-8 is to
        //be assumed.
        if (charset == null) {
            LOG.debug("Charset is null, default charset of UTF-8 will be used.");
            charset = "UTF-8";
        } else if (!(charset.equalsIgnoreCase("utf-8")
                || charset.equalsIgnoreCase("utf-16")
                || charset.equalsIgnoreCase("utf-32"))) {
            LOG.error("Unsupported character set in request {}. "
                    + "JSON handler supports UTF-8, "
                    + "UTF-16 and UTF-32 only.", charset);
            throw new UnsupportedCharsetException("JSON handler supports UTF-8, "
                    + "UTF-16 and UTF-32 only.");
        }

        /*
         * Gson throws Exception if the data is not parseable to JSON.
         * Need not catch it since the source will catch it and return error.
         */
        List<Event> eventList = new ArrayList<Event>(0);
        try {
            eventList = gson.fromJson(reader, listType);
        } catch (JsonSyntaxException ex) {
            throw new HTTPBadRequestException("Request has invalid JSON Syntax.", ex);
        }

        for (Event e : eventList) {
            ((JSONEvent) e).setCharset(charset);
        }
        return getSimpleEvents(eventList);
    }

    @Override
    public void configure(Context context) {
        authToken = context.getString(CONF_INSERT_AUTHTOKEN,"changeit");
        LOG.debug("Configuration Auth Token: "+authToken);
    }

    private List<Event> getSimpleEvents(List<Event> events) {
        List<Event> newEvents = new ArrayList<Event>(events.size());
        for(Event e:events) {
            boolean isValid = false;
            java.util.Map<java.lang.String,java.lang.String> eventHeaders = e.getHeaders();
            if(eventHeaders.get("auth").contains(authToken)){
                LOG.debug("Request has valid authorisation header.");
            }
            else {
                LOG.info("Request has invalid authorisation header.");
                LOG.debug("Header Value: "+eventHeaders.get("auth"));
                throw new HTTPBadRequestException("Request has invalid Authorisation Header.");
            }
            newEvents.add(EventBuilder.withBody(e.getBody(), e.getHeaders()));
        }
        return newEvents;
    }
}
