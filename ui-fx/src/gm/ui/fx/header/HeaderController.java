package gm.ui.fx.header;

import gm.dto.LoadResultDTO;
import gm.engine.api.GuessMarketEngine;
import gm.ui.fx.app.AppController;
import gm.ui.fx.common.Dialogs;
import gm.ui.fx.common.ViewUtils;
import gm.ui.fx.task.LoadEventsTask;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;

import java.io.File;

/**
 * The header of the application: the button that loads a data file, the path of the file that is
 * currently loaded, and the progress of a load that is running.
 */
public class HeaderController {

    private static final String XML_FILES_DESCRIPTION = "Guess Market data files (*.xml)";
    private static final String XML_FILES_PATTERN = "*.xml";

    @FXML private Button loadFileButton;
    @FXML private Label filePathLabel;
    @FXML private HBox progressBox;
    @FXML private ProgressBar loadProgressBar;
    @FXML private Label loadMessageLabel;
    @FXML private Label statusLabel;

    private AppController mainController;
    private GuessMarketEngine engine;

    public void setMainController(AppController mainController) {
        this.mainController = mainController;
    }

    public void setEngine(GuessMarketEngine engine) {
        this.engine = engine;
    }

    /**
     * Loads the given file in the background while its progress is shown. The data that is currently
     * in the system stays in place unless the file is loaded successfully.
     */
    public void loadFile(File file) {
        LoadEventsTask task = new LoadEventsTask(engine, file.getAbsolutePath());
        loadProgressBar.progressProperty().bind(task.progressProperty());
        loadMessageLabel.textProperty().bind(task.messageProperty());
        setLoading(true);

        task.setOnSucceeded(event -> {
            setLoading(false);
            LoadResultDTO result = task.getValue();
            filePathLabel.setText(result.filePath());
            filePathLabel.setTooltip(new Tooltip(result.filePath()));
            statusLabel.setText("Loaded " + result.eventsLoaded() + " events and " + result.usersLoaded()
                    + " users.");
            mainController.refreshAll();
        });
        task.setOnFailed(event -> {
            setLoading(false);
            Dialogs.showError("Loading the file [" + file.getName() + "] failed. The previous data "
                    + "was kept.", task.getException());
        });

        Thread loadThread = new Thread(task, "load-events-file");
        loadThread.setDaemon(true);
        loadThread.start();
    }

    @FXML
    private void onLoadFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Load a Guess Market data file");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter(XML_FILES_DESCRIPTION, XML_FILES_PATTERN));
        File file = fileChooser.showOpenDialog(loadFileButton.getScene().getWindow());
        if (file != null) {
            loadFile(file);
        }
    }

    private void setLoading(boolean loading) {
        if (!loading) {
            loadProgressBar.progressProperty().unbind();
            loadMessageLabel.textProperty().unbind();
        }
        ViewUtils.show(progressBox, loading);
        ViewUtils.show(statusLabel, !loading);
        loadFileButton.setDisable(loading);
    }
}
