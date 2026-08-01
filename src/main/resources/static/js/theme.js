(function () {
    const root = document.documentElement;
    const stored = localStorage.getItem('eclipse-theme');
    const serverTheme = root.getAttribute('data-theme-preference');
    const theme = stored || serverTheme || 'dark';
    root.setAttribute('data-theme', theme === 'light' ? 'light' : 'dark');
})();
