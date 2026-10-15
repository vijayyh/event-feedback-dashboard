/* Attendee feedback form. Organisers are shown a message instead of the form. */
const form = document.getElementById('feedbackForm');
const formCard = document.getElementById('formCard');
const notice = document.getElementById('formNotice');
const eventSelect = document.getElementById('eventId');
const submitBtn = document.getElementById('submitBtn');
const comments = document.getElementById('comments');
const thanks = document.getElementById('thanks');
const blocked = document.getElementById('blocked');

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

function showBlocked(html) {
    blocked.innerHTML = `<div class="card">${html}</div>`;
    blocked.hidden = false;
}

function resetForm(keepEventId) {
    form.reset();
    eventSelect.value = keepEventId || '';
    document.getElementById('commentCount').textContent = '0';
    clearFieldErrors(form);
    notice.hidden = true;
}

document.getElementById('anotherResponse').addEventListener('click', () => {
    const eventId = eventSelect.value;
    thanks.hidden = true;
    resetForm(eventId);
    formCard.hidden = false;
});

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
        wouldRecommend: data.wouldRecommend == null ? null : data.wouldRecommend !== 'true',
        comments: data.comments,
        attendeeName: data.attendeeName,
        attendeeEmail: data.attendeeEmail
    };

    submitBtn.disabled = true;
    try {
        await api(`api/events/${data.eventId}/feedback`, { method: 'POST', body: JSON.stringify(payload) });
        const title = eventSelect.selectedOptions[0].dataset.title;
        document.getElementById('thanksText').textContent = `Your rating for "${title}" is now part of the event's results.`;
        document.getElementById('seeFeedback').href = `event.html?id=${data.eventId}`;
        formCard.hidden = true;
        thanks.hidden = false;
    } catch (err) {
        showFieldErrors(form, err.body && err.body.fieldErrors);
        showNotice(notice, 'error', err.message);
    } finally {
        submitBtn.disabled = false;
    }
});

(async () => {
    try {
        const [events, me] = await Promise.all([api('api/events'), currentUser()]);
        if (me.organiser) {
            showBlocked(`<h2>Organisers cannot give feedback</h2>
                <p class="muted">You are signed in as the organiser. Feedback comes from attendees only, so that
                ratings stay honest. Sign out to test the attendee view, or read the results on the
                <a href="index.html">Events</a> page.</p>`);
            return;
        }
        const open = events.filter(feedbackOpen);
        const wanted = new URLSearchParams(location.search).get('eventId');
        const requested = events.find(e => String(e.id) === wanted);
        if (requested && !feedbackOpen(requested)) {
            showBlocked(`<h2>${escapeHtml(requested.title)}</h2>
                <p class="muted">${requested.status === 'UPCOMING'
                    ? 'Feedback opens when the event starts. Please come back once it is underway.'
                    : 'This event was cancelled, so it cannot be rated.'}</p>
                <a href="event.html?id=${requested.id}">View the event</a>`);
            return;
        }
        if (open.length === 0) {
            showBlocked('<p class="muted">No events are open for feedback right now.</p>');
            return;
        }
        eventSelect.innerHTML = '<option value="">Select the event you attended…</option>' +
            open.map(e => `<option value="${e.id}" data-title="${escapeHtml(e.title)}"
                ${String(e.id) === wanted ? 'selected' : ''}>${escapeHtml(e.title)} (${formatDate(e.eventDate)})</option>`).join('');
        formCard.hidden = false;
    } catch (err) {
        showBlocked(`<p class="notice error">Could not load events: ${escapeHtml(err.message)}</p>`);
    }
})();
