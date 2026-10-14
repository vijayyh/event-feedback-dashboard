/* Alert / exception view (organiser): filter, acknowledge and resolve alerts. */
const list = document.getElementById('alertList');
const tabs = document.getElementById('alertTabs');
const severity = document.getElementById('severity');
const notice = document.getElementById('alertNotice');

const ALERT_TABS = { OPEN: 'Open', ACKNOWLEDGED: 'Acknowledged', RESOLVED: 'Resolved', ALL: 'All' };
const TYPE_LABELS = { LOW_RATING: 'Low rating', NEGATIVE_COMMENT: 'Negative comment', LOW_AVERAGE: 'Low average' };
let activeTab = 'OPEN';

function renderTabs() {
    tabs.innerHTML = Object.entries(ALERT_TABS).map(([value, label]) => `
        <button type="button" class="tab ${value === activeTab ? 'active' : ''}" data-tab="${value}"
                aria-pressed="${value === activeTab}">${label}</button>`).join('');
}

tabs.addEventListener('click', ev => {
    const tab = ev.target.closest('.tab');
    if (!tab) return;
    activeTab = tab.dataset.tab;
    renderTabs();
    loadAlerts();
});
severity.addEventListener('change', loadAlerts);

function actions(a) {
    if (a.status === 'RESOLVED') return `<span class="muted">Resolved ${formatDateTime(a.resolvedAt)}</span>`;
    return `${a.status === 'OPEN' ? `<button class="btn small secondary" type="button" data-id="${a.id}" data-to="ACKNOWLEDGED">Acknowledge</button>` : ''}
            <button class="btn small" type="button" data-id="${a.id}" data-to="RESOLVED">Resolve</button>`;
}

async function loadAlerts() {
    const params = new URLSearchParams();
    if (activeTab !== 'ALL') params.set('status', activeTab);
    if (severity.value) params.set('severity', severity.value);
    try {
        const alerts = await api('api/alerts' + (params.toString() ? '?' + params : ''));
        list.innerHTML = alerts.length === 0
            ? '<li class="empty">No alerts here. ✓</li>'
            : alerts.map(a => `
                <li class="card alert-item sev-${a.severity}">
                    <div class="head">
                        <span><span class="chip sev-${a.severity}">${a.severity.toLowerCase()}</span>
                              <b>${TYPE_LABELS[a.type] || a.type}</b></span>
                        <span class="muted">${formatDateTime(a.createdAt)}</span>
                    </div>
                    <p class="comment" style="margin:8px 0">${escapeHtml(a.message)}</p>
                    <div class="feedback-meta">
                        <a href="event.html?id=${a.eventId}">${escapeHtml(a.eventTitle)}</a>
                        ${a.feedbackRating ? ` · feedback rated ${a.feedbackRating}/5` : ''}
                        · status: ${a.status.toLowerCase()}
                    </div>
                    <div class="actions" style="margin-top:10px;display:flex;gap:8px;flex-wrap:wrap">${actions(a)}</div>
                </li>`).join('');
    } catch (err) {
        list.innerHTML = `<li class="empty">Could not load alerts: ${escapeHtml(err.message)}</li>`;
    }
}

list.addEventListener('click', async ev => {
    const button = ev.target.closest('button[data-id]');
    if (!button) return;
    button.disabled = true;
    try {
        await api(`api/alerts/${button.dataset.id}`, { method: 'PATCH', body: JSON.stringify({ status: button.dataset.to }) });
        showNotice(notice, 'success', `Alert ${button.dataset.to === 'RESOLVED' ? 'resolved' : 'acknowledged'}.`);
        await loadAlerts();
        refreshAlertBadge();
    } catch (err) {
        showNotice(notice, 'error', err.message);
        button.disabled = false;
    }
});

renderTabs();
loadAlerts();
