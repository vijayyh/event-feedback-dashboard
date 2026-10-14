/* Organiser dashboard: summary indicators (auto-refresh), status drill-down and feedback search/workflow. */
const REFRESH_MS = 30000;
const searchForm = document.getElementById('searchForm');
const feedbackRows = document.getElementById('feedbackRows');
const searchNotice = document.getElementById('searchNotice');
const RATING_RANGES = { low: [1, 2], mid: [3, 3], high: [4, 5] };

function kpi(label, value, note, href) {
    const inner = `<div class="kpi-value">${value}</div><div class="kpi-label">${label}</div>
                   ${note ? `<div class="kpi-note">${note}</div>` : ''}`;
    return href ? `<a class="card kpi" href="${href}">${inner}</a>` : `<div class="card kpi">${inner}</div>`;
}

async function loadSummary() {
    const s = await api('api/dashboard/summary');
    document.getElementById('kpis').innerHTML = [
        kpi('Events', s.totalEvents, null, 'index.html'),
        kpi('Responses', s.totalResponses),
        kpi('Average rating', s.averageRating == null ? '–' : Number(s.averageRating).toFixed(2),
            s.averageRating == null ? null : starsHtml(s.averageRating)),
        kpi('Satisfaction', s.satisfactionPercent == null ? '–' : s.satisfactionPercent + '%', 'rated 4 or 5'),
        kpi('Would recommend', s.recommendPercent == null ? '–' : s.recommendPercent + '%')
    ].join('');
    document.getElementById('updated').textContent =
        `Updated ${new Date(s.generatedAt).toLocaleTimeString()} · refreshes every ${REFRESH_MS / 1000} s`;
}

function breakdownRows(counts, labels, chipClass) {
    const total = Object.values(counts).reduce((a, b) => a + b, 0) || 1;
    return Object.entries(counts).map(([status, n]) => `
        <button type="button" class="breakdown-row" data-status="${status}">
            <span class="chip ${chipClass(status)}">${labels[status] || status}</span>
            <span class="dist-bar"><span style="width:${n * 100 / total}%"></span></span>
            <b class="num">${n}</b>
        </button>`).join('');
}

async function loadBreakdown() {
    const b = await api('api/dashboard/status-breakdown');
    document.getElementById('eventBreakdown').innerHTML =
        breakdownRows(b.events, STATUS_LABELS, s => `status-${s}`);
    document.getElementById('feedbackBreakdown').innerHTML =
        breakdownRows(b.feedback, FEEDBACK_STATUS_LABELS, s => `fb-${s}`);
}

/* Drill-down: status count -> events in that status -> event page */
document.getElementById('eventBreakdown').addEventListener('click', async ev => {
    const row = ev.target.closest('.breakdown-row');
    if (!row) return;
    const events = await api(`api/events?status=${row.dataset.status}`);
    document.getElementById('drilldownTitle').textContent = `${STATUS_LABELS[row.dataset.status]} events (${events.length})`;
    document.getElementById('drilldownList').innerHTML = events.length === 0
        ? '<li class="empty">No events in this status.</li>'
        : events.map(e => `
            <li class="feedback-item">
                <div class="head">
                    <a href="event.html?id=${e.id}"><b>${escapeHtml(e.title)}</b></a>
                    <span>${e.responseCount ? `${starsHtml(e.averageRating)} ${Number(e.averageRating).toFixed(1)}` : '<span class="muted">no ratings</span>'}</span>
                </div>
                <div class="feedback-meta">${formatDate(e.eventDate)} · ${e.responseCount} ${e.responseCount === 1 ? 'response' : 'responses'}</div>
            </li>`).join('');
    document.getElementById('drilldown').hidden = false;
});

/* Feedback status click -> filter the search table */
document.getElementById('feedbackBreakdown').addEventListener('click', ev => {
    const row = ev.target.closest('.breakdown-row');
    if (!row) return;
    document.getElementById('statusFilter').value = row.dataset.status;
    searchFeedback();
    searchForm.scrollIntoView({ behavior: 'smooth', block: 'start' });
});

function workflowSelect(f) {
    const options = Object.entries(FEEDBACK_STATUS_LABELS).map(([value, label]) =>
        `<option value="${value}" ${value === f.status ? 'selected' : ''}>${label}</option>`).join('');
    return `<select data-id="${f.id}" aria-label="Follow-up status" ${f.status === 'RESOLVED' ? 'disabled' : ''}>${options}</select>`;
}

let latestSearch = 0;

async function searchFeedback() {
    const request = ++latestSearch;
    const data = new FormData(searchForm);
    const params = new URLSearchParams();
    if (data.get('q')) params.set('q', data.get('q'));
    if (data.get('eventId')) params.set('eventId', data.get('eventId'));
    if (data.get('status')) params.set('status', data.get('status'));
    const range = RATING_RANGES[data.get('rating')];
    if (range) { params.set('minRating', range[0]); params.set('maxRating', range[1]); }
    try {
        const items = await api('api/feedback?' + params);
        if (request !== latestSearch) return; // a newer search has been sent meanwhile
        feedbackRows.innerHTML = items.length === 0
            ? '<tr><td colspan="5" class="empty">No feedback matches these filters.</td></tr>'
            : items.map(f => `
                <tr>
                    <td class="nowrap">${formatDateTime(f.submittedAt)}</td>
                    <td><a href="event.html?id=${f.eventId}">${escapeHtml(f.eventTitle)}</a></td>
                    <td class="nowrap">${starsHtml(f.overallRating)}</td>
                    <td>${f.comments ? escapeHtml(f.comments) : '<span class="muted">–</span>'}
                        <span class="sub">${escapeHtml(f.attendeeName || 'Anonymous')}</span></td>
                    <td>${workflowSelect(f)}</td>
                </tr>`).join('');
    } catch (err) {
        feedbackRows.innerHTML = `<tr><td colspan="5" class="empty">${escapeHtml(err.message)}</td></tr>`;
    }
}

let searchTimer;
searchForm.addEventListener('input', () => { clearTimeout(searchTimer); searchTimer = setTimeout(searchFeedback, 250); });
searchForm.addEventListener('submit', ev => { ev.preventDefault(); searchFeedback(); });

feedbackRows.addEventListener('change', async ev => {
    const select = ev.target.closest('select[data-id]');
    if (!select) return;
    try {
        const updated = await api(`api/feedback/${select.dataset.id}/status`,
            { method: 'PATCH', body: JSON.stringify({ status: select.value }) });
        showNotice(searchNotice, 'success', `Feedback marked as ${FEEDBACK_STATUS_LABELS[updated.status].toLowerCase()}.`);
        await Promise.all([loadBreakdown(), searchFeedback()]);
    } catch (err) {
        showNotice(searchNotice, 'error', err.message);
        await searchFeedback();
    }
});

async function refresh() {
    try {
        await Promise.all([loadSummary(), loadBreakdown()]);
    } catch (err) {
        document.getElementById('updated').textContent = `Could not refresh: ${err.message}`;
    }
}

(async () => {
    const events = await api('api/events');
    document.getElementById('eventFilter').innerHTML += events.map(e =>
        `<option value="${e.id}">${escapeHtml(e.title)}</option>`).join('');
    await Promise.all([refresh(), searchFeedback()]);
    setInterval(refresh, REFRESH_MS);
})();
