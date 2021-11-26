package de.foellix.aql.brew;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.Statement;

import com.mysql.cj.jdbc.MysqlDataSource;

import de.foellix.aql.Log;
import de.foellix.aql.brew.tpfpselector.TPFP;
import de.foellix.aql.config.ConfigHandler;
import de.foellix.aql.datastructure.Answer;
import de.foellix.aql.datastructure.handler.AnswerHandler;
import de.foellix.aql.helper.Helper;

public class DatabaseHandler {
	private static final File DATABASE_PROPERTIES = new File("data/db_config.properties");

	private static DatabaseHandler instance = new DatabaseHandler();

	private boolean available;
	private String time = "0";
	private final MysqlDataSource dataSource = new MysqlDataSource();

	private DatabaseHandler() {
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

			this.available = BREW.getUseDatabase();
			this.available = databaseAvailable();
		} catch (final IOException e) {
			this.available = false;
			Log.msg("Could not read database-config: " + DATABASE_PROPERTIES.getAbsolutePath(), Log.DEBUG);
		}
	}

	public static DatabaseHandler getInstance() {
		return instance;
	}

	public boolean databaseAvailable() {
		if (!this.available) {
			return false;
		} else {
			try {
				this.dataSource.getConnection();
			} catch (final Exception e) {
				Log.warning("Database not available!");
				this.available = false;
				return false;
			}
			return true;
		}
	}

	public boolean databaseInsert(TPFP current, String query, Answer answer, int status) {
		try {
			final Connection conn = this.dataSource.getConnection();
			final Statement stmt = conn.createStatement();
			final StringBuilder sqlQuery = new StringBuilder();
			final String configString = ConfigHandler.getInstance().getConfigFile().getAbsolutePath();

			// Load actual answer
			final File expected = getFile(current, true);
			final File actual = getFile(current, false);
			AnswerHandler.createXML(current.toAnswer(), expected);
			AnswerHandler.createXML(answer, actual);

			// Add to mysql-query
			sqlQuery.append(
					"INSERT INTO `cases`(`testcase`, `status`, `query`, `source`, `sink`, `truepositive`, `falsepositive`, `duration`, `config`, `expected`, `actual`, `entered`) VALUES ('"
							+ current.getId() + "', '" + status + "', '"
							+ query.replaceAll("\\\\", "/").replaceAll("'", "\\\\'") + "', '"
							+ Helper.toString(current.getFrom().getReference()).replaceAll("\\\\", "/").replaceAll("'",
									"\\\\'")
							+ "', '"
							+ Helper.toString(current.getTo().getReference()).replaceAll("\\\\", "/").replaceAll("'",
									"\\\\'")
							+ "', '" + (current.isTruepositive() ? 1 : 0) + "', '" + (current.isFalsepositive() ? 1 : 0)
							+ "', '" + current.getDuration() + "', '" + configString.replaceAll("\\\\", "/") + "', '"
							+ expected.getAbsolutePath().replaceAll("\\\\", "/") + "', '"
							+ actual.getAbsolutePath().replaceAll("\\\\", "/") + "', '" + this.time + "')");

			Log.msg(sqlQuery.toString(), Log.DEBUG_DETAILED);

			stmt.executeUpdate(sqlQuery.toString());

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

	public void setTime() {
		this.time = Long.valueOf(java.lang.System.currentTimeMillis() / 1000L).toString();
	}
}
