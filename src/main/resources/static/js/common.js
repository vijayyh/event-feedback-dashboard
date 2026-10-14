/*
 * Shared helpers for all pages.
 * All URLs are relative so the UI works both at "/" (embedded server)
 * and at "/event-feedback-dashboard/" (WAR on Tomcat).
 */
const CATEGORY_LABELS = {
    WORKSHOP: 'Workshop', SEMINAR: 'Seminar', WEBINAR: 'Webinar', HACKATHON: 'Hackathon',
    CULTURAL: 'Cultural', SPORTS: 'Sports', OTHER: 'Other'
};
const STATUS_LABELS = { UPCOMING: 'Upcoming', ONGOING: 'Ongoing', COMPLETED: 'Completed', CANCELLED: 'Cancelled' };
const STATUS_TABS = ['ALL', 'UPCOMING', 'ONGOING', 'COMPLETED'];

/* ---------- who is using the page ---------- */

/** Resolves to {organiser: boolean, username}. Cached for the page. */
function currentUser() {
    if (!currentUser.promise) {
        currentUser.promise = fetch('api/auth/me', { headers: { Accept: 'application/json' } })
            .then(r => r.json())
            .catch(() => ({ organiser: false, username: null }));
    }
    return currentUser.promise;
}

function drawNav(nav, page, organiser) {
    const links = [
        { href: 'index.html', label: 'Events', pages: ['index.html', 'event.html'] },
        // organisers do not give feedback, so the link is only for attendees
        ...(organiser ? [] : [{ href: 'feedback.html', label: 'Give feedback', pages: ['feedback.html'] }]),
        ...(organiser ? [{ href: 'alerts.html', label: 'Alerts <span class="nav-badge" id="alertBadge" hidden></span>', pages: ['alerts.html'] }] : []),
        { href: 'organiser.html', label: 'Organiser', pages: ['organiser.html', 'login.html'] },
        { href: 'about.html', label: 'About', pages: ['about.html'] }
    ];
    nav.innerHTML = links.map(l =>
        `<a href="${l.href}" class="${l.pages.includes(page) ? 'active' : ''}">${l.label}</a>`).join('');
}

/** Role badge and sign-out appear once the role is known (me != null); the theme button is always there. */
function drawActions(actions, me) {
    const role = !me ? ''
        : `<span class="role-badge ${me.organiser ? 'organiser' : 'attendee'}">
               ${me.organiser ? 'Organiser · signed in' : 'Attendee view'}</span>
           ${me.organiser ? '<button class="btn small ghost" type="button" id="signOutBtn">Sign out</button>' : ''}`;
    actions.innerHTML = `${role}<button class="btn small ghost" type="button" id="themeBtn"></button>`;

    const themeBtn = document.getElementById('themeBtn');
    const label = () => {
        const dark = document.documentElement.dataset.theme === 'dark';
        themeBtn.textContent = dark ? '\u2600 Light' : '\u263E Dark';
        themeBtn.setAttribute('aria-label', dark ? 'Switch to light theme' : 'Switch to dark theme');
    };
    label();
    themeBtn.addEventListener('click', () => { toggleTheme(); label(); });

    const signOut = document.getElementById('signOutBtn');
    if (signOut) {
        signOut.addEventListener('click', async () => {
            await api('api/auth/logout', { method: 'POST' }).catch(() => null);
            location.href = 'index.html';
        });
    }
}

async function renderHeader() {
    const nav = document.getElementById('nav');
    const actions = document.getElementById('topActions');
    if (!nav) return;
    const page = location.pathname.split('/').pop() || 'index.html';
    // draw the attendee menu straight away so the header is never empty, then adjust for the real role
    drawNav(nav, page, false);
    if (actions) drawActions(actions, null);
    const me = await currentUser();
    if (me.organiser) {
        drawNav(nav, page, true);
        refreshAlertBadge();
    }
    if (actions) drawActions(actions, me);
}

/** Shows the number of unresolved alerts next to the Alerts link (organisers only). */
async function refreshAlertBadge() {
    const badge = document.getElementById('alertBadge');
    if (!badge) return;
    try {
        const { unresolved } = await api('api/alerts/count');
        badge.textContent = unresolved;
        badge.hidden = unresolved === 0;
    } catch (err) {
        badge.hidden = true;
    }
}

/* ---------- HTTP ---------- */

function csrfToken() {
    const match = document.cookie.match(/(?:^|;\s*)XSRF-TOKEN=([^;]+)/);
    return match ? decodeURIComponent(match[1]) : '';
}

/** fetch() wrapper: JSON in, JSON out, CSRF header on writes, throws an Error carrying the server's error body. */
async function api(path, options = {}) {
    const method = (options.method || 'GET').toUpperCase();
    const headers = { 'Content-Type': 'application/json', 'Accept': 'application/json', ...(options.headers || {}) };
    if (method !== 'GET') headers['X-XSRF-TOKEN'] = csrfToken();
    const response = await fetch(path, { ...options, method, headers });
    const body = response.status === 204 ? null : await response.json().catch(() => null);
    if (!response.ok) {
        const error = new Error((body && (body.message || body.error)) || `HTTP ${response.status}`);
        error.status = response.status;
        error.body = body;
        throw error;
    }
    return body;
}

/* ---------- formatting ---------- */

/** Escape text before putting it into innerHTML. */
function escapeHtml(value) {
    return String(value ?? '')
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;');
}

function formatDate(isoDate) {
    if (!isoDate) return '–';
    return new Date(isoDate + (isoDate.length === 10 ? 'T00:00:00' : ''))
        .toLocaleDateString(undefined, { day: '2-digit', month: 'short', year: 'numeric' });
}

function formatDateTime(isoInstant) {
    return new Date(isoInstant).toLocaleString(undefined,
        { day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit' });
}

function statusChip(status) {
    return `<span class="chip status-${escapeHtml(status)}">${escapeHtml(STATUS_LABELS[status] || status)}</span>`;
}

/** Five stars, filled to the nearest whole rating. */
function starsHtml(value) {
    const filled = Math.round(Number(value) || 0);
    let html = '';
    for (let i = 1; i <= 5; i++) html += `<span class="${i <= filled ? 'on' : 'off'}">★</span>`;
    return `<span class="stars" role="img" aria-label="${Number(value || 0).toFixed(1)} out of 5">${html}</span>`;
}

function ratingText(value) {
    return value == null ? '–' : `${Number(value).toFixed(1)} / 5`;
}

/** Attendees can rate an event while it is running and after it has finished. */
function feedbackOpen(event) {
    return event.status === 'ONGOING' || event.status === 'COMPLETED';
}

function eventMeta(e) {
    return [CATEGORY_LABELS[e.category] || e.category, formatDate(e.eventDate), e.venue, e.organizer]
        .filter(Boolean).map(escapeHtml).join(' · ');
}

/** Renders the All / Upcoming / Ongoing / Completed tabs with counts. */
function renderStatusTabs(container, events, active, onSelect) {
    const count = s => s === 'ALL' ? events.length : events.filter(e => e.status === s).length;
    container.innerHTML = STATUS_TABS.map(s => `
        <button type="button" class="tab ${s === active ? 'active' : ''}" data-status="${s}" aria-pressed="${s === active}">
            ${s === 'ALL' ? 'All' : STATUS_LABELS[s]}<span class="count">${count(s)}</span>
        </button>`).join('');
    container.onclick = ev => {
        const tab = ev.target.closest('.tab');
        if (tab) onSelect(tab.dataset.status);
    };
}

/* ---------- forms ---------- */

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

document.addEventListener('DOMContentLoaded', renderHeader);
