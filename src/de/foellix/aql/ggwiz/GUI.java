package de.foellix.aql.ggwiz;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import de.foellix.aql.Log;
import de.foellix.aql.Properties;
import de.foellix.aql.config.ConfigHandler;
import de.foellix.aql.datastructure.Answer;
import de.foellix.aql.datastructure.Flow;
import de.foellix.aql.datastructure.KeywordsAndConstants;
import de.foellix.aql.datastructure.handler.AnswerHandler;
import de.foellix.aql.ggwiz.sourceandsinkselector.SourceAndSinkSelector;
import de.foellix.aql.ggwiz.sourceandsinkselector.SourceOrSink;
import de.foellix.aql.ggwiz.sourceandsinkselector.SuSiLoader;
import de.foellix.aql.ggwiz.testcaseselector.FeatureDetector;
import de.foellix.aql.ggwiz.testcaseselector.TestCaseSelector;
import de.foellix.aql.ggwiz.testcaseselector.Testcase;
import de.foellix.aql.ggwiz.tpfpselector.Exporter;
import de.foellix.aql.ggwiz.tpfpselector.Runner;
import de.foellix.aql.ggwiz.tpfpselector.TPFP;
import de.foellix.aql.ggwiz.tpfpselector.TPFPSelector;
import de.foellix.aql.helper.Helper;
import de.foellix.aql.ui.gui.ExitDialog;
import de.foellix.aql.ui.gui.LoadingDialog;
import de.foellix.aql.ui.gui.ProgressDialog;
import de.foellix.aql.ui.gui.SplashScreen;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

public class GUI extends Application {
	private Stage stage;

	private BorderPane mainPane;
	private MenuBar menuBar;
	public static final int TEXT_CASE_SELECTOR = 0;
	public static final int SOURCE_SINK_SELECTOR = 1;
	public static final int TP_FP_SELECTOR = 2;
	private int current = TEXT_CASE_SELECTOR;

	private FileChooser loadFileDialog, saveAndOpenDialog, loadAnswerDialog;
	private DirectoryChooser loadDirectoryDialog;
	private int totalDone, totalMax, currentDone, currentMax;

	private TestCaseSelector testCaseSelector;
	private SourceAndSinkSelector sourceAndSinksSelector;
	private TPFPSelector tpfpSelector;

