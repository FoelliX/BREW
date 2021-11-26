package de.foellix.aql.brew.tpfpselector;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import de.foellix.aql.Log;
import de.foellix.aql.brew.BREW;
import de.foellix.aql.brew.Data;
import de.foellix.aql.brew.MenuBar;
import de.foellix.aql.brew.Statistics;
import de.foellix.aql.brew.sourceandsinkselector.SourceOrSink;
import de.foellix.aql.brew.testcaseselector.Testcase;
import de.foellix.aql.datastructure.Answer;
import de.foellix.aql.datastructure.Attribute;
import de.foellix.aql.datastructure.Flow;
import de.foellix.aql.datastructure.Reference;
import de.foellix.aql.datastructure.handler.AnswerHandler;
import de.foellix.aql.helper.EqualsHelper;
import de.foellix.aql.helper.EqualsOptions;
import de.foellix.aql.helper.Helper;
import de.foellix.aql.system.task.gui.TaskTreeViewer;
import de.foellix.aql.ui.gui.ProgressDialog;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.EventHandler;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;

public class TPFPSelector extends TableView<TPFP> {
	public static final EqualsOptions EQUALS_OPTIONS = new EqualsOptions().setOption(EqualsOptions.CONSIDER_LINENUMBER,
			true);

	private boolean alternate = false;
	private boolean taintBenchLoaded = false;
	private boolean keepGenericNegativeCases = true;
	private boolean keepGenericNegativeCasesDone = false;

	private ProgressDialog progressDialog;
	private int totalMax, totalDone, currentMax, currentDone;
	private boolean restore;
	private boolean loadGivenGroundTruth;

	private FilteredList<TPFP> filteredData;
	private long lastRefresh;

