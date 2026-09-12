package gm.ui.fx.common;

import gm.dto.HistoryPointDTO;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;

import java.util.List;
import java.util.Map;

/**
 * Draws the graphs of the application: a value that was followed over time, as one line per series.
 * <p>
 * The engine records every point with the moment it happened, and the graph shows the seconds that
 * passed since the first point it draws - so all the lines of one graph share the same starting
 * point, whatever the clock of the machine says.
 */
public final class HistoryChart {

    private static final double MILLIS_PER_SECOND = 1000.0;

    private HistoryChart() {
    }

    /**
     * Draws the given series in the chart, replacing whatever it held before.
     *
     * @param seriesByName the name of every line and its points, in the order they should be drawn
     * @return whether there was anything at all to draw
     */
    public static boolean fill(LineChart<Number, Number> chart,
                               Map<String, List<HistoryPointDTO>> seriesByName) {
        chart.getData().clear();
        Long firstTimeMillis = firstTimeOf(seriesByName);
        if (firstTimeMillis == null) {
            return false;
        }
        for (Map.Entry<String, List<HistoryPointDTO>> entry : seriesByName.entrySet()) {
            XYChart.Series<Number, Number> series = new XYChart.Series<>();
            series.setName(entry.getKey());
            for (HistoryPointDTO point : entry.getValue()) {
                double seconds = (point.timeMillis() - firstTimeMillis) / MILLIS_PER_SECOND;
                series.getData().add(new XYChart.Data<>(seconds, point.value()));
            }
            chart.getData().add(series);
        }
        return true;
    }

    /**
     * @return the moment of the earliest point of all the series, or {@code null} when they are all
     *         empty
     */
    private static Long firstTimeOf(Map<String, List<HistoryPointDTO>> seriesByName) {
        Long firstTimeMillis = null;
        for (List<HistoryPointDTO> points : seriesByName.values()) {
            if (!points.isEmpty()) {
                long candidate = points.get(0).timeMillis();
                if (firstTimeMillis == null || candidate < firstTimeMillis) {
                    firstTimeMillis = candidate;
                }
            }
        }
        return firstTimeMillis;
    }
}
