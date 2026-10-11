// Reading what the user typed. The server checks every value again; these only tell a text that is not a
// number at all, before it is sent.
const WHOLE_NUMBER = /^[+-]?\d+$/;
const DECIMAL_NUMBER = /^[+-]?(\d+\.?\d*|\.\d+)$/;

/** @return the whole number in the text, or null when the text is not one */
export function readWholeNumber(text) {
    return WHOLE_NUMBER.test(text) ? Number(text) : null;
}

/** @return the number in the text, or null when the text is not one */
export function readDecimal(text) {
    return DECIMAL_NUMBER.test(text) ? Number(text) : null;
}
