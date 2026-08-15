(function () {
    'use strict';

    var direct = new Set();
    var groups = new Set();

    if (typeof mutedDirectPartners !== 'undefined') {
        (mutedDirectPartners || []).forEach(function (u) {
            direct.add(u);
        });
    }
    if (typeof mutedGroupIds !== 'undefined') {
        (mutedGroupIds || []).forEach(function (g) {
            groups.add(String(g));
        });
    }

    function saveDirect(username, muted) {
        if (muted) {
            direct.add(username);
        } else {
            direct.delete(username);
        }
    }

    function saveGroup(groupId, muted) {
        var key = String(groupId);
        if (muted) {
            groups.add(key);
        } else {
            groups.delete(key);
        }
    }

    function api(url, body) {
        return fetch(url, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(body)
        }).then(function (response) {
            if (!response.ok) {
                throw new Error('HTTP ' + response.status);
            }
            return response.json();
        });
    }

    function refreshButton(btn, muted) {
        btn.classList.toggle('is-muted', muted);
        btn.setAttribute('aria-pressed', muted ? 'true' : 'false');
        var onLabel = btn.getAttribute('data-label-on') || 'Выключить уведомления';
        var offLabel = btn.getAttribute('data-label-off') || 'Включить уведомления';
        btn.setAttribute('aria-label', muted ? onLabel : offLabel);
        btn.setAttribute('title', muted ? onLabel : offLabel);
    }

    function syncSidebarMuted() {
        var items = document.querySelectorAll('.conversation-item');
        for (var i = 0; i < items.length; i++) {
            var el = items[i];
            var partner = el.getAttribute('data-partner');
            var gid = el.getAttribute('data-group-id');
            var isMuted = partner ? direct.has(partner) : (gid ? groups.has(String(gid)) : false);
            el.classList.toggle('is-muted', isMuted);
        }
    }

    function bindDirectButton(btn) {
        var partner = btn.getAttribute('data-partner');
        var state = btn.getAttribute('data-muted') === 'true';
        refreshButton(btn, state);
        btn.addEventListener('click', function () {
            var next = !state;
            api('/api/notifications/direct', { partnerUsername: partner, muted: next })
                .then(function (res) {
                    state = res.muted;
                    saveDirect(partner, state);
                    refreshButton(btn, state);
                    syncSidebarMuted();
                })
                .catch(function () {});
        });
    }

    function bindGroupButton(btn) {
        var gid = btn.getAttribute('data-group-id');
        var state = btn.getAttribute('data-muted') === 'true';
        refreshButton(btn, state);
        btn.addEventListener('click', function () {
            var next = !state;
            api('/api/notifications/group', { groupId: Number(gid), muted: next })
                .then(function (res) {
                    state = res.muted;
                    saveGroup(gid, state);
                    refreshButton(btn, state);
                    syncSidebarMuted();
                })
                .catch(function () {});
        });
    }

    function init() {
        var directBtn = document.getElementById('chat-mute-button');
        if (directBtn) {
            bindDirectButton(directBtn);
        }
        var groupBtn = document.getElementById('group-mute-button');
        if (groupBtn) {
            bindGroupButton(groupBtn);
        }
        syncSidebarMuted();
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }

    window.MuteManager = {
        isDirectMuted: function (username) {
            return direct.has(username);
        },
        isGroupMuted: function (groupId) {
            return groups.has(String(groupId));
        },
        setDirectMuted: function (username, muted) {
            return api('/api/notifications/direct', { partnerUsername: username, muted: muted })
                .then(function (res) {
                    saveDirect(username, res.muted);
                });
        },
        setGroupMuted: function (groupId, muted) {
            return api('/api/notifications/group', { groupId: groupId, muted: muted })
                .then(function (res) {
                    saveGroup(groupId, res.muted);
                });
        }
    };
})();
