package de.foellix.aql.brew.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

import de.foellix.aql.Log;
import de.foellix.aql.system.defaulttools.operators.DefaultOperator;

public class Config {
	public static final String REFERENCE_LEVEL_APP = "App";
	public static final String REFERENCE_LEVEL_CLASS = "Class";
	public static final String REFERENCE_LEVEL_METHOD = "Method";
	public static final String REFERENCE_LEVEL_STATEMENT = "Statement";
	public static final int REFERENCE_LEVEL_APP_VALUE = 0;
	public static final int REFERENCE_LEVEL_CLASS_VALUE = 1;
	public static final int REFERENCE_LEVEL_METHOD_VALUE = 2;
	public static final int REFERENCE_LEVEL_STATEMENT_VALUE = 3;
	public static final String QUERY_WITH_BRIDGE_APPS_REPLACE = "replace";

	public static final String PROPERTIES_FILE = "config.properties";

	public static final String CONNECT_OPERATOR = "connectOperator";
	public static final String ALWAYS_FEATURE_FLOWS = "alwaysFeatureFlows";
	public static final String AUTOMATIC_RELOCATION_DIR = "automaticRelocationDirectory";
	public static final String AUTOMATIC_TAINTBENCH_RELOCATION_DIR = "automaticTaintBenchRelocationDirectory";
	public static final String SAVE_AFTER_AUTO_RELOCATION = "saveAfterAutomaticRelocation";
	public static final String REFERENCE_LEVEL = "referenceLevel";
	public static final String ALWAYS_USE_FROM_TO = "alwaysUseFromTo";
	public static final String DEFAULT_EXCLUDES = "defaultExcludes";
	public static final String EXCLUDE_WHILE_PARSING = "excludeWhileParsing";
	public static final String INITIAL_OPEN_DIRECTORY = "initialDirectory";
	public static final String RETRY = "retry";
	public static final String QUERY_WITH_BRIDGE_APPS = "queryWithBridgeApps";

	private final Properties properties;

	private static Config instance = new Config();

	private Config() {
		this.properties = new Properties();

		final File propertiesFile = new File(PROPERTIES_FILE);
		if (!propertiesFile.exists()) {
			Log.warning("Could not find " + propertiesFile.getAbsolutePath() + ". Continuing with default config.");
			refresh();
		} else {
			try (InputStream input = new FileInputStream(PROPERTIES_FILE)) {
				this.properties.load(input);
			} catch (final IOException e) {
				Log.error("Could not read " + PROPERTIES_FILE + ". (" + e.getMessage() + ")");
			}
			if (!isComplete()) {
				refresh();
			}
		}
	}

	public static Config getInstance() {
		return instance;
	}

	private boolean isComplete() {
		if (get(CONNECT_OPERATOR) == null) {
			return false;
		} else if (get(ALWAYS_FEATURE_FLOWS) == null) {
			return false;
		} else if (get(AUTOMATIC_RELOCATION_DIR) == null) {
			return false;
		} else if (get(AUTOMATIC_TAINTBENCH_RELOCATION_DIR) == null) {
			return false;
		} else if (get(SAVE_AFTER_AUTO_RELOCATION) == null) {
			return false;
		} else if (get(REFERENCE_LEVEL) == null) {
			return false;
		} else if (get(ALWAYS_USE_FROM_TO) == null) {
			return false;
		} else if (get(DEFAULT_EXCLUDES) == null) {
			return false;
		} else if (get(EXCLUDE_WHILE_PARSING) == null) {
			return false;
		} else if (get(INITIAL_OPEN_DIRECTORY) == null) {
			return false;
		} else if (get(RETRY) == null) {
			return false;
		} else if (get(QUERY_WITH_BRIDGE_APPS) == null) {
			return false;
		} else {
			return true;
		}
	}

	private void refresh() {
		if (get(CONNECT_OPERATOR) == null) {
			this.properties.setProperty(CONNECT_OPERATOR, DefaultOperator.OPERATOR_CONNECT);
		}
		if (get(ALWAYS_FEATURE_FLOWS) == null) {
			this.properties.setProperty(ALWAYS_FEATURE_FLOWS, "");
		}
		if (get(AUTOMATIC_RELOCATION_DIR) == null) {
			this.properties.setProperty(AUTOMATIC_RELOCATION_DIR, "");
		}
		if (get(AUTOMATIC_TAINTBENCH_RELOCATION_DIR) == null) {
			this.properties.setProperty(AUTOMATIC_TAINTBENCH_RELOCATION_DIR, "");
		}
		if (get(SAVE_AFTER_AUTO_RELOCATION) == null) {
			this.properties.setProperty(SAVE_AFTER_AUTO_RELOCATION, "false");
		}
		if (get(REFERENCE_LEVEL) == null) {
			this.properties.setProperty(REFERENCE_LEVEL, REFERENCE_LEVEL_APP);
		}
		if (get(ALWAYS_USE_FROM_TO) == null) {
			this.properties.setProperty(ALWAYS_USE_FROM_TO, "false");
		}
		if (get(DEFAULT_EXCLUDES) == null) {
			this.properties.setProperty(DEFAULT_EXCLUDES, "");
		}
		if (get(EXCLUDE_WHILE_PARSING) == null) {
			this.properties.setProperty(EXCLUDE_WHILE_PARSING, "false");
		}
		if (get(INITIAL_OPEN_DIRECTORY) == null) {
			this.properties.setProperty(INITIAL_OPEN_DIRECTORY, ".");
		}
		if (get(RETRY) == null) {
			this.properties.setProperty(RETRY, "true");
		}
		if (get(QUERY_WITH_BRIDGE_APPS) == null) {
			this.properties.setProperty(QUERY_WITH_BRIDGE_APPS, "false");
		}
		store();
	}

	public String get(String property) {
		return this.properties.getProperty(property);
	}

	public int referenceLevelToNumber(String referenceLevel) {
		if (referenceLevel.equalsIgnoreCase(REFERENCE_LEVEL_STATEMENT)) {
			return REFERENCE_LEVEL_STATEMENT_VALUE;
		} else if (referenceLevel.equalsIgnoreCase(REFERENCE_LEVEL_METHOD)) {
			return REFERENCE_LEVEL_METHOD_VALUE;
		} else if (referenceLevel.equalsIgnoreCase(REFERENCE_LEVEL_CLASS)) {
			return REFERENCE_LEVEL_CLASS_VALUE;
		} else {
			return REFERENCE_LEVEL_APP_VALUE;
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