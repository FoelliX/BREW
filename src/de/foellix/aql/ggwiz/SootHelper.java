package de.foellix.aql.ggwiz;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.foellix.aql.ggwiz.testcaseselector.Testcase;
import soot.Scene;
import soot.SootClass;
import soot.options.Options;

public class SootHelper {
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
			// reinitialize
			soot.G.reset();

			// Setup Soot
			Options.v().set_src_prec(Options.src_prec_apk);
			Options.v().set_allow_phantom_refs(true);
			final List<String> excludedPacks = new ArrayList<String>(
					Arrays.asList("com.google.*, java.*, sun.misc.*, android.*, org.apache.*, soot.*, javax.servlet.*"
							.replaceAll(" ", "").split(",")));
			Options.v().set_exclude(excludedPacks);
			Options.v().set_no_bodies_for_excluded(true);

			// Input: .apk and Android library
			Options.v().set_process_dir(Collections.singletonList(testcase.getApk().getAbsolutePath()));
			Options.v().set_force_android_jar("data/android.jar");

			Scene.v().loadNecessaryClasses();

			result = filter(Scene.v().getApplicationClasses());
			this.storage.put(testcase, result);
		}
		return result;
	}

	private Collection<SootClass> filter(Collection<SootClass> classes) {
		final Collection<SootClass> remove = new ArrayList<>();
		for (final SootClass c : classes) {
			if (c.isConcrete()) {
				if (c.getName().endsWith(".R") || c.getName().endsWith(".BuildConfig")) {
					remove.add(c);
				}
			}
		}
		classes.removeAll(remove);
		return classes;
	}
}
