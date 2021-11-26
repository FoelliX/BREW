package de.foellix.aql.brew.taintbench;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.foellix.aql.Log;
import de.foellix.aql.brew.Data;
import de.foellix.aql.brew.GUI;
import de.foellix.aql.brew.sourceandsinkselector.SourceOrSink;
import de.foellix.aql.datastructure.Reference;
import de.foellix.aql.helper.HashHelper;
import de.foellix.aql.helper.Helper;
import de.foellix.aql.helper.KeywordsAndConstantsHelper;

public class SourceOrSinkMapper {
	protected static final String SEPARATOR = "###";
	protected static final String COMMENT = "#";

	private AmbiguityFixesHandler afh;

	private boolean interactive = true;
	private Map<String, Set<SourceOrSink>> sourceOrSinkMap;

	public SourceOrSinkMapper() {
		// Load AmbiguityFixes
		this.afh = new AmbiguityFixesHandler();
		this.afh.loadAmbiguityFixes();
	}

	protected SourceOrSink findSourceOrSink(SourceOrSink input, boolean searchingSource) {
		// 1st search
		if (this.sourceOrSinkMap == null) {
			this.sourceOrSinkMap = new HashMap<>();
			for (final SourceOrSink output : Data.getInstance().getSourceAndSinkList()) {
				final String key = toKey(output.getReference());
				if (this.sourceOrSinkMap.containsKey(key)) {
					this.sourceOrSinkMap.get(key).add(output);
				} else {
					final Set<SourceOrSink> temp = new HashSet<>();
					temp.add(output);
					this.sourceOrSinkMap.put(key, temp);
				}
			}
		}

		Set<SourceOrSink> candidates = new HashSet<>();
		final int max = Helper.countStringOccurences(input.getReference().getClassname(), ".");
		String key;
		for (int i = 0; i < max; i++) {
			key = toKey(input.getReference(), i);
			if (this.sourceOrSinkMap.containsKey(key)) {
				candidates.addAll(this.sourceOrSinkMap.get(key));
				break;
			}
		}
		key = toKey(input.getReference(), 0);
		candidates = filterByMethodName(input, candidates);
		candidates = filterByTargetName(input, candidates);

		if (candidates.isEmpty()) {
			// 2nd search
			for (final SourceOrSink output : Data.getInstance().getSourceAndSinkList()) {
				if (TaintBenchHelper.compare(input, output, true, true)) {
					if (!candidates.contains(output)) {
						candidates.add(output);
					}
				}
			}
		}
		candidates = filterByMethodName(input, candidates);
		candidates = filterByTargetName(input, candidates);

		// Fix ambiguity 1
		if (this.interactive && candidates.size() > 1) {
			candidates = fixAmbiguity(key, candidates, input, true, searchingSource);
		}

		// Correct candidate not found -> Recover
		if (candidates.isEmpty()) {
			for (final SourceOrSink output : Data.getInstance().getSourceAndSinkList()) {
				if (TaintBenchHelper.compare(input, output, false, true)) {
					if (!candidates.contains(output)) {
						candidates.add(output);
					}
				}
			}
			if (candidates.isEmpty()) {
				for (final SourceOrSink output : Data.getInstance().getSourceAndSinkList()) {
					if (TaintBenchHelper.compare(input, output, false, false)) {
						if (!candidates.contains(output)) {
							candidates.add(output);
						}
					}
				}
			}

			// Fix ambiguity 2
			final Set<SourceOrSink> newCandidates = filterByTargetName(input, candidates);
			if (!newCandidates.isEmpty()) {
				candidates = newCandidates;
			}
			if (this.interactive && candidates.size() > 1) {
				candidates = fixAmbiguity(key, candidates, input, false, searchingSource);
			}
		}

		// Return
		if (candidates.size() > 1) {
			final StringBuilder sb = new StringBuilder();
			for (final SourceOrSink candidate : candidates) {
				sb.append("\n" + candidate.getReference().getStatement().getStatementfull());
			}
			Log.msg("Ambiguity introduced by " + candidates.size() + " candidates for \""
					+ input.getReference().getStatement().getStatementfull() + "\". Connecting with ambiguous elements:"
					+ sb.toString(), Log.NORMAL);
			return connectSourcesOrSinks(candidates);
		} else if (candidates.isEmpty()) {
			Log.error("Could not find reference reffered by TaintBench:\n"
					+ Helper.toString(input.getReference(), "\n->"));
			return input;
		}
		return candidates.iterator().next();
	}

