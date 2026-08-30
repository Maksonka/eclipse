'use strict';

var CACHE_NAME = 'shadowvibe-v3';

self.addEventListener('install', function (event) {
    event.waitUntil(caches.open(CACHE_NAME).then(function (cache) {
        return cache.addAll([
            '/css/style.css',
            '/js/notifications.js',
            '/js/csrf.js',
            '/img/icon-192.png',
            '/img/icon-512.png'
        ]);
    }));
    self.skipWaiting();
});

self.addEventListener('activate', function (event) {
    event.waitUntil(
        caches.keys().then(function (keys) {
            return Promise.all(
                keys.filter(function (key) {
                    return key !== CACHE_NAME;
                }).map(function (key) {
                    return caches.delete(key);
                })
            );
        }).then(function () {
            return self.clients.claim();
        })
    );
});

self.addEventListener('fetch', function (event) {
    var request = event.request;
    if (request.method !== 'GET') {
        return;
    }
    var url = new URL(request.url);
    if (url.origin !== self.location.origin) {
        return;
    }

    if (request.mode === 'navigate') {
        event.respondWith(
            fetch(request)
                .then(function (response) {
                    var copy = response.clone();
                    caches.open(CACHE_NAME).then(function (cache) {
                        cache.put('/', copy);
                    });
                    return response;
                })
                .catch(function () {
                    return caches.match('/');
                })
        );
        return;
    }

    event.respondWith(
        caches.match(request).then(function (cached) {
            if (cached) {
                return cached;
            }
            return fetch(request).then(function (response) {
                if (response.ok && (url.pathname.indexOf('/css/') === 0 ||
                    url.pathname.indexOf('/js/') === 0 ||
                    url.pathname.indexOf('/img/') === 0)) {
                    var copy = response.clone();
                    caches.open(CACHE_NAME).then(function (cache) {
                        cache.put(request, copy);
                    });
                }
                return response;
            });
        })
    );
});

self.addEventListener('push', function (event) {
    var data = {};
    if (event.data) {
        try {
            data = event.data.json();
        } catch (e) {
            data = { body: event.data.text() };
        }
    }

    var title = data.title || 'ShadowVibe';
    var options = {
        body: data.body || '',
        icon: '/img/icon-192.png',
        badge: '/img/icon-512.png',
        tag: data.tag || 'default',
        data: { url: data.url || '/chat' }
    };

    event.waitUntil(self.registration.showNotification(title, options));
});

self.addEventListener('notificationclick', function (event) {
    event.notification.close();
    var target = (event.notification.data && event.notification.data.url) || '/chat';
    event.waitUntil(
        self.clients.matchAll({ type: 'window', includeUncontrolled: true }).then(function (list) {
            for (var i = 0; i < list.length; i++) {
                var client = list[i];
                if ('focus' in client) {
                    client.focus();
                    return;
                }
            }
            return self.clients.openWindow(target);
        })
    );
});
