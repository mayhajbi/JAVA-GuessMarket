package gm.client;

import javafx.beans.value.ObservableBooleanValue;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

import java.io.IOException;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.IntConsumer;
import java.util.function.Supplier;

/**
 * Pulls one kind of data from the server again and again, the way the course example does: a timer runs
 * this task at a fixed rate, the request is sent without waiting for its answer, and the answer is handed
 * to a consumer.
 * <p>
 * Both consumers are called on a thread of the HTTP client, so they update the screen through
 * {@code Platform.runLater}.
 *
 * @param <T> the data the request answers with
 */
public class Refresher<T> extends TimerTask {

    /** Reported instead of a status code when the server did not answer at all. */
    public static final int NO_ANSWER = 0;

    private final HttpApi api;
    private final ObservableBooleanValue shouldUpdate;
    private final Supplier<Query<? extends T>> query;
    private final BiConsumer<Query<? extends T>, T> dataConsumer;
    private final IntConsumer statusConsumer;
    /** Whether the previous request is still waiting for its answer; a slow server is not sent more. */
    private final AtomicBoolean waiting = new AtomicBoolean();

    /**
     * @param shouldUpdate   the data is pulled only while this is true
     * @param query          the request to send, asked again before every request; {@code null} when there
     *                       is nothing to pull right now
     * @param dataConsumer   receives every successful answer: the request that was sent and its data
     * @param statusConsumer receives the status code of every answer, or {@link #NO_ANSWER}
     */
    public Refresher(HttpApi api, ObservableBooleanValue shouldUpdate, Supplier<Query<? extends T>> query,
                     BiConsumer<Query<? extends T>, T> dataConsumer, IntConsumer statusConsumer) {
        this.api = api;
        this.shouldUpdate = shouldUpdate;
        this.query = query;
        this.dataConsumer = dataConsumer;
        this.statusConsumer = statusConsumer;
    }

    @Override
    public void run() {
        if (!shouldUpdate.get() || !waiting.compareAndSet(false, true)) {
            return;
        }
        try {
            Query<? extends T> asked = query.get();
            if (asked == null) {
                waiting.set(false);
                return;
            }
            api.getAsync(asked.path(), asked.params(), new Callback() {

                @Override
                public void onFailure(Call call, IOException e) {
                    waiting.set(false);
                    statusConsumer.accept(NO_ANSWER);
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    // The response is closed whatever the answer is - an open one leaks its connection.
                    try (response) {
                        String body = response.body().string();
                        statusConsumer.accept(response.code());
                        if (response.isSuccessful()) {
                            dataConsumer.accept(asked, asked.reader().apply(body));
                        }
                    } finally {
                        waiting.set(false);
                    }
                }
            });
        } catch (RuntimeException failure) {
            // An exception that escapes a timer task stops its timer for good, so the next cycle tries again.
            waiting.set(false);
        }
    }
}
