(function () {
    const root = document.documentElement;
    const stored = localStorage.getItem('eclipse-theme');
    const serverTheme = root.getAttribute('data-theme-preference');
    const allowed = ['light', 'dark', 'aurora', 'sunset'];
    let theme = stored || serverTheme || 'dark';
    if (allowed.indexOf(theme) === -1) theme = 'dark';
    root.setAttribute('data-theme', theme);
})();
