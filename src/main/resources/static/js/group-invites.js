(function () {
    'use strict';

    var invites = [];
    var sectionEl = null;
    var listEl = null;
    var countEl = null;

    function escapeHtml(text) {
        if (text == null) return '';
        return String(text).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
    }

    function renderList() {
        if (!listEl) return;
        listEl.innerHTML = '';
        if (!invites.length) {
            if (sectionEl) sectionEl.hidden = true;
            return;
        }
        if (sectionEl) sectionEl.hidden = false;
        if (countEl) countEl.textContent = invites.length;
        invites.forEach(function (inv) {
            var item = document.createElement('div');
            item.className = 'invite-item';
            item.setAttribute('data-invite-id', inv.id);

            var info = document.createElement('div');
            info.className = 'invite-item-info';

            var title = document.createElement('span');
            title.className = 'invite-item-title';
            title.innerHTML = '<strong>' + escapeHtml(inv.invitedBy) + '</strong> хочет добавить вас в группу';

            var group = document.createElement('span');
            group.className = 'invite-item-group';
            group.textContent = inv.groupName;

            info.appendChild(title);
            info.appendChild(group);
            item.appendChild(info);

            var actions = document.createElement('div');
            actions.className = 'invite-item-actions';

            var accept = document.createElement('button');
            accept.type = 'button';
            accept.className = 'invite-btn accept';
            accept.textContent = 'Принять';
            accept.addEventListener('click', function () { respond(inv.id, 'accept', inv.groupId, inv.groupName); });

            var decline = document.createElement('button');
            decline.type = 'button';
            decline.className = 'invite-btn decline';
            decline.textContent = 'Отклонить';
            decline.addEventListener('click', function () { respond(inv.id, 'decline'); });

            actions.appendChild(accept);
            actions.appendChild(decline);
            item.appendChild(actions);
            listEl.appendChild(item);
        });
    }

    function respond(inviteId, action, groupId, groupName) {
        var inviteItem = document.querySelector('.invite-item[data-invite-id="' + inviteId + '"]');
        if (inviteItem) inviteItem.style.opacity = '0.4';

        fetch('/api/group-invites/' + inviteId + '/' + action, {
            method: 'POST',
            headers: { 'Accept': 'application/json' }
        })
            .then(function (r) {
                if (!r.ok) {
                    return r.json().then(function (d) { throw new Error(d.error || 'Ошибка'); }).catch(function () { throw new Error('Ошибка сервера'); });
                }
                return r.json();
            })
            .then(function () {
                cleanupAccepted(inviteId, groupId, groupName);
            })
            .catch(function () {
                if (inviteItem) inviteItem.style.opacity = '1';
            });
    }

    function cleanupAccepted(inviteId, groupId, groupName) {
        invites = invites.filter(function (i) { return i.id !== inviteId; });
        renderList();
        var item = document.querySelector('.invite-item[data-invite-id="' + inviteId + '"]');
        if (item) item.remove();
        var toast = document.querySelector('.invite-toast[data-invite-id="' + inviteId + '"]');
        if (toast) toast.remove();
        if (!invites.length && sectionEl) sectionEl.hidden = true;
        if (groupId) addGroupToSidebar(groupId, groupName);
    }

    function addGroupToSidebar(groupId, groupName) {
        var groupsList = document.getElementById('groups-list');
        if (!groupsList) return;
        if (groupsList.querySelector('[data-group-id="' + groupId + '"]')) return;

        var groupsSection = groupsList.closest('.sidebar-section');
        if (groupsSection) groupsSection.hidden = false;

        var item = document.createElement('div');
        item.className = 'conversation-item group-item';
        item.setAttribute('data-group-id', groupId);

        item.innerHTML =
            '<a class="conversation-avatar-link" href="/chat/group/' + groupId + '" title="Группа">' +
                '<div class="avatar group-avatar">' +
                    '<img src="/img/default-group.png" alt="" width="40" height="40"/>' +
                '</div>' +
            '</a>' +
            '<a class="conversation-body" href="/chat/group/' + groupId + '">' +
                '<div class="conversation-top">' +
                    '<span class="conversation-name">' + escapeHtml(groupName) + '</span>' +
                '</div>' +
                '<span class="conversation-preview">Вы вступили в группу</span>' +
            '</a>';

        groupsList.prepend(item);
    }

    function showToast(inv) {
        var existing = document.querySelector('.invite-toast[data-invite-id="' + inv.id + '"]');
        if (existing) return;

        var toast = document.createElement('div');
        toast.className = 'invite-toast';
        toast.setAttribute('data-invite-id', inv.id);

        toast.innerHTML =
            '<div class="invite-toast-body">' +
                '<div class="invite-toast-icon">👤</div>' +
                '<div class="invite-toast-info">' +
                    '<span class="invite-toast-title"><strong>' + escapeHtml(inv.invitedBy) + '</strong> хочет добавить вас в группу</span>' +
                    '<span class="invite-toast-group">' + escapeHtml(inv.groupName) + '</span>' +
                '</div>' +
            '</div>' +
            '<div class="invite-toast-actions">' +
                '<button type="button" class="invite-btn accept">Принять</button>' +
                '<button type="button" class="invite-btn decline">Отклонить</button>' +
            '</div>';

        toast.querySelector('.invite-btn.accept').addEventListener('click', function () {
            respond(inv.id, 'accept', inv.groupId, inv.groupName);
        });
        toast.querySelector('.invite-btn.decline').addEventListener('click', function () {
            respond(inv.id, 'decline');
        });

        document.body.appendChild(toast);
        requestAnimationFrame(function () {
            toast.classList.add('is-visible');
        });

        setTimeout(function () {
            if (toast.parentNode) {
                toast.classList.remove('is-visible');
                setTimeout(function () { if (toast.parentNode) toast.remove(); }, 300);
            }
        }, 10000);
    }

    function loadInvites() {
        sectionEl = document.getElementById('profile-invites-section');
        listEl = document.getElementById('profile-invites-list');
        countEl = document.getElementById('profile-invites-count');

        fetch('/api/group-invites', { headers: { 'Accept': 'application/json' } })
            .then(function (r) { return r.json(); })
            .then(function (data) {
                if (data.invites) {
                    invites = data.invites;
                    renderList();
                }
            })
            .catch(function () {});
    }

    function handleInviteEvent(data) {
        if (!data || data.type !== 'group_invite') return;
        if (data.action === 'created' && data.status === 'PENDING') {
            var exists = invites.some(function (i) { return i.id === data.inviteId; });
            if (!exists) {
                var inv = {
                    id: data.inviteId,
                    groupId: data.groupId,
                    groupName: data.groupName,
                    invitedBy: data.invitedBy,
                    invitedUser: data.invitedUser,
                    status: data.status
                };
                invites.unshift(inv);
                renderList();
                showToast(inv);
            }
        } else if (data.action === 'accepted' || data.action === 'declined') {
            invites = invites.filter(function (i) { return i.id !== data.inviteId; });
            renderList();
            var toast = document.querySelector('.invite-toast[data-invite-id="' + data.inviteId + '"]');
            if (toast) toast.remove();
        }
    }

    function ensureSubscribed(stomp) {
        if (!stomp || !stomp.connected) return;
        stomp.subscribe('/user/queue/group-invites', function (payload) {
            try { handleInviteEvent(JSON.parse(payload.body)); } catch (e) {}
        });
    }

    function init() {
        loadInvites();

        var shared = (typeof stompClient !== 'undefined') ? stompClient : null;
        if (shared) {
            var tries = 0;
            var poll = window.setInterval(function () {
                tries++;
                if (ensureSubscribed(shared) || tries >= 40) {
                    window.clearInterval(poll);
                }
            }, 500);
        }
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }

    window.GroupInvites = {
        load: loadInvites,
        handleEvent: handleInviteEvent
    };
})();
