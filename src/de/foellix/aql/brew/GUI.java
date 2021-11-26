package de.foellix.aql.brew;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import java.util.Set;

import de.foellix.aql.Log;
import de.foellix.aql.Properties;
import de.foellix.aql.brew.sourceandsinkselector.SourceAndSinkSelector;
import de.foellix.aql.brew.sourceandsinkselector.SourceOrSink;
import de.foellix.aql.brew.sourceandsinkselector.SuSiLoader;
import de.foellix.aql.brew.testcaseselector.FeatureDetector;
import de.foellix.aql.brew.testcaseselector.TestCaseSelector;
import de.foellix.aql.brew.testcaseselector.Testcase;
import de.foellix.aql.brew.tpfpselector.Runner;
import de.foellix.aql.brew.tpfpselector.TPFP;
import de.foellix.aql.brew.tpfpselector.TPFPSelector;
import de.foellix.aql.config.ConfigHandler;
import de.foellix.aql.datastructure.Answer;
import de.foellix.aql.datastructure.Flow;
import de.foellix.aql.datastructure.handler.AnswerHandler;
import de.foellix.aql.helper.CLIHelper;
import de.foellix.aql.helper.EqualsHelper;
import de.foellix.aql.helper.EqualsOptions;
import de.foellix.aql.helper.Helper;
import de.foellix.aql.helper.KeywordsAndConstantsHelper;
import de.foellix.aql.system.task.gui.TaskTreeViewer;
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
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

public class GUI extends Application {
	private Stage stage;

	private static Scanner scannerIn;

	private BorderPane mainPane;
	private MenuBar menuBar;
	public static final int TEXT_CASE_SELECTOR = 0;
	public static final int SOURCE_SINK_SELECTOR = 1;
	public static final int TP_FP_SELECTOR = 2;
	private int current = TEXT_CASE_SELECTOR;

	private Exporter exporter;
	private FileChooser loadFileDialog, saveAndOpenDialog, loadAnswerDialog;
	private DirectoryChooser loadDirectoryDialog;
	private int totalDone, totalMax, currentDone, currentMax;

	private TestCaseSelector testCaseSelector;
	private SourceAndSinkSelector sourceAndSinksSelector;
	private TPFPSelector tpfpSelector;

	private long time;

	@Override
	public void start(Stage stage) throws Exception {
		this.time = System.currentTimeMillis();
		BREW.init();
		DatabaseHandler.getInstance();
		Data.init();
		init(stage);
		Log.msg("Initialization took: " + (System.currentTimeMillis() - this.time) + "ms", Log.DEBUG_DETAILED);

		// Splash Screen
		if (SplashScreen.SPLASH_SCREEN.exists() && !BREW.getOptions().getNoSplashScreen()) {
			final SplashScreen splashScreen = new SplashScreen(Properties.info().ABBREVIATION + " - "
					+ Properties.info().NAME + " (v. " + Properties.info().VERSION + ")",
					"by " + Properties.info().AUTHOR, Color.WHITE);
			new Thread(() -> {
				try {
					this.time = System.currentTimeMillis() - this.time;
					if (this.time < 3000) {
						Thread.sleep(3000 - this.time);
					}
				} catch (final Exception e) {
					// do nothing
				}
				Platform.runLater(() -> {
					splashScreen.setDone(true);
					startGUI();
				});
			}).start();
		} else {
			startGUI();
		}
	}

