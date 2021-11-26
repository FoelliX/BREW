package de.foellix.aql.brew;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.foellix.aql.Log;
import de.foellix.aql.brew.sourceandsinkselector.SourceOrSink;
import de.foellix.aql.brew.testcaseselector.Testcase;
import de.foellix.aql.brew.tpfpselector.TPFP;
import de.foellix.aql.helper.FileHelper;
import de.foellix.aql.helper.ZipHelper;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;

public class Data implements Serializable {
	private static final long serialVersionUID = -1886706808562692180L;

	public transient static final File DEFAULT_SAVEFILE = new File("data/data.zip");
	private transient static final File TEMP_ZIP_DIRECTORY = new File("data/temp/unzip");

	private boolean testcaseChangedFlag;
	private int testcaseid;
	private List<Testcase> testcaseList;
	private transient ObservableList<Testcase> testcases;
	private boolean sourcesAndSinksChangedFlag;
	private int sourceOrSinkId;
	private List<SourceOrSink> sourceAndSinkList;
	private transient ObservableList<SourceOrSink> sourcesAndSinks;
	private int tpfpid;
	private List<TPFP> tpfpList;
	private transient ObservableList<TPFP> tpfps;
	private Map<Testcase, List<SourceOrSink>> map;
	private Map<SourceOrSink, Testcase> mapR;

	private transient static Data instance = null;

	private File currentSaveFile, lastLoadedFile, lastLoadedFolder;

	private Data() {
		this.testcaseChangedFlag = false;
		this.testcaseid = 0;
		this.testcases = FXCollections.observableArrayList();
		this.sourcesAndSinksChangedFlag = false;
		this.sourceOrSinkId = 0;
		this.sourcesAndSinks = FXCollections.observableArrayList();
		this.sourceAndSinkList = new ArrayList<>();
		this.tpfpid = 0;
		this.tpfps = FXCollections.observableArrayList();
		this.tpfpList = new ArrayList<>();
		this.map = new HashMap<>();
		this.mapR = new HashMap<>();

		this.currentSaveFile = DEFAULT_SAVEFILE;
		this.lastLoadedFile = new File("./doesNotExist.apk");
		this.lastLoadedFolder = new File(".");
	}

	public static Data getInstance() {
		if (instance == null) {
			init(null);
		}
		return instance;
	}

	public static void init() {
		if (instance == null) {
			init(null);
		}
	}

	public static void init(File initialDataFile) {
		if (initialDataFile != null && initialDataFile.exists()) {
			instance = load(initialDataFile);
		} else {
			instance = load();
		}
		if (instance != null) {
			if (instance.currentSaveFile == null || !instance.currentSaveFile.exists()) {
				instance.currentSaveFile = new File("data/data.zip");
			}
			if (instance.testcaseList != null) {
				instance.testcases = FXCollections.observableArrayList(instance.testcaseList);
			} else {
				instance.testcases = FXCollections.observableArrayList();
			}
			if (instance.sourceAndSinkList != null) {
				instance.sourcesAndSinks = FXCollections.observableArrayList(instance.sourceAndSinkList);
			} else {
				instance.sourcesAndSinks = FXCollections.observableArrayList();
			}
			if (instance.tpfpList != null) {
				instance.tpfps = FXCollections.observableArrayList(instance.tpfpList);
			} else {
				instance.tpfps = FXCollections.observableArrayList();
			}
		} else {
			instance = new Data();
		}

		// Listener
		instance.testcases.addListener(new ListChangeListener<Testcase>() {
			@Override
			public void onChanged(Change<? extends Testcase> c) {
				instance.setTestcaseChangedFlag(true);
			}
		});
		instance.sourcesAndSinks.addListener(new ListChangeListener<SourceOrSink>() {
			@Override
			public void onChanged(Change<? extends SourceOrSink> c) {
				instance.setSourcesAndSinksChangedFlag(true);
			}
		});
	}

	public boolean testcasesHaveChanged() {
		return this.testcaseChangedFlag;
	}

	public void setTestcaseChangedFlag(boolean testcaseChangedFlag) {
		this.testcaseChangedFlag = testcaseChangedFlag;
	}

