package com.streamtoolbox.ams.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.gson.JsonArray;
import io.antmedia.statistic.StatsCollector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GetGpuInfoHandler implements ToolHandler {
	private static final Logger logger = LoggerFactory.getLogger(GetGpuInfoHandler.class);

	@Override
	public String handle(JsonNode arguments) throws Exception {
		logger.info("Getting GPU info");

		JsonArray gpuInfo = StatsCollector.getGPUInfoJSObject();
		return gpuInfo.toString();
	}
}
