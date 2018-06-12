package de.foellix.aql.ggwiz;

import static org.fusesource.jansi.Ansi.ansi;
import static org.fusesource.jansi.Ansi.Color.GREEN;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.fusesource.jansi.AnsiConsole;

import de.foellix.aql.Log;
import de.foellix.aql.Properties;
import de.foellix.aql.config.ConfigHandler;
import de.foellix.aql.ggwiz.tpfpselector.Runner;
import de.foellix.aql.system.BackupAndReset;
import javafx.application.Application;

/**
 * BREW: Benchmark Refinement and Execution Wizard
 *
 * A wizard used to generate AQL-Answers that include flows that represent the
 * true and false positives of a taint analysis for a certain set of
 * apps/testcases.
 *
 * @author FPauck
 */
public class BREW {
	private static String config = null;
	private static String output = null;
	private static String debug = null;
	private static boolean nogui = false;
	private static int from = -1, to = -1;

	private static long timeout = -1;

	private static File defaultOutputFolder = new File("output");

	public static void main(String[] args) {
		// Information
		AnsiConsole.systemInstall();
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
		if (args != null && args.length > 0) {
			for (int i = 0; i < args.length; i++) {
				if (args[i].equals("-backup") || args[i].equals("-b")) {
					BackupAndReset.backup();
				}
			}
			for (int i = 0; i < args.length; i++) {
				if (args[i].equals("-nogui")) {
					nogui = true;
				} else if (args[i].equals("-backup") || args[i].equals("-b")) {
					// do nothing
				} else if (args[i].equals("-reset") || args[i].equals("-re") || args[i].equals("-r")) {
					BackupAndReset.reset();
				} else {
					if (args[i].equals("-c") || args[i].equals("-cfg") || args[i].equals("-config")) {
						config = args[i + 1];
					} else if (args[i].equals("-o") || args[i].equals("-out") || args[i].equals("-output")) {
						output = args[i + 1];
					} else if (args[i].equals("-d") || args[i].equals("-debug")) {
						debug = args[i + 1];
					} else if (args[i].equals("-t") || args[i].equals("-timeout")) {
						final String readTimeout = args[i + 1];
						if (readTimeout.contains("h")) {
							timeout = Integer.parseInt(readTimeout.replaceAll("h", "")) * 3600;
						} else if (readTimeout.contains("m")) {
							timeout = Integer.parseInt(readTimeout.replaceAll("m", "")) * 60;
						} else {
							timeout = Integer.parseInt(readTimeout.replaceAll("s", ""));
						}
					} else if (args[i].equals("--from")) {
						from = Integer.parseInt(args[i + 1]);
					} else if (args[i].equals("--to")) {
						to = Integer.parseInt(args[i + 1]);
					} else {
						java.lang.System.exit(0);
					}
					i++;
				}
			}
		}

		// Debug settings
		if (debug != null) {
			if (debug.equals("normal")) {
				Log.setLogLevel(Log.NORMAL);
			} else if (debug.equals("short")) {
				Log.setLogLevel(Log.NORMAL);
				Log.setShorten(true);
			} else if (debug.equals("warning")) {
				Log.setLogLevel(Log.WARNING);
			} else if (debug.equals("error")) {
				Log.setLogLevel(Log.ERROR);
			} else if (debug.equals("debug")) {
				Log.setLogLevel(Log.DEBUG);
			} else if (debug.equals("detailed")) {
				Log.setLogLevel(Log.DEBUG_DETAILED);
			} else if (debug.equals("special")) {
				Log.setLogLevel(Log.DEBUG_SPECIAL);
			} else {
				Log.setLogLevel(Integer.valueOf(debug).intValue());
			}
		}

		// Load custom config
		final File configFile = new File(config);
		if (config != null && configFile.exists()) {
			ConfigHandler.getInstance().setConfig(configFile);
		} else {
			Log.warning("Configuration file does not exist: " + configFile.getAbsolutePath());
		}

		// Start GGWiz
		if (nogui) {
			final Runner runner = new Runner();
			runner.runAll(Data.getInstance().gettpfps(from, to));
		} else {
			Application.launch(GUI.class, args);
		}
	}

	public static long getTimeout() {
		return timeout;
	}

	public static boolean getNoGui() {
		return nogui;
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

	public static String getConfig() {
		return config;
	}

	private static void help() {
		try {
			final File manpage = new File("manpage");
			final byte[] encoded = Files.readAllBytes(Paths.get(manpage.toURI()));
			Log.msg(new String(encoded, StandardCharsets.UTF_8), Log.NORMAL);
		} catch (final IOException e) {
			Log.error("Could not find manpage file.");
		}
	}
}