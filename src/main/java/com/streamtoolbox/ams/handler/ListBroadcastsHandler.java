package com.streamtoolbox.ams.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.rest.BroadcastRestService;
import io.antmedia.rest.McpRestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class ListBroadcastsHandler implements ToolHandler {
	private static final Logger logger = LoggerFactory.getLogger(ListBroadcastsHandler.class);
	private final BroadcastRestService broadcastRestService;

	public ListBroadcastsHandler(BroadcastRestService broadcastRestService) {
		this.broadcastRestService = broadcastRestService;
	}

	@Override
	public String handle(JsonNode arguments) throws Exception {
		logger.info("Listing broadcasts");
		ObjectNode result = McpRestService.OBJECT_MAPPER.createObjectNode();
		
		try {
			List<Broadcast> broadcasts = broadcastRestService.getBroadcastList(0, Integer.MAX_VALUE, null, null, null, null);

			ArrayNode broadcastsArray = McpRestService.OBJECT_MAPPER.createArrayNode();
			for (Broadcast broadcast : broadcasts) {
				JsonNode broadcastNode = McpRestService.OBJECT_MAPPER.valueToTree(broadcast);
				broadcastsArray.add(broadcastNode);
			}

			result.put("success", true);
			result.set("broadcasts", broadcastsArray);
			result.put("count", broadcasts.size());

			logger.info("Successfully retrieved {} broadcasts", broadcasts.size());
		} catch (Exception e) {
			result.put("success", false);
			result.put("message", "Error retrieving broadcasts: " + e.getMessage());
			logger.error("Error retrieving broadcasts", e);
		}
		
		return McpRestService.OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(result);
	}
}