	@Override
	public void start(Stage stage) throws Exception {
		this.stage = stage;

		this.stage.getIcons().add(new Image("file:data/gui/images/icon_16.png", 16, 16, false, true));
		this.stage.getIcons().add(new Image("file:data/gui/images/icon_32.png", 32, 32, false, true));
		this.stage.getIcons().add(new Image("file:data/gui/images/icon_64.png", 64, 64, false, true));

		this.mainPane = new BorderPane();
		this.menuBar = new MenuBar(this);
		this.mainPane.setTop(this.menuBar);
		this.mainPane.setBottom(Statistics.getInstance());

		final Scene scene = new Scene(this.mainPane, 1024, 768);
		scene.getStylesheets().add("file:data/gui/style.css");
		this.stage.setScene(scene);

		stage.getScene().addEventFilter(KeyEvent.KEY_PRESSED, (evt) -> {
			if (evt.getCode() == KeyCode.F5) {
				refreshIDs();
			}
		});

		this.stage.setOnCloseRequest(new EventHandler<WindowEvent>() {
			@Override
			public void handle(final WindowEvent we) {
				we.consume();
				exit();
			}
		});

		// Dialogs
		this.loadDirectoryDialog = new DirectoryChooser();
		this.loadFileDialog = new FileChooser();
		final FileChooser.ExtensionFilter allFilter = new FileChooser.ExtensionFilter("*.* All files", "*.*");
		final FileChooser.ExtensionFilter apkFilter = new FileChooser.ExtensionFilter(
				"*.apk Android Application Package", "*.apk");
		this.loadFileDialog.getExtensionFilters().addAll(allFilter, apkFilter);
		this.loadFileDialog.setSelectedExtensionFilter(apkFilter);
		this.saveAndOpenDialog = new FileChooser();
		final FileChooser.ExtensionFilter serFilter = new FileChooser.ExtensionFilter("*.ser Serializable Object",
				"*.ser");
		this.saveAndOpenDialog.getExtensionFilters().addAll(allFilter, serFilter);
		this.saveAndOpenDialog.setSelectedExtensionFilter(serFilter);
		this.loadAnswerDialog = new FileChooser();
		final FileChooser.ExtensionFilter xmlFilter = new FileChooser.ExtensionFilter(
				"*.xml Extensible Markup Language", "*.xml");
		this.loadAnswerDialog.getExtensionFilters().addAll(allFilter, xmlFilter);
		this.loadAnswerDialog.setSelectedExtensionFilter(xmlFilter);

		// Splash Screen
		final SplashScreen splashScreen = new SplashScreen(Properties.info().ABBRRVIATION + " - "
				+ Properties.info().NAME + " (v. " + Properties.info().VERSION + ")", "by " + Properties.info().AUTHOR,
				Color.WHITE);
		new Thread(() -> {
			long time = System.currentTimeMillis();
			Data.init();
			try {
				time = System.currentTimeMillis() - time;
				if (time < 2000) {
					Thread.sleep(2000 - time);
				}
			} catch (final Exception e) {
				// do nothing
			}
			Platform.runLater(() -> {
				splashScreen.setDone(true);
				this.stage.show();
				setContent(this.current);

				ConfigHandler.getInstance().getConfig();
			});
		}).start();
	}

	public void setContent(int content) {
		this.menuBar.setContent(content);
		switch (content) {
		case 1:
			this.stage.setTitle("BREW - Source & Sink Selector ("
					+ Data.getInstance().getCurrentSaveFile().getAbsolutePath() + ")");
			if (this.sourceAndSinksSelector == null) {
				this.sourceAndSinksSelector = new SourceAndSinkSelector(this.stage);
			} else {
				this.sourceAndSinksSelector.onResume();
			}
			this.mainPane.setCenter(this.sourceAndSinksSelector);
			break;
		case 2:
			this.stage.setTitle("BREW - True & False Positive Selector ("
					+ Data.getInstance().getCurrentSaveFile().getAbsolutePath() + ")");
			if (this.tpfpSelector == null) {
				this.tpfpSelector = new TPFPSelector();
			} else {
				this.tpfpSelector.onResume();
			}
			this.mainPane.setCenter(this.tpfpSelector);
			break;
		default:
			this.stage.setTitle(
					"BREW - TestCase Selector (" + Data.getInstance().getCurrentSaveFile().getAbsolutePath() + ")");
			if (this.testCaseSelector == null) {
				this.testCaseSelector = new TestCaseSelector();
			}
			this.mainPane.setCenter(this.testCaseSelector);
			break;
		}
		Statistics.getInstance().refresh();
	}

	public void prev() {
		if (this.current > TEXT_CASE_SELECTOR) {
			this.current--;
			setContent(this.current);
		}
	}

	public void next() {
		if (this.current < TP_FP_SELECTOR) {
			this.current++;
			setContent(this.current);
		}
	}

	public void loadFolder() {
		if (Data.getInstance().getLastLoadedFolder().exists()
				&& Data.getInstance().getLastLoadedFolder().isDirectory()) {
			this.loadDirectoryDialog.setInitialDirectory(Data.getInstance().getLastLoadedFolder());
		} else {
			this.loadDirectoryDialog.setInitialDirectory(new File("."));
		}
		final List<File> loadFiles = new ArrayList<>();
		loadFiles.add(this.loadDirectoryDialog.showDialog(this.stage));
		load(loadFiles);
	}

