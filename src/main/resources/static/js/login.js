/* Organiser sign-in: posts the form to Spring Security, then opens the organiser page. */
const loginForm = document.getElementById('loginForm');
const loginNotice = document.getElementById('loginNotice');
const loginBtn = document.getElementById('loginBtn');

currentUser().then(me => {
    if (me.organiser) location.replace('organiser.html');
});

loginForm.addEventListener('submit', async (ev) => {
    ev.preventDefault();
    loginNotice.hidden = true;
    loginBtn.disabled = true;
    try {
        const response = await fetch('api/auth/login', {
            method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded', 'X-XSRF-TOKEN': csrfToken() },
            body: new URLSearchParams(new FormData(loginForm))
        });
        if (response.ok) {
            location.href = 'organiser.html';
            return;
        }
        const body = await response.json().catch(() => null);
        showNotice(loginNotice, 'error', (body && body.message) || 'Sign-in failed');
    } catch (err) {
        showNotice(loginNotice, 'error', err.message);
    } finally {
        loginBtn.disabled = false;
    }
});
