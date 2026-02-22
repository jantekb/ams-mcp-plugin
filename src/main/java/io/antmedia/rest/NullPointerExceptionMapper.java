package io.antmedia.rest;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

@Provider
public class NullPointerExceptionMapper implements ExceptionMapper<NullPointerException> {

    @Override
    public Response toResponse(NullPointerException exception) {
        LoggerFactory.getLogger(NullPointerExceptionMapper.class).error(exception.getMessage(), exception);

        Map<String, String> npeResponse = new HashMap<>();
        npeResponse.put("error", "NullPointerException happened internally");

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        exception.printStackTrace(pw);

        npeResponse.put("stackTrace", pw.toString());
        return Response.status(500)
                .entity(npeResponse)
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}