	public void load() {
		this.loadFileDialog.setInitialDirectory(Data.getInstance().getLastLoadedFile().getParentFile());
		load(this.loadFileDialog.showOpenMultipleDialog(this.stage));
	}

	private void load(List<File> loadFiles) {
		if (loadFiles != null && loadFiles.get(0) != null) {
			if (loadFiles.get(0).isDirectory()) {
				Data.getInstance().setLastLoadedFolder(loadFiles.get(0));
			} else {
				Data.getInstance().setLastLoadedFile(loadFiles.get(0));
			}
			try {
				for (final File file : loadFiles) {
					if (!file.isDirectory()) {
						Data.getInstance().getTestcases().add(new Testcase(file));
						Log.msg("File added: " + file.toString(), Log.NORMAL);
					} else {
						Log.msg("Files found recursively in folder: " + file.toString()
								+ " are listed below. All have been added.", Log.NORMAL);
						for (final File apk : findAPKsRecursively(file)) {
							Data.getInstance().getTestcases().add(new Testcase(apk));
							Log.msg(apk.toString(), Log.NORMAL);
						}
					}
				}
			} catch (final Exception e) {
				Log.msg("File not found: " + loadFiles.toString(), Log.ERROR);
			}
		}
	}

	public void newSetup() {
		Data.getInstance().setCurrentSaveFile(null);
		Data.init();
		this.testCaseSelector = null;
		this.sourceAndSinksSelector = null;
		this.tpfpSelector = null;
		setContent(this.current);
	}

	public void open() {
		if (Data.getInstance().getCurrentSaveFile().getParentFile().exists()
				&& Data.getInstance().getCurrentSaveFile().getParentFile().isDirectory()) {
			this.saveAndOpenDialog.setInitialDirectory(Data.getInstance().getCurrentSaveFile().getParentFile());
		} else {
			this.saveAndOpenDialog.setInitialDirectory(new File("."));
		}
		final File openFile = this.saveAndOpenDialog.showOpenDialog(this.stage);
		if (openFile != null) {
			final LoadingDialog loadingDialog = new LoadingDialog("Opening");
			new Thread(() -> {
				Data.getInstance().setCurrentSaveFile(openFile);
				Data.init();
				this.testCaseSelector = null;
				this.sourceAndSinksSelector = null;
				this.tpfpSelector = null;
				Platform.runLater(() -> {
					setContent(this.current);
					loadingDialog.setDone(true);
				});
			}).start();
		}
	}

	public void add() {
		if (Data.getInstance().getCurrentSaveFile().getParentFile().exists()
				&& Data.getInstance().getCurrentSaveFile().getParentFile().isDirectory()) {
			this.saveAndOpenDialog.setInitialDirectory(Data.getInstance().getCurrentSaveFile().getParentFile());
		} else {
			this.saveAndOpenDialog.setInitialDirectory(new File("."));
		}
		final File openFile = this.saveAndOpenDialog.showOpenDialog(this.stage);
		if (openFile != null) {
			final LoadingDialog loadingDialog = new LoadingDialog("Opening");
			new Thread(() -> {
				addFinished(Data.load(openFile));
				Platform.runLater(() -> {
					loadingDialog.setDone(true);
				});
			}).start();
		}
	}

