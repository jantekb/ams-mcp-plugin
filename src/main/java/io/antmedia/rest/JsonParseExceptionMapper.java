package io.antmedia.rest;

import com.google.gson.JsonParseException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

@Provider
public class JsonParseExceptionMapper implements ExceptionMapper<JsonParseException> {

    @Override
    public Response toResponse(JsonParseException exception) {
        LoggerFactory.getLogger(JsonParseExceptionMapper.class)
                .error(exception.getMessage(), exception);

        Map<String, String> response = new HashMap<>();
        response.put("error", "Could not parse incoming JSON message. Details: " + exception.getMessage());
        return Response.status(400)
                .entity(response)
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}
