(function () {
    'use strict';

    if (!('serviceWorker' in navigator)) {
        return;
    }

    var authMeta = document.querySelector('meta[name="pwa-authenticated"]');
    var isAuthed = authMeta && authMeta.getAttribute('content') === 'true';
    var hasSubscription = false;

    navigator.serviceWorker.register('/sw.js').catch(function () {});

    if (!('PushManager' in window) || !('Notification' in window)) {
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
            body: JSON.stringify(subscription)
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

    function ensureSubscription() {
        if (!isAuthed) {
            return Promise.resolve();
        }
        return navigator.serviceWorker.ready
            .then(function (registration) {
            if (Notification.permission !== 'granted') {
                hasSubscription = false;
                return;
            }
            return registration.pushManager.getSubscription()
                .then(function (existing) {
                    if (existing) {
                        hasSubscription = true;
                        return saveSubscription(existing).catch(function () {});
                    }
                    return createSubscription(registration)
                        .then(function (subscription) {
                            hasSubscription = true;
                            return saveSubscription(subscription).catch(function () {});
                        })
                        .catch(function () {});
                });
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
        return navigator.serviceWorker.ready
            .then(function (registration) {
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
        if (isEnabled()) {
            disable().then(updateToggleUI);
        } else {
            requestPermission().then(updateToggleUI);
        }
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
