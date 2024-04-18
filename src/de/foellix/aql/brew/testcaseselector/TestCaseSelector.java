package de.foellix.aql.brew.testcaseselector;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import de.foellix.aql.brew.Data;
import de.foellix.aql.brew.MenuBar;
import de.foellix.aql.brew.Statistics;
import de.foellix.aql.brew.sourceandsinkselector.SourceOrSink;
import de.foellix.aql.helper.FileRelocator;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.EventHandler;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellEditEvent;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;

public class TestCaseSelector extends TableView<Testcase> {
	private FileRelocator fileRelocator;

	private long lastRefresh;

	private int allBtnSelected;

	@SuppressWarnings("unchecked")
	public TestCaseSelector(Stage stage) {
		super();

		final FilteredList<Testcase> filteredData = new FilteredList<>(Data.getInstance().getTestcases());
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
					filteredData.setPredicate(testcase -> {
						for (final String part : newValue.toLowerCase().split(" ")) {
							if (!testcase.getName().toLowerCase().contains(part)
									&& !(testcase.getId() + ":").toLowerCase().contains(part)) {
								return false;
							}
						}
						return true;
					});
				}
			}).start();
		});
		final SortedList<Testcase> sortedList = new SortedList<>(filteredData);
		sortedList.comparatorProperty().bind(this.comparatorProperty());
		this.setItems(sortedList);

		final TableColumn<Testcase, Integer> colID = new TableColumn<>("ID");
		colID.setCellValueFactory(new PropertyValueFactory<Testcase, Integer>("id"));
		colID.setPrefWidth(150);

		final TableColumn<Testcase, String> colName = new TableColumn<>("Apks");
		colName.setCellValueFactory(new PropertyValueFactory<Testcase, String>("name"));
		colName.setMinWidth(500);

		final TableColumn<Testcase, String> colFeature = new TableColumn<>("Detected Features");
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

		final TableColumn<Testcase, Boolean> colActive = new TableColumn<>("Active");
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

		final TableColumn<Testcase, String> colCombine = new TableColumn<>("Combine with IDs (e.g. \"1, 2\")");
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
				delete(this.getSelectionModel().getSelectedItems());
			} else if (event.getCode() == KeyCode.SPACE) {
				final Testcase item = getSelectionModel().getSelectedItem();
				if (item.isActive()) {
					item.setActive(false);
				} else {
					item.setActive(true);
				}
				Data.getInstance().setTestcaseChangedFlag(true);
				this.refresh();
			} else if (event.getCode() == KeyCode.R) {
				event.consume();
				if (TestCaseSelector.this.fileRelocator == null) {
					TestCaseSelector.this.fileRelocator = new FileRelocator(stage);
				}
				final File newApk = TestCaseSelector.this.fileRelocator
						.relocateFile(getSelectionModel().getSelectedItem().getApk());
				if (newApk != null && newApk.exists()) {
					getSelectionModel().getSelectedItem().setApk(newApk);
					refresh();
				}
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
					delete(getSelectionModel().getSelectedItems());
				}
			}
		});
		this.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
			if (newSelection != null) {
				final StringBuilder sb = new StringBuilder();
				sb.append("Testcase (" + (this.getSelectionModel().getSelectedItem().isActive() ? "Active" : "Inactive")
						+ "):\n" + this.getSelectionModel().getSelectedItem().getId() + ") "
						+ this.getSelectionModel().getSelectedItem().getName() + "\n");
				if (this.getSelectionModel().getSelectedItem().getFeatures() != null
						&& !this.getSelectionModel().getSelectedItem().getFeatures().isEmpty()) {
					sb.append("\nFeatures: " + this.getSelectionModel().getSelectedItem().getFeaturesAsString());
				}
				if (this.getSelectionModel().getSelectedItem().getCombine() != null
						&& !this.getSelectionModel().getSelectedItem().getCombine().isEmpty()) {
					sb.append("\nCombine: " + this.getSelectionModel().getSelectedItem().getCombine());
				}
				Statistics.getInstance().getStatisticsBox().setInformation(sb.toString());
			}
		});
		this.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

		onResume();
	}

	private void delete(List<Testcase> testcases) {
		this.allBtnSelected = -1;
		for (final Testcase testcase : new ArrayList<>(testcases)) {
			if (this.allBtnSelected < 0) {
				final Alert alert = new Alert(AlertType.CONFIRMATION);
				final Stage alertStage = (Stage) alert.getDialogPane().getScene().getWindow();
				alertStage.getIcons().add(new Image("file:data/gui/images/icon_16.png", 16, 16, false, true));
				alertStage.getIcons().add(new Image("file:data/gui/images/icon_32.png", 32, 32, false, true));
				alertStage.getIcons().add(new Image("file:data/gui/images/icon_64.png", 64, 64, false, true));
				alert.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO, ButtonType.CANCEL);

				((Button) alert.getDialogPane().lookupButton(ButtonType.YES)).setOnMousePressed((event) -> {
					if (event.isShiftDown()) {
						this.allBtnSelected = 1;
						alert.hide();
					} else {
						this.allBtnSelected = -1;
					}
				});
				((Button) alert.getDialogPane().lookupButton(ButtonType.NO)).setOnMousePressed((event) -> {
					if (event.isShiftDown()) {
						this.allBtnSelected = 2;
						alert.hide();
					} else {
						this.allBtnSelected = -1;
					}
				});
				((Button) alert.getDialogPane().lookupButton(ButtonType.CANCEL)).setOnMousePressed((event) -> {
					if (event.isShiftDown()) {
						this.allBtnSelected = 2;
						alert.hide();
					} else {
						this.allBtnSelected = -1;
					}
				});

				alertStage.addEventHandler(KeyEvent.KEY_PRESSED, (KeyEvent event) -> {
					if (event.getCode() == KeyCode.SHIFT) {
						((Button) alert.getDialogPane().lookupButton(ButtonType.YES)).setText(
								((Button) alert.getDialogPane().lookupButton(ButtonType.YES)).getText() + " (all)");
						((Button) alert.getDialogPane().lookupButton(ButtonType.NO)).setText(
								((Button) alert.getDialogPane().lookupButton(ButtonType.NO)).getText() + " (all)");
					} else if (event.getCode() == KeyCode.ENTER && event.isShiftDown()) {
						if (((Button) alert.getDialogPane().lookupButton(ButtonType.YES)).isFocused()) {
							this.allBtnSelected = 1;
						} else {
							this.allBtnSelected = 2;
						}
						alert.hide();
					}
				});
				alertStage.addEventHandler(KeyEvent.KEY_RELEASED, (KeyEvent event) -> {
					if (event.getCode() == KeyCode.SHIFT) {
						((Button) alert.getDialogPane().lookupButton(ButtonType.YES))
								.setText(((Button) alert.getDialogPane().lookupButton(ButtonType.YES)).getText()
										.replace(" (all)", ""));
						((Button) alert.getDialogPane().lookupButton(ButtonType.NO))
								.setText(((Button) alert.getDialogPane().lookupButton(ButtonType.NO)).getText()
										.replace(" (all)", ""));
					}
				});

				alert.setTitle("Remove");
				alert.setHeaderText(
						"The following testcase will be removed:\n" + testcase.getId() + ": " + testcase.getName());
				alert.setContentText("Proceed?");

				final Optional<ButtonType> result = alert.showAndWait();
				if (result.get() == ButtonType.YES || this.allBtnSelected == 1) {
					removeTestCase(testcase);
				} else if (result.get() == ButtonType.CANCEL || this.allBtnSelected == 2) {
					break;
				}
			} else if (this.allBtnSelected == 1) {
				removeTestCase(testcase);
			}
		}
	}

	private void removeTestCase(Testcase testcase) {
		final List<SourceOrSink> remove = Data.getInstance().getMap().get(testcase);
		Data.getInstance().getMap().remove(testcase);
		if (remove != null) {
			for (final SourceOrSink sourceOrSink : remove) {
				Data.getInstance().getMapR().remove(sourceOrSink);
			}
			final Set<SourceOrSink> temp = new HashSet<>(Data.getInstance().getSourcesAndSinks());
			temp.removeAll(remove);
			Data.getInstance().setSourcesAndSinks(FXCollections.observableArrayList(temp));
		}
		Data.getInstance().getTestcases().remove(testcase);

		Data.getInstance().setTestcaseChangedFlag(true);
		Data.getInstance().setSourcesAndSinksChangedFlag(true);
	}

	public void onResume() {
		MenuBar.activateBtns();
	}
}