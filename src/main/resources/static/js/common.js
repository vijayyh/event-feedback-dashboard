/*
 * Shared helpers for all pages.
 * All URLs are relative so the UI works both at "/" (embedded server)
 * and at "/event-feedback-dashboard/" (WAR on Tomcat).
 */
const NAV = [
    { href: 'index.html', label: 'Overview' },
    { href: 'events.html', label: 'Events' },
    { href: 'feedback.html', label: 'Give Feedback' }
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

/** Form fields -> plain object; empty strings become null. */
function formToObject(form) {
    const data = {};
    for (const [key, value] of new FormData(form).entries()) {
        data[key] = value === '' ? null : value;
    }
    return data;
}

function clearFieldErrors(form) {
    form.querySelectorAll('.field-error').forEach(el => { el.textContent = ''; });
    form.querySelectorAll('.invalid').forEach(el => el.classList.remove('invalid'));
}

/** Show {field: message} from the API next to the matching inputs. */
function showFieldErrors(form, fieldErrors = {}) {
    for (const [field, message] of Object.entries(fieldErrors)) {
        const slot = form.querySelector(`.field-error[data-for="${field}"]`);
        if (slot) slot.textContent = message;
        form.querySelectorAll(`[name="${field}"]`).forEach(el => el.classList.add('invalid'));
    }
}

function showNotice(el, type, message) {
    el.className = `notice ${type}`;
    el.textContent = message;
    el.hidden = false;
}

function formatDate(isoDate) {
    if (!isoDate) return '–';
    return new Date(isoDate + (isoDate.length === 10 ? 'T00:00:00' : ''))
        .toLocaleDateString(undefined, { day: '2-digit', month: 'short', year: 'numeric' });
}

function statusChip(status) {
    const label = String(status || '').replace('_', ' ').toLowerCase();
    return `<span class="chip status-${escapeHtml(status)}">${escapeHtml(label)}</span>`;
}

function ratingText(value) {
    return value == null ? '–' : `${Number(value).toFixed(1)} / 5`;
}

document.addEventListener('DOMContentLoaded', renderNav);
