(function () {
    'use strict';

    var tokenMeta = document.querySelector('meta[name="_csrf"]');
    var headerMeta = document.querySelector('meta[name="_csrf_header"]');
    if (!tokenMeta || !headerMeta) {
        return;
    }
    var csrfToken = tokenMeta.getAttribute('content') || '';
    var csrfHeader = headerMeta.getAttribute('content') || 'X-CSRF-TOKEN';
    if (!csrfToken) {
        return;
    }

    var origFetch = window.fetch;
    window.fetch = function (input, init) {
        init = init || {};
        var method = (init.method || 'GET').toUpperCase();
        if (method === 'GET' || method === 'HEAD' || method === 'OPTIONS') {
            return origFetch(input, init);
        }
        var url = typeof input === 'string' ? input : (input && input.url) || '';
        var sameOrigin = url.indexOf('/') === 0 || url.indexOf(window.location.origin) === 0;
        if (!sameOrigin) {
            return origFetch(input, init);
        }
        var headers = init.headers || {};
        var isHeaders = typeof Headers !== 'undefined' && headers instanceof Headers;
        var next;
        if (isHeaders) {
            next = headers;
        } else {
            next = {};
            for (var k in headers) {
                if (Object.prototype.hasOwnProperty.call(headers, k)) {
                    next[k] = headers[k];
                }
            }
        }
        if (isHeaders) {
            next.set(csrfHeader, csrfToken);
        } else {
            next[csrfHeader] = csrfToken;
        }
        init.headers = next;
        return origFetch(input, init);
    };
})();
