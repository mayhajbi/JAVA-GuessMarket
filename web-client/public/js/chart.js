import {el} from './dom.js';

const SVG_NS = 'http://www.w3.org/2000/svg';
const WIDTH = 600;
const HEIGHT = 260;
const MARGIN = {left: 55, right: 15, top: 10, bottom: 45};
const TICKS = 5;
const MILLIS_PER_SECOND = 1000;
const COLORS = ['#1f77b4', '#d62728', '#2ca02c', '#9467bd', '#ff7f0e', '#17becf'];

function svg(tag, attributes, text) {
    const node = document.createElementNS(SVG_NS, tag);
    for (const [name, value] of Object.entries(attributes)) {
        node.setAttribute(name, value);
    }
    if (text !== undefined) {
        node.textContent = text;
    }
    return node;
}

function ticks(min, max) {
    return Array.from({length: TICKS}, (unused, index) => min + (max - min) * index / (TICKS - 1));
}

/**
 * Draws the value of every option over time, one line per option; the x axis is the seconds since the
 * first price of the event.
 *
 * @param series [{optionName, points: [{timeMillis, value}]}]
 * @param labels {x, y}: the names of the axes
 * @return the chart, or null when no option has a price yet
 */
export function createPriceChart(series, labels = {x: 'Seconds since the first price', y: 'Share value'}) {
    const allPoints = series.flatMap((one) => one.points);
    if (allPoints.length === 0) {
        return null;
    }
    const firstTime = Math.min(...allPoints.map((point) => point.timeMillis));
    const seconds = (point) => (point.timeMillis - firstTime) / MILLIS_PER_SECOND;
    const maxSeconds = Math.max(1, ...allPoints.map(seconds));
    let minValue = Math.min(...allPoints.map((point) => point.value));
    let maxValue = Math.max(...allPoints.map((point) => point.value));
    if (minValue === maxValue) {
        minValue -= 0.5;
        maxValue += 0.5;
    }
    const plotWidth = WIDTH - MARGIN.left - MARGIN.right;
    const plotHeight = HEIGHT - MARGIN.top - MARGIN.bottom;
    const x = (value) => MARGIN.left + value / maxSeconds * plotWidth;
    const y = (value) => MARGIN.top + plotHeight - (value - minValue) / (maxValue - minValue) * plotHeight;

    const chart = svg('svg', {viewBox: `0 0 ${WIDTH} ${HEIGHT}`, class: 'chart'});
    chart.append(
        svg('line', {x1: MARGIN.left, y1: y(minValue), x2: WIDTH - MARGIN.right, y2: y(minValue), class: 'axis'}),
        svg('line', {x1: MARGIN.left, y1: MARGIN.top, x2: MARGIN.left, y2: y(minValue), class: 'axis'}));
    for (const tick of ticks(0, maxSeconds)) {
        chart.append(svg('text', {x: x(tick), y: y(minValue) + 16, 'text-anchor': 'middle'}, tick.toFixed(1)));
    }
    for (const tick of ticks(minValue, maxValue)) {
        chart.append(
            svg('line', {x1: MARGIN.left, y1: y(tick), x2: WIDTH - MARGIN.right, y2: y(tick), class: 'grid'}),
            svg('text', {x: MARGIN.left - 6, y: y(tick) + 4, 'text-anchor': 'end'}, tick.toFixed(2)));
    }
    chart.append(
        svg('text', {x: MARGIN.left + plotWidth / 2, y: HEIGHT - 6, 'text-anchor': 'middle'}, labels.x),
        svg('text', {x: 12, y: HEIGHT / 2, 'text-anchor': 'middle', transform: `rotate(-90 12 ${HEIGHT / 2})`}, labels.y));

    const legend = el('div', {class: 'legend'});
    series.forEach((one, index) => {
        const color = COLORS[index % COLORS.length];
        const path = one.points.map((point) => `${x(seconds(point))},${y(point.value)}`).join(' ');
        chart.append(svg('polyline', {points: path, fill: 'none', stroke: color, 'stroke-width': 2}));
        for (const point of one.points) {
            chart.append(svg('circle', {cx: x(seconds(point)), cy: y(point.value), r: 3, fill: color}));
        }
        legend.append(el('span', {}, el('span', {class: 'swatch', style: `background:${color}`}), one.optionName));
    });
    return el('div', {class: 'chart-box'}, chart, legend);
}
