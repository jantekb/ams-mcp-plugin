package io.antmedia.plugin;

import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.plugin.api.IStreamListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

@Component(value="plugin.mcp")
public class McpPlugin implements ApplicationContextAware, IStreamListener {

	private final static String VERSION = "1.0.0";

	private static Logger logger = LoggerFactory.getLogger(McpPlugin.class);

	private ApplicationContext applicationContext;

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
		AntMediaApplicationAdapter app = getApplication();
		logger.info("MCP Plugin {} is starting in {}", VERSION, app.getName());
	}

	public AntMediaApplicationAdapter getApplication() {
		return (AntMediaApplicationAdapter) applicationContext.getBean(AntMediaApplicationAdapter.BEAN_NAME);
	}

	public String getVersion() {
		return VERSION;
	}

	@Override
	public void joinedTheRoom(String s, String s1) {

	}

	@Override
	public void leftTheRoom(String s, String s1) {

	}
}