	@SuppressWarnings("unchecked")
	public TPFPSelector() {
		super();

		this.loadGivenGroundTruth = true;

		this.filteredData = new FilteredList<>(Data.getInstance().getTPFPs(), tpfp -> {
			for (final String part : MenuBar.searchField.getText().toLowerCase().split(" ")) {
				if (!tpfp.getCase().toLowerCase().contains(part)
						&& !tpfp.getBenchmarkcaseComplete().toLowerCase().contains(part)) {
					return false;
				}
			}
			return true;
		});
		MenuBar.searchField.textProperty().addListener((observable, oldValue, newValue) -> {
			this.lastRefresh = System.currentTimeMillis();
			final long localLastRefresh = this.lastRefresh;

			new Thread(() -> {
				try {
					Thread.sleep(500);
				} catch (final InterruptedException e) {
					// do nothing
				}

				if (this.lastRefresh == localLastRefresh) {
					this.filteredData.setPredicate(tpfp -> {
						for (final String part : newValue.toLowerCase().split(" ")) {
							if (!tpfp.getCase().toLowerCase().contains(part) && (tpfp.getBenchmarkcaseComplete() != null
									&& !tpfp.getBenchmarkcaseComplete().toLowerCase().contains(part))) {
								return false;
							}
						}
						return true;
					});
					Platform.runLater(() -> refresh());
				}
			}).start();
		});
		final SortedList<TPFP> sortedList = new SortedList<>(this.filteredData);
		sortedList.comparatorProperty().bind(this.comparatorProperty());
		this.setItems(sortedList);

		final TableColumn<TPFP, Integer> colID = new TableColumn<>("ID");
		colID.setCellValueFactory(new PropertyValueFactory<TPFP, Integer>("id"));
		colID.setPrefWidth(150);

		final TableColumn<TPFP, String> colTestcase = new TableColumn<>("Benchmark App");
		colTestcase.setCellValueFactory(new PropertyValueFactory<TPFP, String>("benchmarkcase"));
		colTestcase.setMinWidth(200);

		final TableColumn<TPFP, String> colMethod = new TableColumn<>("Benchmark Case");
		colMethod.setCellValueFactory(new PropertyValueFactory<TPFP, String>("case"));
		colMethod.setMinWidth(400);

		final TableColumn<TPFP, Boolean> colTP = new TableColumn<>("Expected (Positive)");
		colTP.setCellValueFactory(new PropertyValueFactory<TPFP, Boolean>("truepositive"));
		colTP.setCellFactory(col -> {
			final CheckBoxTableCell<TPFP, Boolean> cell = new CheckBoxTableCell<>(index -> {
				final BooleanProperty active = new SimpleBooleanProperty(this.getItems().get(index).isTruepositive());
				active.addListener((obs, wasTP, isNowTP) -> {
					final TPFP item = this.getItems().get(index);
					item.setTruepositive(isNowTP);
					if (isNowTP && item.isFalsepositive()) {
						item.setFalsepositive(false);
					}
					this.refresh();
				});
				return active;
			});
			return cell;
		});
		colTP.setPrefWidth(150);

		final TableColumn<TPFP, Boolean> colSink = new TableColumn<>("Not Expected (Negative)");
		colSink.setCellValueFactory(new PropertyValueFactory<TPFP, Boolean>("falsepositive"));
		colSink.setCellFactory(col -> {
			final CheckBoxTableCell<TPFP, Boolean> cell = new CheckBoxTableCell<>(index -> {
				final BooleanProperty active = new SimpleBooleanProperty(this.getItems().get(index).isFalsepositive());
				active.addListener((obs, wasFP, isNowFP) -> {
					final TPFP item = this.getItems().get(index);
					item.setFalsepositive(isNowFP);
					if (isNowFP && item.isTruepositive()) {
						item.setTruepositive(false);
					}
					this.refresh();
				});
				return active;
			});
			return cell;
		});
		colSink.setPrefWidth(150);

		final TableColumn<TPFP, String> colFlowsFound = new TableColumn<>("Flows found");
		colFlowsFound.setCellValueFactory(new PropertyValueFactory<TPFP, String>("flowsFound"));
		colFlowsFound.setStyle("-fx-alignment: CENTER_RIGHT;");
		colFlowsFound.setPrefWidth(100);

		final TableColumn<TPFP, Integer> colDuration = new TableColumn<>("Duration (s)");
		colDuration.setCellValueFactory(new PropertyValueFactory<TPFP, Integer>("duration"));
		colDuration.setStyle("-fx-alignment: CENTER_RIGHT;");
		colDuration.setPrefWidth(100);

		this.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
		this.setEditable(true);
		this.getColumns().addAll(colID, colTestcase, colMethod, colTP, colSink, colFlowsFound, colDuration);
		this.setOnKeyPressed((event) -> {
			if (event.getCode() == KeyCode.DELETE) {
				delete(this.getSelectionModel().getSelectedItem());
			} else if (event.getCode() == KeyCode.V) {
				viewAnswer();
			} else if (event.getCode() == KeyCode.S) {
				viewExecutionGraph();
			} else if (event.getCode() == KeyCode.SPACE) {
				final TPFP item = getSelectionModel().getSelectedItem();
				if (!item.isTruepositive() && !item.isFalsepositive()) {
					item.setTruepositive(true);
				} else if (item.isTruepositive() && !item.isFalsepositive()) {
					item.setTruepositive(false);
					item.setFalsepositive(true);
				} else if (!item.isTruepositive() && item.isFalsepositive()) {
					item.setTruepositive(true);
					item.setFalsepositive(false);
				}
				this.refresh();
			}
			if (event.getCode() != KeyCode.UP && event.getCode() != KeyCode.DOWN) {
				event.consume();
			}
		});
		this.addEventFilter(MouseEvent.MOUSE_RELEASED, new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent event) {
				if (event.getButton() == MouseButton.SECONDARY) {
					event.consume();
					delete(getSelectionModel().getSelectedItem());
				}
			}
		});
		this.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
			if (newSelection != null) {
				final TPFP tpfpSelected = this.getSelectionModel().getSelectedItem();
				final Runner runner = new Runner(tpfpSelected);
				String query = runner.getQuery(true);
				if (!tpfpSelected.getAttributes().isEmpty()) {
					query = Helper.replaceCustomVariables(query, tpfpSelected.getAttributes());
				}
				final StringBuilder fromSB = new StringBuilder("From:");
				for (final SourceOrSink sos : this.getSelectionModel().getSelectedItem().getFrom().getAll()) {
					fromSB.append("\n- " + Helper.toString(sos.getReference(), "\n\t->"));
				}
				final StringBuilder toSB = new StringBuilder("To:");
				for (final SourceOrSink sos : this.getSelectionModel().getSelectedItem().getTo().getAll()) {
					toSB.append("\n- " + Helper.toString(sos.getReference(), "\n\t->"));
				}
				Statistics.getInstance().getStatisticsBox().setInformation(fromSB.toString(), toSB.toString(),
						"Query:\n" + (query == null
								? "** The current configuration does not allow an appropiate query formulation for this case. **"
								: Helper.autoformat(query)) + "\nAttributes:\n"
								+ this.getSelectionModel().getSelectedItem().getAttributesAsString());
			}
		});

		this.setRowFactory(tv -> new TableRow<>() {
			@Override
			public void updateItem(TPFP item, boolean empty) {
				super.updateItem(item, empty);
				TPFPSelector.this.alternate = getIndex() % 2 == 0;
				if (isSelected()) {
					if (item == null) {
						setStyle(
								"-fx-background-color: #0096C9; -fx-selection-bar: #0096C9; -fx-selection-bar-non-focused: #0096C9;");
					} else if (item.isAborted()) {
						setStyle(
								"-fx-background-color: #7bceff; -fx-selection-bar: #7bceff; -fx-selection-bar-non-focused: #7bceff;");
					} else if (item.getStatus() == TPFP.SUCCESSFUL) {
						setStyle(
								"-fx-background-color: #9ab764; -fx-selection-bar: #9ab764; -fx-selection-bar-non-focused: #9ab764;");
					} else if (item.getStatus() == TPFP.FAILED) {
						setStyle(
								"-fx-background-color: #d47575; -fx-selection-bar: #d47575; -fx-selection-bar-non-focused: #d47575;");
					} else {
						setStyle(
								"-fx-background-color: #0096C9; -fx-selection-bar: #0096C9; -fx-selection-bar-non-focused: #0096C9;");
					}
				} else {
					if (item == null) {
						if (TPFPSelector.this.alternate) {
							setStyle("-fx-background-color: #FFFFFF;");
						} else {
							setStyle("-fx-background-color: #F8F8F8;");
						}
					} else if (item.isAborted()) {
						if (TPFPSelector.this.alternate) {
							setStyle("-fx-background-color: #dbf1ff;");
						} else {
							setStyle("-fx-background-color: #d1eeff;");
						}
					} else if (item.getStatus() == TPFP.SUCCESSFUL) {
						if (TPFPSelector.this.alternate) {
							setStyle("-fx-background-color: #f5ffe2;");
						} else {
							setStyle("-fx-background-color: #efffd1;");
						}
					} else if (item.getStatus() == TPFP.FAILED) {
						if (TPFPSelector.this.alternate) {
							setStyle("-fx-background-color: #ffe2e2;");
						} else {
							setStyle("-fx-background-color: #ffd1d1;");
						}
					} else {
						if (TPFPSelector.this.alternate) {
							setStyle("-fx-background-color: #FFFFFF;");
						} else {
							setStyle("-fx-background-color: #F8F8F8;");
						}
					}
				}
				TPFPSelector.this.alternate = !TPFPSelector.this.alternate;
			}
		});

		onResume();
	}

	private void loadTaintBench() {
		if (!Data.getInstance().getTPFPList().isEmpty()) {
			if (BREW.getTaintBenchLoader() != null && !this.taintBenchLoaded) {
				this.taintBenchLoaded = true;
				askKeepGenerics();
				BREW.getTaintBenchLoader().applyToTPFPs(this.keepGenericNegativeCases);
				refresh();
			}
		}
	}

	private void keepGenericNegativeCases() {
		this.keepGenericNegativeCasesDone = false;

		Platform.runLater(() -> {
			final Alert alert = new Alert(AlertType.CONFIRMATION);

			final Stage alertStage = (Stage) alert.getDialogPane().getScene().getWindow();
			alertStage.getIcons().add(new Image("file:data/gui/images/icon_16.png", 16, 16, false, true));
			alertStage.getIcons().add(new Image("file:data/gui/images/icon_32.png", 32, 32, false, true));
			alertStage.getIcons().add(new Image("file:data/gui/images/icon_64.png", 64, 64, false, true));
			alert.setTitle("Generating cases");
			alert.setHeaderText(
					"For each (not specified) combination of source and sink a generic negative case is generated.");
			alert.setContentText("Keep generic negative cases?");
			alert.getButtonTypes().clear();
			alert.getButtonTypes().addAll(ButtonType.YES, ButtonType.NO);

			final Optional<ButtonType> result = alert.showAndWait();
			if (result.get() == ButtonType.YES) {
				this.keepGenericNegativeCases = true;
			} else {
				this.keepGenericNegativeCases = false;
			}

			this.keepGenericNegativeCasesDone = true;
		});
	}

	private void delete(TPFP tpfp) {
		final Alert alert = new Alert(AlertType.CONFIRMATION);

		final Stage alertStage = (Stage) alert.getDialogPane().getScene().getWindow();
		alertStage.getIcons().add(new Image("file:data/gui/images/icon_16.png", 16, 16, false, true));
		alertStage.getIcons().add(new Image("file:data/gui/images/icon_32.png", 32, 32, false, true));
		alertStage.getIcons().add(new Image("file:data/gui/images/icon_64.png", 64, 64, false, true));
		alert.setTitle("Remove");
		alert.setHeaderText("The following case will be removed:\n" + tpfp.getId() + ": " + tpfp.getCase());
		alert.setContentText("Proceed?");

		final Optional<ButtonType> result = alert.showAndWait();
		if (result.get() == ButtonType.OK) {
			Data.getInstance().getTPFPs().remove(tpfp);
		} else {
			alert.hide();
		}
	}

	public void viewAnswer() {
		try {
			new ComparisonViewer(this.getSelectionModel().getSelectedItem());
		} catch (final NullPointerException e) {
			Log.msg("Please select a case first.", Log.NORMAL);
		}
	}

	public void viewExecutionGraph() {
		try {
			if (this.getSelectionModel().getSelectedItem().getTaskTreeSnapshot() != null) {
				TaskTreeViewer.update(this.getSelectionModel().getSelectedItem().getTaskTreeSnapshot());
			} else {
				Log.msg("No execution graph available, yet.", Log.NORMAL);
			}
		} catch (final NullPointerException e) {
			Log.msg("Please select a case first.", Log.NORMAL);
		}
	}

	public void onResume() {
		if (Data.getInstance().sourceAndSinksHaveChanged()) {
			if (Data.getInstance().getTPFPs().isEmpty()) {
				this.restore = false;
			} else {
				final Alert alert = new Alert(AlertType.CONFIRMATION);
				final Stage alertStage = (Stage) alert.getDialogPane().getScene().getWindow();
				alertStage.getIcons().add(new Image("file:data/gui/images/icon_16.png", 16, 16, false, true));
				alertStage.getIcons().add(new Image("file:data/gui/images/icon_32.png", 32, 32, false, true));
				alertStage.getIcons().add(new Image("file:data/gui/images/icon_64.png", 64, 64, false, true));
				alert.setTitle("Restore Option");
				alert.setHeaderText("Sources and/or sinks have changed. Recomputation of all cases required.");
				alert.setContentText("Try to restore active checkboxes from...");
				alert.getButtonTypes().clear();
				final ButtonType buttonTypePrevVersion = new ButtonType("Previous version (Default)");
				final ButtonType buttonTypeGroundTruth = new ButtonType("Given Ground-Truth");
				final ButtonType buttonTypeCancel = new ButtonType("Nothing (Cancel)");
				alert.getButtonTypes().addAll(buttonTypePrevVersion, buttonTypeGroundTruth, buttonTypeCancel);

				// final Timeline idlestage = new Timeline(
				// new KeyFrame(Duration.seconds(5), new EventHandler<ActionEvent>() {
				// @Override
				// public void handle(ActionEvent event) {
				// alert.setResult(ButtonType.YES);
				// alert.hide();
				// }
				// }));
				// idlestage.setCycleCount(1);
				// idlestage.play();

				final Optional<ButtonType> result = alert.showAndWait();
				if (result.get() == buttonTypeCancel) {
					this.restore = false;
					this.loadGivenGroundTruth = false;
				} else if (result.get() == buttonTypeGroundTruth) {
					this.restore = false;
					this.loadGivenGroundTruth = true;
				} else {
					this.restore = true;
					this.loadGivenGroundTruth = false;
				}
			}

			new Thread(() -> {
				final List<TPFP> backup = new ArrayList<>(Data.getInstance().getTPFPs());

				int current = -1;
				if (this.restore) {
					this.totalMax = Data.getInstance().getTestcases().size();
					this.totalDone = 1;
					Platform.runLater(() -> {
						this.progressDialog = new ProgressDialog();
						this.progressDialog.updateProgress(0, 100, this.totalDone, this.totalMax);
					});
					current = 0;
				}
				Data.getInstance().getTPFPs().clear();
				Data.getInstance().getTPFPList().clear();

				for (final Testcase testcase : Data.getInstance().getTestcases()) {
					if (testcase.isActive()) {
						// direct
						final List<SourceOrSink> sources = new ArrayList<>();
						final List<SourceOrSink> sinks = new ArrayList<>();
						for (final SourceOrSink item : Data.getInstance().getMap().get(testcase)) {
							if (item.isSource()) {
								sources.add(item);
							}
							if (item.isSink()) {
								sinks.add(item);
							}
						}
						// combine
						if (testcase.getCombine() != null && !testcase.getCombine().isEmpty()) {
							for (final String refID : testcase.getCombine().replace(" ", "").split(",")) {
								for (final Testcase refTestcase : Data.getInstance().getTestcases()) {
									if (refTestcase.getId() == Integer.valueOf(refID).intValue()) {
										for (final SourceOrSink item : Data.getInstance().getMap().get(refTestcase)) {
											if (item.isSource()) {
												sources.add(item);
											}
											if (item.isSink()) {
												sinks.add(item);
											}
										}
									}
								}
							}
						}
						// output
						this.currentDone = 0;
						this.currentMax = sources.size() * sinks.size();
						for (final SourceOrSink source : sources) {
							for (final SourceOrSink sink : sinks) {
								final TPFP tpfp = new TPFP(source, sink);
								if (source == sink) {
									Log.warning("Considering " + tpfp.getId() + " source and sink ("
											+ tpfp.getFrom().getReference().getStatement().getStatementfull()
											+ ") are the same!");
								}
								if (this.restore) {
									int index = -1;
									try {
										if (tpfp.equals(backup.get(current))) {
											index = current;
										} else {
											index = backup.indexOf(tpfp);
											if (index >= 0) {
												current = index;
											}
										}
									} catch (final IndexOutOfBoundsException e) {
										index = -2;
									}
									if (index >= 0) {
										tpfp.setTruepositive(backup.get(index).isTruepositive());
										tpfp.setFalsepositive(backup.get(index).isFalsepositive());
										tpfp.setAborted(backup.get(index).isAborted());
										tpfp.setStarted(backup.get(index).getStarted());
										tpfp.setEnded(backup.get(index).getEnded());
										tpfp.setStatus(backup.get(index).getStatus());
										tpfp.setActualAnswer(backup.get(index).getActualAnswer());
										tpfp.setAttributes(backup.get(index).getAttributes());
										current++;
									}
								}

								if (this.progressDialog != null) {
									this.progressDialog.updateProgress(this.currentDone++, this.currentMax,
											this.totalDone, this.totalMax);
								}
								Platform.runLater(() -> {
									Data.getInstance().getTPFPs().add(tpfp);
								});
								Data.getInstance().getTPFPList().add(tpfp);
							}
						}
					}
					if (this.restore && this.progressDialog != null) {
						this.progressDialog.updateProgress(100, 100, this.totalDone++, this.totalMax);
					}
				}
				Data.getInstance().setSourcesAndSinksChangedFlag(false);

				refresh(true);

				if (this.restore) {
					while (this.progressDialog == null) {
						try {
							Thread.sleep(500);
						} catch (final InterruptedException e) {
							// do nothing
						}
					}
					this.progressDialog.updateProgress(100, 100, this.totalMax, this.totalMax);
				}

				if (this.loadGivenGroundTruth) {
					this.loadGivenGroundTruth = false;
					loadFromGivenGroundTruth();
				}

				loadTaintBench();

				MenuBar.activateBtns();
			}).start();
		} else {
			loadTaintBench();

			MenuBar.activateBtns();
		}
	}

	private void loadFromGivenGroundTruth() {
		final List<TPFP> toKeep = new LinkedList<>();
		for (final TPFP needle : Data.getInstance().getTPFPList()) {
			needle.setTruepositive(false);
			needle.setFalsepositive(true);
		}
		for (final Testcase testcase : Data.getInstance().getTestcases()) {
			// Load given ground truth AQL-Answer
			File givenGroundTruth;

			// Same name as APK
			if (testcase.getApk().getName().endsWith(".apk")) {
				givenGroundTruth = new File(testcase.getApk().getParentFile(),
						testcase.getApk().getName().replace(".apk", ".xml"));
			} else {
				givenGroundTruth = new File(testcase.getApk().getParentFile(), testcase.getApk().getName() + ".xml");
			}

			// Otherwise use ground-truth.xml
			if (!givenGroundTruth.exists()) {
				givenGroundTruth = new File(testcase.getApk().getParentFile(), "ground-truth.xml");
			}

			// If given ground truth available
			if (givenGroundTruth.exists()) {
				// Keep generic
				askKeepGenerics();

				final Answer answer = AnswerHandler.parseXML(givenGroundTruth);
				if (answer.getFlows() != null && !answer.getFlows().getFlow().isEmpty()) {
					for (final Flow flow : answer.getFlows().getFlow()) {
						final Reference source = Helper.getFrom(flow);
						source.getStatement().setStatementgeneric(
								Helper.cleanupParameters(source.getStatement().getStatementgeneric()));
						final Reference sink = Helper.getTo(flow);
						sink.getStatement().setStatementgeneric(
								Helper.cleanupParameters(sink.getStatement().getStatementgeneric()));
						for (final TPFP needle : Data.getInstance().getTPFPList()) {
							if (needle.getTestcase() == testcase) {
								boolean found = false;
								if (EqualsHelper.equals(needle.getFrom().getReference(), source, EQUALS_OPTIONS)
										&& EqualsHelper.equals(needle.getTo().getReference(), sink, EQUALS_OPTIONS)) {
									final Attribute leaking = Helper.getAttributeByName(flow.getAttributes(),
											"leaking");
									found = true;
									if (leaking == null || !leaking.getValue().equalsIgnoreCase("false")) {
										needle.setTruepositive(true);
										needle.setFalsepositive(false);
									}
								}
								if (found) {
									toKeep.add(needle);
									final StringBuilder sb = new StringBuilder(
											"Selecting \"" + needle.getId() + "\" as ");
									if (needle.isFalsepositive()) {
										sb.append("un-expected");
									} else if (needle.isTruepositive()) {
										sb.append("expected");
									}
									if (needle.isFalsepositive() || needle.isTruepositive()) {
										sb.append(" benchmark case based on the information in: "
												+ givenGroundTruth.getAbsolutePath());
										Log.msg(sb.toString(), Log.DEBUG_DETAILED);
									}
								}
							}
						}
					}
				}
			}
		}

		if (!this.keepGenericNegativeCases) {
			final int size = Data.getInstance().getTPFPList().size();
			Data.getInstance().getTPFPs().clear();
			Data.getInstance().getTPFPs().addAll(toKeep);
			Log.msg("Removed " + (size - toKeep.size()) + " generic testcases.", Log.NORMAL);
		}

		refresh();
	}

	private void askKeepGenerics() {
		keepGenericNegativeCases();
		while (!this.keepGenericNegativeCasesDone) {
			try {
				Thread.sleep(500);
			} catch (final InterruptedException e) {
				Log.error("Interupted while waiting for answer.");
			}
		}
	}

	@Override
	public void refresh() {
		refresh(false);
	}

	public void refresh(boolean noFilter) {
		super.refresh();
		if (noFilter) {
			Statistics.getInstance().refresh(Data.getInstance().getTPFPList());
		} else {
			Statistics.getInstance().refresh(this.filteredData);
		}
	}
}