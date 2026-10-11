// How values are shown: decimals with two digits, "-" for a value that is not available.
export const NOT_AVAILABLE = '-';

export const EVENT_TYPES = [{value: 'LMSR', label: 'LMSR'}, {value: 'ORDER_BOOK', label: 'Order Book'}];
export const EVENT_STATUSES = [
    {value: 'INACTIVE', label: 'Inactive'}, {value: 'ACTIVE', label: 'Active'}, {value: 'CLOSED', label: 'Closed'}];
export const COMMISSION_TYPES = [
    {value: 'ON_PURCHASE', label: 'on-purchase'}, {value: 'ON_CLOSE', label: 'on-close'}];

export function decimal(value) {
    return value.toFixed(2);
}

export function optionalDecimal(value) {
    return value === null || value === undefined ? NOT_AVAILABLE : decimal(value);
}

export function displayName(values, value) {
    return values.find((candidate) => candidate.value === value).label;
}

export function commission(percent, type) {
    return `${percent}% ${displayName(COMMISSION_TYPES, type)}`;
}
