(function () {
    'use strict';

    var authMeta = document.querySelector('meta[name="pwa-authenticated"]');
    var isAuthed = authMeta && authMeta.getAttribute('content') === 'true';
    var hasSubscription = false;
    var pushSupported = ('serviceWorker' in navigator) && ('PushManager' in window) && ('Notification' in window);

    function inform(message, text) {
        try {
            if (window.MessageNotifications) {
                window.MessageNotifications.show({
                    title: message,
                    text: text || '',
                    silent: true
                });
            }
        } catch (e) {}
    }

    var swError = null;
    if ('serviceWorker' in navigator) {
        navigator.serviceWorker.register('/sw.js').catch(function (err) {
            swError = (err && err.message) || 'register failed';
        });
    }

    function informUnsupported() {
        inform('Push не поддерживается', 'Ваш браузер не поддерживает push-уведомления');
    }

    function informSwFailed() {
        inform('Не удалось включить уведомления', 'Сервис-воркер не активировался. Обновите страницу и попробуйте снова');
    }

    function informSubscribeFailed() {
        inform('Не удалось включить уведомления', 'Браузер отклонил подписку на push');
    }

    function informBlocked() {
        inform('Уведомления отключены', 'Разрешите их для этого сайта в настройках браузера (значок замка в адресной строке)');
    }

    if ('serviceWorker' in navigator) {
        navigator.serviceWorker.register('/sw.js').catch(function () {});
    }

    if (!pushSupported) {
        document.addEventListener('click', function (event) {
            if (event.target.closest && event.target.closest('.notifications-toggle')) {
                event.preventDefault();
                informUnsupported();
            }
        });
        return;
    }

    function urlBase64ToUint8Array(base64String) {
        var padding = '='.repeat((4 - base64String.length % 4) % 4);
        var base64 = (base64String + padding).replace(/-/g, '+').replace(/_/g, '/');
        var rawData = window.atob(base64);
        var outputArray = new Uint8Array(rawData.length);
        for (var i = 0; i < rawData.length; ++i) {
            outputArray[i] = rawData.charCodeAt(i);
        }
        return outputArray;
    }

    function saveSubscription(subscription) {
        return fetch('/api/push/register', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                endpoint: subscription.endpoint,
                p256dh: subscription.keys.p256dh,
                auth: subscription.keys.auth
            })
        }).then(function (response) {
            if (!response.ok) {
                throw new Error('register failed');
            }
        });
    }

    function removeSubscription(subscription) {
        return fetch('/api/push/unregister', {
            method: 'DELETE',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ endpoint: subscription.endpoint })
        }).then(function (response) {
            if (!response.ok) {
                throw new Error('unregister failed');
            }
        });
    }

    function createSubscription(registration) {
        return fetch('/api/push/public-key')
            .then(function (response) {
                if (!response.ok) {
                    throw new Error('public-key failed');
                }
                return response.json();
            })
            .then(function (data) {
                return registration.pushManager.subscribe({
                    userVisibleOnly: true,
                    applicationServerKey: urlBase64ToUint8Array(data.publicKey)
                });
            });
    }

    function withReady(timeoutMs) {
        var timeoutPromise = new Promise(function (resolve) {
            setTimeout(function () { resolve(null); }, timeoutMs || 3000);
        });
        return Promise.race([navigator.serviceWorker.ready, timeoutPromise]);
    }

    function withTimeout(promise, ms) {
        var timeoutPromise = new Promise(function (resolve, reject) {
            setTimeout(function () { reject(new Error('timeout')); }, ms || 8000);
        });
        return Promise.race([promise, timeoutPromise]);
    }

    function ensureSubscription() {
        if (!isAuthed) {
            return Promise.resolve();
        }
        return withReady(3000)
            .then(function (registration) {
                if (!registration) {
                    hasSubscription = false;
                    informSwFailed();
                    return;
                }
                if (Notification.permission !== 'granted') {
                    hasSubscription = false;
                    return;
                }
                var subPromise = createSubscription(registration);
                subPromise.then(function (subscription) {
                    hasSubscription = true;
                    updateToggleUI();
                    saveSubscription(subscription).catch(function () {});
                }, function (err) {
                    try { console.error('[pwa] underlying subscribe:', err && err.name, err && err.message); } catch (e2) {}
                });
                return withTimeout(subPromise, 8000).then(
                    function () {},
                    function (err) {
                        hasSubscription = false;
                        updateToggleUI();
                        if (err && err.message === 'timeout') {
                            inform('Не удалось включить уведомления', 'Браузер не смог связаться с push-сервисом');
                        } else if ((err && err.name) === 'NotAllowedError' || (err && err.name) === 'SecurityError') {
                            informBlocked();
                        } else {
                            inform('Не удалось включить уведомления', 'Подписка отклонена (' + ((err && err.name) || '?') + ')');
                        }
                    }
                );
            })
            .catch(function () {});
    }

    function requestPermission() {
        if (Notification.permission === 'granted') {
            return ensureSubscription();
        }
        return Notification.requestPermission().then(function (permission) {
            if (permission === 'granted') {
                return ensureSubscription();
            }
        });
    }

    function disable() {
        return withReady(3000)
            .then(function (registration) {
                if (!registration) {
                    return null;
                }
                return registration.pushManager.getSubscription();
            })
            .then(function (subscription) {
                hasSubscription = false;
                if (!subscription) {
                    return;
                }
                var endpoint = subscription.endpoint;
                return subscription.unsubscribe()
                    .then(function () {
                        return removeSubscription({ endpoint: endpoint }).catch(function () {});
                    });
            })
            .catch(function () {});
    }

    function isEnabled() {
        return Notification.permission === 'granted' && hasSubscription;
    }

    function updateToggleUI() {
        var toggle = document.querySelector('.notifications-toggle');
        if (!toggle) {
            return;
        }
        var enabled = isEnabled();
        toggle.classList.toggle('is-enabled', enabled);
        toggle.setAttribute('aria-pressed', enabled ? 'true' : 'false');
        toggle.title = enabled ? 'Выключить уведомления' : 'Включить уведомления';
    }

    if (navigator.serviceWorker.controller) {
        ensureSubscription();
    } else {
        navigator.serviceWorker.addEventListener('controllerchange', function () {
            ensureSubscription();
        });
    }

    document.addEventListener('click', function (event) {
        var toggle = event.target.closest('.notifications-toggle');
        if (!toggle) {
            return;
        }
        event.preventDefault();
        if (toggle.classList.contains('is-pending')) {
            return;
        }
        updateToggleUI();
        if (isEnabled()) {
            disable().then(updateToggleUI);
            return;
        }
        toggle.classList.add('is-pending');
        requestPermission().then(function () {
            toggle.classList.remove('is-pending');
            updateToggleUI();
            if (Notification.permission === 'denied') {
                informBlocked();
            }
        });
    });

    window.addEventListener('load', updateToggleUI);
    document.addEventListener('visibilitychange', function () {
        if (!document.hidden) {
            updateToggleUI();
        }
    });

    window.PwaNotifications = {
        requestPermission: requestPermission,
        disable: disable,
        isEnabled: isEnabled
    };
})();
