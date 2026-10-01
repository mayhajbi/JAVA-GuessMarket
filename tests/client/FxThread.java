import javafx.application.Platform;

import java.util.concurrent.CompletableFuture;

/**
 * Runs the body of a client check on the JavaFX thread, where the screens have to be built and used,
 * and waits for it. No window is opened.
 */
final class FxThread {

    private FxThread() {
    }

    /**
     * @throws Exception whatever the body threw, so that the check reports it
     */
    static void run(Check.Body body) throws Exception {
        CompletableFuture<Void> done = new CompletableFuture<>();
        Platform.startup(() -> {
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
