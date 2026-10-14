/* Loaded in <head> so the saved theme applies before the page paints. Light is the default. */
(function () {
    let theme = 'light';
    try {
        theme = localStorage.getItem('efd-theme') || 'light';
    } catch (e) {
        // storage blocked: stay on light
    }
    document.documentElement.dataset.theme = theme;
})();

function toggleTheme() {
    const next = document.documentElement.dataset.theme === 'dark' ? 'light' : 'dark';
    document.documentElement.dataset.theme = next;
    try {
        localStorage.setItem('efd-theme', next);
    } catch (e) {
        // not saved, still switched for this page
    }
    return next;
}
