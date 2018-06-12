package de.foellix.aql.ggwiz;

import de.foellix.aql.ui.gui.BackupAndResetMenu;
import de.foellix.aql.ui.gui.FontAwesome;
import de.foellix.aql.ui.gui.MenuHelp;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Button;
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
	private final Button openBtn, saveBtn, prevBtn, nextBtn, loadBtn, detectFeaturesBtn, susiBtn, deselectBtn, viewBtn,
			exportBtn;
	public static Button hideBtn, playBtn;
	private final MenuItem newMenu, openMenu, addMenu, saveAsMenu, saveMenu, loadFolderMenu, loadMenu,
			markSuccessfulResultBasedSerMenu, markSuccessfulResultBasedXmlMenu, removeNotSuccessfulMenu, resetMarkings;
	public static TextField searchField;

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
		edit.getItems().addAll(refreshIDsMenu, hSeparator3, this.removeNotSuccessfulMenu, this.resetMarkings,
				this.markSuccessfulResultBasedSerMenu, this.markSuccessfulResultBasedXmlMenu);

		menuBar.getMenus().addAll(file, edit, new MenuHelp(parent.getStage()));

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
		this.prevBtn = FontAwesome.getInstance().createButton(FontAwesome.ICON_ARROW_LEFT);
		FontAwesome.getInstance().setGreen(this.prevBtn);
		this.prevBtn.setTooltip(new Tooltip(StringConstants.STR_PREVIOUS));
		this.prevBtn.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(final ActionEvent event) {
				parent.prev();
			}
		});
		this.nextBtn = FontAwesome.getInstance().createButton(FontAwesome.ICON_ARROW_RIGHT);
		FontAwesome.getInstance().setGreen(this.nextBtn);
		this.nextBtn.setTooltip(new Tooltip(StringConstants.STR_NEXT));
		this.nextBtn.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(final ActionEvent event) {
				parent.next();
			}
		});
		final Separator separator2 = new Separator();
		final Label searchLabel = new Label("Search: ");
		searchField = new TextField();
		searchField.setTooltip(new Tooltip(StringConstants.STR_SEARCH));
		searchField.setPrefWidth(300);
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
		final Separator separator4 = new Separator();
		this.viewBtn = FontAwesome.getInstance().createButton(FontAwesome.ICON_SEARCH);
		this.viewBtn.setTooltip(new Tooltip(StringConstants.STR_SHOW_IN_VIEWER));
		this.viewBtn.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(final ActionEvent event) {
				parent.view();
			}
		});
		this.exportBtn = FontAwesome.getInstance().createButton(FontAwesome.ICON_EXTERNAL_LINK);
		this.exportBtn.setTooltip(new Tooltip(StringConstants.STR_EXPORT));
		this.exportBtn.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(final ActionEvent event) {
				parent.export();
			}
		});
		playBtn = FontAwesome.getInstance().createButton(FontAwesome.ICON_PLAY);
		FontAwesome.getInstance().setGreen(playBtn);
		playBtn.setTooltip(new Tooltip(StringConstants.STR_START));
		playBtn.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(final ActionEvent event) {
				parent.start();
			}
		});

		toolBar.getItems().addAll(this.openBtn, this.saveBtn, separator1, this.prevBtn, this.nextBtn, separator2,
				searchLabel, searchField, spacer, this.loadBtn, this.detectFeaturesBtn, separator3, this.susiBtn,
				this.deselectBtn, hideBtn, separator4, this.viewBtn, this.exportBtn, playBtn);

		// Build
		this.getChildren().addAll(menuBar, toolBar);
	}

	public void setContent(int content) {
		switch (content) {
		case 1:
			this.loadFolderMenu.setDisable(true);
			this.loadMenu.setDisable(true);
			this.loadBtn.setDisable(true);
			this.detectFeaturesBtn.setDisable(true);
			this.prevBtn.setDisable(false);
			this.nextBtn.setDisable(false);
			this.susiBtn.setDisable(false);
			this.deselectBtn.setDisable(false);
			hideBtn.setDisable(false);
			this.viewBtn.setDisable(true);
			this.exportBtn.setDisable(true);
			playBtn.setDisable(true);
			break;
		case 2:
			this.loadFolderMenu.setDisable(true);
			this.loadMenu.setDisable(true);
			this.loadBtn.setDisable(true);
			this.detectFeaturesBtn.setDisable(true);
			this.prevBtn.setDisable(false);
			this.nextBtn.setDisable(true);
			this.susiBtn.setDisable(true);
			this.deselectBtn.setDisable(true);
			hideBtn.setDisable(true);
			this.viewBtn.setDisable(false);
			this.exportBtn.setDisable(false);
			playBtn.setDisable(false);
			break;
		default:
			this.loadFolderMenu.setDisable(false);
			this.loadMenu.setDisable(false);
			this.loadBtn.setDisable(false);
			this.detectFeaturesBtn.setDisable(false);
			this.prevBtn.setDisable(true);
			this.nextBtn.setDisable(false);
			this.susiBtn.setDisable(true);
			this.deselectBtn.setDisable(true);
			hideBtn.setDisable(true);
			this.viewBtn.setDisable(true);
			this.exportBtn.setDisable(true);
			playBtn.setDisable(true);
			break;
		}
	}
}
