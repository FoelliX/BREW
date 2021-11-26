package de.foellix.aql.brew;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.foellix.aql.Log;
import de.foellix.aql.LogSilencer;
import de.foellix.aql.brew.config.Config;
import de.foellix.aql.brew.testcaseselector.Testcase;
import de.foellix.aql.config.ConfigHandler;
import de.foellix.aql.helper.FileHelper;
import de.foellix.aql.helper.Helper;
import soot.Scene;
import soot.SootClass;
import soot.options.Options;

public class SootHelper {
	public static final String ANDROID_JAR_URL = "https://github.com/Sable/android-platforms/raw/master/android-30/android.jar";
	public static final File ANDROID_JAR_FORCED_FILE = new File("data/android.jar");

	private final Map<Testcase, Collection<SootClass>> storage;

	private static SootHelper instance = new SootHelper();

	private SootHelper() {
		this.storage = new HashMap<>();
	}

	public static SootHelper getInstance() {
		return instance;
	}

	public Collection<SootClass> extract(Testcase testcase) {
		Collection<SootClass> result = this.storage.get(testcase);
		if (result == null) {
			try (final LogSilencer s = new LogSilencer()) {
				// reinitialize
				soot.G.reset();

				// Setup Soot
				Options.v().set_src_prec(Options.src_prec_apk);
				Options.v().set_allow_phantom_refs(true);
				if (Boolean.parseBoolean(Config.getInstance().get(Config.EXCLUDE_WHILE_PARSING))) {
					Options.v().set_exclude(getDefaultExcludes(true));
				} else {
					Options.v().set_include_all(true);
				}
				Options.v().set_keep_line_number(true);
				Options.v().set_process_multiple_dex(true);

				// Input: .apk and Android library
				Options.v().set_process_dir(Collections.singletonList(testcase.getApk().getAbsolutePath()));
				if (ConfigHandler.getInstance().getConfig() != null
						&& ConfigHandler.getInstance().getConfig().getAndroidPlatforms() != null
						&& !ConfigHandler.getInstance().getConfig().getAndroidPlatforms().isEmpty()) {
					Options.v().set_android_jars(ConfigHandler.getInstance().getConfig().getAndroidPlatforms());
				} else {
					if (!ANDROID_JAR_FORCED_FILE.exists()) {
						FileHelper.downloadFile(ANDROID_JAR_URL, ANDROID_JAR_FORCED_FILE, "android.jar");
					}
					Options.v().set_force_android_jar(ANDROID_JAR_FORCED_FILE.getAbsolutePath());
				}

				Scene.v().loadNecessaryClasses();

				result = filter(Scene.v().getApplicationClasses());
				this.storage.put(testcase, result);
			}
		}
		return result;
	}

	private Collection<SootClass> filter(Collection<SootClass> classes) {
		final Collection<SootClass> remove = new ArrayList<>();
		for (final SootClass c : classes) {
			if (c.isConcrete()) {
				if (c.getName().endsWith(".R") || c.getName().endsWith(".BuildConfig")) {
					remove.add(c);
				} else if (!BREW.getIncludeLibraries() && c.isLibraryClass()) {
					remove.add(c);
					Log.msg("Excluding library class: " + c.getPackageName()
							+ " (to avoid exclusion use \"-l\"/\"-libs\"/\"-libraries\" launch parameter.)", Log.DEBUG);
				}
			} else {
				remove.add(c);
			}
		}
		classes.removeAll(remove);
		return classes;
	}

	public static Collection<SootClass> filterDefaultExcludes(Collection<SootClass> classes) {
		final Collection<SootClass> filteredClasses = new ArrayList<>();
		for (final SootClass sc : classes) {
			if (!isExcluded(sc)) {
				filteredClasses.add(sc);
			}
		}
		return filteredClasses;
	}

	private static boolean isExcluded(SootClass sc) {
		for (final String exclude : getDefaultExcludes(false)) {
			if (sc.toString().startsWith(exclude)) {
				return true;
			}
		}
		return false;
	}

	private static String getDefaultExcludesAsString() {
		return Helper.replaceDoubleSpaces(Config.getInstance().get(Config.DEFAULT_EXCLUDES)).replace(", ", ",")
				.replace(".*", ".");
	}

	private static List<String> getDefaultExcludes(boolean withAsterisk) {
		final List<String> defaultExcludes = new ArrayList<>();
		for (final String item : getDefaultExcludesAsString().split(",")) {
			defaultExcludes.add(item + (withAsterisk ? "*" : ""));
		}
		return defaultExcludes;
	}
}