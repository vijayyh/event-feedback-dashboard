/* Organiser page (signed in only): create events, change their status, see their results. */
const form = document.getElementById('eventForm');
const notice = document.getElementById('formNotice');
const manageNotice = document.getElementById('manageNotice');
const rows = document.getElementById('eventRows');
const tabs = document.getElementById('statusTabs');
const saveBtn = document.getElementById('saveBtn');
let allEvents = [];
let activeStatus = 'ALL';

function statusSelect(e) {
    const options = Object.entries(STATUS_LABELS).map(([value, label]) =>
        `<option value="${value}" ${value === e.status ? 'selected' : ''}>${label}</option>`).join('');
    return `<select data-id="${e.id}" aria-label="Status of ${escapeHtml(e.title)}">${options}</select>`;
}

function render() {
    renderStatusTabs(tabs, allEvents, activeStatus, status => { activeStatus = status; render(); });
    const shown = activeStatus === 'ALL' ? allEvents : allEvents.filter(e => e.status === activeStatus);
    if (shown.length === 0) {
        rows.innerHTML = '<tr><td colspan="5" class="empty">No events in this status.</td></tr>';
        return;
    }
    rows.innerHTML = shown.map(e => `
        <tr>
            <td>${escapeHtml(e.title)}<span class="sub">${formatDate(e.eventDate)}${e.venue ? ' · ' + escapeHtml(e.venue) : ''}</span></td>
            <td>${statusSelect(e)}</td>
            <td class="num">${e.responseCount}</td>
            <td class="num">${ratingText(e.averageRating)}</td>
            <td class="nowrap"><a class="btn small secondary" href="event.html?id=${e.id}">Feedback</a></td>
        </tr>`).join('');
}

async function loadEvents() {
    try {
        allEvents = await api('api/events');
        render();
    } catch (err) {
        rows.innerHTML = `<tr><td colspan="5" class="empty">Could not load events: ${escapeHtml(err.message)}</td></tr>`;
    }
}

rows.addEventListener('change', async (ev) => {
    const select = ev.target.closest('select[data-id]');
    if (!select) return;
    const event = allEvents.find(e => String(e.id) === select.dataset.id);
    try {
        const updated = await api(`api/events/${select.dataset.id}/status`,
            { method: 'PATCH', body: JSON.stringify({ status: select.value }) });
        Object.assign(event, updated);
        showNotice(manageNotice, 'success', `"${updated.title}" is now ${STATUS_LABELS[updated.status].toLowerCase()}.`);
        render();
    } catch (err) {
        select.value = event.status;
        showNotice(manageNotice, 'error', err.status === 401 ? 'Your session has expired – please sign in again.' : err.message);
    }
});

form.addEventListener('submit', async (ev) => {
    ev.preventDefault();
    clearFieldErrors(form);
    notice.hidden = true;

    const payload = formToObject(form);
    if (payload.expectedAttendees != null) payload.expectedAttendees = Number(payload.expectedAttendees);

    saveBtn.disabled = true;
    try {
        const created = await api('api/events', { method: 'POST', body: JSON.stringify(payload) });
        showNotice(notice, 'success', `Event "${created.title}" saved.`);
        form.reset();
        await loadEvents();
    } catch (err) {
        showFieldErrors(form, err.body && err.body.fieldErrors);
        showNotice(notice, 'error', err.status === 401 ? 'Your session has expired – please sign in again.' : err.message);
    } finally {
        saveBtn.disabled = false;
    }
});

loadEvents();
