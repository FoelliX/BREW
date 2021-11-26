package de.foellix.aql.brew.taintbench;

import java.util.ArrayList;
import java.util.List;

import de.foellix.aql.Log;
import de.foellix.aql.brew.sourceandsinkselector.SourceOrSink;
import de.foellix.aql.brew.taintbench.datastructure.IR;
import de.foellix.aql.brew.taintbench.datastructure.Sink;
import de.foellix.aql.brew.taintbench.datastructure.Source;
import de.foellix.aql.datastructure.Statement;
import de.foellix.aql.helper.Helper;
import de.foellix.aql.helper.KeywordsAndConstantsHelper;

public class TaintBenchHelper {
	public static final String JIMPLE = "Jimple";

	public static String getJimpleStmt(List<IR> irs) {
		if (irs != null) {
			for (final IR ir : irs) {
				if (ir.getType().equalsIgnoreCase(JIMPLE)) {
					return ir.getIRstatement();
				}
			}
		}
		return null;
	}

	protected static Statement createStatement(String taintBenchStatement, int taintBenchLinenumber) {
		final Statement stmt = new Statement();
		stmt.setStatementfull(taintBenchStatement);
		stmt.setLinenumber(taintBenchLinenumber);

		// Candidates
		final List<String> candidates = new ArrayList<>();
		String temp = taintBenchStatement;
		while (temp.contains("(")) {
			String temp2 = temp.substring(0, temp.indexOf('('));
			temp2 = temp.substring(temp2.lastIndexOf('.') + 1, temp.indexOf('('));
			if (!temp2.isEmpty()) {
				temp2 = temp2 + "(" + numberOfParameters(temp.substring(temp.indexOf('('))) + ")";
				if (!candidates.contains(temp2)) {
					candidates.add(temp2);
				}
			}
			temp = temp.substring(temp.indexOf('(') + 1);
		}
		temp = taintBenchStatement;
		while (temp.contains("(")) {
			String temp2 = temp.substring(0, temp.indexOf('('));
			temp2 = temp.substring(temp2.lastIndexOf(' ') + 1, temp.indexOf('('));
			if (!temp2.isEmpty()) {
				temp2 = temp2 + "(" + numberOfParameters(temp.substring(temp.indexOf('('))) + ")";
				if (!candidates.contains(temp2) && !temp2.contains(".")) {
					candidates.add(temp2);
				}
			}
			temp = temp.substring(temp.indexOf('(') + 1);
		}

		int add = -1;
		for (final String candidate : candidates) {
			if (Character.isUpperCase(candidate.charAt(0))) {
				add = Integer.valueOf(Helper.cut(candidate, "(", ")")).intValue();
				break;
			}
		}
		if (add > -1) {
			candidates.add(KeywordsAndConstantsHelper.CONSTRUCTOR_NAME + "(" + add + ")");
			candidates.add(KeywordsAndConstantsHelper.STATIC_CONSTRUCTOR_NAME + "(" + add + ")");
		}

		final StringBuilder sb = new StringBuilder();
		for (final String candidate : candidates) {
			if (sb.length() != 0) {
				sb.append("#");
			}
			sb.append(candidate);
		}
		stmt.setStatementgeneric(sb.toString());

		return stmt;
	}

	private static int numberOfParameters(String str) {
		if (str.startsWith("()")) {
			return 0;
		}

		int counter = 0;
		int position = 0;
		for (final Character c : str.toCharArray()) {
			position++;
			if (c == '(') {
				counter++;
			} else if (c == ')') {
				counter--;
				if (counter == 0) {
					break;
				}
			}
		}
		String temp = str.substring(1, position);
		temp = temp.replaceAll("\\([^\\)]*\\)", "").replaceAll("\\{[^\\}]*\\}", "{}").replaceAll("\"[^\"]*\"", "\"\"");

		final int number = (temp.length() - temp.replace(",", "").length()) + 1;

		return number;
	}