	private Set<SourceOrSink> filterByTargetName(SourceOrSink input, Set<SourceOrSink> candidates) {
		final Set<SourceOrSink> candidatesCopy = new HashSet<>(candidates);

		// Consider targetName
		if (!input.getReference().getStatement().getStatementfull().contains(TaintBenchLoader.TARGET_NOT_SPECIFIED)) {
			final Set<SourceOrSink> toRemove = new HashSet<>();
			String needle = input.getReference().getStatement().getStatementfull();
			needle = needle.substring(needle.lastIndexOf(" -> ") + 4);
			needle = needle.substring(0, needle.lastIndexOf(" ["));

			for (final SourceOrSink candidate : candidatesCopy) {
				if (!candidate.getReference().getStatement().getStatementfull().contains(" " + needle + "(")) {
					toRemove.add(candidate);
				}
			}
			if (candidatesCopy.size() == toRemove.size()) {
				toRemove.clear();
				for (final SourceOrSink candidate : candidatesCopy) {
					if (!candidate.getReference().getStatement().getStatementfull().contains(needle)
							|| (!candidate.getReference().getStatement().getStatementfull()
									.contains(KeywordsAndConstantsHelper.CONSTRUCTOR_NAME)
									&& !candidate.getReference().getStatement().getStatementfull()
											.contains(KeywordsAndConstantsHelper.STATIC_CONSTRUCTOR_NAME))) {
						toRemove.add(candidate);
					}
				}
			}
			candidatesCopy.removeAll(toRemove);
		}
		return candidatesCopy;
	}

	private Set<SourceOrSink> filterByMethodName(SourceOrSink input, Set<SourceOrSink> candidates) {
		final Set<SourceOrSink> candidatesCopy = new HashSet<>(candidates);

		// Consider method name
		final Set<SourceOrSink> toRemove = new HashSet<>();
		for (final SourceOrSink candidate : candidatesCopy) {
			if (!TaintBenchHelper.compareMethod(input.getReference().getMethod(), candidate.getReference().getMethod(),
					false)) {
				toRemove.add(candidate);
			}
		}
		candidatesCopy.removeAll(toRemove);

		return candidatesCopy;
	}

	private String toKey(Reference reference) {
		return toKey(reference, 0);
	}

	private String toKey(Reference reference, int remove) {
		final StringBuilder keyBuilder = new StringBuilder(Helper.toRAW(reference.getApp()) + ":");
		String classname;
		if (reference.getClassname().contains("$")) {
			classname = reference.getClassname().substring(0, reference.getClassname().indexOf('$'));
		} else if (reference.getClassname().substring(reference.getClassname().lastIndexOf('.') + 1)
				.startsWith("AnonymousClass")) {
			classname = reference.getClassname().substring(0, reference.getClassname().lastIndexOf('.'));
		} else {
			classname = reference.getClassname();
		}
		for (int i = 0; i < remove; i++) {
			classname = classname.substring(0, classname.lastIndexOf('.'));
		}
		keyBuilder.append(classname);
		return HashHelper.md5Hash(keyBuilder.toString());
	}

