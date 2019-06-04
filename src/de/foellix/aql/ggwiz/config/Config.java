package de.foellix.aql.ggwiz.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import de.foellix.aql.Log;

public class Config {
	public static final String PROPERTIES_FILE = "config.properties";

	public static final String COMBINER_KEYWORD = "combinerKeyword";
	public static final String CONNECT_OPERATOR = "connectOperator";
	public static final String ALWAYS_PREPROCESS = "alwaysPreprocess";
	public static final String ALWAYS_FEATURE_FLOWS = "alwaysFeatureFlows";
	public static final String REPLACE_KEYWORDS = "replaceKeywords";
	public static final String REPLACE_FEATURES = "replaceFeatures";
	public static final String ASSUME_EXTERNAL_COMBINER = "assumeExternalCombiner";

	private final Properties properties;
	private Map<String, String> keywordMap;
	private Map<String, String> featureMap;

	private static Config instance = new Config();

	private Config() {
		this.properties = new Properties();

		final File propertiesFile = new File(PROPERTIES_FILE);
		if (!propertiesFile.exists()) {
			Log.warning("Could not find " + propertiesFile.getAbsolutePath() + ". Continuing with default config.");
			this.properties.setProperty(COMBINER_KEYWORD, "COMBINE");
			this.properties.setProperty(CONNECT_OPERATOR, "CONNECT");
			this.properties.setProperty(ALWAYS_PREPROCESS, "");
			this.properties.setProperty(ALWAYS_FEATURE_FLOWS, "");
			this.properties.setProperty(REPLACE_KEYWORDS, "(Reflection, DEOBFUSCATE)");
			this.properties.setProperty(REPLACE_FEATURES, "");
			this.properties.setProperty(ASSUME_EXTERNAL_COMBINER, "false");
			store();
		} else {
			try (InputStream input = new FileInputStream(PROPERTIES_FILE)) {
				this.properties.load(input);
			} catch (final IOException e) {
				Log.error("Could not read " + PROPERTIES_FILE + ". (" + e.getMessage() + ")");
			}
		}

		loadMaps();
	}

	private void loadMaps() {
		this.keywordMap = new HashMap<>();
		this.featureMap = new HashMap<>();

		String temp = this.properties.getProperty(REPLACE_KEYWORDS).replaceAll("\\(", "").replaceAll(" ", "")
				.replaceAll("\n", "").replaceAll("\r", "");
		loadMap(this.keywordMap, temp);

		temp = this.properties.getProperty(REPLACE_FEATURES).replaceAll("\\(", "").replaceAll(" ", "")
				.replaceAll("\n", "").replaceAll("\r", "");
		loadMap(this.featureMap, temp);
	}

	private void loadMap(Map<String, String> map, String string) {
		if (string != null && !string.equals("")) {
			for (final String item : string.split("\\),")) {
				final String key = item.substring(0, item.indexOf(","));
				final String value = item.substring(item.indexOf(",") + 1).replaceAll("\\)", "");
				map.put(key, value);
			}
		}
	}

	public static Config getInstance() {
		return instance;
	}

	public Properties getProperties() {
		return this.properties;
	}

	public String getKeyword(String keyword) {
		if (this.keywordMap.containsKey(keyword)) {
			return this.keywordMap.get(keyword);
		} else {
			return keyword;
		}
	}

	public String getFeature(String feature) {
		if (this.featureMap.containsKey(feature)) {
			return this.featureMap.get(feature);
		} else {
			return feature;
		}
	}

	public void store() {
		try (OutputStream output = new FileOutputStream(PROPERTIES_FILE)) {
			this.properties.store(output, null);
		} catch (final Exception e) {
			Log.error("Could not write " + PROPERTIES_FILE + ". (" + e.getMessage() + ")");
		}
	}
}