	private void addFinished(Data data) {
		int maxid = -1;
		for (final Testcase item : Data.getInstance().getTestcases()) {
			if (maxid < item.getId()) {
				maxid = item.getId();
			}
		}
		for (final Testcase item : data.getTestcaseList()) {
			item.setId(maxid + item.getId());
			if (item.getCombine() != null && !item.getCombine().equals("null") && !item.getCombine().equals("")) {
				final StringBuilder sb = new StringBuilder();
				if (item.getCombine().contains(",")) {
					for (final String value : item.getCombine().replaceAll(" ", "").split(",")) {
						if (sb.length() != 0) {
							sb.append((sb.length() == 0 ? "" : ", ")
									+ String.valueOf(Integer.valueOf(value).intValue() + maxid));
						}
					}
					item.setCombine(sb.toString());
				} else {
					item.setCombine(String.valueOf(Integer.valueOf(item.getCombine()).intValue() + maxid));
				}
			}
			Data.getInstance().getTestcases().add(item);
			Data.getInstance().getMap().put(item, data.getMap().get(item));
			for (final SourceOrSink sourceOrSink : data.getMap().get(item)) {
				Data.getInstance().getMapR().put(sourceOrSink, item);
			}
		}

		maxid = -1;
		for (final SourceOrSink item : Data.getInstance().getSourcesAndSinks()) {
			if (maxid < item.getId()) {
				maxid = item.getId();
			}
		}
		for (final SourceOrSink item : data.getSourceAndSinkList()) {
			item.setId(maxid + item.getId());
			if (item.getCombine() != null && !item.getCombine().equals("null") && !item.getCombine().equals("")) {
				final StringBuilder sb = new StringBuilder();
				if (item.getCombine().contains(",")) {
					for (final String value : item.getCombine().replaceAll(" ", "").split(",")) {
						if (sb.length() != 0) {
							sb.append((sb.length() == 0 ? "" : ", ")
									+ String.valueOf(Integer.valueOf(value).intValue() + maxid));
						}
					}
					item.setCombine(sb.toString());
				} else {
					item.setCombine(String.valueOf(Integer.valueOf(item.getCombine()).intValue() + maxid));
				}
			}
			Data.getInstance().getSourcesAndSinks().add(item);
		}

		maxid = -1;
		for (final TPFP item : Data.getInstance().getTPFPs()) {
			if (maxid < item.getId()) {
				maxid = item.getId();
			}
		}
		for (final TPFP item : data.getTPFPList()) {
			item.setId(maxid + item.getId());
			Data.getInstance().getTPFPs().add(item);
		}

		Data.getInstance().setTestcaseChangedFlag(true);
		Data.getInstance().setSourcesAndSinksChangedFlag(true);

		Platform.runLater(() -> {
			setContent(this.current);
		});
	}

	public void saveAs() {
		if (Data.getInstance().getCurrentSaveFile().getParentFile().exists()
				&& Data.getInstance().getCurrentSaveFile().getParentFile().isDirectory()) {
			this.saveAndOpenDialog.setInitialDirectory(Data.getInstance().getCurrentSaveFile().getParentFile());
		} else {
			this.saveAndOpenDialog.setInitialDirectory(new File("."));
		}
		final File saveFile = this.saveAndOpenDialog.showSaveDialog(this.stage);
		if (saveFile != null) {
			Data.getInstance().setCurrentSaveFile(saveFile);
			save();
			setContent(this.current);
		}
	}

	public void save() {
		final LoadingDialog loadingDialog = new LoadingDialog("Saving");
		new Thread(() -> {
			Data.store();
			Platform.runLater(() -> {
				loadingDialog.setDone(true);
			});
		}).start();
	}

	public void exit() {
		new ExitDialog("Exit", "You will exit the GGWiz now.\n(All unsaved changes will be lost.)", "Proceed?");
	}

	private List<File> findAPKsRecursively(File folder) {
		final List<File> listOfApks = new ArrayList<>();
		for (final File file : folder.listFiles()) {
			if (file.isDirectory()) {
				listOfApks.addAll(findAPKsRecursively(file));
			} else if (file.toString().endsWith(".apk")) {
				listOfApks.add(file);
			}
		}
		return listOfApks;
	}

	public void detectFeatures() {
		FeatureDetector.getInstance().apply(this.stage, this.testCaseSelector);
	}

	public void loadSuSi() {
		SuSiLoader.getInstance().apply();
		this.sourceAndSinksSelector.refresh();
	}

	public void deselectAll() {
		for (final SourceOrSink item : Data.getInstance().getSourcesAndSinks()) {
			item.setSource(false);
			item.setSink(false);
		}
		this.sourceAndSinksSelector.refresh();
	}

