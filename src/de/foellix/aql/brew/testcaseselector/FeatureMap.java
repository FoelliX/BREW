package de.foellix.aql.brew.testcaseselector;

import java.util.HashMap;

import de.foellix.aql.helper.Helper;

public class FeatureMap<K, V> extends HashMap<K, V> {
	private static final long serialVersionUID = -3795387749726764473L;

	@Override
	public V get(Object statement) {
		if (statement instanceof String) {
			final String statementStr = "<" + Helper.cutFromFirstToLast((String) statement, "<", ">") + ">";

			for (final K key : this.keySet()) {
				final String originalPattern = (String) key;
				String pattern = originalPattern.replaceAll("\\(", "\\\\(").replaceAll("\\)", "\\\\)");
				pattern = pattern.replaceAll("\\[", "\\\\[").replaceAll("\\]", "\\\\]");
				pattern = pattern.replace("?", ".?").replace("*", ".*?");
				if (statementStr.matches(pattern)) {
					return super.get(originalPattern);
				}
			}
		}
		return null;
	}
}
