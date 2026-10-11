// Builds an element: {class, on<event>: handler, other: attribute}, then the children (text or nodes).
export function el(tag, props = {}, ...children) {
    const node = document.createElement(tag);
    for (const [name, value] of Object.entries(props)) {
        if (value === null || value === undefined || value === false) {
            continue;
        }
        if (name === 'class') {
            node.className = value;
        } else if (name.startsWith('on')) {
            node.addEventListener(name.slice(2), value);
        } else {
            node.setAttribute(name, value);
        }
    }
    node.append(...children);
    return node;
}