	public void hideUnchecked() {
		this.sourceAndSinksSelector.setHideUnchecked(!this.sourceAndSinksSelector.isHideUnchecked());
	}

	public void view() {
		this.tpfpSelector.view();
	}

	public void export() {
		final Exporter gen = new Exporter(BREW.getOutputFolder());
		gen.export();
	}

	public void markSuccessfulResultBasedSerializable() {
		if (Data.getInstance().getCurrentSaveFile().getParentFile().exists()
				&& Data.getInstance().getCurrentSaveFile().getParentFile().isDirectory()) {
			this.saveAndOpenDialog.setInitialDirectory(Data.getInstance().getCurrentSaveFile().getParentFile());
		} else {
			this.saveAndOpenDialog.setInitialDirectory(new File("."));
		}
		final File openFile = this.saveAndOpenDialog.showOpenDialog(this.stage);
		if (openFile != null) {
			final LoadingDialog loadingDialog = new LoadingDialog("Opening");
			new Thread(() -> {
				markSuccessfulResultBasedSerializableFinished(Data.load(openFile));
				Platform.runLater(() -> {
					loadingDialog.setDone(true);
				});
			}).start();
		}
	}

	private void markSuccessfulResultBasedSerializableFinished(Data load) {
		for (int i = 0; i < load.getTPFPList().size(); i++) {
			if (load.getTPFPList().get(i).getStatus() == TPFP.SUCCESSFUL) {
				if (Data.getInstance().getTPFPs().size() >= i
						&& load.getTPFPList().get(i).equals(Data.getInstance().getTPFPs().get(i))) {
					Data.getInstance().getTPFPs().get(i).setStatus(TPFP.SUCCESSFUL);
					Log.msg("Marked " + Data.getInstance().getTPFPs().get(i).getId(), Log.DEBUG);
				} else {
					for (final TPFP tpfp : Data.getInstance().getTPFPs()) {
						if (load.getTPFPList().get(i).equals(tpfp)) {
							tpfp.setStatus(TPFP.SUCCESSFUL);
							Log.msg("Marked " + tpfp.getId(), Log.DEBUG);
							break;
						}
					}
				}
			}
		}

		refreshAll();
	}

