package de.foellix.aql.ggwiz;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.foellix.aql.Log;
import de.foellix.aql.ggwiz.sourceandsinkselector.SourceOrSink;
import de.foellix.aql.ggwiz.testcaseselector.Testcase;
import de.foellix.aql.ggwiz.tpfpselector.TPFP;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;

public class Data implements Serializable {
	private static final long serialVersionUID = -1886706808562692180L;

	private boolean testcaseChangedFlag;
	private int testcaseid;
	private List<Testcase> testcaseList;
	private transient ObservableList<Testcase> testcases;
	private boolean sourcesAndSinksChangedFlag;
	private int sourceOrSinkid;
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
		this.sourceOrSinkid = 0;
		this.sourcesAndSinks = FXCollections.observableArrayList();
		this.tpfpid = 0;
		this.tpfps = FXCollections.observableArrayList();
		this.map = new HashMap<>();
		this.mapR = new HashMap<>();

		this.currentSaveFile = new File("data/data.ser");
		this.lastLoadedFile = new File("./doesNotExist.apk");
		this.lastLoadedFolder = new File(".");
	}

	public static Data getInstance() {
		if (instance == null) {
			init();
		}
		return instance;
	}

	public static void init() {
		instance = load();
		if (instance != null) {
			if (instance.currentSaveFile == null || !instance.currentSaveFile.exists()) {
				instance.currentSaveFile = new File("data/data.ser");
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
		this.sourceOrSinkid++;
		return this.sourceOrSinkid;
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

	public List<TPFP> getTPFPList() {
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
		this.sourceOrSinkid = sourceOrSinkid;
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
		} catch (final IOException e) {
			Log.msg("Cannot store data: " + e.getMessage(), Log.DEBUG);
		}
	}

	public static Data load() {
		return load((instance == null ? new File("data/data.ser") : instance.currentSaveFile));
	}

	public static Data load(File loadFile) {
		ObjectInputStream in = null;
		try {
			if (loadFile != null && loadFile.exists()) {
				in = new ObjectInputStream(new BufferedInputStream(new FileInputStream(loadFile)));
				final Data loaded = (Data) in.readObject();
				return loaded;
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
}
