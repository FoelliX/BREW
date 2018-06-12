package de.foellix.aql.ggwiz.testcaseselector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import de.foellix.aql.ggwiz.Data;
import de.foellix.aql.ggwiz.MenuBar;
import de.foellix.aql.ggwiz.Statistics;
import de.foellix.aql.ggwiz.sourceandsinkselector.SourceOrSink;
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

public class TestCaseSelector extends TableView<Testcase> {
	public TestCaseSelector() {
		super();

		final FilteredList<Testcase> filteredData = new FilteredList<>(Data.getInstance().getTestcases());
		MenuBar.searchField.textProperty().addListener((observable, oldValue, newValue) -> {
			filteredData.setPredicate(testcase -> {
				for (final String part : newValue.toLowerCase().split(" ")) {
					if (!testcase.getName().toLowerCase().contains(part)
							&& !(testcase.getId() + ":").toLowerCase().contains(part)) {
						return false;
					}
				}
				return true;
			});
		});
		this.setItems(filteredData);

		final TableColumn<Testcase, Integer> colID = new TableColumn<Testcase, Integer>("ID");
		colID.setCellValueFactory(new PropertyValueFactory<Testcase, Integer>("id"));
		colID.setPrefWidth(150);

		final TableColumn<Testcase, String> colName = new TableColumn<Testcase, String>("Apks");
		colName.setCellValueFactory(new PropertyValueFactory<Testcase, String>("name"));
		colName.setMinWidth(500);

		final TableColumn<Testcase, String> colFeature = new TableColumn<Testcase, String>("Detected Features");
		colFeature.setCellValueFactory(new PropertyValueFactory<Testcase, String>("featuresAsString"));
		colFeature.setMinWidth(175);
		colFeature.setCellFactory(TextFieldTableCell.forTableColumn());
		colFeature.setOnEditCommit(new EventHandler<CellEditEvent<Testcase, String>>() {
			@Override
			public void handle(CellEditEvent<Testcase, String> t) {
				final List<String> temp = Arrays.asList(t.getNewValue().replaceAll("(\\s*),(\\s*)", ",").split(","));
				t.getTableView().getItems().get(t.getTablePosition().getRow()).setFeatures(temp);
			}
		});

		final TableColumn<Testcase, Boolean> colActive = new TableColumn<Testcase, Boolean>("Active");
		colActive.setCellValueFactory(new PropertyValueFactory<Testcase, Boolean>("active"));
		colActive.setCellFactory(col -> {
			final CheckBoxTableCell<Testcase, Boolean> cell = new CheckBoxTableCell<>(index -> {
				final BooleanProperty active = new SimpleBooleanProperty(this.getItems().get(index).isActive());
				active.addListener((obs, wasActive, isNowActive) -> {
					final Testcase item = this.getItems().get(index);
					item.setActive(isNowActive);
					Data.getInstance().setTestcaseChangedFlag(true);
				});
				return active;
			});
			return cell;
		});
		colActive.setPrefWidth(150);

		final TableColumn<Testcase, String> colCombine = new TableColumn<Testcase, String>(
				"Combine with IDs (e.g. \"1, 2\")");
		colCombine.setCellValueFactory(new PropertyValueFactory<Testcase, String>("combine"));
		colCombine.setMinWidth(150);
		colCombine.setCellFactory(TextFieldTableCell.forTableColumn());
		colCombine.setOnEditCommit(new EventHandler<CellEditEvent<Testcase, String>>() {
			@Override
			public void handle(CellEditEvent<Testcase, String> t) {
				t.getTableView().getItems().get(t.getTablePosition().getRow()).setCombine(t.getNewValue());
			}
		});
		colCombine.setPrefWidth(150);

		this.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
		this.setEditable(true);
		this.getColumns().addAll(colID, colName, colFeature, colActive, colCombine);
		this.setOnKeyPressed((event) -> {
			if (event.getCode() == KeyCode.DELETE) {
				event.consume();
				delete(this.getSelectionModel().getSelectedItem());
			} else if (event.getCode() == KeyCode.SPACE) {
				final Testcase item = getSelectionModel().getSelectedItem();
				if (item.isActive()) {
					item.setActive(false);
				} else {
					item.setActive(true);
				}
				Data.getInstance().setTestcaseChangedFlag(true);
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
				Statistics.getInstance()
						.setInformation("Testcase:\n" + this.getSelectionModel().getSelectedItem().getId() + ": "
								+ this.getSelectionModel().getSelectedItem().getName());
			}
		});
	}

	private void delete(Testcase testcase) {
		final Alert alert = new Alert(AlertType.CONFIRMATION);

		final Stage alertStage = (Stage) alert.getDialogPane().getScene().getWindow();
		alertStage.getIcons().add(new Image("file:data/gui/images/icon_16.png", 16, 16, false, true));
		alertStage.getIcons().add(new Image("file:data/gui/images/icon_32.png", 32, 32, false, true));
		alertStage.getIcons().add(new Image("file:data/gui/images/icon_64.png", 64, 64, false, true));
		alert.setTitle("Remove");
		alert.setHeaderText("The following testcase will be removed:\n" + testcase.getId() + ": " + testcase.getName());
		alert.setContentText("Proceed?");

		final Optional<ButtonType> result = alert.showAndWait();
		if (result.get() == ButtonType.OK) {
			Data.getInstance().getMap().remove(testcase);
			final List<SourceOrSink> remove = new ArrayList<>();
			for (final SourceOrSink sourceOrSink : Data.getInstance().getSourcesAndSinks()) {
				if (Data.getInstance().getMapR().get(sourceOrSink).equals(testcase)) {
					remove.add(sourceOrSink);
				}
			}
			for (final SourceOrSink sourceOrSink : remove) {
				Data.getInstance().getMapR().remove(sourceOrSink);
			}
			Data.getInstance().getSourcesAndSinks().removeAll(remove);
			Data.getInstance().getTestcases().remove(testcase);

			Data.getInstance().setTestcaseChangedFlag(true);
			Data.getInstance().setSourcesAndSinksChangedFlag(true);
		} else {
			alert.hide();
		}
	}
}