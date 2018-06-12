package de.foellix.aql.ggwiz.tpfpselector;

import de.foellix.aql.system.System;
import de.foellix.aql.ui.gui.IGUI;
import de.foellix.aql.ui.gui.Viewer;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class ComparisonViewer extends Stage implements IGUI {
	private final TPFP tpfp;

	private final Viewer viewerActual;
	private final Viewer viewerExpected;
	private final System system;

	public ComparisonViewer(TPFP tpfp) {
		this.tpfp = tpfp;
		this.system = new System();

		// GUI
		this.setTitle("BREW - Comparison Viewer (" + tpfp.getTestcase() + " = " + tpfp.getCase() + ")");
		this.getIcons().add(new Image("file:data/gui/images/icon_16.png", 16, 16, false, true));
		this.getIcons().add(new Image("file:data/gui/images/icon_32.png", 32, 32, false, true));
		this.getIcons().add(new Image("file:data/gui/images/icon_64.png", 64, 64, false, true));

		final HBox mainPane = new HBox(5);
		final VBox boxExpected = new VBox(0);
		final Button btnExpected = new Button("Expected");
		btnExpected.addEventHandler(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent e) {
				if (boxExpected.getChildren().contains(ComparisonViewer.this.viewerExpected)) {
					boxExpected.getChildren().remove(ComparisonViewer.this.viewerExpected);
				} else {
					boxExpected.getChildren().add(ComparisonViewer.this.viewerExpected);
				}
			}
		});
		btnExpected.setPadding(new Insets(5, 5, 5, 5));
		this.viewerExpected = new Viewer(this);
		boxExpected.getChildren().addAll(btnExpected, this.viewerExpected);
		final VBox boxActual = new VBox(0);
		final HBox innerBoxActual = new HBox();
		final HBox spacer = new HBox();
		HBox.setHgrow(spacer, Priority.ALWAYS);
		final Button btnActual = new Button("Actual");
		innerBoxActual.getChildren().addAll(spacer, btnActual);
		btnActual.addEventHandler(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent e) {
				if (boxActual.getChildren().contains(ComparisonViewer.this.viewerActual)) {
					boxActual.getChildren().remove(ComparisonViewer.this.viewerActual);
				} else {
					boxActual.getChildren().add(ComparisonViewer.this.viewerActual);
				}
			}
		});
		btnActual.setPadding(new Insets(5, 5, 5, 5));
		this.viewerActual = new Viewer(this);
		boxActual.getChildren().addAll(innerBoxActual, this.viewerActual);
		mainPane.getChildren().addAll(boxExpected, boxActual);

		final Scene scene = new Scene(mainPane, 800, 600);
		scene.getStylesheets().add("file:data/gui/style.css");
		scene.getStylesheets().add("file:data/gui/xml_highlighting.css");
		this.setScene(scene);
		this.show();

		// Run
		this.system.getAnswerReceivers().remove(this.viewerExpected.viewerXML);
		this.system.getAnswerReceivers().remove(this.viewerExpected.viewerGraph);
		this.viewerExpected.openAnswer(this.tpfp.toAnswer());
		final Runner runner = new Runner(tpfp);
		this.system.query(runner.getQuery());
	}

	@Override
	public Stage getStage() {
		return this;
	}

	@Override
	public System getSystem() {
		return this.system;
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