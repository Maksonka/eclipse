(function () {
    'use strict';

    var modal = document.getElementById('favorites-modal');
    var modalClose = document.getElementById('favorites-close');
    var favoritesList = document.getElementById('favorites-list');
    var favoritesToggle = document.getElementById('favorites-toggle');
    var favoritesCount = document.getElementById('favorites-count');

    var favIds = new Set();
    var favType = null;
    var favRef = null;

    function escapeHtml(text) {
        if (text == null) {
            return '';
        }
        return String(text)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;');
    }

    function currentChat() {
        if (typeof activeGroupId === 'number' && activeGroupId) {
            return { type: 'GROUP', groupId: activeGroupId };
        }
        if (typeof activeChatUsername === 'string' && activeChatUsername) {
            return { type: 'DIRECT', partner: activeChatUsername };
        }
        return null;
    }

    function messagesContainer() {
        return document.getElementById('messages');
    }

    function isFavorited(messageId) {
        return messageId != null && favIds.has(String(messageId));
    }

    function createStarBadge() {
        var star = document.createElement('span');
        star.className = 'favorite-badge';
        star.title = 'В избранном';
        star.setAttribute('aria-label', 'В избранном');
        star.setAttribute('aria-hidden', 'true');
        star.textContent = '★';
        return star;
    }

    function markRow(rowEl, messageId) {
        if (!rowEl) {
            return;
        }
        var bubble = rowEl.querySelector('.message-bubble') || rowEl;
        if (isFavorited(messageId)) {
            if (!bubble.querySelector('.favorite-badge')) {
                bubble.appendChild(createStarBadge());
            }
        } else {
            var star = bubble.querySelector('.favorite-badge');
            if (star) {
                star.remove();
            }
        }
    }

    function refreshStars() {
        var container = messagesContainer();
        if (!container) {
            return;
        }
        container.querySelectorAll('.message-row[data-message-id]').forEach(function (row) {
            markRow(row, row.getAttribute('data-message-id'));
        });
    }

    function loadChatStars() {
        var ctx = currentChat();
        if (!ctx) {
            return;
        }
        var params = 'type=' + encodeURIComponent(ctx.type);
        if (ctx.type === 'GROUP') {
            params += '&groupId=' + encodeURIComponent(String(ctx.groupId));
        } else {
            params += '&partner=' + encodeURIComponent(ctx.partner);
        }
        fetch('/api/favorites/ids?' + params, {
            headers: { 'Accept': 'application/json' }
        })
            .then(function (r) {
                return r.json().then(function (d) {
                    return { ok: r.ok, data: d };
                });
            })
            .then(function (res) {
                if (res.ok && res.data && Array.isArray(res.data.ids)) {
                    favIds = new Set(res.data.ids.map(String));
                    refreshStars();
                }
            })
            .catch(function () {});
    }

    function updateCountBadge(count) {
        if (!favoritesCount) {
            return;
        }
        if (count > 0) {
            favoritesCount.hidden = false;
            favoritesCount.textContent = count > 99 ? '99+' : String(count);
        } else {
            favoritesCount.hidden = true;
        }
    }

    function loadCount() {
        fetch('/api/favorites/count', {
            headers: { 'Accept': 'application/json' }
        })
            .then(function (r) {
                return r.json().then(function (d) {
                    return { ok: r.ok, data: d };
                });
            })
            .then(function (res) {
                if (res.ok && res.data && typeof res.data.count === 'number') {
                    updateCountBadge(res.data.count);
                }
            })
            .catch(function () {});
    }

    function createAvatar(item) {
        var wrap = document.createElement('span');
        wrap.className = item.type === 'GROUP' ? 'favorites-item-avatar group' : 'favorites-item-avatar';
        if (item.chatAvatarFilename) {
            var img = document.createElement('img');
            img.className = 'favorites-item-avatar-img';
            img.src = (item.type === 'GROUP' ? '/uploads/group-avatars/' : '/uploads/avatars/') +
                encodeURIComponent(item.chatAvatarFilename);
            img.alt = item.chatTitle || '';
            wrap.appendChild(img);
        } else if (item.type === 'GROUP') {
            wrap.textContent = '👥';
        } else {
            var letter = document.createElement('span');
            letter.className = 'favorites-item-avatar-letter';
            letter.textContent = item.chatTitle ? item.chatTitle.charAt(0).toUpperCase() : '?';
            wrap.appendChild(letter);
        }
        return wrap;
    }

    function renderList(favorites) {
        if (!favoritesList) {
            return;
        }
        favoritesList.innerHTML = '';
        if (!favorites || !favorites.length) {
            var empty = document.createElement('div');
            empty.className = 'favorites-empty';
            empty.textContent = 'Пока нет избранных сообщений.';
            favoritesList.appendChild(empty);
            return;
        }
        favorites.forEach(function (item) {
            var row = document.createElement('a');
            row.className = 'favorites-item';
            row.href = item.chatHref + '#msg-' + item.messageId;
            row.setAttribute('data-message-id', item.messageId);
            row.setAttribute('data-type', item.type);

            row.appendChild(createAvatar(item));

            var body = document.createElement('span');
            body.className = 'favorites-item-body';

            var top = document.createElement('span');
            top.className = 'favorites-item-top';
            var chat = document.createElement('span');
            chat.className = 'favorites-item-chat';
            chat.textContent = item.chatTitle || '';
            var time = document.createElement('time');
            time.className = 'favorites-item-time';
            time.textContent = item.favoritedAt || '';
            top.appendChild(chat);
            top.appendChild(time);
            body.appendChild(top);

            var sender = document.createElement('span');
            sender.className = 'favorites-item-sender';
            sender.textContent = item.senderUsername || '';
            body.appendChild(sender);

            var preview = document.createElement('span');
            preview.className = 'favorites-item-preview';
            if (item.attachmentType === 'image' && item.attachmentFilename) {
                var thumb = document.createElement('img');
                thumb.className = 'favorites-item-thumb';
                thumb.src = '/uploads/messages/' + encodeURIComponent(item.attachmentFilename);
                thumb.alt = item.attachmentOriginalName || 'Фото';
                thumb.addEventListener('click', function (e) {
                    e.preventDefault();
                    e.stopPropagation();
                    var lb = document.getElementById('lightbox');
                    if (lb) {
                        var content = lb.querySelector('.lightbox-content');
                        content.innerHTML = '<img src="' + this.src + '" alt="Фото" class="lightbox-img"/>';
                        lb.classList.add('is-open');
                        document.body.style.overflow = 'hidden';
                    }
                });
                preview.appendChild(thumb);
                var cap = document.createElement('span');
                cap.textContent = item.preview || '';
                preview.appendChild(cap);
            } else {
                preview.textContent = item.preview || '';
            }
            body.appendChild(preview);

            row.appendChild(body);

            var unstar = document.createElement('button');
            unstar.type = 'button';
            unstar.className = 'favorites-item-unstar';
            unstar.title = 'Убрать из избранного';
            unstar.setAttribute('aria-label', 'Убрать из избранного');
            unstar.textContent = '★';
            row.appendChild(unstar);

            favoritesList.appendChild(row);
        });
    }

    function loadList() {
        if (!favoritesList) {
            return;
        }
        favoritesList.innerHTML = '<div class="favorites-loading">Загрузка...</div>';
        fetch('/api/favorites', {
            headers: { 'Accept': 'application/json' }
        })
            .then(function (r) {
                return r.json().then(function (d) {
                    return { ok: r.ok, data: d };
                });
            })
            .then(function (res) {
                if (!res.ok || !res.data) {
                    renderList(null);
                    return;
                }
                renderList(res.data.favorites);
                updateCountBadge(res.data.count);
            })
            .catch(function () {
                renderList(null);
            });
    }

    function openModal() {
        if (!modal) {
            return;
        }
        modal.hidden = false;
        document.body.classList.add('favorites-modal-open');
        loadList();
    }

    function closeModal() {
        if (!modal) {
            return;
        }
        modal.hidden = true;
        document.body.classList.remove('favorites-modal-open');
    }

    function handleFavoriteEvent(event) {
        if (!event) {
            return;
        }
        if (event.error) {
            if (window.showAlert) {
                window.showAlert(event.error);
            } else {
                alert(event.error);
            }
            return;
        }
        if (event.messageId == null || typeof event.favorited !== 'boolean') {
            return;
        }
        var key = String(event.messageId);
        if (event.favorited) {
            favIds.add(key);
        } else {
            favIds.delete(key);
        }
        refreshStars();
        if (modal && !modal.hidden) {
            loadList();
        } else {
            loadCount();
        }
    }

    var activeStomp = null;

    function sendToggle(messageId, type) {
        if (!activeStomp || !activeStomp.connected || messageId == null) {
            return;
        }
        var destination = type === 'GROUP' ? '/app/group.favorite' : '/app/chat.favorite';
        activeStomp.send(destination, {}, JSON.stringify({ messageId: Number(messageId) }));
    }

    function ensureSubscribed(client) {
        if (!client || !client.connected) {
            return false;
        }
        if (activeStomp === client) {
            return true;
        }
        activeStomp = client;
        client.subscribe('/user/queue/favorites', function (payload) {
            try {
                handleFavoriteEvent(JSON.parse(payload.body));
            } catch (e) {}
        });
        loadChatStars();
        loadCount();
        return true;
    }

    function connectSocket() {
        var shared = (typeof stompClient !== 'undefined') ? stompClient : null;
        if (shared) {
            var tries = 0;
            var poll = window.setInterval(function () {
                tries++;
                if (ensureSubscribed(shared) || tries >= 40) {
                    window.clearInterval(poll);
                }
            }, 500);
            return;
        }
        var socket = new SockJS('/ws');
        var own = Stomp.over(socket);
        own.debug = null;
        own.connect({}, function () {
            ensureSubscribed(own);
        }, function () {
            loadChatStars();
            loadCount();
        });
    }

    if (favoritesToggle) {
        favoritesToggle.addEventListener('click', function () {
            if (modal) {
                modal.hidden ? openModal() : closeModal();
            }
        });
    }
    if (modalClose) {
        modalClose.addEventListener('click', closeModal);
    }
    if (modal) {
        modal.addEventListener('click', function (e) {
            if (e.target === modal) {
                closeModal();
            }
        });
    }
    if (favoritesList) {
        favoritesList.addEventListener('click', function (e) {
            var unstar = e.target.closest('.favorites-item-unstar');
            if (!unstar) {
                return;
            }
            e.preventDefault();
            e.stopPropagation();
            var row = unstar.closest('.favorites-item');
            if (!row) {
                return;
            }
            var messageId = row.getAttribute('data-message-id');
            var type = row.getAttribute('data-type');
            if (messageId) {
                sendToggle(messageId, type || 'DIRECT');
            }
        });
    }
    document.addEventListener('keydown', function (e) {
        if (e.key === 'Escape' && modal && !modal.hidden) {
            closeModal();
        }
    });

    if (typeof Stomp !== 'undefined' && typeof SockJS !== 'undefined') {
        connectSocket();
    }

    window.Favorites = {
        isFavorited: isFavorited,
        markRow: markRow,
        refreshStars: refreshStars,
        toggle: sendToggle,
        open: openModal,
        close: closeModal
    };
})();