	private Set<SourceOrSink> fixAmbiguity(String key, Set<SourceOrSink> candidates, SourceOrSink input,
			boolean checkLineNumber, boolean searchingSource) {
		final Set<SourceOrSink> toKeep = new HashSet<>();

		// No definition check
		if (input.getReference().getStatement().getStatementfull().contains(TaintBenchLoader.TARGET_NOT_SPECIFIED)) {
			Log.warning("TargetName and TargetNo was not specified for: " + (searchingSource ? "SOURCE" : "SINK")
					+ ":\n\t" + input.getReference().getStatement().getStatementfull() + "\n\t(Line "
					+ input.getReference().getStatement().getLinenumber() + ": " + input.getReference().getClassname()
					+ " - " + input.getReference().getMethod() + " - in app \""
					+ input.getReference().getApp().getFile() + "\".)");
			return candidates;
		}

		// Line-number check
		if (checkLineNumber) {
			final int inputLineNo = Helper.getLineNumberSafe(input.getReference());
			for (final SourceOrSink sourceOrSink : candidates) {
				final int candidateLineNo = Helper.getLineNumberSafe(sourceOrSink.getReference());
				if (candidateLineNo == inputLineNo) {
					toKeep.add(sourceOrSink);
				}
			}
		}

		// Otherwise: interactive
		if (toKeep.size() != 1 && this.interactive) {
			if (toKeep.size() > 1) {
				candidates = new HashSet<>(toKeep);
				toKeep.clear();
			}

			final List<String> loaded = this.afh.getLoadedSelections()
					.get(Helper.getLineNumberSafe(input.getReference()) + ":" + key
							+ (searchingSource ? ":SOURCE" : ":SINK"));
			final StringBuilder selectionBuilder = new StringBuilder();

			StringBuilder sb = new StringBuilder("\nFound multiple candidates for the following "
					+ (searchingSource ? "SOURCE" : "SINK") + " (Checking line numbers: "
					+ (checkLineNumber ? "yes" : "no") + "):\n\t"
					+ input.getReference().getStatement().getStatementfull() + "\n\t(Line "
					+ input.getReference().getStatement().getLinenumber() + ": " + input.getReference().getClassname()
					+ " - " + input.getReference().getMethod() + " - in app \""
					+ input.getReference().getApp().getFile() + "\".)\nWhich one(s) do you want to keep?");
			int counter = 0;
			final Map<Integer, SourceOrSink> intMap = new HashMap<>();
			final List<SourceOrSink> candidatesSorted = new ArrayList<>(candidates);
			Collections.sort(candidatesSorted, (c1, c2) -> {
				return Helper.getLineNumberSafe(c1.getReference()) - Helper.getLineNumberSafe(c2.getReference());
			});
			for (final SourceOrSink candidate : candidatesSorted) {
				intMap.put(counter, candidate);
				sb.append("\n\t" + (counter + 1) + ") Line " + Helper.getLineNumberSafe(candidate.getReference()) + ": "
						+ candidate.getReference().getStatement().getStatementfull());
				if (loaded != null && loaded
						.contains((checkLineNumber ? candidate.getReference().getStatement().getLinenumber() + ":" : "")
								+ candidate.getReference().getStatement().getStatementfull())) {
					if (selectionBuilder.length() == 0) {
						selectionBuilder.append((counter + 1));
					} else {
						selectionBuilder.append("," + (counter + 1));
					}
				}
				counter++;
			}
			sb.append("\nSelection (e.g. \"1,3\"): ");

			boolean newFix = false;
			final String selection;
			if (selectionBuilder.length() > 0) {
				selection = selectionBuilder.toString();
			} else {
				Log.msg(sb.toString(), Log.NORMAL);
				selection = GUI.getScanner().nextLine();
				newFix = true;
			}
			sb = new StringBuilder(COMMENT + " " + input.getReference().getStatement().getStatementfull() + "\n"
					+ Helper.getLineNumberSafe(input.getReference()) + ":" + key
					+ (searchingSource ? ":SOURCE" : ":SINK"));
			if (!selection.isEmpty() && !selection.equals("0")) {
				for (final String selectionPart : selection.replaceAll(" ", "").split(",")) {
					final int i = Integer.parseInt(selectionPart) - 1;
					toKeep.add(intMap.get(i));
					sb.append(SEPARATOR + intMap.get(i).getReference().getStatement().getLinenumber() + ":"
							+ intMap.get(i).getReference().getStatement().getStatementfull());
				}
				if (newFix) {
					this.afh.saveAmbiguityFixes(sb.toString());
				}
			}
		}

		return toKeep;
	}

	private SourceOrSink connectSourcesOrSinks(Set<SourceOrSink> candidates) {
		final StringBuilder ids = new StringBuilder();
		for (final SourceOrSink candidate : candidates) {
			ids.append(candidate.getId() + ", ");
		}
		ids.setLength(ids.length() - 2);
		final SourceOrSink selected = candidates.iterator().next();
		selected.setCombine(ids.toString());
		return selected;
	}
}