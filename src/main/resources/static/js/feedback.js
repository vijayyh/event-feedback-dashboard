/* Feedback page: attendee picks an event, rates it and optionally comments. */
const form = document.getElementById('feedbackForm');
const notice = document.getElementById('formNotice');
const eventSelect = document.getElementById('eventId');
const recent = document.getElementById('recent');
const submitBtn = document.getElementById('submitBtn');
const comments = document.getElementById('comments');

/* Build the three 1-5 rating button groups. */
document.querySelectorAll('.rating-group[data-field]').forEach(group => {
    const field = group.dataset.field;
    const buttons = [1, 2, 3, 4, 5].map(n => `
        <input type="radio" id="${field}-${n}" name="${field}" value="${n}">
        <label for="${field}-${n}">${n}</label>`).join('');
    group.insertAdjacentHTML('beforeend', `
        <div class="rating-options">${buttons}</div>
        <div class="rating-scale"><span>1 = poor</span><span>5 = excellent</span></div>
        <small class="field-error" data-for="${field}"></small>`);
});

comments.addEventListener('input', () => {
    document.getElementById('commentCount').textContent = comments.value.length;
});

async function loadEvents() {
    try {
        const events = (await api('api/events')).filter(e => e.status !== 'CANCELLED');
        const preselect = new URLSearchParams(location.search).get('eventId');
        eventSelect.innerHTML = '<option value="">Select the event you attended…</option>' +
            events.map(e => `<option value="${e.id}" ${String(e.id) === preselect ? 'selected' : ''}>
                ${escapeHtml(e.title)} (${formatDate(e.eventDate)})</option>`).join('');
        if (eventSelect.value) loadRecent(eventSelect.value);
    } catch (err) {
        eventSelect.innerHTML = '<option value="">Could not load events</option>';
        showNotice(notice, 'error', err.message);
    }
}

async function loadRecent(eventId) {
    if (!eventId) {
        recent.innerHTML = '<li class="empty">Choose an event to see its feedback.</li>';
        document.getElementById('recentFor').textContent = '';
        return;
    }
    const items = await api(`api/events/${eventId}/feedback`);
    document.getElementById('recentFor').textContent = `(${items.length})`;
    recent.innerHTML = items.length === 0
        ? '<li class="empty">No feedback yet – be the first!</li>'
        : items.slice(0, 10).map(f => `
            <li>
                <span class="score">${f.overallRating} / 5</span>
                · content ${f.contentRating} · organisation ${f.organizationRating}
                · ${f.wouldRecommend ? 'recommends' : 'does not recommend'}
                ${f.comments ? `<div>${escapeHtml(f.comments)}</div>` : ''}
                <div class="feedback-meta">${escapeHtml(f.attendeeName || 'Anonymous')} ·
                    ${new Date(f.submittedAt).toLocaleString()}</div>
            </li>`).join('');
}

eventSelect.addEventListener('change', () => loadRecent(eventSelect.value));

form.addEventListener('submit', async (ev) => {
    ev.preventDefault();
    clearFieldErrors(form);
    notice.hidden = true;

    const data = formToObject(form);
    if (!data.eventId) {
        showFieldErrors(form, { eventId: 'Please choose an event' });
        return;
    }
    const payload = {
        overallRating: data.overallRating && Number(data.overallRating),
        contentRating: data.contentRating && Number(data.contentRating),
        organizationRating: data.organizationRating && Number(data.organizationRating),
        wouldRecommend: data.wouldRecommend == null ? null : data.wouldRecommend === 'true',
        comments: data.comments,
        attendeeName: data.attendeeName,
        attendeeEmail: data.attendeeEmail
    };

    submitBtn.disabled = true;
    try {
        await api(`api/events/${data.eventId}/feedback`, { method: 'POST', body: JSON.stringify(payload) });
        showNotice(notice, 'success', 'Thank you! Your feedback has been recorded.');
        await loadRecent(data.eventId);
    } catch (err) {
        showFieldErrors(form, err.body && err.body.fieldErrors);
        showNotice(notice, 'error', err.message);
    } finally {
        submitBtn.disabled = false;
    }
});

loadEvents();