	private void init(Stage stage) {
		// TODO: (After 2.0.0 release) Check if future openjfx versions still require this silencing (last tested with 18-ea+4; Part 2/2)
		Log.setSilence(false);

		this.stage = stage;

		this.stage.getIcons().add(new Image("file:data/gui/images/icon_16.png", 16, 16, false, true));
		this.stage.getIcons().add(new Image("file:data/gui/images/icon_32.png", 32, 32, false, true));
		this.stage.getIcons().add(new Image("file:data/gui/images/icon_64.png", 64, 64, false, true));

		this.mainPane = new BorderPane();
		this.menuBar = new MenuBar(this);
		this.mainPane.setTop(this.menuBar);
		this.mainPane.setBottom(Statistics.getInstance().getStatisticsBox());

		final Scene scene = new Scene(this.mainPane, 1024, 768);
		scene.getStylesheets().add("file:data/gui/style.css");
		this.stage.setScene(scene);

		// Key presses
		stage.getScene().addEventFilter(KeyEvent.KEY_PRESSED, (evt) -> {
			if (evt.getCode() == KeyCode.F5) {
				refreshIDs();
			}
		});

		// Closing window
		this.stage.setOnCloseRequest(new EventHandler<WindowEvent>() {
			@Override
			public void handle(final WindowEvent we) {
				we.consume();
				exit();
			}
		});

		// Drag and Drop
		scene.setOnDragOver(new EventHandler<DragEvent>() {
			@Override
			public void handle(DragEvent event) {
				final Dragboard db = event.getDragboard();
				if (db.hasFiles()) {
					event.acceptTransferModes(TransferMode.COPY);
				} else {
					event.consume();
				}
			}
		});
		scene.setOnDragDropped(new EventHandler<DragEvent>() {
			@Override
			public void handle(DragEvent event) {
				final Dragboard db = event.getDragboard();
				boolean success = false;
				if (db.hasFiles()) {
					success = true;
					openFile(db.getFiles().get(0));
				}
				event.setDropCompleted(success);
				event.consume();
			}
		});

		// Dialogs
		this.exporter = null;
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
		final FileChooser.ExtensionFilter zipFilter = new FileChooser.ExtensionFilter(
				"*.zip Zipped Serializable Object", "*.zip");
		this.saveAndOpenDialog.getExtensionFilters().addAll(allFilter, zipFilter, serFilter);
		this.saveAndOpenDialog.setSelectedExtensionFilter(zipFilter);
		this.loadAnswerDialog = new FileChooser();
		final FileChooser.ExtensionFilter xmlFilter = new FileChooser.ExtensionFilter(
				"*.xml Extensible Markup Language", "*.xml");
		this.loadAnswerDialog.getExtensionFilters().addAll(allFilter, xmlFilter);
		this.loadAnswerDialog.setSelectedExtensionFilter(xmlFilter);
	}

