package de.foellix.aql.brew;

import static org.fusesource.jansi.Ansi.ansi;
import static org.fusesource.jansi.Ansi.Color.GREEN;

import java.io.File;

import de.foellix.aql.Log;
import de.foellix.aql.Properties;
import de.foellix.aql.brew.config.Config;
import de.foellix.aql.brew.sourceandsinkselector.SourceOrSink;
import de.foellix.aql.brew.taintbench.TaintBenchLoader;
import de.foellix.aql.brew.testcaseselector.Testcase;
import de.foellix.aql.brew.tpfpselector.Runner;
import de.foellix.aql.config.ConfigHandler;
import de.foellix.aql.datastructure.App;
import de.foellix.aql.helper.CLIHelper;
import de.foellix.aql.helper.FileRelocator;
import de.foellix.aql.helper.Helper;
import de.foellix.aql.helper.ManpageReader;
import de.foellix.aql.system.BackupAndReset;
import de.foellix.aql.system.Options;
import javafx.application.Application;

// TODO: (After 2.0.0 release) Implement (JUnit) test case export

/**
 * BREW: Benchmark Refinement and Execution Wizard
 *
 * A wizard used to generate AQL-Answers that include flows that represent the true and false positives of a taint analysis for a certain set of apps/testcases.
 *
 * @author FPauck
 */
public class BREW {
	private static String output = null;
	private static boolean nogui = false;
	public static int from = -1, to = -1;
	private static boolean includeLibraries = false;
	private static boolean writeback = false;
	private static boolean configEvaluated = false;
	private static boolean useDatabase = false;

	private static final Options options = new Options();

	private static File defaultOutputFolder = new File("output");
	private static File explicitDataFile = null;
	private static File taintBenchFindingsDirectory = null;
	private static File taintBenchAppsDirectory = null;
	private static TaintBenchLoader tbl = null;