	public int getTestcaseId() {
		this.testcaseid++;
		return this.testcaseid;
	}

	public ObservableList<Testcase> getTestcases() {
		return this.testcases;
	}

	public List<Testcase> getTestcaseList() {
		return this.testcaseList;
	}

	public boolean sourceAndSinksHaveChanged() {
		return this.sourcesAndSinksChangedFlag;
	}

	public void setSourcesAndSinksChangedFlag(boolean sourcesAndSinksChangedFlag) {
		this.sourcesAndSinksChangedFlag = sourcesAndSinksChangedFlag;
	}

	public int getSourceOrSinkId() {
		this.sourceOrSinkId++;
		return this.sourceOrSinkId;
	}

	public ObservableList<SourceOrSink> getSourcesAndSinks() {
		return this.sourcesAndSinks;
	}

	public List<SourceOrSink> getSourceAndSinkList() {
		return this.sourceAndSinkList;
	}

	public int getTPFPId() {
		this.tpfpid++;
		return this.tpfpid;
	}

	public ObservableList<TPFP> getTPFPs() {
		return this.tpfps;
	}

	public synchronized List<TPFP> getTPFPList() {
		return this.tpfpList;
	}

	public List<TPFP> gettpfps(int from, int to) {
		if (from == -1 && to == -1) {
			return getTPFPs();
		}
		final List<TPFP> filteredList = new ArrayList<>();
		for (final TPFP tpfp : this.tpfps) {
			if ((from == -1 || tpfp.getId() >= from) && (to == -1 || tpfp.getId() <= to)) {
				filteredList.add(tpfp);
			}
		}
		return filteredList;
	}

	public Map<Testcase, List<SourceOrSink>> getMap() {
		return this.map;
	}

	public Map<SourceOrSink, Testcase> getMapR() {
		return this.mapR;
	}

	public void setTestcaseid(int testcaseid) {
		this.testcaseid = testcaseid;
	}

	public void setSourceOrSinkid(int sourceOrSinkid) {
		this.sourceOrSinkId = sourceOrSinkid;
	}

	public void setTpfpid(int tpfpid) {
		this.tpfpid = tpfpid;
	}

	public static void store() {
		instance.testcaseList = new ArrayList<>(instance.testcases);
		instance.sourceAndSinkList = new ArrayList<>(instance.sourcesAndSinks);
		instance.tpfpList = new ArrayList<>(instance.tpfps);
		try {
			final FileOutputStream fileOut = new FileOutputStream(instance.currentSaveFile);
			final ObjectOutputStream out = new ObjectOutputStream(fileOut);
			out.writeObject(instance);
			out.close();
			fileOut.close();

			if (instance.currentSaveFile.getAbsolutePath().endsWith(".zip")) {
				final File tempFile = new File(TEMP_ZIP_DIRECTORY,
						instance.currentSaveFile.getName().replace(".zip", ".ser"));
				try {
					tempFile.mkdirs();
					Files.move(instance.currentSaveFile.toPath(), tempFile.toPath(),
							StandardCopyOption.REPLACE_EXISTING);
				} catch (final IOException e) {
					Log.error("Could not move file to \"" + TEMP_ZIP_DIRECTORY + "\" while zipping output."
							+ Log.getExceptionAppendix(e));
				}
				ZipHelper.zip(tempFile, instance.currentSaveFile);
			}
		} catch (final IOException e) {
			Log.msg("Cannot store data: " + e.getMessage(), Log.DEBUG);
		}
	}

	public static Data load() {
		return load((instance == null ? DEFAULT_SAVEFILE : instance.currentSaveFile));
	}