	protected static void writeBack(Object sourceOrSink, String jimpleStmt) {
		List<IR> irs;
		if (sourceOrSink instanceof Source) {
			irs = ((Source) sourceOrSink).getIRs();
		} else if (sourceOrSink instanceof Sink) {
			irs = ((Sink) sourceOrSink).getIRs();
		} else {
			Log.error("Trying to write back an object which is neither a Source nor a Sink.");
			return;
		}
		if (irs != null) {
			for (final IR ir : irs) {
				if (ir.getType().equalsIgnoreCase(TaintBenchHelper.JIMPLE)) {
					ir.setIRstatement(jimpleStmt);
					return;
				}
			}
		} else {
			irs = new ArrayList<>();
		}
		final IR ir = new IR();
		ir.setType(TaintBenchHelper.JIMPLE);
		ir.setIRstatement(jimpleStmt);
		irs.add(ir);
		if (sourceOrSink instanceof Source) {
			((Source) sourceOrSink).setIRs(irs);
		} else if (sourceOrSink instanceof Sink) {
			((Sink) sourceOrSink).setIRs(irs);
		}
	}

	public static boolean compare(SourceOrSink input, SourceOrSink output, boolean checkStatement,
			boolean checkReturnType) {
		if (compareClass(input.getReference().getClassname(), output.getReference().getClassname())) {
			if (compareMethod(input.getReference().getMethod(), output.getReference().getMethod(), checkReturnType)) {
				if (!checkStatement || (compareStatement(input.getReference().getStatement().getStatementgeneric(),
						output.getReference().getStatement().getStatementgeneric()))) {
					return true;
				}
			}
		}
		return false;
	}

	public static boolean compareClass(String tbClass, String jimpleClass) {
		return jimpleClass.equals(tbClass)
				|| (jimpleClass.contains("$") && jimpleClass.substring(0, jimpleClass.indexOf('$'))
						.equals(tbClass.substring(0, tbClass.lastIndexOf('.'))))
				|| (jimpleClass.contains("$") && jimpleClass.substring(0, jimpleClass.indexOf('$')).equals(tbClass));
	}

