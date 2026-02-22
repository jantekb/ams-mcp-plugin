package com.streamtoolbox.ams.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.antmedia.rest.BroadcastRestService;
import io.antmedia.rest.McpRestService;
import io.antmedia.rest.model.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StopRecordingHandler implements ToolHandler {
	private static final Logger logger = LoggerFactory.getLogger(StopRecordingHandler.class);
	private final BroadcastRestService broadcastRestService;

	public StopRecordingHandler(BroadcastRestService broadcastRestService) {
		this.broadcastRestService = broadcastRestService;
	}

	@Override
	public String handle(JsonNode arguments) throws Exception {
		String streamId = arguments.get("streamId").asText();
		logger.info("Stop recording for stream: {}", streamId);

		Result res = broadcastRestService.enableRecording(streamId, false, "mp4", 0);

		ObjectNode result = McpRestService.OBJECT_MAPPER.createObjectNode();
		result.put("success", res.isSuccess());
		result.put("streamId", streamId);
		result.put("recordingStatus", "stopped");

		return McpRestService.OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(result);
	}
}
