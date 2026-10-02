import javafx.application.Platform;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Runs the body of a client check on the JavaFX thread, where the screens have to be built and used,
 * and waits for it. No window is opened.
 */
final class FxThread {

    private FxThread() {
    }

    /**
     * Starts the JavaFX thread and runs the body on it.
     *
     * @throws Exception whatever the body threw, so that the check reports it
     */
    static void run(Check.Body body) throws Exception {
        runWith(Platform::startup, body);
    }

    /**
     * Runs a further step of a check on the JavaFX thread, after everything the previous step asked that
     * thread to do later.
     *
     * @throws Exception whatever the body threw, so that the check reports it
     */
    static void later(Check.Body body) throws Exception {
        runWith(Platform::runLater, body);
    }

    private static void runWith(Consumer<Runnable> thread, Check.Body body) throws Exception {
        CompletableFuture<Void> done = new CompletableFuture<>();
        thread.accept(() -> {
            try {
                body.run();
                done.complete(null);
            } catch (Throwable failure) {
                done.completeExceptionally(failure);
            }
        });
        done.get();
    }
}
