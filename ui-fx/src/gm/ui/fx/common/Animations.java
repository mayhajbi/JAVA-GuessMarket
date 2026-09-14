package gm.ui.fx.common;

import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.scene.Node;
import javafx.util.Duration;

/**
 * The three animations of the application (bonus): the window fades in when a file was loaded, the
 * status of an event is pulsed once it was opened or closed, and the details of an event slide in
 * when a different event is chosen.
 * <p>
 * They are all <b>turned off</b> when the application starts and the user turns them on from the
 * header. Every animation leaves the node exactly as it found it, so turning them on and off in the
 * middle of the work never leaves anything half animated.
 */
public final class Animations {

    private static final Duration FADE_IN = Duration.millis(900);
    /** One way; the pulse grows and shrinks back, so it lasts twice as long. */
    private static final Duration PULSE = Duration.millis(350);
    private static final Duration SLIDE_IN = Duration.millis(450);

    private static final double PULSE_SCALE = 1.3;
    private static final double SLIDE_FROM_X = 140;

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
        play(fadeFromTransparent(FADE_IN, node), () -> node.setOpacity(1));
    }

    /**
     * Grows the node clearly and back. Used on the status of an event that was just opened or closed.
     */
    public static void pulse(Node node) {
        ScaleTransition pulse = new ScaleTransition(PULSE, node);
        pulse.setFromX(1);
        pulse.setFromY(1);
        pulse.setToX(PULSE_SCALE);
        pulse.setToY(PULSE_SCALE);
        pulse.setCycleCount(2);
        pulse.setAutoReverse(true);
        play(pulse, () -> {
            node.setScaleX(1);
            node.setScaleY(1);
        });
    }

    /**
     * Slides the node into its place from the right while it fades in. Used on the details of a newly
     * chosen event.
     */
    public static void slideIn(Node node) {
        TranslateTransition slide = new TranslateTransition(SLIDE_IN, node);
        slide.setFromX(SLIDE_FROM_X);
        slide.setToX(0);
        play(new ParallelTransition(slide, fadeFromTransparent(SLIDE_IN, node)), () -> {
            node.setTranslateX(0);
            node.setOpacity(1);
        });
    }

    /**
     * Plays the animation when the animations are turned on.
     *
     * @param restore puts the node back exactly as it was once the animation is over
     */
    private static void play(Animation animation, Runnable restore) {
        if (!enabled) {
            return;
        }
        animation.setOnFinished(finished -> restore.run());
        animation.play();
    }

    private static FadeTransition fadeFromTransparent(Duration duration, Node node) {
        FadeTransition fade = new FadeTransition(duration, node);
        fade.setFromValue(0);
        fade.setToValue(1);
        return fade;
    }
}
