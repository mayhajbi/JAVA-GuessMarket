package gm.ui.fx.common;

import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.scene.Node;
import javafx.util.Duration;

/**
 * The three animations of the application (bonus): the window fades in when a file was loaded, the
 * name of an event is pulsed when it is opened or closed, and the details of an event slide in when
 * a different event is chosen.
 * <p>
 * They are all <b>turned off</b> when the application starts and the user turns them on from the
 * header. Every animation leaves the node exactly as it found it, so turning them on and off in the
 * middle of the work never leaves anything half animated.
 */
public final class Animations {

    private static final Duration FADE_IN = Duration.millis(800);
    private static final Duration PULSE = Duration.millis(300);
    private static final Duration SLIDE_IN = Duration.millis(500);

    private static final double PULSE_SCALE = 1.08;
    private static final double SLIDE_FROM_X = 45;

    private static boolean enabled;

    private Animations() {
    }

    public static void setEnabled(boolean enabled) {
        Animations.enabled = enabled;
    }

    /**
     * Brings the node up from fully transparent. Used on the whole window after a file was loaded.
     */
    public static void fadeIn(Node node) {
        if (!enabled) {
            return;
        }
        FadeTransition fade = new FadeTransition(FADE_IN, node);
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.setOnFinished(finished -> node.setOpacity(1));
        fade.play();
    }

    /**
     * Grows the node a little and back. Used on the name of an event that was just opened or closed.
     */
    public static void pulse(Node node) {
        if (!enabled) {
            return;
        }
        ScaleTransition pulse = new ScaleTransition(PULSE, node);
        pulse.setFromX(1);
        pulse.setFromY(1);
        pulse.setToX(PULSE_SCALE);
        pulse.setToY(PULSE_SCALE);
        pulse.setCycleCount(2);
        pulse.setAutoReverse(true);
        pulse.setOnFinished(finished -> {
            node.setScaleX(1);
            node.setScaleY(1);
        });
        pulse.play();
    }

    /**
     * Slides the node into its place from the right. Used on the details of a newly chosen event.
     */
    public static void slideIn(Node node) {
        if (!enabled) {
            return;
        }
        TranslateTransition slide = new TranslateTransition(SLIDE_IN, node);
        slide.setFromX(SLIDE_FROM_X);
        slide.setToX(0);
        slide.setOnFinished(finished -> node.setTranslateX(0));
        slide.play();
    }
}
