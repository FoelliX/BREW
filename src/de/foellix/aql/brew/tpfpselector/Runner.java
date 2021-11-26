package de.foellix.aql.brew.tpfpselector;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import de.foellix.aql.Log;
import de.foellix.aql.brew.BREW;
import de.foellix.aql.brew.Data;
import de.foellix.aql.brew.DatabaseHandler;
import de.foellix.aql.brew.MenuBar;
import de.foellix.aql.brew.Statistics;
import de.foellix.aql.brew.config.Config;
import de.foellix.aql.brew.sourceandsinkselector.SourceOrSink;
import de.foellix.aql.brew.testcaseselector.Testcase;
import de.foellix.aql.datastructure.Answer;
import de.foellix.aql.datastructure.Flow;
import de.foellix.aql.datastructure.Flows;
import de.foellix.aql.datastructure.Reference;
import de.foellix.aql.datastructure.handler.QueryHandler;
import de.foellix.aql.datastructure.query.Query;
import de.foellix.aql.helper.ConnectHelper;
import de.foellix.aql.helper.EqualsHelper;
import de.foellix.aql.helper.EqualsOptions;
import de.foellix.aql.helper.Helper;
import de.foellix.aql.system.AQLSystem;
import de.foellix.aql.system.IAnswerAvailable;
import de.foellix.aql.system.task.Task;
import de.foellix.aql.system.task.gui.TaskTreeViewer;
import de.foellix.aql.transformations.QueryTransformer;
import de.foellix.aql.ui.gui.FontAwesome;
import javafx.application.Platform;

public class Runner implements IAnswerAvailable {
	private static final int ABORTED = -1;
	private static final String FEATURES = "%FEATURES%";

	public static boolean considerLinenumbers = true;
	public static boolean considerApp = false;

	private final TPFPSelector parent;

	private AQLSystem aqlSystem;
	private Map<String, AbortedQuery> abortedQueries;
	private Map<String, PreviousQuery> previousQueries;

	private TPFP current;
	private String currentQuery;
	private String currentQueryKey;

	private boolean running = false;
	private int max = 0;
	private int counter = 0;

	public Runner() {
		this.current = null;
		this.parent = null;
		init();
	}

	public Runner(TPFP tpfp) {
		this.current = tpfp;
		this.parent = null;
		init();
	}

	public Runner(TPFPSelector parent) {
		this.parent = parent;
		init();
	}

	private void init() {
		this.abortedQueries = new HashMap<>();
		this.previousQueries = new HashMap<>();

		if (this.parent != null || this.current == null) {
			this.aqlSystem = new AQLSystem(BREW.getOptions());
			this.aqlSystem.getAnswerReceivers().add(this);
		}
	}

	public void runAll(List<TPFP> tpfps) {
		if (!BREW.getNoGui()) {
			Platform.runLater(() -> {
				MenuBar.playBtn.setDisable(true);
				MenuBar.playBtn.setText(FontAwesome.ICON_HOUR_GLASS);
				FontAwesome.getInstance().setRed(MenuBar.playBtn);
			});
		}

		this.abortedQueries.clear();
		this.previousQueries.clear();
		Log.msg("Runner started", Log.NORMAL);
		DatabaseHandler.getInstance().setTime();

		new Thread(() -> {
			this.counter = 0;
			for (final TPFP tpfp : tpfps) {
				this.running = true;
				this.max = tpfps.size();
				this.counter++;
				this.run(tpfp);

				int waitInterval = 50;
				while (this.running) {
					try {
						Thread.sleep(waitInterval);
						if (waitInterval < 1000) {
							waitInterval++;
						}
					} catch (final InterruptedException e) {
						Log.error("Runner thread interrupted.");
					}
				}
			}
			if (this.parent == null) {
				Log.msg("Storing data (" + Data.getInstance().getCurrentSaveFile().getAbsolutePath() + ")... ",
						Log.NORMAL, false);
				Data.store();
				Log.setPrefixEnabled(false);
				Log.msg("done!", Log.NORMAL);
				Log.setPrefixEnabled(true);
			}
			Log.msg("Runner finished", Log.NORMAL);

			if (!BREW.getNoGui()) {
				Platform.runLater(() -> {
					MenuBar.playBtn.setDisable(false);
					MenuBar.playBtn.setText(FontAwesome.ICON_PLAY);
					FontAwesome.getInstance().setGreen(MenuBar.playBtn);
				});
			}
		}).start();
	}

