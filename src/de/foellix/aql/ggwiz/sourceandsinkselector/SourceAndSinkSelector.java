package de.foellix.aql.ggwiz.sourceandsinkselector;

import java.util.List;
import java.util.Optional;

import de.foellix.aql.ggwiz.Data;
import de.foellix.aql.ggwiz.MenuBar;
import de.foellix.aql.ggwiz.Statistics;
import de.foellix.aql.helper.Helper;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.transformation.FilteredList;
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

	private boolean hideUnchecked = false;

	public SourceAndSinkSelector(Stage stage) {
		super();
		this.stage = stage;

		final FilteredList<SourceOrSink> filteredData = new FilteredList<>(Data.getInstance().getSourcesAndSinks(),
				sourceOrSink -> {
					if (this.hideUnchecked && !sourceOrSink.isSink() && !sourceOrSink.isSource()) {
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
		MenuBar.searchField.textProperty().addListener((observable, oldValue, newValue) -> {
			filteredData.setPredicate(sourceOrSink -> {
				if (this.hideUnchecked && !sourceOrSink.isSink() && !sourceOrSink.isSource()) {
					return false;
				}
				for (final String part : newValue.toLowerCase().split(" ")) {
					if (!sourceOrSink.getStatement().toLowerCase().contains(part)
							&& !sourceOrSink.getTestcaseComplete().toLowerCase().contains(part)) {
						return false;
					}
				}
				return true;
			});
		});
		MenuBar.hideBtn.onMouseClickedProperty().set(new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent event) {
				filteredData.setPredicate(sourceOrSink -> {
					if (SourceAndSinkSelector.this.hideUnchecked && !sourceOrSink.isSink()
							&& !sourceOrSink.isSource()) {
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
		this.setItems(filteredData);

		final TableColumn<SourceOrSink, Integer> colID = new TableColumn<SourceOrSink, Integer>("ID");
		colID.setCellValueFactory(new PropertyValueFactory<SourceOrSink, Integer>("id"));
		colID.setPrefWidth(150);

		final TableColumn<SourceOrSink, String> colTestcase = new TableColumn<SourceOrSink, String>("Testcase");
		colTestcase.setCellValueFactory(new PropertyValueFactory<SourceOrSink, String>("testcase"));
		colTestcase.setMinWidth(200);

		final TableColumn<SourceOrSink, String> colMethod = new TableColumn<SourceOrSink, String>("Statement");
		colMethod.setCellValueFactory(new PropertyValueFactory<SourceOrSink, String>("statement"));
		colMethod.setMinWidth(400);

		final TableColumn<SourceOrSink, Boolean> colSource = new TableColumn<SourceOrSink, Boolean>("Source");
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

		final TableColumn<SourceOrSink, Boolean> colSink = new TableColumn<SourceOrSink, Boolean>("Sink");
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

		final TableColumn<SourceOrSink, String> colCombine = new TableColumn<SourceOrSink, String>(
				"Combine with IDs (e.g. \"1, 2\")");
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
				Statistics.getInstance().setInformation(
						"Reference:\n" + Helper.toString(this.getSelectionModel().getSelectedItem().getReference()));
			}
		});

		onResume();
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
		}
	}

	public boolean isHideUnchecked() {
		return this.hideUnchecked;
	}

	public void setHideUnchecked(boolean hideUnchecked) {
		this.hideUnchecked = hideUnchecked;
	}
}