package com.streamtoolbox.ams.handler;

import com.fasterxml.jackson.databind.JsonNode;

public interface ToolHandler {
	String handle(JsonNode arguments) throws Exception;
}
