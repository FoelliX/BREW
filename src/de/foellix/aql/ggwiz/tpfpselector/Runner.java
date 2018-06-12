package de.foellix.aql.ggwiz.tpfpselector;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.mysql.jdbc.jdbc2.optional.MysqlDataSource;

import de.foellix.aql.Log;
import de.foellix.aql.config.ConfigHandler;
import de.foellix.aql.config.Tool;
import de.foellix.aql.datastructure.Answer;
import de.foellix.aql.datastructure.Flow;
import de.foellix.aql.datastructure.KeywordsAndConstants;
import de.foellix.aql.datastructure.handler.AnswerHandler;
import de.foellix.aql.ggwiz.BREW;
import de.foellix.aql.ggwiz.Data;
import de.foellix.aql.ggwiz.EqualsHelper;
import de.foellix.aql.ggwiz.MenuBar;
import de.foellix.aql.ggwiz.sourceandsinkselector.SourceOrSink;
import de.foellix.aql.ggwiz.testcaseselector.Testcase;
import de.foellix.aql.helper.Helper;
import de.foellix.aql.system.IAnswerAvailable;
import de.foellix.aql.system.System;
import de.foellix.aql.ui.gui.FontAwesome;
import javafx.application.Platform;

public class Runner implements IAnswerAvailable {
	private static final String COMBINER_KEYWORD = "COMBINE";
	private static final int ABORTED = -1;

	private static final File DATABASE_PROPERTIES = new File("data/db_config.properties");

	private final TPFPSelector parent;

	private System aqlSystem;
	private boolean combineApks;
	private boolean combinerAvailable;

	private TPFP current;

	private boolean running = false;
	private String time = "0";

	private final MysqlDataSource dataSource = new MysqlDataSource();
	private boolean databaseExport;

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
		if (this.parent != null || this.current == null) {
			this.aqlSystem = new System();
			this.aqlSystem.setStoreAnswers(false);
			if (BREW.getTimeout() > 0) {
				this.aqlSystem.getScheduler().setTimeout(BREW.getTimeout());
			}
			this.aqlSystem.getAnswerReceivers().add(this);
		}

		// InterApp tool available
		this.combineApks = true;
		for (final Tool tool : ConfigHandler.getInstance().getConfig().getTools().getTool()) {
			if (tool.getQuestions().contains(KeywordsAndConstants.MODE_INTER_FLOWS)) {
				this.combineApks = false;
				break;
			}
		}

		// Tools required for transformation available
		boolean tool1 = false;
		boolean tool2 = false;
		boolean tool3 = false;
		for (final Tool tool : ConfigHandler.getInstance().getConfig().getTools().getTool()) {
			if (tool.getQuestions().contains(KeywordsAndConstants.MODE_INTRA_FLOWS)) {
				tool1 = true;
			}
			if (tool.getQuestions().contains(KeywordsAndConstants.MODE_INTENTSINKS)) {
				tool2 = true;
			}
			if (tool.getQuestions().contains(KeywordsAndConstants.MODE_INTENTSOURCES)) {
				tool3 = true;
			}
			if (tool1 && tool2 && tool3) {
				this.combineApks = false;
				break;
			}
		}

		// Combiner available
		this.combinerAvailable = false;
		if (ConfigHandler.getInstance().getConfig().getPreprocessors() != null
				&& ConfigHandler.getInstance().getConfig().getPreprocessors().getTool() != null) {
			for (final Tool tool : ConfigHandler.getInstance().getConfig().getPreprocessors().getTool()) {
				if (tool.getQuestions().contains(COMBINER_KEYWORD)) {
					this.combinerAvailable = true;
					break;
				}
			}
		}

