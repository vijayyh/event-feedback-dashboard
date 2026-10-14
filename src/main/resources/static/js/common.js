/*
 * Shared helpers for all pages.
 * All URLs are relative so the UI works both at "/" (embedded server)
 * and at "/event-feedback-dashboard/" (WAR on Tomcat).
 */
const NAV = [
    { href: 'index.html', label: 'Overview' }
];

function renderNav() {
    const current = location.pathname.split('/').pop() || 'index.html';
    const nav = document.getElementById('nav');
    if (!nav) return;
    nav.innerHTML = NAV.map(item =>
        `<a href="${item.href}" class="${item.href === current ? 'active' : ''}">${item.label}</a>`
    ).join('');
}

/** Escape text before putting it into innerHTML. */
function escapeHtml(value) {
    return String(value ?? '')
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;');
}

/** fetch() wrapper: JSON in, JSON out, throws an Error carrying the server's error body. */
async function api(path, options = {}) {
    const response = await fetch(path, {
        headers: { 'Content-Type': 'application/json', 'Accept': 'application/json' },
        ...options
    });
    const body = response.status === 204 ? null : await response.json().catch(() => null);
    if (!response.ok) {
        const error = new Error((body && (body.message || body.error)) || `HTTP ${response.status}`);
        error.body = body;
        throw error;
    }
    return body;
}

document.addEventListener('DOMContentLoaded', renderNav);
