/* Event page: details, rating summary and every piece of feedback, readable by everyone. */
const eventId = new URLSearchParams(location.search).get('id');
const header = document.getElementById('eventHeader');

/** Uses GET /api/events/{id}/summary (distribution and averages are computed on the server). */
function renderSummary(sum) {
    const summary = document.getElementById('summary');
    if (sum.responseCount === 0) {
        summary.innerHTML = '<p class="empty">No ratings yet.</p>';
        return;
    }
    const rows = [5, 4, 3, 2, 1].map(star => {
        const n = sum.distribution[star] || 0;
        return `<div class="dist-row"><span>${star} ★</span>
                    <div class="dist-bar"><span style="width:${n * 100 / sum.responseCount}%"></span></div>
                    <span class="num">${n}</span></div>`;
    }).join('');
    summary.innerHTML = `
        <div class="summary">
            <div>
                <div class="big-score">${Number(sum.averageRating).toFixed(1)}</div>
                <div>${starsHtml(sum.averageRating)}</div>
                <div class="muted">${sum.responseCount} ${sum.responseCount === 1 ? 'response' : 'responses'}</div>
            </div>
            <div>${rows}</div>
        </div>
        <div class="facts">
            <span><b>${sum.recommendPercent}%</b> would recommend it</span>
            <span>Content / speakers <b>${Number(sum.contentAverage).toFixed(1)}</b> / 5</span>
            <span>Organisation &amp; venue <b>${Number(sum.organizationAverage).toFixed(1)}</b> / 5</span>
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
        const [event, feedback, summary, me] = await Promise.all([
            api(`api/events/${eventId}`), api(`api/events/${eventId}/feedback`),
            api(`api/events/${eventId}/summary`), currentUser()]);
        document.title = `${event.title} · Event Feedback Dashboard`;
        renderEventHeader(event, me.organiser);
        renderSummary(summary);
        renderFeedback(feedback);
        document.getElementById('eventBody').hidden = false;
    } catch (err) {
        header.innerHTML = `<p class="empty">${err.status === 404 ? 'This event does not exist.' : escapeHtml(err.message)}
            <a href="index.html">Back to events</a></p>`;
    }
})();