	public void markSuccessfulResultBasedAQLAnswer() {
		boolean overapproximate;
		final Alert alert = new Alert(AlertType.CONFIRMATION);
		final Stage alertStage = (Stage) alert.getDialogPane().getScene().getWindow();
		alertStage.getIcons().add(new Image("file:data/gui/images/icon_16.png", 16, 16, false, true));
		alertStage.getIcons().add(new Image("file:data/gui/images/icon_32.png", 32, 32, false, true));
		alertStage.getIcons().add(new Image("file:data/gui/images/icon_64.png", 64, 64, false, true));
		alert.setTitle("Settings");
		alert.setHeaderText(
				"Statments can be matched on specific or generic level. The latter case leads to over-approximation.");
		alert.setContentText("Over-approximate?");
		alert.getButtonTypes().clear();
		alert.getButtonTypes().add(ButtonType.YES);
		alert.getButtonTypes().add(ButtonType.NO);

		final Optional<ButtonType> result = alert.showAndWait();
		if (result.get() == ButtonType.NO) {
			overapproximate = false;
		} else {
			overapproximate = true;
		}

		if (Data.getInstance().getCurrentSaveFile().getParentFile().exists()
				&& Data.getInstance().getCurrentSaveFile().getParentFile().isDirectory()) {
			this.loadAnswerDialog.setInitialDirectory(Data.getInstance().getCurrentSaveFile().getParentFile());
		} else {
			this.loadAnswerDialog.setInitialDirectory(new File("."));
		}
		final List<File> openFiles = this.loadAnswerDialog.showOpenMultipleDialog(this.stage);
		if (openFiles != null) {
			final List<Flow> collectedFlows = new ArrayList<>();
			for (final File openFile : openFiles) {
				final Answer answer = AnswerHandler.parseXML(openFile);
				if (answer != null && answer.getFlows() != null && !answer.getFlows().getFlow().isEmpty()) {
					collectedFlows.addAll(answer.getFlows().getFlow());
				}
			}

			final ProgressDialog progressDialog = new ProgressDialog();
			this.totalMax = collectedFlows.size();
			this.totalDone = 0;
			this.currentMax = Data.getInstance().getTPFPs().size();
			progressDialog.updateProgress(0, this.currentMax, this.totalDone, this.totalMax);

			final List<Integer> markedList = new ArrayList<>();

			new Thread(() -> {
				for (final Flow flow : collectedFlows) {
					if (!SuSiLoader.getInstance().getIgnore()
							.contains(flow.getReference().get(0).getStatement().getStatementgeneric())
							&& !SuSiLoader.getInstance().getIgnore()
									.contains(flow.getReference().get(1).getStatement().getStatementgeneric())) {
						this.currentDone = 0;
						int count = 0;
						for (final TPFP tpfp : Data.getInstance().getTPFPs()) {
							if ((overapproximate
									&& EqualsHelper.equalsIgnoreApp(Helper.getFrom(flow.getReference()),
											tpfp.getFrom().getReference())
									&& EqualsHelper.equalsIgnoreApp(Helper.getTo(flow.getReference()),
											tpfp.getTo().getReference()))
									|| (!overapproximate
											&& EqualsHelper.equals(Helper.getFrom(flow.getReference()).getStatement(),
													tpfp.getFrom().getReference().getStatement())
											&& EqualsHelper.equals(Helper.getTo(flow.getReference()).getStatement(),
													tpfp.getTo().getReference().getStatement())
											&& EqualsHelper.equalsIgnoreApp(Helper.getFrom(flow.getReference()),
													tpfp.getFrom().getReference())
											&& EqualsHelper.equalsIgnoreApp(Helper.getTo(flow.getReference()),
													tpfp.getTo().getReference()))) {
								if (tpfp.isTruepositive()) {
									tpfp.setStatus(TPFP.SUCCESSFUL);
								} else {
									tpfp.setStatus(TPFP.FAILED);
								}

								markedList.add(tpfp.getId());

								if (Log.logIt(Log.DEBUG)) {
									final Flow temp = new Flow();
									tpfp.getFrom().getReference().setType(KeywordsAndConstants.REFERENCE_TYPE_FROM);
									temp.getReference().add(tpfp.getFrom().getReference());
									tpfp.getTo().getReference().setType(KeywordsAndConstants.REFERENCE_TYPE_TO);
									temp.getReference().add(tpfp.getTo().getReference());
									Log.msg(Helper.toString(temp) + "\n == \n" + Helper.toString(flow),
											Log.DEBUG_DETAILED);
								}
								break;
							}
							count++;
							this.currentDone++;
							if (count >= this.currentMax / 100) {
								progressDialog.updateProgress(this.currentDone, this.currentMax, this.totalDone,
										this.totalMax);
								count = 0;
							}
						}
					}
					progressDialog.updateProgress(0, this.currentMax, this.totalDone++, this.totalMax);
				}
				progressDialog.updateProgress(this.currentMax, this.currentMax, this.totalMax, this.totalMax);
				markSuccessfulResultBasedAQLAnswerFinished(markedList);
			}).start();
		}
	}

