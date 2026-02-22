package io.antmedia.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.datastore.db.DataStoreFactory;
import io.antmedia.plugin.McpPlugin;
import io.antmedia.plugin.mcp.McpServer;
import jakarta.servlet.ServletContext;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.WebApplicationContext;

@Path("/mcp")
@jakarta.inject.Singleton
public class McpRestService {

	@Context
	protected ServletContext servletContext;

	private static final Logger logger = LoggerFactory.getLogger(McpRestService.class);
	private static final String MCP_PROTOCOL_VERSION = "2025-06-18";
	private static final String DEFAULT_PROTOCOL_VERSION = "2025-03-26";
	private static final String PROTOCOL_VERSION_HEADER = "MCP-Protocol-Version";
	
	public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
	
	private McpServer mcpServer;
	private boolean initialized = false;
	
	public McpRestService() {
	}
	
	private void ensureInitialized() {
		if (!initialized) {
			try {
				ApplicationContext appCtx = (ApplicationContext) servletContext.getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE);
				if (appCtx != null) {
					BroadcastRestService restServiceV2 = null;
					try {
						restServiceV2 = appCtx.getBean(BroadcastRestService.class);

						// BroadcastRestService is unfortunately both Jersey-managed and Spring managed
						// it uses Jersey's injection to have a non-null ServletContext, which
						// then in turn is used to get the spring application context.
						// here we can only look this up from Spring, which means it will have null ServletContext
						// but we can leverage this method to force-inject the root application context as a hack

						restServiceV2.setAppCtx(appCtx);

						// there is a bug in RestServiceBase where for getting the datastorefactory
						// the appCtx it not used, hence we also need to save a DataStoreFactory

						restServiceV2.setDataStoreFactory(appCtx.getBean(DataStoreFactory.class));
						AntMediaApplicationAdapter applicationAdapter = (AntMediaApplicationAdapter) appCtx.getBean(AntMediaApplicationAdapter.BEAN_NAME);
						mcpServer = new McpServer(restServiceV2, applicationAdapter);

					} catch (Exception e) {
						logger.warn("Failed to get BroadcastRestService by type", e);
					}
					initialized = true;
					logger.info("McpServer initialized with BroadcastRestService");
				}
			} catch (Exception e) {
				logger.error("Error during initialization: {}", e.getMessage(), e);
			}
		}
	}
	
	@GET
	@Path("/version")
	@Produces(MediaType.TEXT_PLAIN)
	public String getVersion() {
		return getPluginApp().getVersion();
	}

	/**
	 * Validate protocol version header
	 */
	private String validateProtocolVersion(String protocolVersion) {
		if (protocolVersion == null) {
			logger.debug("No protocol version header, using default: {}", DEFAULT_PROTOCOL_VERSION);
			return DEFAULT_PROTOCOL_VERSION;
		}
		if (!MCP_PROTOCOL_VERSION.equals(protocolVersion) && !DEFAULT_PROTOCOL_VERSION.equals(protocolVersion)) {
			logger.warn("Unsupported protocol version: {}", protocolVersion);
			return null;
		}
		return protocolVersion;
	}
	
	/**
	 * Validate Origin header for security
	 */
	private boolean validateOrigin(String origin) {
		if (origin == null) {
			return true;
		}
		if (origin.contains("localhost") || origin.contains("127.0.0.1")) {
			return true;
		}
		logger.warn("Potential DNS rebinding attack from origin: {}", origin);
		return false;
	}

	/**
	 * Handle POST requests (client-to-server messages)
	 * Uses Streamable HTTP with JSON responses (no SSE)
	 */
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response handlePost(
			String body,
			@HeaderParam(PROTOCOL_VERSION_HEADER) String protocolVersion,
			@HeaderParam("Accept") String accept,
			@HeaderParam("Origin") String origin) {
		
		try {
			if (!validateOrigin(origin)) {
				return Response.status(400).entity("{\"error\":\"Invalid origin\"}").build();
			}
			
			String validatedVersion = validateProtocolVersion(protocolVersion);
			if (validatedVersion == null) {
				return Response.status(400).entity("{\"error\":\"Unsupported protocol version\"}").build();
			}
			
			if (accept == null || (!accept.contains("application/json") && !accept.contains("text/event-stream"))) {
				return Response.status(400).entity("{\"error\":\"Invalid Accept header\"}").build();
			}

			JsonNode jsonRequest = OBJECT_MAPPER.readTree(body);
			String method = jsonRequest.has("method") ? jsonRequest.get("method").asText() : null;
			boolean hasId = jsonRequest.has("id");
			boolean isNotification = method != null && !hasId;
			boolean isResponse = jsonRequest.has("result") || jsonRequest.has("error");
			boolean isRequest = method != null && hasId;
			
			logger.info("Type: method={}, hasId={}, isRequest={}, isNotification={}, isResponse={}", 
				method, hasId, isRequest, isNotification, isResponse);
			
			if (isNotification || isResponse) {
				logger.info("Notification/response - returning 202");
				return Response.status(202).build();
			} 
			
			if (isRequest) {
				ensureInitialized();
				
				if (mcpServer == null) {
					logger.error("McpServer failed to initialize");
					return Response.serverError().entity("{\"error\":\"Server initialization failed\"}").build();
				}
				
				String response = mcpServer.handleRequest(body);
				return Response.ok(response).type(MediaType.APPLICATION_JSON).build();
			}
			
			return Response.status(400).entity("{\"error\":\"Invalid JSON-RPC message\"}").build();
			
		} catch (Exception e) {
			logger.error("EXCEPTION in handlePost", e);
			return Response.serverError().entity("{\"error\":\"" + e.getMessage() + "\"}").build();
		}
	}
	private McpPlugin getPluginApp() {
		ApplicationContext appCtx = (ApplicationContext) servletContext.getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE);
		return (McpPlugin) appCtx.getBean("plugin.mcp");
	}
}
