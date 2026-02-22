package com.streamtoolbox.ams.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.gson.JsonObject;
import io.antmedia.statistic.StatsCollector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GetCpuInfoHandler implements ToolHandler {
	private static final Logger logger = LoggerFactory.getLogger(GetCpuInfoHandler.class);

	@Override
	public String handle(JsonNode arguments) throws Exception {
		logger.info("Getting CPU info");

		JsonObject cpuInfo = StatsCollector.getCPUInfoJSObject();
		return cpuInfo.toString();
	}
}
