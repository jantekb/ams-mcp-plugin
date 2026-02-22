package io.antmedia.rest;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

@Provider
public class StampExceptionMapper implements ExceptionMapper<Exception> {

    @Override
    public Response toResponse(Exception exception) {

        Map<String, String> response = new HashMap<>();
        response.put("error", "Unhandled exception occured internally: " + exception.getMessage());
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        exception.printStackTrace(pw);

        response.put("stackTrace", pw.toString());

        return Response.status(500)
                .entity(response)
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}