	private void run(TPFP tpfp) {
		tpfp.reset();
		this.current = tpfp;

		// Attributes
		if (!tpfp.getAttributes().isEmpty()) {
			this.aqlSystem.setGlobalVariables(tpfp.getAttributes());
		} else {
			this.aqlSystem.getGlobalVariables().clear();
		}

		// Get query
		this.currentQuery = getQuery();
		this.currentQueryKey = getQuery(true);
		if (!tpfp.getAttributes().isEmpty()) {
			this.currentQueryKey = Helper.replaceCustomVariables(this.currentQueryKey, tpfp.getAttributes());
		}
		if (this.currentQuery == null) {
			Log.warning("Could not get query for benchmark case: " + tpfp.toString());
			return;
		}

		// Run
		Log.msg("*** Starting Benchmark Case " + this.counter + "/" + this.max + " *** (ID: " + tpfp.getId() + ")",
				Log.NORMAL);
		if (!this.abortedQueries.containsKey(this.currentQueryKey)) {
			if (!this.previousQueries.containsKey(this.currentQueryKey)) {
				final PreviousQuery pq = new PreviousQuery(java.lang.System.currentTimeMillis());
				tpfp.setStarted(pq.getStarted());
				this.previousQueries.put(this.currentQueryKey, pq);
				this.aqlSystem.query(this.currentQuery);
			} else {
				Log.msg("(Same query as before! Reusing previous answer.)", Log.NORMAL);
				tpfp.setStarted(this.previousQueries.get(this.currentQueryKey).getStarted());
				answerAvailable(this.previousQueries.get(this.currentQueryKey).getAnswer(),
						this.previousQueries.get(this.currentQueryKey).getStatus());
			}
		} else {
			Log.msg("(Execution upon query was aborted before! Not re-executing.)", Log.NORMAL);
			tpfp.setStarted(this.abortedQueries.get(this.currentQueryKey).getStarted());
			answerAvailable(new Answer(), Task.STATUS_EXECUTION_UNKNOWN);
		}
	}

	public String getQuery() {
		return getQuery(false);
	}