		// Database
		try {
			final java.util.Properties prop = new java.util.Properties();
			final FileInputStream in = new FileInputStream(DATABASE_PROPERTIES);
			prop.load(in);
			in.close();

			this.dataSource.setUser(prop.getProperty("user"));
			if (prop.getProperty("password") != null && !prop.getProperty("password").equals("")
					&& !prop.getProperty("password").equals(" ")) {
				this.dataSource.setPassword(prop.getProperty("password"));
			}
			this.dataSource.setServerName(prop.getProperty("server"));
			this.dataSource.setDatabaseName(prop.getProperty("database"));

			this.databaseExport = true;
		} catch (final IOException e) {
			Log.msg("Could not read database-config: " + DATABASE_PROPERTIES.getAbsolutePath(), Log.DEBUG);
			this.databaseExport = false;
		}
	}

	public void run(TPFP tpfp) {
		if (this.aqlSystem != null) {
			this.current = tpfp;

			final String query = getQuery();
			if (query == null) {
				return;
			} else {
				tpfp.setStarted(java.lang.System.currentTimeMillis());
				Log.msg("Running testcase " + tpfp.getId(), Log.NORMAL);
				this.aqlSystem.query(query);
			}
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

		Log.msg("Runner started", Log.NORMAL);
		this.time = Long.valueOf(java.lang.System.currentTimeMillis() / 1000L).toString();

		new Thread(() -> {
			for (final TPFP tpfp : tpfps) {
				this.running = true;
				this.run(tpfp);

				int waitInterval = 50;
				while (this.running == true) {
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
				noGuiSave();
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

	private void noGuiSave() {
		Log.msg("Storing data", Log.NORMAL);
		Data.getInstance().setCurrentSaveFile(new File("data/data.ser"));
		Data.store();
	}

	public String getQuery() {
		return getQuery(false);
	}

	public String getQuery(boolean viewOnly) {
		String query;
		if ((Data.getInstance().getMapR().get(this.current.getFrom()).getCombine() != null
				&& !Data.getInstance().getMapR().get(this.current.getFrom()).getCombine().equals(""))
				|| (Data.getInstance().getMapR().get(this.current.getTo()).getCombine() != null
						&& !Data.getInstance().getMapR().get(this.current.getTo()).getCombine().equals(""))) {
			if (this.combineApks) {
				if (this.combinerAvailable) {
					// combine
					String appIDs = Data.getInstance().getMapR().get(this.current.getFrom()).getCombine();
					if (appIDs != null && !appIDs.equals("")) {
						appIDs += "," + Data.getInstance().getMapR().get(this.current.getTo()).getCombine();
					} else {
						appIDs = Data.getInstance().getMapR().get(this.current.getTo()).getCombine();
					}
					String apps = "";
					for (final String id : appIDs.replace(" ", "").split(",")) {
						for (final Testcase testcase : Data.getInstance().getTestcases()) {
							if (testcase.getId() == Integer.valueOf(id).intValue()) {
								if (!apps.equals("")) {
									if (!apps.contains(testcase.getApk().getAbsolutePath())) {
										apps += " " + testcase.getApk().getAbsolutePath();
									}
								} else {
									apps = testcase.getApk().getAbsolutePath();
								}
							}
						}
					}
					// from and to
					final File fromApk = new File(this.current.getFrom().getReference().getApp().getFile());
					final File toApk = new File(this.current.getTo().getReference().getApp().getFile());
					if (!apps.contains(fromApk.getAbsolutePath())) {
						if (!apps.equals("")) {
							apps += " " + fromApk.getAbsolutePath();
						} else {
							apps = fromApk.getAbsolutePath();
						}
					}
					if (!apps.contains(toApk.getAbsolutePath())) {
						if (!apps.equals("")) {
							apps += " " + toApk.getAbsolutePath();
						} else {
							apps = toApk.getAbsolutePath();
						}
					}
					// query
					query = "Flows IN App('" + apps + "' | '" + COMBINER_KEYWORD + "'"
							+ getQueryKeywords(getQueryFeaturing()) + ") "
							+ getQueryFeaturingString(getQueryFeaturing()) + "?";
				} else {
					if (!viewOnly) {
						answerAvailable(new Answer(), KeywordsAndConstants.ANSWER_STATUS_UNKNOWN);
					}
					query = null;
				}
			} else {
				// search apps & bridges
				String appIDs = Data.getInstance().getMapR().get(this.current.getFrom()).getCombine();
				if (appIDs != null && !appIDs.equals("")) {
					appIDs += "," + Data.getInstance().getMapR().get(this.current.getTo()).getCombine();
				} else {
					appIDs = Data.getInstance().getMapR().get(this.current.getTo()).getCombine();
				}
				final List<String> apps = new ArrayList<>();
				final File fromApk = new File(this.current.getFrom().getReference().getApp().getFile());
				final File toApk = new File(this.current.getTo().getReference().getApp().getFile());
				apps.add(fromApk.getAbsolutePath());
				if (!fromApk.getAbsolutePath().equals(toApk.getAbsolutePath())) {
					apps.add(toApk.getAbsolutePath());
				}
				for (final String id : appIDs.replace(" ", "").split(",")) {
					for (final Testcase testcase : Data.getInstance().getTestcases()) {
						if (testcase.getId() == Integer.valueOf(id).intValue()) {
							if (!apps.contains(testcase.getApk().getAbsolutePath())) {
								apps.add(testcase.getApk().getAbsolutePath());
							}
						}
					}
				}
				// query
				if (apps.size() == 1) {
					query = "Flows IN App('" + fromApk.getAbsolutePath() + "'" + getQueryKeywords(getQueryFeaturing())
							+ ") " + getQueryFeaturingString(getQueryFeaturing()) + "?";
				} else if (apps.size() == 2 && !fromApk.getAbsolutePath().equals(toApk.getAbsolutePath())) {
					query = "Flows FROM App('" + this.current.getFrom().getReference().getApp().getFile() + "'"
							+ getQueryKeywords(getQueryFeaturing(this.current.getFrom())) + ") TO App('"
							+ this.current.getTo().getReference().getApp().getFile() + "'"
							+ getQueryKeywords(getQueryFeaturing(this.current.getTo())) + ") "
							+ getQueryFeaturingString(getQueryFeaturing()) + "?";
				} else {
					query = "CONNECT [ ";
					for (final String app : apps) {
						if (!query.equals("CONNECT [ ")) {
							query += ", ";
						}
						boolean addedfrom = false;
						if (!app.equals(fromApk.getAbsolutePath())) {
							query += "Flows FROM App('" + fromApk.getAbsolutePath() + "'"
									+ getQueryKeywords(getQueryFeaturing(this.current.getFrom())) + ") TO App('" + app
									+ "'" + getQueryKeywords(getQueryFeaturing(this.current.getTo())) + ") "
									+ getQueryFeaturingString(getQueryFeaturing()) + "?";
							addedfrom = true;
						}
						if (!app.equals(toApk.getAbsolutePath())) {
							if (addedfrom) {
								query += ", ";
							}
							query += "Flows FROM App('" + app + "'"
									+ getQueryKeywords(getQueryFeaturing(this.current.getFrom())) + ") TO App('"
									+ toApk.getAbsolutePath() + "'"
									+ getQueryKeywords(getQueryFeaturing(this.current.getTo())) + ") "
									+ getQueryFeaturingString(getQueryFeaturing()) + "?";
						}
					}
					query += "]";
				}
			}
		} else {
			query = "Flows IN App('" + this.current.getFrom().getReference().getApp().getFile() + "'"
					+ getQueryKeywords(getQueryFeaturing()) + ") " + getQueryFeaturingString(getQueryFeaturing()) + "?";
		}
		return query;
	}

	private List<String> getQueryFeaturing() {
		if (this.current.getFrom() == null && this.current.getTo() == null) {
			return null;
		}
		// Get features
		final List<String> currentFeatures;
		if (this.current.getFrom() == null) {
			currentFeatures = Data.getInstance().getMapR().get(this.current.getFrom()).getFeatures();
			if (this.current.getTo() == null) {
				for (final String feature : Data.getInstance().getMapR().get(this.current.getTo()).getFeatures()) {
					if (!currentFeatures.contains(feature)) {
						currentFeatures.add(feature);
					}
				}
				Collections.sort(currentFeatures);
			}
		} else {
			currentFeatures = Data.getInstance().getMapR().get(this.current.getTo()).getFeatures();
		}
		return currentFeatures;
	}

	private List<String> getQueryFeaturing(SourceOrSink sourceOrSink) {
		return Data.getInstance().getMapR().get(sourceOrSink).getFeatures();
	}

	private String getQueryFeaturingString(List<String> currentFeatures) {
		// Build and return string
		if (currentFeatures == null || currentFeatures.isEmpty()) {
			return "";
		} else {
			final StringBuilder featuring = new StringBuilder("FEATURING ");
			boolean first = true;
			for (final String feature : currentFeatures) {
				if (first) {
					first = false;
				} else {
					featuring.append(", ");
				}
				featuring.append("'" + feature + "'");
			}
			featuring.append(" ");
			return featuring.toString();
		}
	}

	private String getQueryKeywords(List<String> currentFeatures) {
		if (currentFeatures != null && ConfigHandler.getInstance().getConfig().getPreprocessors() != null
				&& ConfigHandler.getInstance().getConfig().getPreprocessors().getTool() != null
				&& !ConfigHandler.getInstance().getConfig().getPreprocessors().getTool().isEmpty()) {
			final StringBuilder sb = new StringBuilder();
			for (final String keyword : currentFeatures) {
				for (final Tool preprocessor : ConfigHandler.getInstance().getConfig().getPreprocessors().getTool()) {
					if (keyword.equals(preprocessor.getQuestions())) {
						sb.append(" | '" + keyword + "'");
					}
				}
			}
			return sb.toString();
		} else {
			return "";
		}
	}

	@Override
	public void answerAvailable(Answer answer, int status) {
		this.current.setEnded(java.lang.System.currentTimeMillis());
		if (status != KeywordsAndConstants.ANSWER_STATUS_SUCCESSFUL) {
			this.current.setAborted(true);
		}
		if (contains(answer, this.current.toAnswer())) {
			this.current.setStatus((this.current.isTruepositive() ? TPFP.SUCCESSFUL : TPFP.FAILED));
		} else {
			this.current.setStatus((this.current.isTruepositive() ? TPFP.FAILED : TPFP.SUCCESSFUL));
		}
		if (this.parent != null) {
			this.parent.refresh();
		} else {
			if (this.current.getStatus() == TPFP.SUCCESSFUL) {
				Log.msg("Case: " + this.current.getCase() + ": Successful!", Log.NORMAL);
			} else if (this.current.getStatus() == TPFP.FAILED) {
				Log.msg("Case: " + this.current.getCase() + ": Failed!", Log.NORMAL);
			}
		}

		// Insert to database (if accessible)
		if (this.databaseExport) {
			if (!databaseInsert(answer, (this.current.isAborted() ? ABORTED : this.current.getStatus()))) {
				Log.warning("Database not accessible!");
			}
		}

		this.running = false;
	}

	/**
	 * One flow of contains has to be found in answer.
	 *
	 * @param answer
	 * @param contains
	 */
	private boolean contains(final Answer answer, final Answer contains) {
		if (contains.getFlows() != null && contains.getFlows().getFlow() != null) {
			if (answer.getFlows() != null && answer.getFlows().getFlow() != null) {
				for (final Flow needle : contains.getFlows().getFlow()) {
					for (final Flow flow : answer.getFlows().getFlow()) {
						if (EqualsHelper.equalsIgnoreApp(needle, flow)) {
							return true;
						}
					}
				}
			}
			return false;
		} else {
			return true;
		}
	}

	private boolean databaseInsert(Answer answer, int status) {
		try {
			final Connection conn = this.dataSource.getConnection();
			final Statement stmt = conn.createStatement();
			final StringBuilder query = new StringBuilder();
			String configString = new File("config.xml").getAbsolutePath();
			if (BREW.getConfig() != null) {
				configString = new File(BREW.getConfig()).getAbsolutePath();
			}

			// Load actual answer
			final File expected = getFile(this.current, true);
			final File actual = getFile(this.current, false);
			AnswerHandler.createXML(this.current.toAnswer(), expected);
			AnswerHandler.createXML(answer, actual);

			// Add to mysql-query
			query.append(
					"INSERT INTO `cases`(`testcase`, `status`, `query`, `source`, `sink`, `truepositive`, `falsepositive`, `duration`, `config`, `expected`, `actual`, `entered`) VALUES ('"
							+ this.current.getId() + "', '" + status + "', '"
							+ getQuery().replaceAll("\\\\", "/").replaceAll("'", "\\\\'") + "', '"
							+ Helper.toString(this.current.getFrom().getReference()).replaceAll("\\\\", "/") + "', '"
							+ Helper.toString(this.current.getTo().getReference()).replaceAll("\\\\", "/") + "', '"
							+ (this.current.isTruepositive() ? 1 : 0) + "', '"
							+ (this.current.isFalsepositive() ? 1 : 0) + "', '" + this.current.getDuration() + "', '"
							+ configString.replaceAll("\\\\", "/") + "', '"
							+ expected.getAbsolutePath().replaceAll("\\\\", "/") + "', '"
							+ actual.getAbsolutePath().replaceAll("\\\\", "/") + "', '" + this.time + "')");

			Log.msg(query.toString(), Log.DEBUG_DETAILED);

			stmt.executeUpdate(query.toString());

			stmt.close();
			conn.close();
		} catch (final Exception e) {
			Log.msg("An error occurred while accessing the database: " + e.getMessage(), Log.DEBUG);
			return false;
		}
		return true;
	}

	private File getFile(TPFP tpfp, boolean expected) {
		File returnFile;

		final String filename = tpfp.getId() + "_" + this.time + "_" + (expected ? "expected" : "actual") + "_";
		final int i = 1;
		do {
			returnFile = new File(BREW.getOutputFolder().getAbsolutePath() + "/" + filename + i + ".xml");
		} while (returnFile.exists());

		return returnFile;
	}
}