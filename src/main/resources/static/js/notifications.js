(function () {
    'use strict';

    var MAX_VISIBLE = 3;
    var HIDE_DELAY = 5000;
    var BURST_WINDOW = 350;
    var SOUND_GAP = 1200;

    var container = null;
    var audioCtx = null;
    var lastSoundAt = 0;
    var burstTimer = null;
    var burstCount = 0;
    var burstOpts = null;

    function ensureContainer() {
        if (container) {
            return container;
        }
        container = document.createElement('div');
        container.className = 'notification-toasts';
        container.setAttribute('aria-live', 'polite');
        document.body.appendChild(container);
        return container;
    }

    function dismissToast(toast) {
        toast.classList.remove('is-visible');
        setTimeout(function () {
            if (toast.parentNode) {
                toast.parentNode.removeChild(toast);
            }
        }, 300);
    }

    function removeInstant(toast) {
        toast.classList.remove('is-visible');
        if (toast.parentNode) {
            toast.parentNode.removeChild(toast);
        }
    }

    function unlockAudio() {
        if (!audioCtx) {
            try {
                var AC = window.AudioContext || window.webkitAudioContext;
                if (AC) {
                    audioCtx = new AC();
                }
            } catch (e) {
                audioCtx = null;
            }
        }
        if (audioCtx && audioCtx.state === 'suspended') {
            try { audioCtx.resume(); } catch (e) {}
        }
    }

    if (document.addEventListener) {
        document.addEventListener('pointerdown', unlockAudio, true);
        document.addEventListener('keydown', unlockAudio, true);
        document.addEventListener('touchstart', unlockAudio, true);
    }

    function playSound() {
        var nowMs = Date.now();
        if (nowMs - lastSoundAt < SOUND_GAP) {
            return;
        }
        lastSoundAt = nowMs;
        unlockAudio();
        if (!audioCtx) {
            return;
        }
        try {
            var now = audioCtx.currentTime;
            var tone = function (freq, start, dur) {
                var osc = audioCtx.createOscillator();
                var gain = audioCtx.createGain();
                osc.type = 'sine';
                osc.frequency.value = freq;
                gain.gain.setValueAtTime(0.0001, start);
                gain.gain.exponentialRampToValueAtTime(0.15, start + 0.02);
                gain.gain.exponentialRampToValueAtTime(0.0001, start + dur);
                osc.connect(gain);
                gain.connect(audioCtx.destination);
                osc.start(start);
                osc.stop(start + dur + 0.02);
            };
            tone(880, now, 0.1);
            tone(1174.66, now + 0.08, 0.14);
        } catch (e) {}
    }

    function renderToast(options) {
        var opts = options || {};
        var title = opts.title || 'Новое сообщение';
        var text = opts.text || '';
        var sender = opts.sender || '';
        var href = opts.href || '';

        var toasts = ensureContainer();
        while (toasts.children.length >= MAX_VISIBLE) {
            removeInstant(toasts.firstChild);
        }

        playSound();

        var toast = document.createElement('div');
        toast.className = 'notification-toast';

        var avatar = document.createElement('div');
        avatar.className = 'notification-toast-avatar';
        avatar.textContent = (sender.charAt(0) || '?').toUpperCase();

        var body = document.createElement('div');
        body.className = 'notification-toast-body';

        var titleEl = document.createElement('span');
        titleEl.className = 'notification-toast-title';
        titleEl.textContent = title;
        body.appendChild(titleEl);

        var textEl = document.createElement('span');
        textEl.className = 'notification-toast-text';
        textEl.textContent = text;
        body.appendChild(textEl);

        var closeBtn = document.createElement('button');
        closeBtn.className = 'notification-toast-close';
        closeBtn.setAttribute('aria-label', 'Закрыть');
        closeBtn.innerHTML = '&times;';
        closeBtn.addEventListener('click', function (e) {
            e.stopPropagation();
            dismissToast(toast);
        });

        toast.appendChild(avatar);
        toast.appendChild(body);
        toast.appendChild(closeBtn);

        if (href) {
            toast.classList.add('is-clickable');
            toast.addEventListener('click', function () {
                window.location.href = href;
            });
        }

        toasts.appendChild(toast);
        requestAnimationFrame(function () {
            toast.classList.add('is-visible');
        });

        var hideTimer = setTimeout(function () {
            dismissToast(toast);
        }, HIDE_DELAY);

        toast.addEventListener('mouseenter', function () {
            clearTimeout(hideTimer);
        });
        toast.addEventListener('mouseleave', function () {
            clearTimeout(hideTimer);
            hideTimer = setTimeout(function () {
                dismissToast(toast);
            }, HIDE_DELAY);
        });
    }

    function flushBurst() {
        burstTimer = null;
        var opts = burstOpts;
        var count = burstCount;
        burstOpts = null;
        burstCount = 0;
        if (!opts) {
            return;
        }
        if (count > 2) {
            renderToast({
                sender: opts.sender,
                title: opts.title,
                text: (opts.text || '') + ' · ещё ' + (count - 1),
                href: opts.href
            });
        } else {
            renderToast(opts);
        }
    }

    function showNotification(options) {
        if (!burstTimer) {
            burstTimer = setTimeout(flushBurst, BURST_WINDOW);
            burstCount = 0;
        }
        burstCount++;
        burstOpts = options;
    }

    window.MessageNotifications = {
        show: showNotification
    };
})();