	private void startGUI() {
		this.stage.show();
		setContent(this.current);
		if (!BREW.getConfigEvaluated()) {
			CLIHelper.setStage(this.stage);
		}

		ConfigHandler.getInstance().getConfig();

		if (BREW.getOptions().getDrawGraphs()) {
			final TaskTreeViewer ttv = new TaskTreeViewer();
			try {
				ttv.start(new Stage());
			} catch (final Exception e) {
				Log.warning("Could not start execution graph viewer!");
			}
		}
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
				this.stage.setTitle("BREW - Positive & Negative Case Selector ("
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
						"BREW - App Selector (" + Data.getInstance().getCurrentSaveFile().getAbsolutePath() + ")");
				if (this.testCaseSelector == null) {
					this.testCaseSelector = new TestCaseSelector(this.stage);
				} else {
					this.testCaseSelector.onResume();
				}
				this.mainPane.setCenter(this.testCaseSelector);
				break;
		}
		Statistics.getInstance().refresh();
	}

	public void prev() {
		if (this.current > TEXT_CASE_SELECTOR) {
			this.current--;
			MenuBar.deactivateBtns();
			setContent(this.current);
		}
	}

	public void next() {
		if (this.current < TP_FP_SELECTOR) {
			this.current++;
			MenuBar.deactivateBtns();
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
		Data.init(null);
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
		openFile(openFile);
	}

	private void openFile(File file) {
		if (file != null) {
			final LoadingDialog loadingDialog = new LoadingDialog("Opening");
			MenuBar.deactivateBtns();
			new Thread(() -> {
				Data.init(file);
				Data.getInstance().setCurrentSaveFile(file);
				this.testCaseSelector = null;
				this.sourceAndSinksSelector = null;
				this.tpfpSelector = null;
				Platform.runLater(() -> {
					setContent(this.current);
					loadingDialog.setDone(true);
				});
				MenuBar.activateBtns();
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
		new ExitDialog("Exit",
				"You will exit the " + Properties.info().ABBREVIATION + " now.\n(All unsaved changes will be lost.)",
				"Proceed?");
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
		Data.getInstance().deselectAllSourcesAndSinks();
		this.sourceAndSinksSelector.refresh();
	}

	public void hideUnchecked() {
		this.sourceAndSinksSelector.setHideUnchecked(!this.sourceAndSinksSelector.isHideUnchecked());
	}

	public void viewAnswer() {
		this.tpfpSelector.viewAnswer();
	}

	public void viewExecutionGraph() {
		this.tpfpSelector.viewExecutionGraph();
	}

	public void exportAnswers() {
		final LoadingDialog loadingDialog = new LoadingDialog("Exporting");
		new Thread(() -> {
			createExporter();
			this.exporter.exportAnswers();
			Platform.runLater(() -> {
				loadingDialog.setDone(true);
			});
		}).start();
	}

	public void exportSourcesAndSinks() {
		final LoadingDialog loadingDialog = new LoadingDialog("Exporting");
		new Thread(() -> {
			createExporter();
			this.exporter.exportSourcesAndSinksAllFormats(Data.getInstance().getSourcesAndSinks());
			Platform.runLater(() -> {
				loadingDialog.setDone(true);
			});
		}).start();
	}

	private void createExporter() {
		if (this.exporter == null) {
			this.exporter = new Exporter(BREW.getOutputFolder());
		}
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
			try {
				if (i < load.getTPFPList().size() && load.getTPFPList().get(i).getStatus() == TPFP.SUCCESSFUL) {
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
			} catch (final IndexOutOfBoundsException e) {
				break;
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
				final EqualsOptions options = new EqualsOptions();
				options.setOption(EqualsOptions.PRECISELY_REFERENCE, overapproximate);

				for (final Flow flow : collectedFlows) {
					if (!SuSiLoader.getInstance().getIgnore()
							.contains(flow.getReference().get(0).getStatement().getStatementgeneric())
							&& !SuSiLoader.getInstance().getIgnore()
									.contains(flow.getReference().get(1).getStatement().getStatementgeneric())) {
						this.currentDone = 0;
						int count = 0;
						for (final TPFP tpfp : Data.getInstance().getTPFPs()) {
							if (EqualsHelper.equals(Helper.getFrom(flow.getReference()), tpfp.getFrom().getReference(),
									options)
									&& EqualsHelper.equals(Helper.getTo(flow.getReference()),
											tpfp.getTo().getReference(), options)) {
								if (tpfp.isTruepositive()) {
									tpfp.setStatus(TPFP.SUCCESSFUL);
								} else {
									tpfp.setStatus(TPFP.FAILED);
								}

								markedList.add(tpfp.getId());

								if (Log.logIt(Log.DEBUG)) {
									final Flow temp = new Flow();
									tpfp.getFrom().getReference()
											.setType(KeywordsAndConstantsHelper.REFERENCE_TYPE_FROM);
									temp.getReference().add(tpfp.getFrom().getReference());
									tpfp.getTo().getReference().setType(KeywordsAndConstantsHelper.REFERENCE_TYPE_TO);
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
			if (tpfp.getStatus() == TPFP.DEFAULT) {
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
			tpfp.reset();
		}
		refreshAll();
	}

	public void refreshResults() {
		for (final TPFP tpfp : Data.getInstance().getTPFPs()) {
			if (Runner.contains(tpfp.getActualAnswer(), tpfp.toAnswer())) {
				tpfp.setStatus((tpfp.isTruepositive() ? TPFP.SUCCESSFUL : TPFP.FAILED));
			} else {
				tpfp.setStatus((tpfp.isTruepositive() ? TPFP.FAILED : TPFP.SUCCESSFUL));
			}
		}
		refreshAll();
	}

	public void compareTo() {
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
				compareTo(Data.load(openFile));
				Platform.runLater(() -> {
					loadingDialog.setDone(true);
				});
			}).start();
		}
	}

	private void compareTo(Data load) {
		int foundCounter = 0;
		int introducedTPs = 0; // Introduced TPs; Eliminated FNs
		int introducedFPs = 0; // Introduced FPs; Eliminated TNs
		int introducedTNs = 0; // Introduced TNs; Eliminated FPs
		int introducedFNs = 0; // Introduced FNs; Eliminated TPs
		final Set<String> introducedByNonAbortCounter = new HashSet<>();
		final Set<String> eliminatedByAbortCounter = new HashSet<>();
		final int[] introducedByNonAborts = new int[] { 0, 0, 0 };
		final int[] eliminatedByAborts = new int[] { 0, 0, 0 };
		for (final TPFP loadedTPFP : load.getTPFPList()) {
			TPFP found = null;
			for (final TPFP tpfp : Data.getInstance().getTPFPs()) {
				if (loadedTPFP.equals(tpfp)) {
					found = tpfp;
					foundCounter++;
					break;
				}
			}
			if (found == null) {
				for (final TPFP tpfp : Data.getInstance().getTPFPs()) {
					if (loadedTPFP.getId() == tpfp.getId()) {
						if (loadedTPFP.equalsSloppy(tpfp)) {
							found = tpfp;
							foundCounter++;
						}
						break;
					}
				}
			}
			if (found != null) {
				// Overall
				if (found.getStatus() != loadedTPFP.getStatus()) {
					if (found.getStatus() == TPFP.SUCCESSFUL && found.isTruepositive()) {
						introducedFNs++;
						Log.msg(introducedFNs + ") Introduced FN / Eliminated TP: " + found.getId(), Log.NORMAL);
					} else if (found.getStatus() == TPFP.SUCCESSFUL && found.isFalsepositive()) {
						introducedFPs++;
						Log.msg(introducedFPs + ") Introduced FP / Eliminated TN: " + found.getId(), Log.NORMAL);
					} else if (found.getStatus() != TPFP.SUCCESSFUL && found.isTruepositive()) {
						introducedTPs++;
						Log.msg(introducedTPs + ") Introduced TP / Eliminated FN: " + found.getId(), Log.NORMAL);
					} else if (found.getStatus() != TPFP.SUCCESSFUL && found.isFalsepositive()) {
						introducedTNs++;
						Log.msg(introducedTNs + ") Introduced TN / Eliminated FP: " + found.getId(), Log.NORMAL);
					}
				}

				// Aborts
				if (loadedTPFP.isAborted() && !found.isAborted()) {
					if (found.getStatus() == TPFP.SUCCESSFUL && found.isTruepositive()) {
						eliminatedByAborts[0]++;
					} else if (found.getStatus() != TPFP.SUCCESSFUL && found.isFalsepositive()) {
						eliminatedByAborts[1]++;
					}
					eliminatedByAborts[2]++;
					final String identifier = found.getFrom().getReference().getApp().getFile() + "#"
							+ found.getTo().getReference().getApp().getFile();
					if (!eliminatedByAbortCounter.contains(identifier)) {
						eliminatedByAbortCounter.add(identifier);
					}
					Log.msg("Abort introduced: " + found.getId(), Log.NORMAL);
				}
				// Non-Aborts
				if (!loadedTPFP.isAborted() && found.isAborted()) {
					if (loadedTPFP.getStatus() == TPFP.SUCCESSFUL && loadedTPFP.isTruepositive()) {
						introducedByNonAborts[0]++;
					} else if (loadedTPFP.getStatus() != TPFP.SUCCESSFUL && loadedTPFP.isFalsepositive()) {
						introducedByNonAborts[1]++;
					}
					introducedByNonAborts[2]++;
					final String identifier = found.getFrom().getReference().getApp().getFile() + "#"
							+ found.getTo().getReference().getApp().getFile();
					if (!introducedByNonAbortCounter.contains(identifier)) {
						introducedByNonAbortCounter.add(identifier);
					}
					Log.msg("Abort eliminated: " + found.getId(), Log.NORMAL);
				}
			} else {
				Log.msg("Case " + loadedTPFP.getId() + " not found!", Log.NORMAL);
			}
		}

		final String result = "Found: " + foundCounter + " of " + Data.getInstance().getTPFPs().size()
				+ "\n\nIntroduced TPs; Eliminated FNs: " + introducedTPs + "\nIntroduced FPs; Eliminated TNs: "
				+ introducedFPs + "\nIntroduced TNs; Eliminated FPs: " + introducedTNs
				+ "\nIntroduced FNs; Eliminated TPs: " + introducedFNs + "\n\nIntroduced TPs - Eliminated TPs: "
				+ (introducedTPs - introducedFNs) + "\nIntroduced FPs - Eliminated FPs: "
				+ (introducedFPs - introducedTNs) + "\nIntroduced TNs - Eliminated TNs: "
				+ (introducedTNs - introducedFPs) + "\nIntroduced FNs - Eliminated FNs: "
				+ (introducedFNs - introducedTPs) + "\n\nEliminated Aborts: " + introducedByNonAborts[2] + " ("
				+ introducedByNonAbortCounter.size() + "; TPs: " + introducedByNonAborts[0] + ", FPs: "
				+ introducedByNonAborts[1] + ")" + "\nIntroduced Aborts: " + eliminatedByAborts[2] + " ("
				+ eliminatedByAbortCounter.size() + "; TPs: " + eliminatedByAborts[0] + ", FPs: "
				+ eliminatedByAborts[1] + ")";
		Platform.runLater(() -> {
			final Alert alert = new Alert(AlertType.INFORMATION);
			final Stage alertStage = (Stage) alert.getDialogPane().getScene().getWindow();
			alertStage.getIcons().add(new Image("file:data/gui/images/icon_16.png", 16, 16, false, true));
			alertStage.getIcons().add(new Image("file:data/gui/images/icon_32.png", 32, 32, false, true));
			alertStage.getIcons().add(new Image("file:data/gui/images/icon_64.png", 64, 64, false, true));
			alert.setTitle("Comparison Result");
			alert.setHeaderText(result);
			alert.setContentText("This result was also printed to the console.");
			alert.show();
		});
		Log.msg(result, Log.NORMAL);

		refreshAll();
	}

	public void refreshIDs() {
		Data.getInstance().refreshIDs();
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
		if ((MenuBar.fromField.getText() == null || MenuBar.fromField.getText().isEmpty())
				&& (MenuBar.toField.getText() == null || MenuBar.toField.getText().isEmpty())) {
			runner.runAll(Data.getInstance().getTPFPs());
		} else {
			int from = Data.getInstance().getTPFPs().get(0).getId();
			if (MenuBar.fromField.getText() != null && !MenuBar.fromField.getText().isEmpty()) {
				from = Integer.valueOf(MenuBar.fromField.getText()).intValue();
			}
			int to = Data.getInstance().getTPFPs().get(Data.getInstance().getTPFPs().size() - 1).getId();
			if (MenuBar.toField.getText() != null && !MenuBar.toField.getText().isEmpty()) {
				to = Integer.valueOf(MenuBar.toField.getText()).intValue();
			}
			runner.runAll(Data.getInstance().gettpfps(from, to));
		}
	}

	public Stage getStage() {
		return this.stage;
	}

	public static Scanner getScanner() {
		if (scannerIn == null) {
			scannerIn = new Scanner(System.in);
		}
		return scannerIn;
	}
}