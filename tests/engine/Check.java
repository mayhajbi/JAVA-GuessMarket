import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * The helper shared by the engine checks, which extend it to use its methods by their plain names.
 * A check class puts all of its assertions in a body given to
 * {@link #run}: a failed assertion is recorded and the check goes on, so one run reports every
 * mismatch. The check prints exactly one {@code [PASS]} or {@code [FAIL]} line for itself (the
 * failed assertions follow a {@code [FAIL]} line) and exits with 0 when everything held, 1 otherwise,
 * which is what {@code tests\run-all.bat} reads.
 */
public abstract class Check {

    /** The folder of the data files, relative to the tests folder the checks run from. */
    public static final String DATA = "data/";

    private static final double EXACT = 1e-6;
    private static final double CENTS = 0.005;

    private static final List<String> failures = new ArrayList<>();
    private static int assertions;

    /** The assertions of one check. */
    public interface Body {
        void run() throws Exception;
    }

    public static void run(String name, Body body) {
        try {
            body.run();
        } catch (Throwable thrown) {
            failures.add("unexpected " + thrown + " - the check stopped here");
            assertions++;
            report(name);
            thrown.printStackTrace(System.out);
            System.exit(1);
        }
        report(name);
        System.exit(failures.isEmpty() ? 0 : 1);
    }

    private static void report(String name) {
        if (failures.isEmpty()) {
            System.out.println("[PASS] " + name + ": all " + assertions + " assertions passed");
            return;
        }
        System.out.println("[FAIL] " + name + ": " + failures.size() + " of " + assertions
                + " assertions failed");
        for (String failure : failures) {
            System.out.println("  - " + failure);
        }
    }

    public static void expect(Object expected, Object actual, String what) {
        expectTrue(Objects.equals(expected, actual), what + " -> expected [" + expected + "] got ["
                + actual + "]");
    }

    public static void expectTrue(boolean condition, String what) {
        assertions++;
        if (!condition) {
            failures.add(what);
        }
    }

    public static void expectFalse(boolean condition, String what) {
        expectTrue(!condition, what);
    }

    /** Equal down to a millionth: for values the engine computes exactly. */
    public static void near(double expected, double actual, String what) {
        expectTrue(Math.abs(expected - actual) < EXACT, what + " -> expected " + expected + " got " + actual);
    }

    /** Equal to the cent: for balances. */
    public static void money(double expected, double actual, String what) {
        expectTrue(Math.abs(expected - actual) <= CENTS, what + " -> expected " + expected + " got " + actual);
    }

    /** Like {@link #near}, where "no value" is also an answer. */
    public static void nearOrNull(Double expected, Double actual, String what) {
        if (expected == null || actual == null) {
            expect(expected, actual, what);
        } else {
            near(expected, actual, what);
        }
    }

    public static void expectThrows(Class<? extends Throwable> type, Runnable action, String what) {
        try {
            action.run();
        } catch (Throwable thrown) {
            expectTrue(type.isInstance(thrown), what + " -> expected " + type.getSimpleName() + " but got "
                    + thrown);
            return;
        }
        expectTrue(false, what + " -> expected " + type.getSimpleName() + " but nothing was thrown");
    }
}
