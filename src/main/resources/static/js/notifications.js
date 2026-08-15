(function () {
    'use strict';

    var MAX_VISIBLE = 3;
    var HIDE_DELAY = 5000;
    var BURST_WINDOW = 350;
    var SOUND_GAP = 1200;

    var container = null;
    var soundEl = null;
    var lastSoundAt = 0;
    var burstTimer = null;
    var burstCount = 0;
    var burstOpts = null;
    var audioUnlocked = false;

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

    function closeForTag(tag) {
        if (!tag || !container) {
            return;
        }
        var toasts = container.querySelectorAll('.notification-toast');
        for (var i = 0; i < toasts.length; i++) {
            if (toasts[i].getAttribute('data-tag') === tag) {
                removeInstant(toasts[i]);
            }
        }
    }

    function removeInstant(toast) {
        toast.classList.remove('is-visible');
        if (toast.parentNode) {
            toast.parentNode.removeChild(toast);
        }
    }

    function getSound() {
        if (!soundEl) {
            soundEl = new Audio('/sounds/notification.mp3');
            soundEl.preload = 'auto';
            soundEl.volume = 0.3;
        }
        return soundEl;
    }

    function unlockAudio() {
        var el = getSound();
        try {
            var p = el.play();
            if (p && typeof p.then === 'function') {
                p.then(function () {
                    el.pause();
                    el.currentTime = 0;
                }).catch(function () {});
            }
        } catch (e) {}
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
        try {
            var el = getSound();
            el.currentTime = 0;
            var p = el.play();
            if (p && typeof p.catch === 'function') {
                p.catch(function () {});
            }
        } catch (e) {}
    }

    function renderToast(options) {
        var opts = options || {};
        var title = opts.title || 'Новое сообщение';
        var text = opts.text || '';
        var sender = opts.sender || '';
        var href = opts.href || '';
        var tag = opts.tag || '';

        var toasts = ensureContainer();

        if (tag) {
            closeForTag(tag);
        }

        while (toasts.children.length >= MAX_VISIBLE) {
            removeInstant(toasts.firstChild);
        }

        if (!opts.silent) {
            playSound();
        }

        var toast = document.createElement('div');
        toast.className = 'notification-toast';
        if (tag) {
            toast.setAttribute('data-tag', tag);
        }

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
                href: opts.href,
                silent: opts.silent
            });
        } else {
            renderToast(opts);
        }
    }

    function canUseSystemNotification() {
        return document.hidden
            && 'Notification' in window
            && Notification.permission === 'granted';
    }

    function showSystemNotification(options) {
        var opts = options || {};
        var title = opts.title || 'Новое сообщение';
        var body = opts.text || '';
        var href = opts.href || '';
        var tag = opts.tag || '';
        try {
            var notification = new Notification(title, {
                body: body,
                icon: '/img/icon-192.png',
                badge: '/img/icon-512.png',
                tag: tag || undefined,
                data: { url: href }
            });
            if (href) {
                notification.onclick = function () {
                    notification.close();
                    window.focus();
                    window.location.href = href;
                };
            }
            return true;
        } catch (e) {
            return false;
        }
    }

    function showNotification(options) {
        if (canUseSystemNotification() && showSystemNotification(options)) {
            return;
        }
        if (!burstTimer) {
            burstTimer = setTimeout(flushBurst, BURST_WINDOW);
            burstCount = 0;
        }
        burstCount++;
        burstOpts = options;
    }

    window.MessageNotifications = {
        show: showNotification,
        closeForTag: closeForTag
    };
})();