	public void markSuccessfulResultBasedAQLAnswerFinished(List<Integer> list) {
		for (final TPFP tpfp : Data.getInstance().getTPFPs()) {
			if (tpfp.getStatus() != TPFP.SUCCESSFUL) {
				if (tpfp.isFalsepositive()) {
					tpfp.setStatus(TPFP.SUCCESSFUL);
				} else {
					tpfp.setStatus(TPFP.FAILED);
				}
			}
		}

		if (Log.logIt(Log.DEBUG)) {
			Collections.sort(list);
			final StringBuilder sb = new StringBuilder("Marked (" + list.size() + "): ");
			for (int i = 0; i < list.size(); i++) {
				sb.append(list.get(i) + (i != list.size() - 1 ? ", " : ""));
			}
			Log.msg(sb.toString(), Log.DEBUG);
		}

		refreshAll();
	}

	public void removeNotSuccessFul() {
		final List<TPFP> removeList = new ArrayList<>();
		final List<TPFP> keepList = new ArrayList<>();
		for (final TPFP tpfp : Data.getInstance().getTPFPs()) {
			if (tpfp.getStatus() != TPFP.SUCCESSFUL) {
				removeList.add(tpfp);
			} else {
				keepList.add(tpfp);
			}
		}

		final ProgressDialog progressDialog = new ProgressDialog();
		final int totalMax = removeList.size();
		progressDialog.updateProgress(1, totalMax, 1, totalMax);
		new Thread(() -> {
			int currentDone = 1;
			for (final TPFP tpfp : removeList) {
				boolean sourceInKeepList = false;
				for (final TPFP check : keepList) {
					if (check.getFrom().equals(tpfp.getFrom())) {
						sourceInKeepList = true;
						break;
					}
				}
				boolean sinkInKeepList = false;
				for (final TPFP check : keepList) {
					if (check.getTo().equals(tpfp.getTo())) {
						sinkInKeepList = true;
						break;
					}
				}
				if (!sourceInKeepList) {
					tpfp.getFrom().setSource(false);
				}
				if (!sinkInKeepList) {
					tpfp.getTo().setSink(false);
				}
				progressDialog.updateProgress(currentDone++, totalMax, 1, totalMax);
			}

			int totalDone = 1;
			for (final TPFP tpfp : removeList) {
				Data.getInstance().getTPFPs().remove(tpfp);
				progressDialog.updateProgress(totalMax, totalMax, totalDone++, totalMax);
			}
		}).start();
	}

	public void resetMarkings() {
		for (final TPFP tpfp : Data.getInstance().getTPFPs()) {
			tpfp.setStatus(TPFP.DEFAULT);
			tpfp.setAborted(false);
		}
		refreshAll();
	}

	public void refreshIDs() {
		final Map<Integer, Integer> remember = new HashMap<>();

		// Testcases
		int id = 0;
		for (final Testcase item : Data.getInstance().getTestcases()) {
			id++;
			remember.put(item.getId(), id);
			item.setId(id);
		}
		Data.getInstance().setTestcaseid(id);
		for (final Testcase item : Data.getInstance().getTestcases()) {
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
		for (final SourceOrSink item : Data.getInstance().getSourcesAndSinks()) {
			final Testcase temp = Data.getInstance().getMapR().get(item);
			id++;
			remember.put(item.getId(), id);
			item.setId(id);
			newMapR.put(item, temp);
		}
		Data.getInstance().setMapR(newMapR);
		Data.getInstance().setSourceOrSinkid(id);
		for (final SourceOrSink item : Data.getInstance().getSourcesAndSinks()) {
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
		for (final TPFP item : Data.getInstance().getTPFPs()) {
			id++;
			item.setId(id);
		}
		Data.getInstance().setTpfpid(id);

		refreshAll();
	}

	private void refreshAll() {
		if (this.testCaseSelector != null) {
			this.testCaseSelector.refresh();
		}
		if (this.sourceAndSinksSelector != null) {
			this.sourceAndSinksSelector.refresh();
		}
		if (this.tpfpSelector != null) {
			this.tpfpSelector.refresh();
		}
	}

	public void start() {
		final Runner runner = new Runner(this.tpfpSelector);
		runner.runAll(Data.getInstance().getTPFPs());
	}

	public Stage getStage() {
		return this.stage;
	}
}
