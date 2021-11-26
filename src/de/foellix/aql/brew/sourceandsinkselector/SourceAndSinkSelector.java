package de.foellix.aql.brew.sourceandsinkselector;

import java.util.List;
import java.util.Optional;

import de.foellix.aql.brew.BREW;
import de.foellix.aql.brew.Data;
import de.foellix.aql.brew.MenuBar;
import de.foellix.aql.brew.Statistics;
import de.foellix.aql.helper.Helper;
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
import javafx.scene.control.TableColumn.CellEditEvent;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;

public class SourceAndSinkSelector extends TableView<SourceOrSink> {
	private Stage stage;
	private boolean taintBenchLoaded = false;

	private boolean hideUnchecked = false;

	private long lastRefresh;

	@SuppressWarnings("unchecked")
	public SourceAndSinkSelector(Stage stage) {
		super();
		this.stage = stage;

		final FilteredList<SourceOrSink> filteredData = new FilteredList<>(Data.getInstance().getSourcesAndSinks(),
				sourceOrSink -> {
					if (this.hideUnchecked && !sourceOrSink.isSink() && !sourceOrSink.isSource()
							&& (sourceOrSink.getCombine() == null || sourceOrSink.getCombine().isEmpty())) {
						return false;
					}
					for (final String part : MenuBar.searchField.getText().toLowerCase().split(" ")) {
						if (sourceOrSink.getTestcaseComplete() == null) {
							Platform.runLater(() -> MenuBar.searchField.setText(""));
							continue;
						}
						if (!sourceOrSink.getStatement().toLowerCase().contains(part)
								&& !sourceOrSink.getTestcaseComplete().toLowerCase().contains(part)) {
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
					filteredData.setPredicate(sourceOrSink -> {
						if (this.hideUnchecked && !sourceOrSink.isSink() && !sourceOrSink.isSource()
								&& !sourceOrSink.isSource()
								&& (sourceOrSink.getCombine() == null || sourceOrSink.getCombine().isEmpty())) {
							return false;
						}
						for (final String part : newValue.toLowerCase().split(" ")) {
							if (!sourceOrSink.getStatement().toLowerCase().contains(part)
									&& (sourceOrSink.getTestcaseComplete() != null
											&& !sourceOrSink.getTestcaseComplete().toLowerCase().contains(part))) {
								return false;
							}
						}
						return true;
					});
				}
			}).start();
		});
		MenuBar.hideBtn.onMouseClickedProperty().set(new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent event) {
				filteredData.setPredicate(sourceOrSink -> {
					if (SourceAndSinkSelector.this.hideUnchecked && !sourceOrSink.isSink() && !sourceOrSink.isSource()
							&& !sourceOrSink.isSource()
							&& (sourceOrSink.getCombine() == null || sourceOrSink.getCombine().isEmpty())) {
						return false;
					}
					for (final String part : MenuBar.searchField.getText().toLowerCase().split(" ")) {
						if (!sourceOrSink.getStatement().toLowerCase().contains(part)
								&& !sourceOrSink.getTestcaseComplete().toLowerCase().contains(part)) {
							return false;
						}
					}
					return true;
				});
			}
		});
		final SortedList<SourceOrSink> sortedList = new SortedList<>(filteredData);
		sortedList.comparatorProperty().bind(this.comparatorProperty());
		this.setItems(sortedList);

		final TableColumn<SourceOrSink, Integer> colID = new TableColumn<>("ID");
		colID.setCellValueFactory(new PropertyValueFactory<SourceOrSink, Integer>("id"));
		colID.setPrefWidth(150);

		final TableColumn<SourceOrSink, String> colTestcase = new TableColumn<>("Benchmark App");
		colTestcase.setCellValueFactory(new PropertyValueFactory<SourceOrSink, String>("testcase"));
		colTestcase.setMinWidth(200);

		final TableColumn<SourceOrSink, String> colMethod = new TableColumn<>("Statement");
		colMethod.setCellValueFactory(new PropertyValueFactory<SourceOrSink, String>("statement"));
		colMethod.setMinWidth(400);

		final TableColumn<SourceOrSink, Boolean> colSource = new TableColumn<>("Source");
		colSource.setCellValueFactory(new PropertyValueFactory<SourceOrSink, Boolean>("source"));
		colSource.setCellFactory(col -> {
			final CheckBoxTableCell<SourceOrSink, Boolean> cell = new CheckBoxTableCell<>(index -> {
				final BooleanProperty active = new SimpleBooleanProperty(this.getItems().get(index).isSource());
				active.addListener((obs, wasActive, isNowSource) -> {
					final SourceOrSink item = this.getItems().get(index);
					item.setSource(isNowSource);
					Data.getInstance().setSourcesAndSinksChangedFlag(true);
				});
				return active;
			});
			return cell;
		});
		colSource.setPrefWidth(150);

		final TableColumn<SourceOrSink, Boolean> colSink = new TableColumn<>("Sink");
		colSink.setCellValueFactory(new PropertyValueFactory<SourceOrSink, Boolean>("sink"));
		colSink.setCellFactory(col -> {
			final CheckBoxTableCell<SourceOrSink, Boolean> cell = new CheckBoxTableCell<>(index -> {
				final BooleanProperty active = new SimpleBooleanProperty(this.getItems().get(index).isSink());
				active.addListener((obs, wasActive, isNowSink) -> {
					final SourceOrSink item = this.getItems().get(index);
					item.setSink(isNowSink);
					Data.getInstance().setSourcesAndSinksChangedFlag(true);
				});
				return active;
			});
			return cell;
		});
		colSink.setPrefWidth(150);

		final TableColumn<SourceOrSink, String> colCombine = new TableColumn<>("Combine with IDs (e.g. \"1, 2\")");
		colCombine.setCellValueFactory(new PropertyValueFactory<SourceOrSink, String>("combine"));
		colCombine.setMinWidth(150);
		colCombine.setCellFactory(TextFieldTableCell.forTableColumn());
		colCombine.setOnEditCommit(new EventHandler<CellEditEvent<SourceOrSink, String>>() {
			@Override
			public void handle(CellEditEvent<SourceOrSink, String> t) {
				t.getTableView().getItems().get(t.getTablePosition().getRow()).setCombine(t.getNewValue());
			}
		});
		colCombine.setPrefWidth(150);

		this.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
		this.setEditable(true);
		this.getColumns().addAll(colID, colTestcase, colMethod, colSource, colSink, colCombine);
		this.setOnKeyPressed((event) -> {
			if (event.getCode() == KeyCode.DELETE) {
				event.consume();
				delete(this.getSelectionModel().getSelectedItem());
			} else if (event.getCode() == KeyCode.SPACE) {
				final SourceOrSink item = getSelectionModel().getSelectedItem();
				if (!item.isSource() && !item.isSink()) {
					item.setSource(true);
				} else if (item.isSource() && !item.isSink()) {
					item.setSource(false);
					item.setSink(true);
				} else if (!item.isSource() && item.isSink()) {
					item.setSource(true);
					item.setSink(true);
				} else {
					item.setSource(false);
					item.setSink(false);
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
				Statistics.getInstance().getStatisticsBox()
						.setInformation("Reference:\n" + Helper.toString(newSelection.getReference(), "\n->"));
			}
		});

		onResume();

		loadTaintBench();
	}

	public void loadTaintBench() {
		if (!Data.getInstance().getSourceAndSinkList().isEmpty()) {
			if (BREW.getTaintBenchLoader() != null && !this.taintBenchLoaded) {
				this.taintBenchLoaded = true;
				BREW.getTaintBenchLoader().applyToSourcesAndSinks();
			}
		}
	}

	private void delete(SourceOrSink sourceOrSink) {
		final Alert alert = new Alert(AlertType.CONFIRMATION);

		final Stage alertStage = (Stage) alert.getDialogPane().getScene().getWindow();
		alertStage.getIcons().add(new Image("file:data/gui/images/icon_16.png", 16, 16, false, true));
		alertStage.getIcons().add(new Image("file:data/gui/images/icon_32.png", 32, 32, false, true));
		alertStage.getIcons().add(new Image("file:data/gui/images/icon_64.png", 64, 64, false, true));
		alert.setTitle("Remove");
		alert.setHeaderText("The following " + (sourceOrSink.isSource() ? "source" : "sink") + " will be removed:\n"
				+ sourceOrSink.getId() + ": " + sourceOrSink.getStatement());
		alert.setContentText("Proceed?");

		final Optional<ButtonType> result = alert.showAndWait();
		if (result.get() == ButtonType.OK) {
			for (final List<SourceOrSink> item : Data.getInstance().getMap().values()) {
				if (item.contains(sourceOrSink)) {
					item.remove(sourceOrSink);
				}
			}
			Data.getInstance().getMapR().remove(sourceOrSink);
			Data.getInstance().getSourcesAndSinks().remove(sourceOrSink);
		} else {
			alert.hide();
		}
	}

	public void onResume() {
		if (Data.getInstance().getTestcases() != null && !Data.getInstance().testcasesHaveChanged()) {
			// Sample test
			if (Data.getInstance().getTestcases().isEmpty()
					|| !Data.getInstance().getTestcases().get(0).getApk().exists()) {
				Data.getInstance().setTestcaseChangedFlag(true);
			}
		}

		if (Data.getInstance().testcasesHaveChanged()) {
			final SourceAndSinkExtractor extractor = new SourceAndSinkExtractor(this.stage);
			extractor.extractAll(this);
			Data.getInstance().setTestcaseChangedFlag(false);
		} else {
			MenuBar.activateBtns();
		}
	}

	public boolean isHideUnchecked() {
		return this.hideUnchecked;
	}

	public void setHideUnchecked(boolean hideUnchecked) {
		this.hideUnchecked = hideUnchecked;
	}
}