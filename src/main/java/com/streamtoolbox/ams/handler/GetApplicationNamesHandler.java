package com.streamtoolbox.ams.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.rest.McpRestService;
import org.red5.server.api.scope.IScope;
import org.red5.server.scope.Scope;
import org.red5.server.util.ScopeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GetApplicationNamesHandler implements ToolHandler {
	private static final Logger logger = LoggerFactory.getLogger(GetApplicationNamesHandler.class);
	private final AntMediaApplicationAdapter applicationAdapter;
    public GetApplicationNamesHandler(AntMediaApplicationAdapter applicationAdapter) {
		this.applicationAdapter = applicationAdapter;
    }

    @Override
	public String handle(JsonNode arguments) throws Exception {
		logger.info("Getting application names");
		ArrayNode result = McpRestService.OBJECT_MAPPER.createArrayNode();

		IScope root = ScopeUtils.findRoot(applicationAdapter.getScope());

		// logic copied from AdminApplication.java, as it is not reachable from here easily

		java.util.Set<String> names = root.getScopeNames();
		List<String> apps = new ArrayList<>();
		for (String name : names) {
			IScope scope = root.getScope(name);
			if (scope instanceof Scope) {
				Scope appScope = (Scope) scope;
				if(!name.equals("root") && appScope.isRunning()) {
					apps.add(name);
				}
			}
		}
		Collections.sort(apps);
		apps.forEach(app -> result.add(app));

		return McpRestService.OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(result);
	}
}
