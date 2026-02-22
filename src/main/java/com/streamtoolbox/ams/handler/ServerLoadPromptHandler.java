package com.streamtoolbox.ams.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.antmedia.statistic.StatsCollector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServerLoadPromptHandler implements PromptHandler {
	private static final Logger logger = LoggerFactory.getLogger(ServerLoadPromptHandler.class);

	@Override
	public String handle(JsonNode arguments) throws Exception {
		logger.info("Getting server CPU and GPU load");

		JsonObject cpuInfo = StatsCollector.getCPUInfoJSObject();
		JsonArray gpuInfo = StatsCollector.getGPUInfoJSObject();

		StringBuilder result = new StringBuilder();
		result.append("Server Load Information:\n\n");

		result.append("CPU Information:\n");
		if (cpuInfo != null) {
			result.append("  Usage: ").append(cpuInfo.get("processCPULoad")).append("%\n");
			result.append("  System Load: ").append(cpuInfo.get("systemCPULoad")).append("%\n");
		} else {
			result.append("  CPU information not available\n");
		}

		result.append("\nGPU Information:\n");
		if (gpuInfo != null && gpuInfo.size() > 0) {
			for (int i = 0; i < gpuInfo.size(); i++) {
				JsonObject gpu = gpuInfo.get(i).getAsJsonObject();
				result.append("  GPU ").append(i).append(":\n");
				result.append("    Model: ").append(gpu.get("modelName")).append("\n");
				result.append("    Utilization: ").append(gpu.get("utilization")).append("%\n");
				result.append("    Memory Used: ").append(gpu.get("memoryUsed")).append(" MB\n");
				result.append("    Memory Total: ").append(gpu.get("memoryTotal")).append(" MB\n");
			}
		} else {
			result.append("  No GPU information available\n");
		}

		return result.toString();
	}
}
