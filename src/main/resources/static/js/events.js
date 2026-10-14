/* Events page: register an event and list all events with their feedback statistics. */
const form = document.getElementById('eventForm');
const notice = document.getElementById('formNotice');
const rows = document.getElementById('eventRows');
const saveBtn = document.getElementById('saveBtn');

const CATEGORY_LABELS = {
    WORKSHOP: 'Workshop', SEMINAR: 'Seminar', WEBINAR: 'Webinar', HACKATHON: 'Hackathon',
    CULTURAL: 'Cultural', SPORTS: 'Sports', OTHER: 'Other'
};

async function loadEvents() {
    try {
        const events = await api('api/events');
        document.getElementById('eventCount').textContent = `(${events.length})`;
        if (events.length === 0) {
            rows.innerHTML = '<tr><td colspan="6" class="empty">No events yet – register the first one.</td></tr>';
            return;
        }
        rows.innerHTML = events.map(e => `
            <tr>
                <td>
                    ${escapeHtml(e.title)}
                    <span class="sub">${escapeHtml(CATEGORY_LABELS[e.category] || e.category)}${e.venue ? ' · ' + escapeHtml(e.venue) : ''}</span>
                </td>
                <td class="nowrap">${formatDate(e.eventDate)}</td>
                <td>${statusChip(e.status)}</td>
                <td class="num">${e.responseCount}</td>
                <td class="num">${ratingText(e.averageRating)}</td>
                <td class="nowrap">${e.status === 'CANCELLED' ? '' :
                    `<a class="btn small secondary" href="feedback.html?eventId=${e.id}">Feedback</a>`}</td>
            </tr>`).join('');
    } catch (err) {
        rows.innerHTML = `<tr><td colspan="6" class="empty">Could not load events: ${escapeHtml(err.message)}</td></tr>`;
    }
}

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
        showNotice(notice, 'error', err.message);
    } finally {
        saveBtn.disabled = false;
    }
});

loadEvents();