	public static boolean compareMethod(String tbMethod, String jimpleMethod, boolean checkReturnType) {
		tbMethod = tbMethod
				.replaceAll(KeywordsAndConstantsHelper.CONSTRUCTOR_NAME,
						"#" + KeywordsAndConstantsHelper.CONSTRUCTOR_NAME.substring(1,
								KeywordsAndConstantsHelper.CONSTRUCTOR_NAME.length() - 1) + "#")
				.replaceAll(KeywordsAndConstantsHelper.STATIC_CONSTRUCTOR_NAME,
						"#" + KeywordsAndConstantsHelper.STATIC_CONSTRUCTOR_NAME.substring(1,
								KeywordsAndConstantsHelper.STATIC_CONSTRUCTOR_NAME.length() - 1) + "#");
		jimpleMethod = jimpleMethod
				.replaceAll(KeywordsAndConstantsHelper.CONSTRUCTOR_NAME,
						"#" + KeywordsAndConstantsHelper.CONSTRUCTOR_NAME.substring(1,
								KeywordsAndConstantsHelper.CONSTRUCTOR_NAME.length() - 1) + "#")
				.replaceAll(KeywordsAndConstantsHelper.STATIC_CONSTRUCTOR_NAME,
						"#" + KeywordsAndConstantsHelper.STATIC_CONSTRUCTOR_NAME.substring(1,
								KeywordsAndConstantsHelper.STATIC_CONSTRUCTOR_NAME.length() - 1) + "#");

		// Handle TaintBench
		// E.g.: private static void getIt(String filePath, Context context)
		String temp = tbMethod.substring(0, tbMethod.indexOf('('));
		final String tbMethodName = temp.substring(temp.lastIndexOf(' ') + 1, temp.length());
		temp = temp.substring(0, temp.lastIndexOf(' '));
		String tbReturnType = null;
		if (checkReturnType) {
			tbReturnType = temp.substring(temp.lastIndexOf(' ') + 1);
			if (tbReturnType.contains(".")) {
				tbReturnType = tbReturnType.substring(tbReturnType.lastIndexOf('.') + 1);
			}
			if (tbReturnType.contains("<")) {
				tbReturnType = tbReturnType.substring(0, tbReturnType.indexOf('<'));
			}
		}
		temp = tbMethod.substring(tbMethod.indexOf('(') + 1, tbMethod.indexOf(')'));
		if (temp.contains("<")) {
			temp = temp.replaceAll("\\<[^>]*>", "");
		}
		final String[] tbParams = temp.replaceAll(", ", ",").split(",");
		for (int i = 0; i < tbParams.length; i++) {
			if (tbParams[i].contains("...")) {
				tbParams[i] = tbParams[i].replace("...", "[]");
			}
			if (tbParams[i].startsWith("final ")) {
				tbParams[i] = tbParams[i].substring(6);
			}
			if (tbParams[i].contains(" ")) {
				tbParams[i] = tbParams[i].substring(0, tbParams[i].indexOf(' '));
			}
			if (tbParams[i].contains(".")) {
				tbParams[i] = tbParams[i].substring(tbParams[i].lastIndexOf('.') + 1);
			}
		}

		// Handle Jimple
		// E.g.: <ca.ji.no.method10.BaiduUtils: void getIt(java.lang.String,android.content.Context)>
		temp = jimpleMethod.substring(0, jimpleMethod.indexOf('('));
		final String jimpleMethodName = temp.substring(temp.lastIndexOf(' ') + 1, temp.length());
		temp = temp.substring(0, temp.lastIndexOf(' '));
		String jimpleReturnType = null;
		if (checkReturnType) {
			jimpleReturnType = temp.substring(temp.lastIndexOf(' ') + 1);
			if (jimpleReturnType.contains(".")) {
				jimpleReturnType = jimpleReturnType.substring(jimpleReturnType.lastIndexOf('.') + 1);
			}
		}
		temp = jimpleMethod.substring(jimpleMethod.indexOf('(') + 1, jimpleMethod.indexOf(')'));
		final String[] jimpleParams = temp.split(",");
		for (int i = 0; i < jimpleParams.length; i++) {
			if (jimpleParams[i].contains(".")) {
				jimpleParams[i] = jimpleParams[i].substring(jimpleParams[i].lastIndexOf('.') + 1);
			}
		}

		// Compare
		if (tbMethodName.equals(jimpleMethodName) && (!checkReturnType || tbReturnType.equals(jimpleReturnType))) {
			if (tbParams.length != jimpleParams.length) {
				return false;
			}
			for (int i = 0; i < tbParams.length; i++) {
				if (!tbParams[i].equals(jimpleParams[i])) {
					return false;
				}
			}
			return true;
		} else {
			return false;
		}
	}

	public static boolean compareStatement(String tbStmt, String jimpleStmt) {
		if (tbStmt.isEmpty()) {
			// Keeping this for now (Source: "String packagename = packageInfo.packageName;" breaks everything otherwise).
			return false;
		}

		String temp = jimpleStmt.substring(0, jimpleStmt.indexOf('('));
		final String methodName = Helper.cut(temp, " ", Helper.OCCURENCE_LAST);
		temp = Helper.cutFromFirstToLast(jimpleStmt, "<", ">");
		temp = Helper.cutFromFirstToLast(temp, "(", ")");
		final int numberOfParams = (temp.isEmpty() ? 0 : temp.length() - temp.replace(",", "").length() + 1);

		final List<String> candidates = new ArrayList<>();
		for (final String candidate : tbStmt.split("#")) {
			final String candidateName = candidate.substring(0, min(candidate.indexOf('('), candidate.length()));
			final int candidateNumber = Integer.valueOf(Helper.cut(candidate, "(", ")"));

			if (methodName.equals(candidateName) && numberOfParams == candidateNumber) {
				candidates.add(candidate);
			}
		}
		if (!candidates.isEmpty()) {
			if (candidates.size() >= 2) {
				Log.warning("Multiple candidates for statement match found! Ambiguity unavoidable for statement: "
						+ tbStmt);
			}
			return true;
		}

		return false;
	}

	private static int min(int x, int y) {
		if (x < 0 && y < 0) {
			return -1;
		} else if (x < 0) {
			return y;
		} else if (y < 0) {
			return x;
		} else {
			return Math.min(x, y);
		}
	}
}