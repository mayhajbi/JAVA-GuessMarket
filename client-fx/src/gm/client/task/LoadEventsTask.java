package gm.client.task;

import gm.dto.UploadResultDTO;
import gm.engine.api.GuessMarketEngine;
import javafx.concurrent.Task;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Uploads a file of events to the server on a background thread, so the window stays responsive while the
 * request is on its way. The upload takes the time the server needs, so there is no artificial delay and
 * the progress is not known: the screen shows an indeterminate progress bar while the task runs.
 * <p>
 * The task belongs to the user interface: the engine only offers a plain upload method and knows nothing
 * about JavaFX.
 */
public class LoadEventsTask extends Task<UploadResultDTO> {

    private final GuessMarketEngine engine;
    private final String userName;
    private final Path file;

    public LoadEventsTask(GuessMarketEngine engine, String userName, Path file) {
        this.engine = engine;
        this.userName = userName;
        this.file = file;
    }

    @Override
    protected UploadResultDTO call() throws IOException {
        updateMessage("Uploading the file...");
        try (InputStream content = Files.newInputStream(file)) {
            return engine.uploadEvents(userName, file.getFileName().toString(), content);
        }
    }
}