	public static void main(String[] args) {
		// Information
		final String authorStr1 = "Author: " + Properties.info().AUTHOR;
		final String authorStr2 = "(" + Properties.info().AUTHOR_EMAIL + ")";
		final String space = "       ".substring(Math.min(Properties.info().VERSION.length() + 3, 7));
		final String centerspace1 = "                 ".substring(Math.min(17, authorStr1.length() / 2));
		final String centerspace2 = "                 ".substring(Math.min(17, authorStr2.length() / 2));
		Log.msg(ansi().bold().fg(GREEN)
				.a("  ____  _____  ______          __\r\n" + " |  _ \\|  __ \\|  __\\ \\" + space).reset()
				.a("v. " + Properties.info().VERSION.substring(0, Math.min(Properties.info().VERSION.length(), 5)))
				.bold().fg(GREEN)
				.a("/ /\r\n" + " | |_) | |__) | |__ \\ \\  /\\  / / \r\n" + " |  _ <|  _  /|  __| \\ \\/  \\/ /  \r\n"
						+ " | |_) | | \\ \\| |___  \\  /\\  /   \r\n" + " |____/|_|  \\_\\_____|  \\/  \\/    \r\n")
				.reset().a("\r\n" + centerspace1 + authorStr1 + "\r\n" + centerspace2 + authorStr2 + "\r\n\r\n"),
				Log.NORMAL);

		// Check resources availability
		CLIHelper.checkResources();

		// Check for help parameter
		if (args == null) {
			help();
			return;
		}
		for (final String arg : args) {
			if (arg.equals("-help") || arg.equals("-h") || arg.equals("-?") || arg.equals("-man")
					|| arg.equals("-manpage")) {
				help();
				return;
			}
		}

		// Parse parameters and check for GUI
		boolean firstConfig = true;
		if (args != null && args.length > 0) {
			for (int i = 0; i < args.length; i++) {
				if (args[i].equals("-backup") || args[i].equals("-b")) {
					BackupAndReset.backup();
				}
			}
			for (int i = 0; i < args.length; i++) {
				if (args[i].equals("-nogui") || args[i].equals("-noGui") || args[i].equals("-noGUI")) {
					nogui = true;
				} else if (args[i].equals("-ns") || args[i].equals("-noSplash")) {
					options.setNoSplashScreen(true);
				} else if (args[i].equals("-dg") || args[i].equals("-deg") || args[i].equals("-drawExecutionGraph")) {
					options.setDrawGraphs(true);
				} else if (args[i].equals("-backup") || args[i].equals("-bak") || args[i].equals("-b")) {
					// do nothing
				} else if (args[i].equals("-reset") || args[i].equals("-re") || args[i].equals("-r")) {
					BackupAndReset.reset();
					if (args.length > i + 1 && (args[i + 1].equals("output") || args[i + 1].equals("temp")
							|| args[i + 1].equals("answers"))) {
						BackupAndReset.resetOutputDirectories();
						i++;
					}
				} else if (args[i].equals("-l") || args[i].equals("-libs") || args[i].equals("-libraries")) {
					includeLibraries = true;
				} else if (args[i].equals("-db") || args[i].equals("-database")) {
					useDatabase = true;
				} else if (args[i].equals("-taintbench-writeback") || args[i].equals("-tbwb")) {
					writeback = true;
				} else {
					if (args[i].equals("-c") || args[i].equals("-cfg") || args[i].equals("-config")) {
						if (firstConfig) {
							configEvaluated = CLIHelper.evaluateConfig(args[i + 1]);
							firstConfig = false;
						} else {
							final File oldConfig = ConfigHandler.getInstance().getConfigFile();
							configEvaluated = CLIHelper.evaluateConfig(args[i + 1]);
							ConfigHandler.getInstance().mergeWith(oldConfig);
						}
					} else if (args[i].equals("-rules")) {
						CLIHelper.evaluateRules(args[i + 1]);
					} else if (args[i].equals("-o") || args[i].equals("-out") || args[i].equals("-output")) {
						output = args[i + 1];
					} else if (args[i].equals("-data")) {
						explicitDataFile = new File(args[i + 1]);
					} else if (args[i].equals("-taintbench") || args[i].equals("-tb")) {
						taintBenchFindingsDirectory = new File(args[i + 1]);
					} else if (args[i].equals("-taintbenchapps") || args[i].equals("-tba")) {
						taintBenchAppsDirectory = new File(args[i + 1]);
					} else if (args[i].equals("-d") || args[i].equals("-debug")) {
						CLIHelper.evaluateLogLevel(args[i + 1]);
					} else if (args[i].equals("-df") || args[i].equals("-dtf") || args[i].equals("-debugToFile")) {
						Log.setLogToFileLevel(CLIHelper.evaluateLogLevel(args[i + 1], false));
					} else if (args[i].equals("-t") || args[i].equals("-timeout")) {
						options.setTimeout(CLIHelper.evaluateTimeout(args[i + 1]));
						if (args.length > i + 2) {
							final int mode = CLIHelper.evaluateTimeoutMode(args[i + 2]);
							if (mode != -1) {
								options.setTimeoutMode(mode);
								i++;
							}
						}
					} else if (args[i].equals("--from")) {
						from = Integer.parseInt(args[i + 1]);
					} else if (args[i].equals("--to")) {
						to = Integer.parseInt(args[i + 1]);
					} else {
						Log.error("Unknown launch parameter (" + args[i] + "). Canceling execution!");
						java.lang.System.exit(0);
					}
					i++;
				}
			}
		}

		// Start BREW
		if (nogui) {
			// Initialize
			init();

			// Relocate files
			if (Config.getInstance().get(Config.AUTOMATIC_RELOCATION_DIR) != null
					&& !Config.getInstance().get(Config.AUTOMATIC_RELOCATION_DIR).isEmpty()) {
				boolean relocated = false;
				final File relocationDir = new File(Config.getInstance().get(Config.AUTOMATIC_RELOCATION_DIR));
				for (final Testcase testcase : Data.getInstance().getTestcases()) {
					if (!testcase.getApk().exists()) {
						if (relocationDir.exists() && relocationDir.isDirectory()) {
							// Adapt testcase
							File apk = FileRelocator.recursivelySearchFile(testcase.getApk(), relocationDir);
							if (apk == null) {
								apk = FileRelocator.recursivelySearchFile(testcase.getApk(), relocationDir, true);
								if (apk == null) {
									Log.error("Could not automatically relocate " + testcase.getApk().getAbsolutePath()
											+ " in " + relocationDir.getAbsolutePath()
											+ ". Please adjust the \"automaticRelocationDirectory\" in \"config.properties\".");
									System.exit(0);
								} else {
									Log.msg("Ignored parent directory while relocating "
											+ testcase.getApk().getAbsolutePath() + " in "
											+ relocationDir.getAbsolutePath() + ".", Log.DEBUG);
								}
							}
							Log.msg("Automatically relocated \"" + testcase.getApk().getAbsolutePath() + "\" to \""
									+ apk.getAbsolutePath() + "\".", Log.NORMAL);
							testcase.setApk(apk);

							// Adapt sources and sinks
							final App app = Helper.createApp(apk);
							for (final SourceOrSink sourceOrSink : Data.getInstance().getMap().get(testcase)) {
								sourceOrSink.getReference().setApp(app);
							}

							relocated = true;
						} else {
							Log.error("Could not automatically relocate " + testcase.getApk().getAbsolutePath()
									+ ". Relocation directory is not valid: " + relocationDir.getAbsolutePath()
									+ ". Please adjust the \"automaticRelocationDirectory\" in \"config.properties\".");
							System.exit(0);
						}
					}
				}
				TaintBenchLoader.relocate();
				Data.getInstance().refreshIDs();

				// Save relocation result
				if (Boolean.parseBoolean(Config.getInstance().get(Config.SAVE_AFTER_AUTO_RELOCATION)) && relocated) {
					Log.msg("App files automatically relocated! Saving changes ("
							+ Data.getInstance().getCurrentSaveFile().getAbsolutePath() + ")... ", Log.NORMAL, false);
					Data.store();
					Log.setPrefixEnabled(false);
					Log.msg("done!", Log.NORMAL);
					Log.setPrefixEnabled(true);
				}
			}

			// Run benchmark
			final Runner runner = new Runner();
			runner.runAll(Data.getInstance().gettpfps(from, to));
		} else {
			// TODO: (After 2.0.0 release) Check if future openjfx versions still require this silencing (last tested with 18-ea+4; Part 1/2)
			if (!Log.logIt(Log.DEBUG_DETAILED)) {
				Log.setSilence(true);
			}
			Application.launch(GUI.class, args);
		}
	}

