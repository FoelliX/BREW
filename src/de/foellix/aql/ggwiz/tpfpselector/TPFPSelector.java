package de.foellix.aql.ggwiz.tpfpselector;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import de.foellix.aql.Log;
import de.foellix.aql.ggwiz.Data;
import de.foellix.aql.ggwiz.MenuBar;
import de.foellix.aql.ggwiz.Statistics;
import de.foellix.aql.ggwiz.sourceandsinkselector.SourceOrSink;
import de.foellix.aql.ggwiz.testcaseselector.Testcase;
import de.foellix.aql.helper.Helper;
import de.foellix.aql.ui.gui.ProgressDialog;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.transformation.FilteredList;
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
	boolean alternate = false;

	private ProgressDialog progressDialog;
	private int totalMax, totalDone, currentMax, currentDone;
	private boolean restore;

	private FilteredList<TPFP> filteredData;
	private long lastRefresh;

	public TPFPSelector() {
		super();

		this.filteredData = new FilteredList<>(Data.getInstance().getTPFPs(), tpfp -> {
			for (final String part : MenuBar.searchField.getText().toLowerCase().split(" ")) {
				if (!tpfp.getCase().toLowerCase().contains(part)
						&& !tpfp.getTestcaseComplete().toLowerCase().contains(part)) {
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
							if (!tpfp.getCase().toLowerCase().contains(part) && (tpfp.getTestcaseComplete() != null
									&& !tpfp.getTestcaseComplete().toLowerCase().contains(part))) {
								return false;
							}
						}
						return true;
					});
					refresh();
				}
			}).start();
		});
		this.setItems(this.filteredData);

		final TableColumn<TPFP, Integer> colID = new TableColumn<TPFP, Integer>("ID");
		colID.setCellValueFactory(new PropertyValueFactory<TPFP, Integer>("id"));
		colID.setPrefWidth(150);

		final TableColumn<TPFP, String> colTestcase = new TableColumn<TPFP, String>("Testcase");
		colTestcase.setCellValueFactory(new PropertyValueFactory<TPFP, String>("testcase"));
		colTestcase.setMinWidth(200);

		final TableColumn<TPFP, String> colMethod = new TableColumn<TPFP, String>("Case");
		colMethod.setCellValueFactory(new PropertyValueFactory<TPFP, String>("case"));
		colMethod.setMinWidth(400);

		final TableColumn<TPFP, Boolean> colTP = new TableColumn<TPFP, Boolean>("True Positive");
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

		final TableColumn<TPFP, Boolean> colSink = new TableColumn<TPFP, Boolean>("False Positive");
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

		final TableColumn<TPFP, Integer> colDuration = new TableColumn<TPFP, Integer>("Duration (s)");
		colDuration.setCellValueFactory(new PropertyValueFactory<TPFP, Integer>("duration"));
		colDuration.setStyle("-fx-alignment: CENTER_RIGHT;");
		colDuration.setPrefWidth(100);

		this.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
		this.setEditable(true);
		this.getColumns().addAll(colID, colTestcase, colMethod, colTP, colSink, colDuration);
		this.setOnKeyPressed((event) -> {
			if (event.getCode() == KeyCode.DELETE) {
				delete(this.getSelectionModel().getSelectedItem());
			} else if (event.getCode() == KeyCode.V) {
				view();
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
				final Runner runner = new Runner(this.getSelectionModel().getSelectedItem());
				final String query = runner.getQuery(true);
				Statistics.getInstance().setInformation(
						"From:\n" + Helper.toString(this.getSelectionModel().getSelectedItem().getFrom().getReference(),
								"\n->"),
						"To:\n" + Helper.toString(this.getSelectionModel().getSelectedItem().getTo().getReference(),
								"\n->"),
						"Query:\n" + (query == null
								? "** The current configuration does not allow an appropiate query formulation for this case. **"
								: query));
			}
		});

		this.setRowFactory(tv -> new TableRow<TPFP>() {
			@Override
			public void updateItem(TPFP item, boolean empty) {
				super.updateItem(item, empty);
				TPFPSelector.this.alternate = getIndex() % 2 == 0;
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
				TPFPSelector.this.alternate = !TPFPSelector.this.alternate;
			}
		});

		setStyle("-fx-text-fill: #000000; -fx-selection-bar: #FFFFFF; -fx-selection-bar-non-focused: #FFFFFF;");

		onResume();
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

	public void view() {
		try {
			new ComparisonViewer(this.getSelectionModel().getSelectedItem());
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
				alert.setContentText("Try to restore active checkboxes?");
				alert.getButtonTypes().clear();
				alert.getButtonTypes().add(ButtonType.YES);
				alert.getButtonTypes().add(ButtonType.NO);

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
				if (result.get() == ButtonType.NO) {
					this.restore = false;
				} else {
					this.restore = true;
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
							}
						}
					}
					if (this.restore && this.progressDialog != null) {
						this.progressDialog.updateProgress(100, 100, this.totalDone++, this.totalMax);
					}
				}
				Data.getInstance().setSourcesAndSinksChangedFlag(false);

				this.refresh();

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
			}).start();
		}
	}

	@Override
	public void refresh() {
		super.refresh();
		Platform.runLater(() -> {
			Statistics.getInstance().refresh(this.filteredData);
		});
	}
}