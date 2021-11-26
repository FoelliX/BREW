package de.foellix.aql.brew.tpfpselector;

import de.foellix.aql.datastructure.Answer;
import de.foellix.aql.datastructure.Flow;
import de.foellix.aql.datastructure.Flows;
import de.foellix.aql.helper.EqualsHelper;
import de.foellix.aql.helper.EqualsOptions;
import de.foellix.aql.helper.Helper;
import de.foellix.aql.system.AQLSystem;
import de.foellix.aql.ui.gui.IGUI;
import de.foellix.aql.ui.gui.LoadingDialog;
import de.foellix.aql.ui.gui.Viewer;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TitledPane;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

public class ComparisonViewer extends Stage implements IGUI {
	private final TPFP tpfp;

	private boolean actual;
	private boolean expected;
	private final Viewer viewerActual;
	private final Viewer viewerExpected;
	private final AQLSystem aqlSystem;

	private Answer expectedFiltered;
	private Answer actualFiltered;

	public ComparisonViewer(TPFP tpfp) {
		this.tpfp = tpfp;
		this.aqlSystem = new AQLSystem();
		this.expected = true;
		this.actual = true;

		// GUI
		this.setTitle("BREW - Comparison Viewer (" + tpfp.getBenchmarkcase() + " = " + tpfp.getCase() + ")");
		this.getIcons().add(new Image("file:data/gui/images/icon_16.png", 16, 16, false, true));
		this.getIcons().add(new Image("file:data/gui/images/icon_32.png", 32, 32, false, true));
		this.getIcons().add(new Image("file:data/gui/images/icon_64.png", 64, 64, false, true));

		// Viewer
		final HBox viewerPane = new HBox();
		this.viewerExpected = new Viewer(this);
		final TitledPane expectedPane = new TitledPane("Expected", this.viewerExpected);
		expectedPane.setCollapsible(false);
		this.viewerActual = new Viewer(this);
		final TitledPane actualPane = new TitledPane("Actual", this.viewerActual);
		actualPane.setCollapsible(false);
		viewerPane.getChildren().addAll(expectedPane, actualPane);

		// View Menu
		final Menu viewMenu = new Menu("View");
		final CheckMenuItem menuBtnToggleExpected = new CheckMenuItem("Show expected result");
		menuBtnToggleExpected.setSelected(true);
		menuBtnToggleExpected.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				ComparisonViewer.this.expected = !ComparisonViewer.this.expected;
				setVisible(viewerPane, expectedPane, actualPane, ComparisonViewer.this.expected,
						ComparisonViewer.this.actual);
			}
		});
		final CheckMenuItem menuBtnToggleActual = new CheckMenuItem("Show actual result");
		menuBtnToggleActual.setSelected(true);
		menuBtnToggleActual.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				ComparisonViewer.this.actual = !ComparisonViewer.this.actual;
				setVisible(viewerPane, expectedPane, actualPane, ComparisonViewer.this.expected,
						ComparisonViewer.this.actual);
			}
		});
		viewMenu.getItems().addAll(menuBtnToggleExpected, menuBtnToggleActual);

		// Filter Menu
		final Menu filterMenu = new Menu("Filter");
		final CheckMenuItem menuBtnRemoveRedundant = new CheckMenuItem("Remove redundant elements");
		menuBtnRemoveRedundant.setSelected(false);
		final SeparatorMenuItem separator = new SeparatorMenuItem();
		final MenuItem menuBtnShowMatchesOnly = new MenuItem("Flow matches only");
		menuBtnShowMatchesOnly.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				applyFilter(true, false, menuBtnRemoveRedundant.isSelected());
			}
		});
		final MenuItem menuBtnShowMatchesOnlyPrecise = new MenuItem("Flow matches only (Precise)");
		menuBtnShowMatchesOnlyPrecise.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				applyFilter(true, true, menuBtnRemoveRedundant.isSelected());
			}
		});
		final MenuItem menuBtnShowAll = new MenuItem("Complete Answer");
		menuBtnShowAll.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				applyFilter(false, false, menuBtnRemoveRedundant.isSelected());
			}
		});
		filterMenu.getItems().addAll(menuBtnShowMatchesOnly, menuBtnShowMatchesOnlyPrecise, menuBtnShowAll, separator,
				menuBtnRemoveRedundant);

		// Menu
		final MenuBar menuBar = new MenuBar();
		menuBar.getMenus().addAll(viewMenu, filterMenu);

		// Main
		final BorderPane mainPane = new BorderPane();
		mainPane.setTop(menuBar);
		mainPane.setCenter(viewerPane);

		// Scene
		final Scene scene = new Scene(mainPane, 800, 600);
		scene.getStylesheets().add("file:data/gui/style.css");
		scene.getStylesheets().add("file:data/gui/highlight.css");
		this.setScene(scene);
		this.show();

		// Run
		this.aqlSystem.getAnswerReceivers().remove(this.viewerExpected.viewerXML);
		this.aqlSystem.getAnswerReceivers().remove(this.viewerExpected.viewerWeb);
		applyFilter(false, false, false);
	}

	private void setVisible(HBox viewerPane, TitledPane expectedPane, TitledPane actualPane, boolean expected,
			boolean actual) {
		viewerPane.getChildren().clear();
		if (expected) {
			if (actual) {
				viewerPane.getChildren().addAll(expectedPane, actualPane);
			} else {
				viewerPane.getChildren().add(expectedPane);
			}
		} else {
			if (actual) {
				viewerPane.getChildren().add(actualPane);
			}
		}
	}

	private void applyFilter(boolean matchesOnly, boolean precise, boolean removeRedundant) {
		final LoadingDialog loadingDialog = new LoadingDialog("Filtering");

		final EqualsOptions options = new EqualsOptions();
		options.setOption(EqualsOptions.IGNORE_APP, true);
		options.setOption(EqualsOptions.PRECISELY_REFERENCE, precise);

		new Thread(() -> {
			if (matchesOnly) {
				if (this.tpfp.getActualAnswer() != null) {
					filter(this.tpfp.toAnswer(), this.tpfp.getActualAnswer(), options, removeRedundant);
				} else {
					this.expectedFiltered = new Answer();
					this.actualFiltered = new Answer();
				}
				this.viewerExpected.openAnswer(this.expectedFiltered);
				this.viewerActual.openAnswer(this.actualFiltered);
			} else {
				this.viewerExpected.openAnswer(this.tpfp.toAnswer());
				if (this.tpfp.getActualAnswer() != null) {
					if (removeRedundant) {
						this.viewerActual.openAnswer(Helper.removeRedundant(Helper.copy(this.tpfp.getActualAnswer()),
								EqualsOptions.DEFAULT));
					} else {
						this.viewerActual.openAnswer(this.tpfp.getActualAnswer());
					}
				}
			}
			Platform.runLater(() -> {
				loadingDialog.setDone(true);
			});
		}).start();
	}

	private void filter(final Answer expected, Answer actual, EqualsOptions options, boolean removeRedundant) {
		this.expectedFiltered = new Answer();
		this.expectedFiltered.setFlows(new Flows());
		this.actualFiltered = new Answer();
		this.actualFiltered.setFlows(new Flows());

		if (removeRedundant) {
			actual = Helper.removeRedundant(Helper.copy(actual),
					new EqualsOptions().setOption(EqualsOptions.PRECISELY_REFERENCE, true));
		}

		if (expected.getFlows() != null && expected.getFlows().getFlow() != null && actual.getFlows() != null
				&& actual.getFlows().getFlow() != null) {
			for (final Flow expectedFlow : expected.getFlows().getFlow()) {
				for (final Flow actualFlow : actual.getFlows().getFlow()) {
					if (EqualsHelper.equals(expectedFlow, actualFlow, options)) {
						if (!this.expectedFiltered.getFlows().getFlow().contains(expectedFlow)) {
							this.expectedFiltered.getFlows().getFlow().add(expectedFlow);
						}
						if (!this.actualFiltered.getFlows().getFlow().contains(actualFlow)) {
							this.actualFiltered.getFlows().getFlow().add(actualFlow);
							if (removeRedundant && options.getOption(EqualsOptions.PRECISELY_REFERENCE)) {
								break;
							}
						}
					}
				}
			}
		}
	}

	@Override
	public Stage getStage() {
		return this;
	}

	@Override
	public AQLSystem getSystem() {
		return this.aqlSystem;
	}

	@Override
	public void newFile() {
		// not required, no menu bar - do nothing
	}

	@Override
	public void open() {
		// not required, no menu bar - do nothing
	}

	@Override
	public void save() {
		// not required, no menu bar - do nothing
	}

	@Override
	public void saveAs() {
		// not required, no menu bar - do nothing
	}

	@Override
	public void exit() {
		// not required, no menu bar - do nothing
	}
}