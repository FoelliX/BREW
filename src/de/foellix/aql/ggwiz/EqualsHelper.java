package de.foellix.aql.ggwiz;

import de.foellix.aql.Log;
import de.foellix.aql.datastructure.Flow;
import de.foellix.aql.datastructure.KeywordsAndConstants;
import de.foellix.aql.datastructure.Reference;
import de.foellix.aql.helper.Helper;

public class EqualsHelper extends de.foellix.aql.helper.EqualsHelper {
	public static boolean equalsIgnoreApp(final Flow path1, final Flow path2) {
		Reference path1From = null;
		Reference path1To = null;
		Reference path2From = null;
		Reference path2To = null;
		for (final Reference ref : path1.getReference()) {
			if (ref.getType().equals(KeywordsAndConstants.REFERENCE_TYPE_FROM)) {
				path1From = ref;
			} else if (ref.getType().equals(KeywordsAndConstants.REFERENCE_TYPE_TO)) {
				path1To = ref;
			}
		}
		for (final Reference ref : path2.getReference()) {
			if (ref.getType().equals(KeywordsAndConstants.REFERENCE_TYPE_FROM)) {
				path2From = ref;
			} else if (ref.getType().equals(KeywordsAndConstants.REFERENCE_TYPE_TO)) {
				path2To = ref;
			}
		}

		if (equalsIgnoreApp(path1From, path2From) && equalsIgnoreApp(path1To, path2To)) {
			return true;
		} else {
			return false;
		}
	}

	public static boolean equalsIgnoreApp(final Reference reference1, final Reference reference2) {
		if (equals(reference1, reference2)) {
			return true;
		} else {
			final String warning = "\n" + Helper.toString(reference1) + "\nis only equal to\n"
					+ Helper.toString(reference2)
					+ "\nif the App source is ignored. Reason might be an app merge for example.";
			if (reference1.getClassname().equals(reference2.getClassname())) {
				if (reference1.getMethod().equals(reference2.getMethod())) {
					if (equals(reference1.getStatement(), reference2.getStatement())) {
						return true;
					} else {
						if (reference1.getStatement().getStatementgeneric() == null
								|| reference1.getStatement().getStatementgeneric().equals("")
								|| reference2.getStatement().getStatementgeneric() == null
								|| reference2.getStatement().getStatementgeneric().equals("")) {
							if (Helper.cut(reference1.getStatement().getStatementfull(), "<", ">")
									.equals(Helper.cut(reference2.getStatement().getStatementfull(), "<", ">"))) {
								Log.warning(warning + "\nStatements are equal only on generic level:\n"
										+ reference1.getStatement().getStatementfull() + "\n"
										+ reference2.getStatement().getStatementfull());
								return true;
							}
						} else if (reference1.getStatement().getStatementgeneric()
								.equals(reference2.getStatement().getStatementgeneric())) {
							Log.warning(warning + "\nStatements are equal only on generic level:\n"
									+ reference1.getStatement().getStatementfull() + "\n"
									+ reference2.getStatement().getStatementfull());
							return true;
						}
					}
				}
			}
		}
		return false;
	}
}