	public String getQuery(boolean viewOnly) {
		// Get IDs
		final Set<Integer> appIDs = new HashSet<>();
		appIDs.addAll(getAppIDs(Data.getInstance().getMapR().get(this.current.getFrom()).getCombine()));

		// Get apps
		final Set<File> apps = new HashSet<>();
		for (final Integer id : appIDs) {
			final File app = findApp(id);
			if (app != null) {
				apps.add(app);
			} else {
				Log.warning("Could not find Testcase associated with ID: " + id);
			}
		}
		final File fromApp = new File(this.current.getFrom().getReference().getApp().getFile());
		final File toApp = new File(this.current.getTo().getReference().getApp().getFile());
		apps.add(fromApp);
		apps.add(toApp);

		// Bridge detection
		boolean bridgeDetected = false;
		boolean sameStartAndEnd = false;
		boolean overrideApp = false;
		final boolean simpleMode = Config.getInstance().get(Config.QUERY_WITH_BRIDGE_APPS)
				.equals(Config.QUERY_WITH_BRIDGE_APPS_REPLACE);
		if ((Boolean.parseBoolean(Config.getInstance().get(Config.QUERY_WITH_BRIDGE_APPS)) || simpleMode)
				&& (apps.size() > 2
						|| (apps.size() == 2 && EqualsHelper.equals(this.current.getFrom().getReference().getApp(),
								this.current.getTo().getReference().getApp())))) {
			bridgeDetected = true;
			if (EqualsHelper.equals(this.current.getFrom().getReference().getApp(),
					this.current.getTo().getReference().getApp())) {
				sameStartAndEnd = true;
				if (simpleMode && apps.size() == 2
						&& Config.getInstance().get(Config.REFERENCE_LEVEL).equals(Config.REFERENCE_LEVEL_APP)) {
					overrideApp = true;
				}
			}

		}

		// Initial query
		final StringBuilder qb = new StringBuilder();
		if (bridgeDetected) {
			final Set<File> appsWithoutFromAndTo = new HashSet<>(apps);
			appsWithoutFromAndTo.remove(fromApp);
			appsWithoutFromAndTo.remove(toApp);

			if (overrideApp && appsWithoutFromAndTo.size() == 1) {
				final Reference overrideAppRef = new Reference();
				overrideAppRef.setApp(Helper.createApp(appsWithoutFromAndTo.iterator().next()));
				qb.append("Flows FROM " + getReference(this.current.getFrom()) + " TO "
						+ Helper.toString(overrideAppRef) + " " + FEATURES + "?");
			} else {
				qb.append(Config.getInstance().get(Config.CONNECT_OPERATOR) + " [ ");

				// Back and forth
				qb.append("Flows FROM " + getReference(this.current.getFrom()) + " TO "
						+ getReference(this.current.getTo()) + " " + FEATURES + "?, ");
				if (!sameStartAndEnd) {
					qb.append("Flows FROM " + getReference(this.current.getTo()) + " TO "
							+ getReference(this.current.getFrom()) + " " + FEATURES + "?, ");
				}

				// Connect to bridges
				for (final File app : appsWithoutFromAndTo) {
					qb.append("Flows FROM " + getReference(this.current.getFrom()) + " TO " + getReference(app) + " "
							+ FEATURES + "?, ");
					if (!sameStartAndEnd) {
						qb.append("Flows FROM " + getReference(this.current.getTo()) + " TO " + getReference(app) + " "
								+ FEATURES + "?, ");
					}
					qb.append("Flows FROM " + getReference(app) + " TO " + getReference(this.current.getFrom()) + " "
							+ FEATURES + "?, ");
					if (!sameStartAndEnd) {
						qb.append("Flows FROM " + getReference(app) + " TO " + getReference(this.current.getTo()) + " "
								+ FEATURES + "?, ");
					}
				}

				// Connect bridges
				for (final File app1 : appsWithoutFromAndTo) {
					for (final File app2 : appsWithoutFromAndTo) {
						if (app1 == app2) {
							continue;
						}
						qb.append("Flows FROM " + getReference(app1) + " TO " + getReference(app2) + " " + FEATURES
								+ "?, ");
						qb.append("Flows FROM " + getReference(app2) + " TO " + getReference(app1) + " " + FEATURES
								+ "?, ");
					}
				}

				qb.setLength(qb.length() - 2);
				qb.append(" ] ?");
			}
		} else if (apps.size() == 2) {
			qb.append("Flows FROM " + getReference(this.current.getFrom()) + " TO " + getReference(this.current.getTo())
					+ " " + FEATURES + "?");
		} else {
			qb.append("Flows ");
			if (!Boolean.parseBoolean(Config.getInstance().get(Config.ALWAYS_USE_FROM_TO))
					&& Config.getInstance().get(Config.REFERENCE_LEVEL).equals(Config.REFERENCE_LEVEL_APP)) {
				qb.append("IN " + getReference(this.current.getFrom()));
			} else {
				qb.append("FROM " + getReference(this.current.getFrom()) + " TO " + getReference(this.current.getTo()));
			}
			qb.append(" " + FEATURES + "?");
		}

		// Apply features
		final Set<String> features = new TreeSet<>();
		if (!Config.getInstance().get(Config.ALWAYS_FEATURE_FLOWS).isEmpty()) {
			for (final String feature : Config.getInstance().get(Config.ALWAYS_FEATURE_FLOWS).replace(" ", "")
					.split(",")) {
				features.add(feature);
			}
		}
		List<Testcase> testcases = Data.getInstance().getTestcaseList();
		if (testcases == null || testcases.isEmpty()) {
			testcases = Data.getInstance().getTestcases();
		}
		for (final Testcase testcase : testcases) {
			if (appIDs.contains(testcase.getId())) {
				if (testcase.getFeatures() != null) {
					features.addAll(testcase.getFeatures());
				}
			}
		}
		if (Data.getInstance().getMapR().containsKey(this.current.getFrom())
				&& Data.getInstance().getMapR().get(this.current.getFrom()).getFeatures() != null) {
			features.addAll(Data.getInstance().getMapR().get(this.current.getFrom()).getFeatures());
		}
		if (Data.getInstance().getMapR().containsKey(this.current.getTo())
				&& Data.getInstance().getMapR().get(this.current.getTo()).getFeatures() != null) {
			features.addAll(Data.getInstance().getMapR().get(this.current.getTo()).getFeatures());
		}
		String featureStr;
		if (!features.isEmpty()) {
			final StringBuilder fb = new StringBuilder("FEATURING ");
			for (final String feature : features) {
				fb.append("'" + feature + "', ");
			}
			fb.setLength(fb.length() - 2);
			fb.append(" ");
			featureStr = fb.toString();
		} else {
			featureStr = "";
		}

		// Compose query
		String query = qb.toString().replace(FEATURES, featureStr);

		// Apply transformations
		if (viewOnly) {
			final Query queryObj = QueryHandler.parseQuery(query);
			QueryTransformer.transform(queryObj, queryObj.getQuestions().iterator().next());
			query = queryObj.toString();
		}

		// Return final query
		return query;
	}

