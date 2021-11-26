package de.foellix.aql.brew.tpfpselector;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.foellix.aql.Log;
import de.foellix.aql.datastructure.Reference;
import de.foellix.aql.helper.Helper;

public class QueryAdapter {
	public static final int SLICER_MODE_NONE = -1;
	public static final int SLICER_MODE_FROM = 1;
	public static final int SLICER_MODE_TO = 2;
	public static final int SLICER_MODE_FROM_TO = 3;

	private static final String REPLACEMENT_NEEDLE = "%REPLACEMENT_NEEDLE_ID%";
	private static final String[] KEYS = new String[] { "IN", "FROM", "TO" };

	public static String issueSlicer(String query, Reference from, Reference to, int mode) {
		if (mode == SLICER_MODE_NONE) {
			return query;
		}
		final Map<String, String> map = new HashMap<>();
		int counter = 0;
		final int groupId = 4; // match the app part, which is no. 4

		for (final String key : KEYS) {
			String and;
			if (mode == SLICER_MODE_FROM) {
				and = "|IntentSinks";
				if (key == KEYS[2]) {
					continue;
				}
			} else if (mode == SLICER_MODE_TO) {
				and = "|IntentSources";
				if (key == KEYS[1]) {
					continue;
				}
			} else {
				and = "|IntentSinks|IntentSources";
			}
			final Matcher m = Pattern.compile("(Flows" + and + ")(.*?) " + key + " (.*?)App\\((.*?)\\)").matcher(query);
			int offset = 0;
			while (m.find()) {
				if (!m.group(groupId).startsWith("$")) {
					final String needle = REPLACEMENT_NEEDLE.replace("ID", String.valueOf(++counter));
					query = query.substring(0, m.start(groupId) + offset) + needle
							+ query.substring(m.end(groupId) + offset);
					offset += (needle.length() - m.group(groupId).length());
					map.put(needle, key + ":" + m.group(groupId));
				}
			}
		}

		for (final String key : map.keySet()) {
			final String type = map.get(key).substring(0, map.get(key).indexOf(":"));
			final String file = map.get(key).substring(map.get(key).indexOf(":") + 2, map.get(key).length() - 1);

			if (type.equals("IN") && file.equals(from.getApp().getFile())) {
				if (!file.equals(to.getApp().getFile())) {
					Log.warning("Ignoring different second app (" + to.getApp().getFile() + ") while slicing!");
					if (mode == SLICER_MODE_FROM_TO) {
						query = query.replace(key, "{ Slice FROM " + Helper.toString(from, true) + " ! }");
					} else if (mode == SLICER_MODE_FROM) {
						query = query.replace(key, "{ Slice FROM " + Helper.toString(from, true) + " ! }");
					} else if (mode == SLICER_MODE_TO) {
						query = query.replace(key, "{ Slice TO " + Helper.toString(from, true) + " ! }");
					}
				} else {
					if (mode == SLICER_MODE_FROM_TO) {
						query = query.replace(key, "{ Slice FROM " + Helper.toString(from, true) + " TO "
								+ Helper.toString(to, true) + " ! }");
					} else if (mode == SLICER_MODE_FROM) {
						query = query.replace(key, "{ Slice FROM " + Helper.toString(from, true) + " ! }");
					} else if (mode == SLICER_MODE_TO) {
						query = query.replace(key, "{ Slice TO " + Helper.toString(to, true) + " ! }");
					}
				}
			} else if (type.equals("FROM") && file.equals(from.getApp().getFile())
					&& (mode == SLICER_MODE_FROM || mode == SLICER_MODE_FROM_TO)) {
				query = query.replace(key, "{ Slice FROM " + Helper.toString(from, true) + " ! }");
			} else if (type.equals("TO") && file.equals(to.getApp().getFile())
					&& (mode == SLICER_MODE_TO || mode == SLICER_MODE_FROM_TO)) {
				query = query.replace(key, "{ Slice TO " + Helper.toString(to, true) + " ! }");
			}
		}

		return query;
	}
}