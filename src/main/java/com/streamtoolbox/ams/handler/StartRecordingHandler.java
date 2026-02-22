package com.streamtoolbox.ams.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.antmedia.rest.BroadcastRestService;
import io.antmedia.rest.McpRestService;
import io.antmedia.rest.model.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StartRecordingHandler implements ToolHandler {
	private static final Logger logger = LoggerFactory.getLogger(StartRecordingHandler.class);
	private final BroadcastRestService broadcastRestService;

	public StartRecordingHandler(BroadcastRestService broadcastRestService) {
		this.broadcastRestService = broadcastRestService;
	}

	@Override
	public String handle(JsonNode arguments) throws Exception {
		String streamId = arguments.get("streamId").asText();
		String recordingType = arguments.has("recordingType") ? arguments.get("recordingType").asText() : null;
		
		if (recordingType != null && !recordingType.equals("mp4") && !recordingType.equals("webm")) {
			throw new IllegalArgumentException("Invalid recordingType: " + recordingType + ". Allowed values are: mp4, webm");
		}
		
		logger.info("Starting recording for stream: {} with type: {}", streamId, recordingType);

		Result res = broadcastRestService.enableRecording(streamId, true, recordingType, 0);

		ObjectNode result = McpRestService.OBJECT_MAPPER.createObjectNode();
		result.put("success", res.isSuccess());
		result.put("streamId", streamId);
		if (recordingType != null) {
			result.put("recordingType", recordingType);
		}
		result.put("recordingStatus", "started");

		return McpRestService.OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(result);
	}
}