	public static Data load(File loadFile) {
		ObjectInputStream in = null;
		try {
			if (loadFile != null && loadFile.exists()) {
				if (loadFile.getAbsolutePath().endsWith(".zip")) {
					FileHelper.deleteDir(TEMP_ZIP_DIRECTORY);
					TEMP_ZIP_DIRECTORY.mkdirs();
					ZipHelper.unzip(loadFile, TEMP_ZIP_DIRECTORY, true);
					loadFile = TEMP_ZIP_DIRECTORY.listFiles()[0];
				}

				in = new ObjectInputStream(new BufferedInputStream(new FileInputStream(loadFile)));
				try {
					final Data loaded = (Data) in.readObject();
					in.close();
					return loaded;
				} catch (final ClassNotFoundException e) {
					if (e.getMessage().contains(".ggwiz.")) {
						in.close();
						final File updatedFile = BackwardCompatibilityHelper.updateFile(loadFile);
						Log.msg("Old file format detected! Updated and stored updated file: "
								+ updatedFile.getAbsolutePath(), Log.NORMAL);
						in = new ObjectInputStream(new BufferedInputStream(new FileInputStream(updatedFile)));
						final Data loaded = (Data) in.readObject();
						in.close();
						return loaded;
					} else {
						throw e;
					}
				}
			} else {
				return null;
			}
		} catch (final Exception e) {
			e.printStackTrace();
			Log.msg("No data available to be loaded: " + e.getMessage(), Log.DEBUG);
			return null;
		} finally {
			try {
				in.close();
			} catch (final Exception e) {
				// do nothing
			}
		}
	}

	public void deselectAllSourcesAndSinks() {
		for (final SourceOrSink item : this.sourcesAndSinks) {
			item.setSource(false);
			item.setSink(false);
		}
	}

	public File getCurrentSaveFile() {
		return this.currentSaveFile;
	}

	public void setCurrentSaveFile(File currentSaveFile) {
		this.currentSaveFile = currentSaveFile;
	}

	public File getLastLoadedFile() {
		return this.lastLoadedFile;
	}

	public void setLastLoadedFile(File lastLoadedFile) {
		this.lastLoadedFile = lastLoadedFile;
	}

	public File getLastLoadedFolder() {
		return this.lastLoadedFolder;
	}

	public void setLastLoadedFolder(File lastLoadedFolder) {
		this.lastLoadedFolder = lastLoadedFolder;
	}

	public void setSourcesAndSinks(ObservableList<SourceOrSink> sourcesAndSinks) {
		this.sourcesAndSinks = sourcesAndSinks;
	}

	public void setMap(Map<Testcase, List<SourceOrSink>> map) {
		this.map = map;
	}

	public void setMapR(Map<SourceOrSink, Testcase> mapR) {
		this.mapR = mapR;
	}

	public void refreshIDs() {
		final Map<Integer, Integer> remember = new HashMap<>();

		// Testcases
		int id = 0;
		for (final Testcase item : this.testcases) {
			id++;
			remember.put(item.getId(), id);
			item.setId(id);
		}
		this.testcaseid = id;
		for (final Testcase item : this.testcases) {
			if (item.getCombine() != null && !item.getCombine().equals("")) {
				String newCombine = "";
				for (final String combine : item.getCombine().replace(" ", "").split(",")) {
					newCombine += (!newCombine.equals("") ? ", " : "") + remember.get(Integer.valueOf(combine));
				}
				item.setCombine(newCombine);
			}
		}
		remember.clear();

		// Sources and Sinks
		final Map<SourceOrSink, Testcase> newMapR = new HashMap<>();
		id = 0;
		for (final SourceOrSink item : this.sourcesAndSinks) {
			final Testcase temp = this.mapR.get(item);
			id++;
			remember.put(item.getId(), id);
			item.setId(id);
			newMapR.put(item, temp);
		}
		this.mapR = newMapR;
		this.sourceOrSinkId = id;
		for (final SourceOrSink item : this.sourcesAndSinks) {
			if (item.getCombine() != null && !item.getCombine().equals("")) {
				String newCombine = "";
				for (final String combine : item.getCombine().replace(" ", "").split(",")) {
					newCombine += (!newCombine.equals("") ? ", " : "") + remember.get(Integer.valueOf(combine));
				}
				item.setCombine(newCombine);
			}
		}
		remember.clear();

		// TPFPs
		id = 0;
		for (final TPFP item : this.tpfps) {
			id++;
			item.setId(id);
		}
		this.tpfpid = id;
	}

	public SourceOrSink getSourceOrSinkById(int id) {
		for (final SourceOrSink sourceOrSink : this.sourcesAndSinks) {
			if (sourceOrSink.getId() == id) {
				return sourceOrSink;
			}
		}
		return null;
	}
}