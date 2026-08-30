(function () {
    "use strict";

    var tip = null;
    var tipTimer = null;
    var currentEl = null;

    function ensureTip() {
        if (tip) return;
        tip = document.createElement('div');
        tip.className = 'custom-tooltip';
        tip.setAttribute('role', 'tooltip');
        document.body.appendChild(tip);
    }

    function show(el, text) {
        ensureTip();
        tip.textContent = text;
        tip.classList.add('visible');
        position(el);
    }

    function position(el) {
        var r = el.getBoundingClientRect();
        var tw = tip.offsetWidth;
        var th = tip.offsetHeight;
        var left = r.left + r.width / 2 - tw / 2;
        var top = r.top - th - 8;
        if (top < 8) {
            top = r.bottom + 8;
            tip.classList.add('below');
        } else {
            tip.classList.remove('below');
        }
        left = Math.max(8, Math.min(left, window.innerWidth - tw - 8));
        tip.style.left = left + 'px';
        tip.style.top = top + 'px';
    }

    function hide() {
        if (tipTimer) { clearTimeout(tipTimer); tipTimer = null; }
        if (tip) tip.classList.remove('visible');
        currentEl = null;
    }

    document.addEventListener('mouseover', function (e) {
        var el = e.target.closest ? e.target.closest('[title], [data-orig-title]') : null;
        if (el && el === currentEl) return;
        hide();
        currentEl = el;
        if (!el) return;
        var title = el.getAttribute('data-orig-title') || el.getAttribute('title');
        if (!title) return;
        el.setAttribute('data-orig-title', title);
        el.removeAttribute('title');
        tipTimer = setTimeout(function () {
            if (currentEl === el) show(el, title);
        }, 350);
    });

    document.addEventListener('mouseout', function (e) {
        var el = e.target;
        if (currentEl && (currentEl === el || currentEl.contains(el))) {
            hide();
        }
    });

    window.addEventListener('resize', function () {
        if (currentEl && tip && tip.classList.contains('visible')) {
            position(currentEl);
        }
    });

    window.addEventListener('scroll', function () {
        if (currentEl && tip && tip.classList.contains('visible')) {
            position(currentEl);
        }
    }, true);
})();
