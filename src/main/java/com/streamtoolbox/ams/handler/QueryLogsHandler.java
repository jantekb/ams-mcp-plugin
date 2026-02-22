package com.streamtoolbox.ams.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.antmedia.rest.McpRestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class QueryLogsHandler implements ToolHandler {
	private static final Logger logger = LoggerFactory.getLogger(QueryLogsHandler.class);
	private static final int MAX_MATCHING_LINES = 10000;
	private static final int MAX_RESULT_SIZE_BYTES = 512000;
	private static final Pattern LOG_PATTERN = Pattern.compile(
		"^(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2},\\d{3}) \\[.*?\\] (TRACE|DEBUG|INFO|WARN|ERROR)\\s+.*$"
	);
	private static final DateTimeFormatter LOG_TIMESTAMP_FORMAT = 
		DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss,SSS");
	private static final Map<String, Integer> SEVERITY_LEVELS = Map.of(
		"TRACE", 0,
		"DEBUG", 1,
		"INFO", 2,
		"WARN", 3,
		"ERROR", 4
	);
	
	private final String logFilePath;
	
	public QueryLogsHandler() {
		this("log/ant-media-server.log");
	}
	
	public QueryLogsHandler(String logFilePath) {
		this.logFilePath = logFilePath;
	}
	
	@Override
	public String handle(JsonNode arguments) throws Exception {
		String sinceStr = arguments.get("since").asText();
		String untilStr = arguments.get("until").asText();
		String minSeverity = arguments.get("minSeverity").asText();
		String filterRegex = arguments.has("filterRegex") ? arguments.get("filterRegex").asText() : null;

		logger.info("Querying logs: since={}, until={}, minSeverity={}, filterRegex={}", 
			sinceStr, untilStr, minSeverity, filterRegex);
		
		ZonedDateTime since = ZonedDateTime.parse(sinceStr);
		ZonedDateTime until = ZonedDateTime.parse(untilStr);
		int minSeverityLevel = SEVERITY_LEVELS.getOrDefault(minSeverity, 2);
		Pattern regex = filterRegex != null ? Pattern.compile(filterRegex) : null;
		
		Path logFile = Paths.get(logFilePath);
		if (!Files.exists(logFile)) {
			throw new IOException("Log file not found: " + logFilePath);
		}
		
		List<String> matchingLines = new ArrayList<>();
		long totalLinesRead = 0;
		long totalBytesProcessed = 0;
		int currentResultSize = 0;
		boolean resultSizeLimitReached = false;
		
		try (BufferedReader reader = Files.newBufferedReader(logFile, StandardCharsets.UTF_8)) {
			String line;
			String currentLogLine = null;
			
			while ((line = reader.readLine()) != null) {
				totalLinesRead++;
				totalBytesProcessed += line.getBytes(StandardCharsets.UTF_8).length + 1;
				
				Matcher matcher = LOG_PATTERN.matcher(line);
				if (matcher.matches()) {
					if (currentLogLine != null && shouldIncludeLine(currentLogLine, since, until, minSeverityLevel, regex)) {
						if (matchingLines.size() < MAX_MATCHING_LINES) {
							int lineSize = currentLogLine.getBytes(StandardCharsets.UTF_8).length + 1;
							if (currentResultSize + lineSize < MAX_RESULT_SIZE_BYTES) {
								matchingLines.add(currentLogLine);
								currentResultSize += lineSize;
							} else {
								resultSizeLimitReached = true;
								break;
							}
						} else {
							logger.warn("Reached max matching lines limit: {}", MAX_MATCHING_LINES);
							break;
						}
					}
					currentLogLine = line;
				} else if (currentLogLine != null) {
					currentLogLine += "\n" + line;
				}
			}
			
			if (currentLogLine != null && !resultSizeLimitReached && shouldIncludeLine(currentLogLine, since, until, minSeverityLevel, regex)) {
				if (matchingLines.size() < MAX_MATCHING_LINES) {
					int lineSize = currentLogLine.getBytes(StandardCharsets.UTF_8).length + 1;
					if (currentResultSize + lineSize < MAX_RESULT_SIZE_BYTES) {
						matchingLines.add(currentLogLine);
					} else {
						resultSizeLimitReached = true;
					}
				}
			}
		}
		
		ObjectNode result = McpRestService.OBJECT_MAPPER.createObjectNode();
		result.put("success", true);
		result.put("matchingLines", matchingLines.size());
		result.put("logs", String.join("\n", matchingLines));
		result.put("since", sinceStr);
		result.put("until", untilStr);
		result.put("minSeverity", minSeverity);
		result.put("totalLinesRead", totalLinesRead);
		result.put("totalBytesProcessed", totalBytesProcessed);
		if (resultSizeLimitReached) {
			result.put("warning", "Result size limit reached. Some matching lines may be excluded.");
		}
		
		logger.info("Query returned {} matching log lines from {} total lines ({} bytes processed)", 
			matchingLines.size(), totalLinesRead, totalBytesProcessed);
		return McpRestService.OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(result);
	}
	
	private boolean shouldIncludeLine(String logLine, ZonedDateTime since, ZonedDateTime until, 
	                                   int minSeverityLevel, Pattern regex) {
		Matcher matcher = LOG_PATTERN.matcher(logLine.split("\n")[0]);
		if (!matcher.matches()) {
			return false;
		}
		
		String timestampStr = matcher.group(1);
		String severity = matcher.group(2);
		
		try {
			LocalDateTime logTime = LocalDateTime.parse(timestampStr, LOG_TIMESTAMP_FORMAT);
			ZonedDateTime logTimeZoned = logTime.atZone(since.getZone());
			
			if (logTimeZoned.isBefore(since) || logTimeZoned.isAfter(until)) {
				return false;
			}
			
			int logSeverityLevel = SEVERITY_LEVELS.getOrDefault(severity, 0);
			if (logSeverityLevel < minSeverityLevel) {
				return false;
			}
			
			if (regex != null && !regex.matcher(logLine).find()) {
				return false;
			}
			
			return true;
		} catch (DateTimeParseException e) {
			logger.debug("Failed to parse timestamp in log line: {}", logLine);
			return false;
		}
	}
}
