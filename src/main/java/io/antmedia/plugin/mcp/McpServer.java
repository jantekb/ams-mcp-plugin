package io.antmedia.plugin.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.streamtoolbox.ams.handler.DeleteBroadcastHandler;
import com.streamtoolbox.ams.handler.GetApplicationNamesHandler;
import com.streamtoolbox.ams.handler.GetCpuInfoHandler;
import com.streamtoolbox.ams.handler.GetGpuInfoHandler;
import com.streamtoolbox.ams.handler.ListBroadcastsHandler;
import com.streamtoolbox.ams.handler.PromptHandler;
import com.streamtoolbox.ams.handler.QueryLogsHandler;
import com.streamtoolbox.ams.handler.ServerLoadPromptHandler;
import com.streamtoolbox.ams.handler.StartRecordingHandler;
import com.streamtoolbox.ams.handler.StopBroadcastHandler;
import com.streamtoolbox.ams.handler.StopRecordingHandler;
import com.streamtoolbox.ams.handler.ToolHandler;
import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.rest.BroadcastRestService;
import io.antmedia.rest.McpRestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class McpServer {
	
	private static final Logger logger = LoggerFactory.getLogger(McpServer.class);
	private static final String MCP_VERSION = "2025-06-18";
	private static final int MAX_CHAR_SIZE = 512000;

	private final Map<String, ToolHandler> toolHandlers = new ConcurrentHashMap<>();
	private final Map<String, PromptHandler> promptHandlers = new ConcurrentHashMap<>();

	public McpServer(BroadcastRestService broadcastRestService, AntMediaApplicationAdapter applicationAdapter) {
		toolHandlers.put("start_recording", new StartRecordingHandler(broadcastRestService));
		toolHandlers.put("stop_recording", new StopRecordingHandler(broadcastRestService));
		toolHandlers.put("query_logs", new QueryLogsHandler());
		toolHandlers.put("get_application_names", new GetApplicationNamesHandler(applicationAdapter));
		toolHandlers.put("list_broadcasts", new ListBroadcastsHandler(broadcastRestService));
		toolHandlers.put("stop_broadcast", new StopBroadcastHandler(broadcastRestService));
		toolHandlers.put("delete_broadcast", new DeleteBroadcastHandler(broadcastRestService));
		toolHandlers.put("get_cpu_info", new GetCpuInfoHandler());
		toolHandlers.put("get_gpu_info", new GetGpuInfoHandler());

		promptHandlers.put("server_load", new ServerLoadPromptHandler());

		logger.info("McpServer initialized with RestServiceV2");
	}
	
	public String handleRequest(String requestJson) {
		logger.info("Incoming MCP request payload: {}", requestJson);
		try {
			JsonNode request = McpRestService.OBJECT_MAPPER.readTree(requestJson);
			String method = request.get("method").asText();
			logger.info("Processing method: {}", method);
			
			String response;
			switch (method) {
				case "initialize":
					response = handleInitialize(request);
					break;
				case "tools/list":
					response = handleToolsList(request);
					break;
				case "tools/call":
					response = handleToolsCall(request);
					break;
				case "prompts/list":
					response = handlePromptsList(request);
					break;
				case "prompts/get":
					response = handlePromptsGet(request);
					break;
				case "resources/list":
					response = handleResourcesList(request);
					break;
				case "resources/read":
					response = handleResourcesRead(request);
					break;
				default:
					response = createErrorResponse(request, -32601, "Method not found: " + method);
			}
			
			String logResponse = response.length() > 1024 ? response.substring(0, 1024) + "...(limited to 1024)" : response;
			logger.info("Response for method '{}': {}", method, logResponse);
			return response;
		} catch (Exception e) {
			logger.error("Error handling MCP request", e);
			String errorResponse = createErrorResponse(null, -32603, "Internal error: " + e.getMessage());
			logger.info("Error response: {}", errorResponse);
			return errorResponse;
		}
	}
	
	private String handleInitialize(JsonNode request) throws Exception {
		ObjectNode response = McpRestService.OBJECT_MAPPER.createObjectNode();
		response.put("jsonrpc", "2.0");
		response.set("id", request.get("id"));
		
		ObjectNode result = McpRestService.OBJECT_MAPPER.createObjectNode();
		result.put("protocolVersion", MCP_VERSION);
		
		ObjectNode serverInfo = McpRestService.OBJECT_MAPPER.createObjectNode();
		serverInfo.put("name", "ant-media-mcp-server");
		serverInfo.put("version", "1.0.0");
		result.set("serverInfo", serverInfo);
		
		ObjectNode capabilities = McpRestService.OBJECT_MAPPER.createObjectNode();
		
		ObjectNode toolsCapability = McpRestService.OBJECT_MAPPER.createObjectNode();
		toolsCapability.put("listChanged", false);
		capabilities.set("tools", toolsCapability);
		
		ObjectNode resourcesCapability = McpRestService.OBJECT_MAPPER.createObjectNode();
		resourcesCapability.put("subscribe", false);
		resourcesCapability.put("listChanged", false);
		capabilities.set("resources", resourcesCapability);

		ObjectNode promptsCapability = McpRestService.OBJECT_MAPPER.createObjectNode();
		promptsCapability.put("listChanged", false);
		capabilities.set("prompts", promptsCapability);
		
		result.set("capabilities", capabilities);
		
		response.set("result", result);
		return McpRestService.OBJECT_MAPPER.writeValueAsString(response);
	}
	
	private String handleToolsList(JsonNode request) throws Exception {
		ObjectNode response = McpRestService.OBJECT_MAPPER.createObjectNode();
		response.put("jsonrpc", "2.0");
		response.set("id", request.get("id"));
		
		ObjectNode result = McpRestService.OBJECT_MAPPER.createObjectNode();
		ArrayNode tools = McpRestService.OBJECT_MAPPER.createArrayNode();
		
		// start_recording tool
		ObjectNode startRecordingProps = McpRestService.OBJECT_MAPPER.createObjectNode();
		startRecordingProps.set("streamId", McpRestService.OBJECT_MAPPER.createObjectNode()
			.put("type", "string")
			.put("description", "ID of the stream to start recording"));
		
		ObjectNode recordingTypeSchema = McpRestService.OBJECT_MAPPER.createObjectNode();
		recordingTypeSchema.put("type", "string");
		recordingTypeSchema.put("description", "Recording format: 'mp4' or 'webm'. Defaults to 'mp4' if not specified.");
		recordingTypeSchema.set("enum", McpRestService.OBJECT_MAPPER.createArrayNode().add("mp4").add("webm"));
		startRecordingProps.set("recordingType", recordingTypeSchema);
		
		ObjectNode startRecordingSchema = McpRestService.OBJECT_MAPPER.createObjectNode();
		startRecordingSchema.put("type", "object");
		startRecordingSchema.set("properties", startRecordingProps);
		startRecordingSchema.set("required", McpRestService.OBJECT_MAPPER.createArrayNode().add("streamId"));
		tools.add(createToolDefinition("start_recording", "Start recording a broadcast stream", startRecordingSchema));
		
		// stop_recording tool
		ObjectNode stopRecordingProps = McpRestService.OBJECT_MAPPER.createObjectNode();
		stopRecordingProps.set("streamId", McpRestService.OBJECT_MAPPER.createObjectNode()
			.put("type", "string")
			.put("description", "ID of the stream to stop recording"));
		
		ObjectNode stopRecordingSchema = McpRestService.OBJECT_MAPPER.createObjectNode();
		stopRecordingSchema.put("type", "object");
		stopRecordingSchema.set("properties", stopRecordingProps);
		stopRecordingSchema.set("required", McpRestService.OBJECT_MAPPER.createArrayNode().add("streamId"));
		tools.add(createToolDefinition("stop_recording", "Stop recording a broadcast stream", stopRecordingSchema));
		
		ObjectNode queryLogsProps = McpRestService.OBJECT_MAPPER.createObjectNode();
		queryLogsProps.set("since", McpRestService.OBJECT_MAPPER.createObjectNode()
			.put("type", "string")
			.put("description", "ISO 8601 timestamp for start of log range (e.g., 2026-02-12T23:00:00Z)"));
		queryLogsProps.set("until", McpRestService.OBJECT_MAPPER.createObjectNode()
			.put("type", "string")
			.put("description", "ISO 8601 timestamp for end of log range (e.g., 2026-02-12T23:59:59Z)"));
		
		ObjectNode minSeveritySchema = McpRestService.OBJECT_MAPPER.createObjectNode();
		minSeveritySchema.put("type", "string");
		minSeveritySchema.put("description", "Minimum severity level: TRACE, DEBUG, INFO, or ERROR");
		minSeveritySchema.set("enum", McpRestService.OBJECT_MAPPER.createArrayNode().add("TRACE").add("DEBUG").add("INFO").add("ERROR"));
		queryLogsProps.set("minSeverity", minSeveritySchema);
		
		queryLogsProps.set("filterRegex", McpRestService.OBJECT_MAPPER.createObjectNode()
			.put("type", "string")
			.put("description", "Optional regex pattern to filter log messages"));
		
		ObjectNode queryLogsSchema = McpRestService.OBJECT_MAPPER.createObjectNode();
		queryLogsSchema.put("type", "object");
		queryLogsSchema.set("properties", queryLogsProps);
		queryLogsSchema.set("required", McpRestService.OBJECT_MAPPER.createArrayNode().add("since").add("until").add("minSeverity"));
		tools.add(createToolDefinition("query_logs", "Query Ant Media Server logs with time range and severity filtering", queryLogsSchema));
		
		ObjectNode getAppNamesSchema = McpRestService.OBJECT_MAPPER.createObjectNode();
		getAppNamesSchema.put("type", "object");
		getAppNamesSchema.set("properties", McpRestService.OBJECT_MAPPER.createObjectNode());
		tools.add(createToolDefinition("get_application_names", "Get list of application names", getAppNamesSchema));
		
		ObjectNode listBroadcastsSchema = McpRestService.OBJECT_MAPPER.createObjectNode();
		listBroadcastsSchema.put("type", "object");
		listBroadcastsSchema.set("properties", McpRestService.OBJECT_MAPPER.createObjectNode());
		tools.add(createToolDefinition("list_broadcasts", "Get alphabetically sorted list of broadcast stream names", listBroadcastsSchema));
		
		ObjectNode stopBroadcastProps = McpRestService.OBJECT_MAPPER.createObjectNode();
		stopBroadcastProps.set("streamId", McpRestService.OBJECT_MAPPER.createObjectNode()
			.put("type", "string")
			.put("description", "ID of the stream to stop broadcasting"));
		
		ObjectNode stopBroadcastSchema = McpRestService.OBJECT_MAPPER.createObjectNode();
		stopBroadcastSchema.put("type", "object");
		stopBroadcastSchema.set("properties", stopBroadcastProps);
		stopBroadcastSchema.set("required", McpRestService.OBJECT_MAPPER.createArrayNode().add("streamId"));
		tools.add(createToolDefinition("stop_broadcast", "Stop a broadcast stream", stopBroadcastSchema));
		
		ObjectNode deleteBroadcastProps = McpRestService.OBJECT_MAPPER.createObjectNode();
		deleteBroadcastProps.set("streamId", McpRestService.OBJECT_MAPPER.createObjectNode()
			.put("type", "string")
			.put("description", "ID of the stream to delete"));
		
		ObjectNode deleteBroadcastSchema = McpRestService.OBJECT_MAPPER.createObjectNode();
		deleteBroadcastSchema.put("type", "object");
		deleteBroadcastSchema.set("properties", deleteBroadcastProps);
		deleteBroadcastSchema.set("required", McpRestService.OBJECT_MAPPER.createArrayNode().add("streamId"));
		tools.add(createToolDefinition("delete_broadcast", "Delete a broadcast stream", deleteBroadcastSchema));
		
		ObjectNode getCpuInfoSchema = McpRestService.OBJECT_MAPPER.createObjectNode();
		getCpuInfoSchema.put("type", "object");
		getCpuInfoSchema.set("properties", McpRestService.OBJECT_MAPPER.createObjectNode());
		tools.add(createToolDefinition("get_cpu_info", "Get CPU information from the server", getCpuInfoSchema));
		
		ObjectNode getGpuInfoSchema = McpRestService.OBJECT_MAPPER.createObjectNode();
		getGpuInfoSchema.put("type", "object");
		getGpuInfoSchema.set("properties", McpRestService.OBJECT_MAPPER.createObjectNode());
		tools.add(createToolDefinition("get_gpu_info", "Get GPU information from the server", getGpuInfoSchema));
		
		result.set("tools", tools);
		response.set("result", result);
		return McpRestService.OBJECT_MAPPER.writeValueAsString(response);
	}
	
	private ObjectNode createToolDefinition(String name, String description, ObjectNode inputSchema) {
		ObjectNode tool = McpRestService.OBJECT_MAPPER.createObjectNode();
		tool.put("name", name);
		tool.put("description", description);
		tool.set("inputSchema", inputSchema);
		return tool;
	}
	
	private String handleToolsCall(JsonNode request) throws Exception {
		JsonNode params = request.get("params");
		String toolName = params.get("name").asText();
		JsonNode arguments = params.get("arguments");
		
		ToolHandler handler = toolHandlers.get(toolName);
		if (handler == null) {
			return createErrorResponse(request, -32602, "Unknown tool: " + toolName);
		}
		
		String result;
		try {
			result = handler.handle(arguments);
		} catch (Exception e) {
			logger.error("Error executing tool '{}': {}", toolName, e.getMessage(), e);
			return createErrorResponse(request, -32603, "Tool execution failed: " + e.getMessage());
		}
		
		ObjectNode response = McpRestService.OBJECT_MAPPER.createObjectNode();
		response.put("jsonrpc", "2.0");
		response.set("id", request.get("id"));
		
		ObjectNode resultNode = McpRestService.OBJECT_MAPPER.createObjectNode();
		ArrayNode content = McpRestService.OBJECT_MAPPER.createArrayNode();
		ObjectNode textContent = McpRestService.OBJECT_MAPPER.createObjectNode();
		textContent.put("type", "text");
		textContent.put("text", result);
		content.add(textContent);
		resultNode.set("content", content);
		
		response.set("result", resultNode);
		return McpRestService.OBJECT_MAPPER.writeValueAsString(response);
	}
	
	private String handleResourcesList(JsonNode request) throws Exception {
		ObjectNode response = McpRestService.OBJECT_MAPPER.createObjectNode();
		response.put("jsonrpc", "2.0");
		response.set("id", request.get("id"));
		
		ObjectNode result = McpRestService.OBJECT_MAPPER.createObjectNode();
		ArrayNode resources = McpRestService.OBJECT_MAPPER.createArrayNode();
		
		ObjectNode mainLog = McpRestService.OBJECT_MAPPER.createObjectNode();
		mainLog.put("uri", "log://ant-media-server.log");
		mainLog.put("name", "Ant Media Server Log");
		mainLog.put("description", "Last " + MAX_CHAR_SIZE + " characters of the main server log");
		mainLog.put("mimeType", "text/plain");
		resources.add(mainLog);
		
		ObjectNode errorLog = McpRestService.OBJECT_MAPPER.createObjectNode();
		errorLog.put("uri", "log://antmedia-error.log");
		errorLog.put("name", "Ant Media Server Error Log");
		errorLog.put("description", "Last " + MAX_CHAR_SIZE + " characters of ERROR level logs");
		errorLog.put("mimeType", "text/plain");
		resources.add(errorLog);
		
		result.set("resources", resources);
		response.set("result", result);
		return McpRestService.OBJECT_MAPPER.writeValueAsString(response);
	}
	
	private String handleResourcesRead(JsonNode request) throws Exception {
		JsonNode params = request.get("params");
		String uri = params.get("uri").asText();
		
		if (!uri.startsWith("log://")) {
			return createErrorResponse(request, -32602, "Invalid URI scheme. Expected log://");
		}
		
		String fileName = uri.substring("log://".length());
		String logFilePath;
		boolean filterErrorOnly = false;
		
		if ("ant-media-server.log".equals(fileName)) {
			logFilePath = "logs/ant-media-server.log";
		} else if ("antmedia-error.log".equals(fileName)) {
			logFilePath = "logs/ant-media-server.log";
			filterErrorOnly = true;
		} else {
			return createErrorResponse(request, -32602, "Unknown log file. Expected ant-media-server.log or antmedia-error.log");
		}
		
		logger.info("Reading log resource: {} (path: {}, errorOnly: {})", uri, logFilePath, filterErrorOnly);
		
		String content;
		try {
			content = readLogFile(logFilePath, filterErrorOnly, MAX_CHAR_SIZE);
		} catch (IOException e) {
			logger.error("Error retrieving log content for {}", uri, e);
			return createErrorResponse(request, -32603, "Error retrieving log: " + e.getMessage());
		}
		
		ObjectNode response = McpRestService.OBJECT_MAPPER.createObjectNode();
		response.put("jsonrpc", "2.0");
		response.set("id", request.get("id"));
		
		ObjectNode result = McpRestService.OBJECT_MAPPER.createObjectNode();
		ArrayNode contents = McpRestService.OBJECT_MAPPER.createArrayNode();
		
		ObjectNode textContent = McpRestService.OBJECT_MAPPER.createObjectNode();
		textContent.put("uri", uri);
		textContent.put("mimeType", "text/plain");
		textContent.put("text", content);
		contents.add(textContent);
		
		result.set("contents", contents);
		response.set("result", result);
		return McpRestService.OBJECT_MAPPER.writeValueAsString(response);
	}
	
	private String handlePromptsList(JsonNode request) throws Exception {
		ObjectNode response = McpRestService.OBJECT_MAPPER.createObjectNode();
		response.put("jsonrpc", "2.0");
		response.set("id", request.get("id"));

		ObjectNode result = McpRestService.OBJECT_MAPPER.createObjectNode();
		ArrayNode prompts = McpRestService.OBJECT_MAPPER.createArrayNode();

		ObjectNode serverLoadPrompt = McpRestService.OBJECT_MAPPER.createObjectNode();
		serverLoadPrompt.put("name", "server_load");
		serverLoadPrompt.put("description", "What is the CPU and GPU load on the server?");
		prompts.add(serverLoadPrompt);

		result.set("prompts", prompts);
		response.set("result", result);
		return McpRestService.OBJECT_MAPPER.writeValueAsString(response);
	}

	private String handlePromptsGet(JsonNode request) throws Exception {
		JsonNode params = request.get("params");
		String promptName = params.get("name").asText();

		PromptHandler handler = promptHandlers.get(promptName);
		if (handler == null) {
			return createErrorResponse(request, -32602, "Unknown prompt: " + promptName);
		}

		JsonNode arguments = params.has("arguments") ? params.get("arguments") : McpRestService.OBJECT_MAPPER.createObjectNode();

		String result;
		try {
			result = handler.handle(arguments);
		} catch (Exception e) {
			logger.error("Error executing prompt '{}': {}", promptName, e.getMessage(), e);
			return createErrorResponse(request, -32603, "Prompt execution failed: " + e.getMessage());
		}

		ObjectNode response = McpRestService.OBJECT_MAPPER.createObjectNode();
		response.put("jsonrpc", "2.0");
		response.set("id", request.get("id"));

		ObjectNode resultNode = McpRestService.OBJECT_MAPPER.createObjectNode();
		ArrayNode messages = McpRestService.OBJECT_MAPPER.createArrayNode();

		ObjectNode userMessage = McpRestService.OBJECT_MAPPER.createObjectNode();
		userMessage.put("role", "user");
		ObjectNode userContent = McpRestService.OBJECT_MAPPER.createObjectNode();
		userContent.put("type", "text");
		userContent.put("text", "What is the CPU and GPU load on the server?");
		userMessage.set("content", userContent);
		messages.add(userMessage);

		ObjectNode assistantMessage = McpRestService.OBJECT_MAPPER.createObjectNode();
		assistantMessage.put("role", "assistant");
		ObjectNode assistantContent = McpRestService.OBJECT_MAPPER.createObjectNode();
		assistantContent.put("type", "text");
		assistantContent.put("text", result);
		assistantMessage.set("content", assistantContent);
		messages.add(assistantMessage);

		resultNode.set("messages", messages);
		response.set("result", resultNode);
		return McpRestService.OBJECT_MAPPER.writeValueAsString(response);
	}

	private String readLogFile(String logFilePath, boolean errorOnly, int maxCharSize) throws IOException {
		Path path = Paths.get(logFilePath);
		
		if (!Files.exists(path)) {
			throw new IOException("Log file not found: " + logFilePath);
		}
		
		StringBuilder content = new StringBuilder();
		int charCount = 0;
		
		try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
			String line;
			boolean includeCurrentBlock = !errorOnly;
			StringBuilder currentBlock = new StringBuilder();
			
			while ((line = reader.readLine()) != null && charCount < maxCharSize) {
				if (line.matches("^\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2},\\d{3} \\[.*?\\] .*$")) {
					if (includeCurrentBlock && currentBlock.length() > 0) {
						String block = currentBlock.toString();
						if (charCount + block.length() <= maxCharSize) {
							content.append(block);
							charCount += block.length();
						} else {
							content.append(block, 0, maxCharSize - charCount);
							charCount = maxCharSize;
							break;
						}
					}
					
					currentBlock = new StringBuilder();
					includeCurrentBlock = !errorOnly || line.contains(" ERROR ");
				}
				
				currentBlock.append(line).append("\n");
			}
			
			if (includeCurrentBlock && currentBlock.length() > 0 && charCount < maxCharSize) {
				String block = currentBlock.toString();
				if (charCount + block.length() <= maxCharSize) {
					content.append(block);
				} else {
					content.append(block, 0, maxCharSize - charCount);
				}
			}
		}
		
		if (content.length() == 0) {
			return errorOnly ? "No ERROR logs found in the last " + maxCharSize + " characters" 
			                 : "Log file is empty";
		}
		
		return content.toString();
	}
	
	private String createErrorResponse(JsonNode request, int code, String message) {
		try {
			ObjectNode response = McpRestService.OBJECT_MAPPER.createObjectNode();
			response.put("jsonrpc", "2.0");
			if (request != null && request.has("id")) {
				response.set("id", request.get("id"));
			} else {
				response.putNull("id");
			}
			
			ObjectNode error = McpRestService.OBJECT_MAPPER.createObjectNode();
			error.put("code", code);
			error.put("message", message);
			response.set("error", error);
			
			return McpRestService.OBJECT_MAPPER.writeValueAsString(response);
		} catch (Exception e) {
			return "{\"jsonrpc\":\"2.0\",\"id\":null,\"error\":{\"code\":-32603,\"message\":\"Internal error\"}}";
		}
	}
}
