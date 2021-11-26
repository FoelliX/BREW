package de.foellix.aql.brew.taintbench;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.foellix.aql.Log;

public class AmbiguityFixesHandler {
	private static final File AMBIGUITY_FIX_FILE = new File("data/ambiguityFixes.txt");

	private final Map<String, List<String>> loadedSelections;

	public AmbiguityFixesHandler() {
		this.loadedSelections = new HashMap<>();
	}

	protected void saveAmbiguityFixes(String entry) {
		loadAmbiguityFixLine(entry.substring(entry.indexOf("\n") + 1));
		try (FileWriter fw = new FileWriter(AMBIGUITY_FIX_FILE, true)) {
			fw.write(entry + "\n");
		} catch (final IOException e) {
			Log.warning("Could not store ambiguity resolvings in " + AMBIGUITY_FIX_FILE.getAbsolutePath()
					+ Log.getExceptionAppendix(e));
		}
	}

	protected void loadAmbiguityFixes() {
		if (AMBIGUITY_FIX_FILE.exists()) {
			try {
				for (final String line : Files.readAllLines(AMBIGUITY_FIX_FILE.toPath())) {
					if (line.startsWith(SourceOrSinkMapper.COMMENT)) {
						continue;
					}
					loadAmbiguityFixLine(line);
				}
			} catch (final Exception e) {
				Log.error("While reading ambiguity fixes in " + AMBIGUITY_FIX_FILE.getAbsolutePath()
						+ Log.getExceptionAppendix(e));
			}
		}
	}

	private void loadAmbiguityFixLine(String line) {
		final String key = line.substring(0, line.indexOf(SourceOrSinkMapper.SEPARATOR));
		final List<String> value = Arrays.asList(
				line.substring(line.indexOf(SourceOrSinkMapper.SEPARATOR) + SourceOrSinkMapper.SEPARATOR.length())
						.split(SourceOrSinkMapper.SEPARATOR));
		this.loadedSelections.put(key, value);
	}

	public Map<String, List<String>> getLoadedSelections() {
		return this.loadedSelections;
	}
}
