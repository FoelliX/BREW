package de.foellix.aql.brew;

import de.foellix.aql.brew.tpfpselector.Runner;
import de.foellix.aql.system.task.gui.TaskTreeViewer;
import de.foellix.aql.ui.gui.BackupAndResetMenu;
import de.foellix.aql.ui.gui.FontAwesome;
import de.foellix.aql.ui.gui.MenuHelp;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Button;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Separator;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TextField;
import javafx.scene.control.ToolBar;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class MenuBar extends VBox {
	private static int content;
	private static Button prevBtn, nextBtn;
	private final Button openBtn, saveBtn, loadBtn, detectFeaturesBtn, susiBtn, deselectBtn, exportSourcesAndSinksBtn,
			viewExecutionGraphBtn, viewAnswerBtn, exportAnswersBtn;
	public static Button hideBtn, playBtn;
	private final MenuItem newMenu, openMenu, addMenu, saveAsMenu, saveMenu, loadFolderMenu, loadMenu,
			markSuccessfulResultBasedSerMenu, markSuccessfulResultBasedXmlMenu, removeNotSuccessfulMenu, resetMarkings,
			refreshResults, compareTo;
	private final CheckMenuItem considerLinenumbers, considerApp;
	public static TextField searchField;
	public static NumberTextField fromField, toField;
	private Label fromLabel, toLabel;

	MenuBar(final GUI parent) {
		// MenuBar
		final javafx.scene.control.MenuBar menuBar = new javafx.scene.control.MenuBar();
		final Menu file = new Menu("File");
		this.newMenu = FontAwesome.getInstance().createMenuItem(FontAwesome.ICON_FILE, StringConstants.STR_NEW);
		this.newMenu.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(final ActionEvent event) {
				parent.newSetup();
			}
		});
		this.openMenu = FontAwesome.getInstance().createMenuItem(FontAwesome.ICON_FOLDER_OPEN_ALT,
				StringConstants.STR_OPEN);
		this.openMenu.setAccelerator(new KeyCodeCombination(KeyCode.O, KeyCombination.CONTROL_DOWN));
		this.openMenu.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(final ActionEvent event) {
				parent.open();
			}
		});
		this.addMenu = FontAwesome.getInstance().createMenuItem(FontAwesome.ICON_FOLDER_OPEN, StringConstants.STR_ADD);
		this.addMenu.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(final ActionEvent event) {
				parent.add();
			}
		});
		this.saveAsMenu = FontAwesome.getInstance().createMenuItem(FontAwesome.ICON_SAVE, StringConstants.STR_SAVE_AS);
		this.saveAsMenu.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(final ActionEvent event) {
				parent.saveAs();
			}
		});
		this.saveMenu = FontAwesome.getInstance().createMenuItem(FontAwesome.ICON_SAVE, StringConstants.STR_SAVE);
		this.saveMenu.setAccelerator(new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN));
		this.saveMenu.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(final ActionEvent event) {
				parent.save();
			}
		});
		final SeparatorMenuItem hSeparator1 = new SeparatorMenuItem();
		this.loadFolderMenu = FontAwesome.getInstance().createMenuItem(FontAwesome.ICON_DOWNLOAD_ALT,
				StringConstants.STR_LOAD_FOLDER);
		this.loadFolderMenu.setAccelerator(new KeyCodeCombination(KeyCode.L, KeyCombination.CONTROL_DOWN));
		this.loadFolderMenu.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(final ActionEvent event) {
				parent.loadFolder();
			}
		});
		this.loadMenu = FontAwesome.getInstance().createMenuItem(FontAwesome.ICON_DOWNLOAD_ALT,
				StringConstants.STR_LOAD_FILE);
		this.loadMenu.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(final ActionEvent event) {
				parent.load();
			}
		});
		final SeparatorMenuItem hSeparator2 = new SeparatorMenuItem();
		final MenuItem exitMenu = FontAwesome.getInstance().createMenuItem(FontAwesome.ICON_OFF,
				StringConstants.STR_EXIT);
		exitMenu.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(final ActionEvent event) {
				parent.exit();
			}
		});
		file.getItems().addAll(this.newMenu, this.openMenu, this.addMenu, this.saveMenu, this.saveAsMenu, hSeparator1,
				this.loadFolderMenu, this.loadMenu, hSeparator2);
		file.getItems().addAll(new BackupAndResetMenu());
		file.getItems().add(exitMenu);

		final Menu edit = new Menu("Edit");
		final MenuItem refreshIDsMenu = FontAwesome.getInstance().createMenuItem(FontAwesome.ICON_REFRESH,
				StringConstants.STR_REFRESH_IDS);
		refreshIDsMenu.setAccelerator(new KeyCodeCombination(KeyCode.F5));
		refreshIDsMenu.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(final ActionEvent event) {
				parent.refreshIDs();
			}
		});
		final SeparatorMenuItem hSeparator3 = new SeparatorMenuItem();
		this.markSuccessfulResultBasedSerMenu = FontAwesome.getInstance().createMenuItem(FontAwesome.ICON_CIRCLE_PLUS,
				StringConstants.STR_MARK_SUCCESSFUL_RESULT_BASED_SER);
		this.markSuccessfulResultBasedSerMenu.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(final ActionEvent event) {
				parent.markSuccessfulResultBasedSerializable();
			}
		});
		this.markSuccessfulResultBasedXmlMenu = FontAwesome.getInstance().createMenuItem(FontAwesome.ICON_CIRCLE_PLUS,
				StringConstants.STR_MARK_SUCCESSFUL_RESULT_BASED_XML);
		this.markSuccessfulResultBasedXmlMenu.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(final ActionEvent event) {
				parent.markSuccessfulResultBasedAQLAnswer();
			}
		});
		this.removeNotSuccessfulMenu = FontAwesome.getInstance().createMenuItem(FontAwesome.ICON_CIRCLE_MINUS,
				StringConstants.STR_REMOVE_NOT_SUCCESSFUL);
		this.removeNotSuccessfulMenu.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(final ActionEvent event) {
				parent.removeNotSuccessFul();
			}
		});
		this.resetMarkings = FontAwesome.getInstance().createMenuItem(FontAwesome.ICON_CIRCLE_TIMES,
				StringConstants.STR_RESET_MARKINGS);
		this.resetMarkings.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(final ActionEvent event) {
				parent.resetMarkings();
			}
		});
		this.refreshResults = FontAwesome.getInstance().createMenuItem(FontAwesome.ICON_CIRCLE_ARROW_UP,
				StringConstants.STR_REFRESH_RESULTS);
		this.refreshResults.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(final ActionEvent event) {
				parent.refreshResults();
			}
		});
		final SeparatorMenuItem hSeparator4 = new SeparatorMenuItem();
		this.compareTo = FontAwesome.getInstance().createMenuItem(FontAwesome.ICON_EXCHANGE,
				StringConstants.STR_COMPARE_TO);
		this.compareTo.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(final ActionEvent event) {
				parent.compareTo();
			}
		});
		edit.getItems().addAll(refreshIDsMenu, hSeparator3, this.removeNotSuccessfulMenu, this.resetMarkings,
				this.refreshResults, this.markSuccessfulResultBasedSerMenu, this.markSuccessfulResultBasedXmlMenu,
				hSeparator4, this.compareTo);

		final Menu options = new Menu("Options");
		this.considerLinenumbers = FontAwesome.getInstance()
				.createCheckMenuItem(StringConstants.STR_CONSIDER_LINENUMBERS);
		this.considerLinenumbers.setSelected(true);
		this.considerLinenumbers.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(final ActionEvent event) {
				Runner.considerLinenumbers = MenuBar.this.considerLinenumbers.isSelected();
				parent.refreshResults();
			}
		});
		this.considerApp = FontAwesome.getInstance().createCheckMenuItem(StringConstants.STR_CONSIDER_APP);
		this.considerApp.setSelected(false);
		this.considerApp.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(final ActionEvent event) {
				Runner.considerApp = MenuBar.this.considerApp.isSelected();
				parent.refreshResults();
			}
		});
		options.getItems().addAll(this.considerLinenumbers, this.considerApp);

		menuBar.getMenus().addAll(file, edit, options, new MenuHelp(parent.getStage()));

		// Toolbar
		final ToolBar toolBar = new ToolBar();
		this.openBtn = FontAwesome.getInstance().createButton(FontAwesome.ICON_FOLDER_OPEN_ALT);
		this.openBtn.setTooltip(new Tooltip(StringConstants.STR_OPEN));
		this.openBtn.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(final ActionEvent event) {
				parent.open();
			}
		});
		this.saveBtn = FontAwesome.getInstance().createButton(FontAwesome.ICON_SAVE);
		this.saveBtn.setTooltip(new Tooltip(StringConstants.STR_SAVE));
		this.saveBtn.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(final ActionEvent event) {
				parent.save();
			}
		});
		final Separator separator1 = new Separator();
		prevBtn = FontAwesome.getInstance().createButton(FontAwesome.ICON_ARROW_LEFT);
		FontAwesome.getInstance().setGreen(prevBtn);
		prevBtn.setTooltip(new Tooltip(StringConstants.STR_PREVIOUS));
		prevBtn.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(final ActionEvent event) {
				parent.prev();
				deactivateBtns();
			}
		});
		nextBtn = FontAwesome.getInstance().createButton(FontAwesome.ICON_ARROW_RIGHT);
		FontAwesome.getInstance().setGreen(nextBtn);
		nextBtn.setTooltip(new Tooltip(StringConstants.STR_NEXT));
		nextBtn.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(final ActionEvent event) {
				parent.next();
				deactivateBtns();
			}
		});
		final Separator separator2 = new Separator();
		final Label searchLabel = new Label("Search: ");
		searchField = new TextField();
		searchField.setTooltip(new Tooltip(StringConstants.STR_SEARCH));
		searchField.setPrefWidth(200);
		final HBox spacer = new HBox();
		HBox.setHgrow(spacer, Priority.ALWAYS);
		this.loadBtn = FontAwesome.getInstance().createButton(FontAwesome.ICON_DOWNLOAD_ALT);
		this.loadBtn.setTooltip(new Tooltip(StringConstants.STR_LOAD_FOLDER));
		this.loadBtn.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(final ActionEvent event) {
				parent.loadFolder();
			}
		});
		this.detectFeaturesBtn = FontAwesome.getInstance().createButton(FontAwesome.ICON_LIST_OL);
		this.detectFeaturesBtn.setTooltip(new Tooltip(StringConstants.STR_DETECT_FEATURES));
		this.detectFeaturesBtn.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(final ActionEvent event) {
				parent.detectFeatures();
			}
		});
		final Separator separator3 = new Separator();
		this.susiBtn = FontAwesome.getInstance().createButton(FontAwesome.ICON_CHECK);
		this.susiBtn.setTooltip(new Tooltip(StringConstants.STR_LOAD_SUSI));
		this.susiBtn.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(final ActionEvent event) {
				parent.loadSuSi();
			}
		});
		this.deselectBtn = FontAwesome.getInstance().createButton(FontAwesome.ICON_CHECK_EMPTY);
		this.deselectBtn.setTooltip(new Tooltip(StringConstants.STR_DESELECT_ALL));
		this.deselectBtn.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(final ActionEvent event) {
				parent.deselectAll();
			}
		});
		hideBtn = FontAwesome.getInstance().createButton(FontAwesome.ICON_MINUS_SQUARE);
		hideBtn.setTooltip(new Tooltip(StringConstants.STR_HIDE_UNCHECKED));
		hideBtn.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(final ActionEvent event) {
				parent.hideUnchecked();
			}
		});
		this.exportSourcesAndSinksBtn = FontAwesome.getInstance().createButton(FontAwesome.ICON_EXTERNAL_LINK);
		this.exportSourcesAndSinksBtn.setTooltip(new Tooltip(StringConstants.STR_EXPORT_SOURCES_AND_SINKS));
		this.exportSourcesAndSinksBtn.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(final ActionEvent event) {
				parent.exportSourcesAndSinks();
			}
		});
		final Separator separator4 = new Separator();
		this.viewAnswerBtn = FontAwesome.getInstance().createButton(FontAwesome.ICON_SEARCH);
		this.viewAnswerBtn.setTooltip(new Tooltip(StringConstants.STR_SHOW_IN_VIEWER));
		this.viewAnswerBtn.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(final ActionEvent event) {
				parent.viewAnswer();
			}
		});
		this.exportAnswersBtn = FontAwesome.getInstance().createButton(FontAwesome.ICON_EXTERNAL_LINK);
		this.exportAnswersBtn.setTooltip(new Tooltip(StringConstants.STR_EXPORT_ANSWERS));
		this.exportAnswersBtn.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(final ActionEvent event) {
				parent.exportAnswers();
			}
		});
		this.fromLabel = new Label("From: ");
		fromField = new NumberTextField(BREW.from);
		fromField.setTooltip(new Tooltip(StringConstants.STR_FROM));
		fromField.setPrefWidth(40);
		this.toLabel = new Label("To: ");
		toField = new NumberTextField(BREW.to);
		toField.setTooltip(new Tooltip(StringConstants.STR_TO));
		toField.setPrefWidth(40);
		playBtn = FontAwesome.getInstance().createButton(FontAwesome.ICON_PLAY);
		FontAwesome.getInstance().setGreen(playBtn);
		playBtn.setTooltip(new Tooltip(StringConstants.STR_START));
		playBtn.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(final ActionEvent event) {
				parent.start();
			}
		});

		toolBar.getItems().addAll(this.openBtn, this.saveBtn, separator1, prevBtn, nextBtn, separator2, searchLabel,
				searchField, spacer, this.loadBtn, this.detectFeaturesBtn, separator3, this.susiBtn, this.deselectBtn,
				hideBtn, this.exportSourcesAndSinksBtn, separator4);
		if (!BREW.getNoGui() && BREW.getOptions().getDrawGraphs()) {
			this.viewExecutionGraphBtn = FontAwesome.getInstance().createButton(FontAwesome.ICON_SITEMAP);
			this.viewExecutionGraphBtn.setTooltip(new Tooltip(StringConstants.STR_SHOW_EXECUTION_GRAPH));
			this.viewExecutionGraphBtn.setOnAction(new EventHandler<ActionEvent>() {
				@Override
				public void handle(final ActionEvent event) {
					if (!TaskTreeViewer.getStage().isShowing()) {
						TaskTreeViewer.getStage().show();
					} else {
						TaskTreeViewer.getStage().requestFocus();
					}
					parent.viewExecutionGraph();
				}
			});

			toolBar.getItems().add(this.viewExecutionGraphBtn);
		} else {
			this.viewExecutionGraphBtn = null;
		}
		toolBar.getItems().addAll(this.viewAnswerBtn, this.exportAnswersBtn, this.fromLabel, fromField, this.toLabel,
				toField, playBtn);

		// Build
		this.getChildren().addAll(menuBar, toolBar);
	}

	public void setContent(int content) {
		MenuBar.content = content;
		switch (content) {
			case 1:
				this.loadFolderMenu.setDisable(true);
				this.loadMenu.setDisable(true);
				this.loadBtn.setDisable(true);
				this.detectFeaturesBtn.setDisable(true);
				this.susiBtn.setDisable(false);
				this.deselectBtn.setDisable(false);
				hideBtn.setDisable(false);
				this.exportSourcesAndSinksBtn.setDisable(false);
				this.viewAnswerBtn.setDisable(true);
				this.exportAnswersBtn.setDisable(true);
				this.fromLabel.setDisable(true);
				fromField.setDisable(true);
				this.toLabel.setDisable(true);
				toField.setDisable(true);
				playBtn.setDisable(true);
				if (this.viewExecutionGraphBtn != null) {
					this.viewExecutionGraphBtn.setDisable(true);
				}
				break;
			case 2:
				this.loadFolderMenu.setDisable(true);
				this.loadMenu.setDisable(true);
				this.loadBtn.setDisable(true);
				this.detectFeaturesBtn.setDisable(true);
				this.susiBtn.setDisable(true);
				this.deselectBtn.setDisable(true);
				hideBtn.setDisable(true);
				this.exportSourcesAndSinksBtn.setDisable(true);
				this.viewAnswerBtn.setDisable(false);
				this.exportAnswersBtn.setDisable(false);
				this.fromLabel.setDisable(false);
				fromField.setDisable(false);
				this.toLabel.setDisable(false);
				toField.setDisable(false);
				playBtn.setDisable(false);
				if (this.viewExecutionGraphBtn != null) {
					this.viewExecutionGraphBtn.setDisable(false);
				}
				break;
			default:
				this.loadFolderMenu.setDisable(false);
				this.loadMenu.setDisable(false);
				this.loadBtn.setDisable(false);
				this.detectFeaturesBtn.setDisable(false);
				this.susiBtn.setDisable(true);
				this.deselectBtn.setDisable(true);
				hideBtn.setDisable(true);
				this.exportSourcesAndSinksBtn.setDisable(true);
				this.viewAnswerBtn.setDisable(true);
				this.exportAnswersBtn.setDisable(true);
				this.fromLabel.setDisable(true);
				fromField.setDisable(true);
				this.toLabel.setDisable(true);
				toField.setDisable(true);
				playBtn.setDisable(true);
				if (this.viewExecutionGraphBtn != null) {
					this.viewExecutionGraphBtn.setDisable(true);
				}
				break;
		}
	}

	public static void deactivateBtns() {
		nextBtn.setDisable(true);
		prevBtn.setDisable(true);
	}

	public static void activateBtns() {
		Platform.runLater(() -> {
			switch (content) {
				case 1:
					prevBtn.setDisable(false);
					nextBtn.setDisable(false);
					break;
				case 2:
					prevBtn.setDisable(false);
					nextBtn.setDisable(true);
					break;
				default:
					prevBtn.setDisable(true);
					nextBtn.setDisable(false);
					break;
			}
		});
	}

	class NumberTextField extends TextField {
		public NumberTextField(int startValue) {
			super();
			if (startValue >= 0) {
				setText(String.valueOf(startValue));
			}
		}

		@Override
		public void replaceText(int start, int end, String text) {
			if (text.matches("[0-9]") || text == "") {
				super.replaceText(start, end, text);
			}
		}

		@Override
		public void replaceSelection(String text) {
			if (text.matches("[0-9]") || text == "") {
				super.replaceSelection(text);
			}
		}
	}
}
