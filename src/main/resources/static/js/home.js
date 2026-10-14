/* Public events page: status tabs + one card per event with its rating. */
const cards = document.getElementById('eventCards');
const tabs = document.getElementById('statusTabs');
let allEvents = [];
let activeStatus = new URLSearchParams(location.search).get('status') || 'ALL';
let isOrganiser = false;

function ratingLine(e) {
    if (e.responseCount > 0) {
        return `${starsHtml(e.averageRating)} <b>${Number(e.averageRating).toFixed(1)}</b>
                <span class="muted">from ${e.responseCount} ${e.responseCount === 1 ? 'response' : 'responses'}</span>`;
    }
    if (e.status === 'UPCOMING') return '<span class="muted">Feedback opens when the event starts</span>';
    if (e.status === 'CANCELLED') return '<span class="muted">Event cancelled</span>';
    return `<span class="muted">No feedback yet${isOrganiser ? '' : ' – be the first'}</span>`;
}

function render() {
    renderStatusTabs(tabs, allEvents, activeStatus, status => {
        activeStatus = status;
        const url = status === 'ALL' ? location.pathname : `${location.pathname}?status=${status}`;
        history.replaceState(null, '', url);
        render();
    });
    const shown = activeStatus === 'ALL' ? allEvents : allEvents.filter(e => e.status === activeStatus);
    if (shown.length === 0) {
        const filtered = searchBox.value.trim() || categorySelect.value;
        cards.innerHTML = `<p class="empty">No ${activeStatus === 'ALL' ? '' : STATUS_LABELS[activeStatus].toLowerCase() + ' '}events${filtered ? ' match your search' : ''}.</p>`;
        return;
    }
    cards.innerHTML = shown.map(e => `
        <article class="card event-card">
            <div class="top">${statusChip(e.status)}<span class="muted">${escapeHtml(CATEGORY_LABELS[e.category] || '')}</span></div>
            <h3><a href="event.html?id=${e.id}">${escapeHtml(e.title)}</a></h3>
            <div class="meta">${[formatDate(e.eventDate), e.venue].filter(Boolean).map(escapeHtml).join(' · ')}</div>
            <div class="rating-line">${ratingLine(e)}</div>
            <div class="actions">
                <a class="btn small secondary" href="event.html?id=${e.id}">View feedback</a>
                ${feedbackOpen(e) && !isOrganiser ? `<a class="btn small" href="feedback.html?eventId=${e.id}">Give feedback</a>` : ''}
            </div>
        </article>`).join('');
}

const searchBox = document.getElementById('search');
const categorySelect = document.getElementById('category');

/** Keyword and category go to the server (GET /api/events?q=&category=); the status tabs filter the result. */
async function loadEvents() {
    const params = new URLSearchParams();
    if (searchBox.value.trim()) params.set('q', searchBox.value.trim());
    if (categorySelect.value) params.set('category', categorySelect.value);
    try {
        allEvents = await api('api/events' + (params.toString() ? '?' + params : ''));
        render();
    } catch (err) {
        cards.innerHTML = `<p class="empty">Could not load events: ${escapeHtml(err.message)}</p>`;
    }
}

let searchTimer;
searchBox.addEventListener('input', () => {
    clearTimeout(searchTimer);
    searchTimer = setTimeout(loadEvents, 250);
});
categorySelect.addEventListener('change', loadEvents);

(async () => {
    isOrganiser = (await currentUser()).organiser;
    if (!STATUS_TABS.includes(activeStatus)) activeStatus = 'ALL';
    await loadEvents();
})();
