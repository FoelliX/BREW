package de.foellix.aql.ggwiz.testcaseselector;

import java.io.File;
import java.io.Serializable;
import java.util.List;

import de.foellix.aql.ggwiz.Data;

public class Testcase implements Serializable {
	private static final long serialVersionUID = -6913306086830564879L;

	private static final String placeholder = "###backslash###";

	private File apk;

	private int id;
	private boolean active;
	private String combine;
	private List<String> features;

	public Testcase(File apk) {
		this.apk = apk;

		this.id = Data.getInstance().getTestcaseId();

		this.active = true;
		this.combine = "";
	}

	public void setId(int id) {
		this.id = id;
	}

	public File getApk() {
		return this.apk;
	}

	public void setApk(File apk) {
		this.apk = apk;
	}

	public final String getName() {
		String remove = new File("./").getAbsolutePath();
		remove = remove.substring(0, remove.length() - 1).replace("\\", placeholder);
		final String apkStr = this.apk.toString().replace("\\", placeholder);
		return apkStr.replace(remove, "").replace(placeholder, "/");
	}

	public boolean isActive() {
		return this.active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public String getCombine() {
		return this.combine;
	}

	public void setCombine(String combine) {
		this.combine = combine;
	}

	public int getId() {
		return this.id;
	}

	public List<String> getFeatures() {
		return this.features;
	}

	public String getFeaturesAsString() {
		if (this.features == null || this.features.size() == 0) {
			return "";
		} else {
			final StringBuilder sb = new StringBuilder();
			for (final String feature : this.features) {
				sb.append(feature + ", ");
			}
			return sb.toString().substring(0, sb.toString().length() - 2);
		}
	}

	public void setFeatures(List<String> features) {
		this.features = features;
	}
}
