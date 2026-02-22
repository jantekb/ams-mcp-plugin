package com.streamtoolbox.ams.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.antmedia.rest.BroadcastRestService;
import io.antmedia.rest.McpRestService;
import io.antmedia.rest.model.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeleteBroadcastHandler implements ToolHandler {
	private static final Logger logger = LoggerFactory.getLogger(DeleteBroadcastHandler.class);
	private final BroadcastRestService broadcastRestService;

	public DeleteBroadcastHandler(BroadcastRestService broadcastRestService) {
		this.broadcastRestService = broadcastRestService;
	}

	@Override
	public String handle(JsonNode arguments) throws Exception {
		String streamId = arguments.get("streamId").asText();
		logger.info("Deleting broadcast for stream: {}", streamId);

		Result res = broadcastRestService.deleteBroadcast(streamId, true);


		ObjectNode result = McpRestService.OBJECT_MAPPER.createObjectNode();
		result.put("success", res.isSuccess());
		result.put("message", res.getMessage());
		result.put("streamId", streamId);

		return McpRestService.OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(result);
	}
}
