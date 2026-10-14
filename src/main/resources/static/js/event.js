/* Event page: details, rating summary and every piece of feedback, readable by everyone. */
const eventId = new URLSearchParams(location.search).get('id');
const header = document.getElementById('eventHeader');

function average(items, field) {
    return items.reduce((sum, f) => sum + f[field], 0) / items.length;
}

function renderHeader(e, organiser) {
    const action = organiser
        ? '<span class="muted">Organisers can read feedback but cannot submit it.</span>'
        : feedbackOpen(e)
            ? `<a class="btn" href="feedback.html?eventId=${e.id}">Give feedback</a>`
            : `<span class="muted">${e.status === 'UPCOMING' ? 'Feedback opens when the event starts.' : 'This event was cancelled.'}</span>`;
    header.innerHTML = `
        <div class="card stack" style="margin-bottom:16px">
            <div>
                ${statusChip(e.status)}
                <h1 style="margin-top:8px">${escapeHtml(e.title)}</h1>
                <div class="muted">${eventMeta(e)}</div>
            </div>
            ${e.description ? `<p style="margin:0">${escapeHtml(e.description)}</p>` : ''}
            <div>${action}</div>
        </div>`;
}

function renderSummary(items) {
    const summary = document.getElementById('summary');
    if (items.length === 0) {
        summary.innerHTML = '<p class="empty">No ratings yet.</p>';
        return;
    }
    const overall = average(items, 'overallRating');
    const recommend = Math.round(items.filter(f => f.wouldRecommend).length * 100 / items.length);
    const rows = [5, 4, 3, 2, 1].map(star => {
        const n = items.filter(f => f.overallRating === star).length;
        return `<div class="dist-row"><span>${star} ★</span>
                    <div class="dist-bar"><span style="width:${n * 100 / items.length}%"></span></div>
                    <span class="num">${n}</span></div>`;
    }).join('');
    summary.innerHTML = `
        <div class="summary">
            <div>
                <div class="big-score">${overall.toFixed(1)}</div>
                <div>${starsHtml(overall)}</div>
                <div class="muted">${items.length} ${items.length === 1 ? 'response' : 'responses'}</div>
            </div>
            <div>${rows}</div>
        </div>
        <div class="facts">
            <span><b>${recommend}%</b> would recommend it</span>
            <span>Content / speakers <b>${average(items, 'contentRating').toFixed(1)}</b> / 5</span>
            <span>Organisation &amp; venue <b>${average(items, 'organizationRating').toFixed(1)}</b> / 5</span>
        </div>`;
}

function renderFeedback(items) {
    document.getElementById('feedbackCount').textContent = `(${items.length})`;
    const list = document.getElementById('feedbackList');
    if (items.length === 0) {
        list.innerHTML = '<li class="empty">Nobody has shared feedback for this event yet.</li>';
        return;
    }
    list.innerHTML = items.map(f => `
        <li class="feedback-item">
            <div class="head">
                <span>${starsHtml(f.overallRating)} <b>${f.overallRating} / 5</b></span>
                <span class="chip ${f.wouldRecommend ? 'live' : 'down'}">${f.wouldRecommend ? 'Recommends' : 'Does not recommend'}</span>
            </div>
            ${f.comments ? `<p class="comment">“${escapeHtml(f.comments)}”</p>` : '<p class="no-comment">No comment, rating only</p>'}
            <div class="tags">
                <span class="chip">Content ${f.contentRating}/5</span>
                <span class="chip">Organisation ${f.organizationRating}/5</span>
            </div>
            <div class="feedback-meta">${escapeHtml(f.attendeeName || 'Anonymous')} · ${formatDateTime(f.submittedAt)}</div>
        </li>`).join('');
}

(async () => {
    if (!eventId) {
        header.innerHTML = '<p class="empty">No event selected. <a href="index.html">Browse events</a>.</p>';
        return;
    }
    try {
        const [event, feedback, me] = await Promise.all([
            api(`api/events/${eventId}`), api(`api/events/${eventId}/feedback`), currentUser()]);
        document.title = `${event.title} · Event Feedback Dashboard`;
        renderHeader(event, me.organiser);
        renderSummary(feedback);
        renderFeedback(feedback);
        document.getElementById('eventBody').hidden = false;
    } catch (err) {
        header.innerHTML = `<p class="empty">${err.status === 404 ? 'This event does not exist.' : escapeHtml(err.message)}
            <a href="index.html">Back to events</a></p>`;
    }
})();