	private String getReference(SourceOrSink fromOrTo) {
		final int lvl = Config.getInstance().referenceLevelToNumber(Config.getInstance().get(Config.REFERENCE_LEVEL));
		final Reference fromOrToRef = fromOrTo.getDeepest().getReference();
		final Reference ref = new Reference();
		if (lvl >= Config.REFERENCE_LEVEL_STATEMENT_VALUE) {
			ref.setStatement(fromOrToRef.getStatement());
			ref.getStatement().setLinenumber(Helper.getLineNumberSafe(fromOrToRef.getStatement()));
		}
		if (lvl >= Config.REFERENCE_LEVEL_METHOD_VALUE) {
			ref.setMethod(fromOrToRef.getMethod());
		}
		if (lvl >= Config.REFERENCE_LEVEL_CLASS_VALUE) {
			ref.setClassname(fromOrToRef.getClassname());
		}
		ref.setApp(fromOrToRef.getApp());
		return Helper.toString(ref);
	}

	private String getReference(File app) {
		final Reference ref = new Reference();
		ref.setApp(Helper.createApp(app));
		return Helper.toString(ref);
	}

	private Set<Integer> getAppIDs(String combine) {
		final Set<Integer> ids = new HashSet<>();
		if (combine != null && !combine.isEmpty()) {
			for (final String idStr : combine.replace(" ", "").split(",")) {
				ids.add(Integer.valueOf(idStr).intValue());
			}
		}
		return ids;
	}

	private File findApp(int id) {
		for (final Testcase testcase : Data.getInstance().getTestcases()) {
			if (testcase.getId() == id) {
				return testcase.getApk();
			}
		}
		return null;
	}

