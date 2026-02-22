# Ant Media Server MCP Plugin

A Model Context Protocol (MCP) server plugin for Ant Media Server that enables AI assistants to interact with and manage Ant Media Server instances.

## Overview

This plugin exposes Ant Media Server functionality through the Model Context Protocol, 
allowing AI assistants to monitor, manage, and control streaming operations.

**MCP Protocol Version:** 2025-06-18  

## Build

```bash
mvn clean package
```

This produces `target/ams-mcp.jar`

## Installation

1. Build the plugin using Maven
2. Copy `target/ams-mcp.jar` to your Ant Media Server plugins directory
3. Restart Ant Media Server
4. The plugin will be automatically loaded, you should see this in the logs:

```
2026-02-22 23:54:14,596 [Loader:/LiveApp] INFO  io.antmedia.plugin.McpPlugin - MCP Plugin 1.0.0 is starting in LiveApp
```

5. Configure the MCP connection to http://yourserver:5080/LiveApp/rest/mcp

## Available MCP Tools

### Broadcast Management

#### `list_broadcasts`
Get alphabetically sorted list of broadcast stream names.

**Parameters:** None

#### `stop_broadcast`
Stop a broadcast stream.

**Parameters:**
- `streamId` (string, required) - ID of the stream to stop broadcasting

#### `delete_broadcast`
Delete a broadcast stream.

**Parameters:**
- `streamId` (string, required) - ID of the stream to delete

### Recording Management

#### `start_recording`
Start recording a broadcast stream.

**Parameters:**
- `streamId` (string, required) - ID of the stream to start recording
- `recordingType` (string, optional) - Recording format: `mp4` or `webm`. Defaults to `mp4` if not specified.

#### `stop_recording`
Stop recording a broadcast stream.

**Parameters:**
- `streamId` (string, required) - ID of the stream to stop recording

### Server Information

#### `get_cpu_info`
Get CPU information from the server.

**Parameters:** None

#### `get_gpu_info`
Get GPU information from the server.

**Parameters:** None

#### `get_application_names`
Get list of application names.

**Parameters:** None

### Log Management

#### `query_logs`
Query Ant Media Server logs with time range and severity filtering.

**Parameters:**
- `since` (string, required) - ISO 8601 timestamp for start of log range (e.g., `2026-02-12T23:00:00Z`)
- `until` (string, required) - ISO 8601 timestamp for end of log range (e.g., `2026-02-12T23:59:59Z`)
- `minSeverity` (string, required) - Minimum severity level: `TRACE`, `DEBUG`, `INFO`, or `ERROR`
- `filterRegex` (string, optional) - Regex pattern to filter log messages

## Available Resources

### `log://ant-media-server.log`
Last 512,000 characters of the main server log.

### `log://antmedia-error.log`
Last 512,000 characters of ERROR level logs.
