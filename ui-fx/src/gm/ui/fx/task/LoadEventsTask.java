package gm.ui.fx.task;

import gm.dto.LoadResultDTO;
import gm.engine.api.GuessMarketEngine;
import javafx.concurrent.Task;

/**
 * Loads a data file on a background thread, so the window stays responsive and can show the progress.
 * <p>
 * The task belongs to the user interface: the engine only offers a plain loading method and knows
 * nothing about JavaFX.
 */
public class LoadEventsTask extends Task<LoadResultDTO> {

    // The real loading takes a few milliseconds. The exercise asks for a short artificial delay, so
    // the progress of the load is actually visible.
    private static final int DELAY_STEPS = 15;
    private static final long DELAY_STEP_MILLIS = 100;

    private final GuessMarketEngine engine;
    private final String filePath;

    public LoadEventsTask(GuessMarketEngine engine, String filePath) {
        this.engine = engine;
        this.filePath = filePath;
    }

    @Override
    protected LoadResultDTO call() throws InterruptedException {
        updateMessage("Reading the file...");
        for (int step = 1; step <= DELAY_STEPS; step++) {
            Thread.sleep(DELAY_STEP_MILLIS);
            updateProgress(step, DELAY_STEPS + 1);
        }

        updateMessage("Validating the content...");
        LoadResultDTO result = engine.loadEventsFile(filePath);
        updateProgress(1, 1);
        return result;
    }
}
