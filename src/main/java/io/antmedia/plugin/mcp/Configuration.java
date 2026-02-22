package io.antmedia.plugin.mcp;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import com.typesafe.config.ConfigFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Collections;
import java.util.List;

public class Configuration {

	private static final Logger logger = LoggerFactory.getLogger(Configuration.class);

	private Config config;

	public Configuration(String applicationName) {
		this.applicationName = applicationName;

		ConfigFactory.invalidateCaches();
		String appSpecificConfigLocation, generalConfigLocation;

		if(System.getProperty("mcp.config.file") != null) {
			generalConfigLocation = System.getProperty("mcp.config.file");
			appSpecificConfigLocation = System.getProperty("mcp.config.file");
		} else {
			generalConfigLocation = "/usr/local/antmedia/conf/mcp.conf";
			appSpecificConfigLocation = "/usr/local/antmedia/conf/mcp-" + applicationName.toLowerCase() + ".conf";
		}
		if(new File(appSpecificConfigLocation).canRead()) {
			System.setProperty("config.file", appSpecificConfigLocation);
			logger.info("Loading app specific mcp configuration from " + appSpecificConfigLocation);
		} else if (new File(generalConfigLocation).canRead()) {
			System.setProperty("config.file", generalConfigLocation);
			logger.info("Loading mcp configuration from " + generalConfigLocation);
		}
		else {
			logger.error("mcp configuration file is not readable on path: " + generalConfigLocation);
		}
		try {
			this.config = ConfigFactory.load();
		} catch (Exception e) {
			logger.error("Error loading configuration: {}", e);
		}
	}
	private String applicationName;
	
	private String basePath;

	private Object lock = new Object();

	public String getBasePath() {
		return basePath;
	}

	public void setBasePath(String basePath) {
		this.basePath = basePath;
	}

	public Object getLock() {
		return lock;
	}

	public String getApplicationName() {
		return applicationName;
	}


	/**
	 * Get the frame sampling rate - how many frames to skip before processing one
	 * @return frame sampling rate (every Nth frame will be processed)
	 */
	public int getFrameSamplingRate() {
		return config.getInt("mcp.frame.sampling.rate");
	}

	/**
	 * Get AWS region for Rekognition service
	 * @return AWS region
	 */
	public String getAwsRegion() {
		return config.getString("mcp.aws.region");
	}

	/**
	 * Get AWS access key ID
	 * @return AWS access key ID
	 */
	public String getAwsAccessKeyId() {
		return config.getString("mcp.aws.access.key.id");
	}

	/**
	 * Get AWS secret access key
	 * @return AWS secret access key
	 */
	public String getAwsSecretAccessKey() {
		return config.getString("mcp.aws.secret.access.key");
	}

	/**
	 * Get minimum confidence threshold for moderation results
	 * @return confidence threshold (0.0 to 1.0)
	 */
	public double getModerationConfidenceThreshold() {
		return config.getDouble("mcp.confidence.threshold");
	}

	/**
	 * Check if moderation is enabled
	 * @return true if moderation is enabled
	 */
	public boolean isModerationEnabled() {
		return config.getBoolean("mcp.enabled");
	}

	public List<String> getModeratedStreamIds() {
		return getStringListOrSingle("mcp.stream.ids");
	}

	public List<String> getModerationLabels() {
		return getStringListOrSingle("mcp.labels");
	}

	private List<String> getStringListOrSingle(String key) {
		try {
			return config.getStringList(key);
		} catch (ConfigException.WrongType wrongType) {
			if(wrongType.getMessage().contains("has type STRING rather than LIST")) {
				return Collections.singletonList(config.getString(key));
			} else {
				throw wrongType;
			}
		}
	}

	public String getModerationAction() {
		return config.getString("mcp.action");
	}

	public String getNotifyWebhookUrl() {
		return config.getString("mcp.notify.webhook.url");
	}

	public boolean isIncludeImageInNotification() {
		return config.getBoolean("mcp.notify.webhook.include.image");
	}

	public boolean isCaptureImageOnModeration() {
		return config.getBoolean("mcp.capture.image.enabled");
	}

	public String getCaptureImageDirectory() {
		return config.getString("mcp.capture.image.directory");
	}

}