	@Override
	public void answerAvailable(Object answer, int status) {
		if (answer instanceof Answer) {
			final Answer castedAnswer = (Answer) answer;

			if (this.previousQueries.get(this.currentQueryKey).getAnswer() != castedAnswer) {
				this.previousQueries.get(this.currentQueryKey).setAnswer(castedAnswer);
				this.previousQueries.get(this.currentQueryKey).setStatus(status);
				this.previousQueries.get(this.currentQueryKey).setEnded(java.lang.System.currentTimeMillis());
			}
			if (!this.abortedQueries.containsKey(this.currentQueryKey)) {
				this.current.setEnded(this.previousQueries.get(this.currentQueryKey).getEnded());
			} else {
				this.current.setEnded(this.abortedQueries.get(this.currentQueryKey).getEnded());
			}

			if (status != Task.STATUS_EXECUTION_SUCCESSFUL) {
				this.current.setAborted(true);
				if (!this.abortedQueries.containsKey(this.currentQueryKey)) {
					this.abortedQueries.put(this.currentQueryKey,
							new AbortedQuery(this.current.getStarted(), this.current.getEnded()));
				}
			}
			if (contains(castedAnswer, this.current.toAnswer())) {
				this.current.setStatus((this.current.isTruepositive() ? TPFP.SUCCESSFUL : TPFP.FAILED));
			} else {
				this.current.setStatus((this.current.isTruepositive() ? TPFP.FAILED : TPFP.SUCCESSFUL));
			}
			if (this.parent != null) {
				this.parent.refresh();
			} else {
				Statistics.getInstance().refresh();
			}
			final StringBuilder sb = new StringBuilder("*** Finished Benchmark Case " + this.counter + "/" + this.max
					+ " *** (ID: " + this.current.getId() + ") \"" + this.current.getCase() + "\": ");
			if (this.current.getStatus() == TPFP.SUCCESSFUL) {
				sb.append("Successful!");
			} else if (this.current.getStatus() == TPFP.FAILED) {
				sb.append("Failed!");
			}
			if (BREW.getNoGui()) {
				sb.append(" (" + Statistics.getInstance().getStatisticsAsString(false) + ")");
			}
			Log.msg(sb.toString(), Log.NORMAL);
			this.current.setActualAnswer(castedAnswer);
			if (!BREW.getNoGui() && this.aqlSystem.getOptions().getDrawGraphs()) {
				this.current.setTaskTreeSnapshot(TaskTreeViewer.getTaskTreeSnapshot());
			}

			// Insert to database (if accessible)
			if (DatabaseHandler.getInstance().databaseAvailable()) {
				if (!DatabaseHandler.getInstance().databaseInsert(this.current, getQuery(), castedAnswer,
						(this.current.isAborted() ? ABORTED : this.current.getStatus()))) {
					Log.warning("Database not accessible!");
				}
			}

			this.running = false;
		}
	}

	/**
	 * One flow of contains has to be found in answer.
	 *
	 * @param actualAnswer
	 * @param expectedAnswer
	 */
	public static boolean contains(final Answer actualAnswer, final Answer expectedAnswer) {
		final EqualsOptions options = new EqualsOptions();
		options.setOption(EqualsOptions.IGNORE_APP, !considerApp);
		options.setOption(EqualsOptions.CONSIDER_LINENUMBER, considerLinenumbers);

		if (expectedAnswer.getFlows() != null && expectedAnswer.getFlows().getFlow() != null) {
			if (actualAnswer != null && actualAnswer.getFlows() != null && actualAnswer.getFlows().getFlow() != null) {
				if (contains(expectedAnswer.getFlows().getFlow(), actualAnswer.getFlows().getFlow(), options)) {
					return true;
				} else {
					// Compute transitive hull an retry
					final Flows hull = new Flows();
					hull.getFlow().addAll(actualAnswer.getFlows().getFlow());
					ConnectHelper.computeTransitiveHull(hull, options);
					if (contains(expectedAnswer.getFlows().getFlow(), hull.getFlow(), options)) {
						return true;
					}
				}
			}

			return false;
		} else {
			return true;
		}
	}

	private static boolean contains(List<Flow> expectedFlows, List<Flow> actualFlows, EqualsOptions options) {
		for (final Flow needle : expectedFlows) {
			for (final Flow flow : actualFlows) {
				if (EqualsHelper.equals(needle, flow, options)) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Get all flows contained
	 *
	 * @param answer
	 * @param contains
	 *
	 * @return a List of contained flows
	 */
	public static List<Flow> getContained(final Answer answer, final Answer contains) {
		final List<Flow> flows = new ArrayList<>();

		final EqualsOptions options = new EqualsOptions();
		options.setOption(EqualsOptions.IGNORE_APP, true);

		if (contains.getFlows() != null && contains.getFlows().getFlow() != null) {
			if (answer.getFlows() != null && answer.getFlows().getFlow() != null) {
				for (final Flow flow : answer.getFlows().getFlow()) {
					for (final Flow needle : contains.getFlows().getFlow()) {
						if (EqualsHelper.equals(needle, flow, options)) {
							flows.add(flow);
							break;
						}
					}
				}
			}
		}

		return flows;
	}
}