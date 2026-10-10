import gm.client.Constants;
import gm.client.HttpApi;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * A server that is up but never answers: an automatic update gives up after its own, shorter timeout
 * and not after the one of the actions of the user. Needs nothing running: a local socket accepts the
 * connection and stays silent.
 */
public class HungServerTest extends Check {

    public static void main(String[] args) {
        run("hung-server", HungServerTest::check);
    }

    static void check() throws Exception {
        try (ServerSocket silent = new ServerSocket(0)) {
            HttpApi api = new HttpApi("http://localhost:" + silent.getLocalPort());
            CompletableFuture<Boolean> failed = new CompletableFuture<>();
            long start = System.nanoTime();
            api.getAsync("/events", Map.of(), new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    failed.complete(true);
                }

                @Override
                public void onResponse(Call call, Response response) {
                    response.close();
                    failed.complete(false);
                }
            });
            boolean noAnswer = failed.get(Constants.TIMEOUT_SECONDS * 2, TimeUnit.SECONDS);
            double seconds = (System.nanoTime() - start) / 1e9;
            api.shutdown();

            expectTrue(noAnswer, "a server that does not answer is reported as a failure");
            expectTrue(seconds >= Constants.REFRESH_TIMEOUT_SECONDS - 0.5,
                    "an update waits for the timeout of the updates before it gives up");
            expectTrue(seconds < Constants.TIMEOUT_SECONDS - 1,
                    "an update does not wait for the timeout of the actions of the user");
        }
    }
}
