package de.foellix.aql.ggwiz.tpfpselector;

import java.io.File;

import de.foellix.aql.datastructure.handler.AnswerHandler;
import de.foellix.aql.ggwiz.Data;

public class Exporter {
	private final File folder;

	public Exporter(File folder) {
		this.folder = folder;
	}

	public void export() {
		// Export AQL-Answers
		for (final TPFP tpfp : Data.getInstance().getTPFPs()) {
			AnswerHandler.createXML(tpfp.toAnswer(), new File(this.folder.getAbsolutePath() + "/" + tpfp.getId() + "_"
					+ (tpfp.isTruepositive() ? "tp" : "fp") + "_" + getName(tpfp) + ".xml"));
		}
	}

	private String getName(TPFP tpfp) {
		String name = tpfp.getTestcase();
		int from = name.lastIndexOf("/");
		if (from < 0) {
			from = name.lastIndexOf("\\\\");
		}
		final int to = name.lastIndexOf(".apk");
		if (from > 0 && to > 0) {
			name = name.substring(from + 1, to);
		} else {
			name = name.replaceAll("[^A-Za-z0-9]", "");
		}
		return name;
	}
}