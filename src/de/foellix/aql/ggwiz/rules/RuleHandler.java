package de.foellix.aql.ggwiz.rules;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import de.foellix.aql.Log;

public class RuleHandler {
	private static final File DEFAULT_RULES_FILE = new File("rules.xml");

	public static final String REPLACE_QUERY = "%QUERY%";
	public static final String REPLACE_FILE = "%FILE_#%";
	public static final String REPLACE_FEATURE = "%FEATURE_#%";
	public static final String REPLACE_FEATURES = "%FEATURES%";

	private File rulesFile;
	private Rules activeRules;

	private static RuleHandler instance = new RuleHandler();

	private RuleHandler() {
		setRules(DEFAULT_RULES_FILE);
	}

	public static RuleHandler getInstance() {
		return instance;
	}

	public void setRules(File rulesFile) {
		this.rulesFile = rulesFile;
		init();
	}

	private void init() {
		if (this.rulesFile.exists()) {
			try {
				final Reader reader = new FileReader(this.rulesFile);
				final JAXBContext jaxbContext = JAXBContext.newInstance(Rules.class);
				final Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
				this.activeRules = (Rules) jaxbUnmarshaller.unmarshal(reader);
				reader.close();
			} catch (final JAXBException | IOException e) {
				Log.error("Cannot parse XML document currently. It must be corrupted (" + e.getClass().getSimpleName()
						+ "): " + e.getMessage());
			}
		} else {
			this.activeRules = new Rules();
		}
	}

	public List<Rule> getActiveRules() {
		return this.activeRules.getRule();
	}
}