	public static void init() {
		// Load Data
		boolean dataLoaded = false;
		if (explicitDataFile != null) {
			if (explicitDataFile.exists()) {
				Log.msg("Loading data file: " + explicitDataFile.getAbsolutePath(), Log.DEBUG);
				Data.init(explicitDataFile);
				Data.getInstance().setCurrentSaveFile(explicitDataFile);
				dataLoaded = true;
			} else {
				Log.warning("Could not find data file: " + explicitDataFile.getAbsolutePath());
			}
		} else {
			Data.getInstance().setCurrentSaveFile(Data.DEFAULT_SAVEFILE);
		}

		// Load properties
		Config.getInstance();
		if (!dataLoaded && !Data.getInstance().getCurrentSaveFile().exists()) {
			Data.getInstance().setCurrentSaveFile(
					new File(Config.getInstance().get(Config.INITIAL_OPEN_DIRECTORY), "not_available_file"));
		}
		Data.getInstance().setLastLoadedFile(
				new File(Config.getInstance().get(Config.INITIAL_OPEN_DIRECTORY), "not_available_file"));
		Data.getInstance().setLastLoadedFolder(new File(Config.getInstance().get(Config.INITIAL_OPEN_DIRECTORY)));
		options.setRetry(Boolean.parseBoolean(Config.getInstance().get(Config.RETRY)));

		// Load TaintBench
		if (taintBenchFindingsDirectory != null) {
			if (taintBenchAppsDirectory == null) {
				taintBenchAppsDirectory = taintBenchFindingsDirectory;
			}
			tbl = new TaintBenchLoader(taintBenchFindingsDirectory, taintBenchAppsDirectory);
		}
	}

	public static Options getOptions() {
		return options;
	}

	public static boolean getNoGui() {
		return nogui;
	}

	public static boolean getIncludeLibraries() {
		return includeLibraries;
	}

	public static boolean getUseDatabase() {
		return useDatabase;
	}

	public static boolean getTaintBenchWriteBack() {
		return writeback;
	}

	public static boolean getConfigEvaluated() {
		return configEvaluated;
	}

	public static File getOutputFolder() {
		if (output != null) {
			final File outputFolder = new File(output);
			if (!outputFolder.exists()) {
				outputFolder.mkdirs();
			}
			if (outputFolder.exists()) {
				return outputFolder;
			}
		}
		return defaultOutputFolder;
	}

	public static TaintBenchLoader getTaintBenchLoader() {
		return tbl;
	}

	private static void help() {
		Log.msg(ManpageReader.getInstance().getManpageContent(), Log.NORMAL);
	}
}