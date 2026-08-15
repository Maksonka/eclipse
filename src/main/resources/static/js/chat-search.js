(function () {
    'use strict';

    var searchInput = document.getElementById('sidebar-search-input');
    var searchModeInput = document.getElementById('sidebar-search-mode');
    var searchForm = document.getElementById('sidebar-search-form');
    var modeButtons = document.querySelectorAll('.sidebar-search-mode');

    function updateSearchPlaceholder() {
        if (!searchInput) {
            return;
        }
        var mode = searchModeInput ? searchModeInput.value : 'users';
        searchInput.placeholder = mode === 'messages'
            ? 'Поиск по сообщениям...'
            : 'Поиск по нику...';
    }

    function setSidebarMode(mode) {
        if (searchModeInput) {
            searchModeInput.value = mode;
        }
        modeButtons.forEach(function (btn) {
            var active = btn.getAttribute('data-search-mode') === mode;
            btn.classList.toggle('is-active', active);
        });
        updateSearchPlaceholder();
    }

    modeButtons.forEach(function (btn) {
        btn.addEventListener('click', function () {
            var mode = btn.getAttribute('data-search-mode');
            if (!searchModeInput || searchModeInput.value === mode) {
                return;
            }
            setSidebarMode(mode);
            if (searchForm && searchInput) {
                searchForm.submit();
            }
        });
    });

    if (searchInput) {
        searchInput.addEventListener('input', updateSearchPlaceholder);
    }
    updateSearchPlaceholder();

    var messagesContainer = document.getElementById('messages');
    var modal = document.getElementById('chat-search-modal');
    var modalForm = document.getElementById('chat-search-form');
    var modalInput = document.getElementById('chat-search-input');
    var modalResults = document.getElementById('chat-search-results');
    var modalClose = document.getElementById('chat-search-close');
    var searchButton = document.getElementById('chat-search-button');

    var HIGHLIGHT_DURATION = 2600;
    var highlightTimeout = null;

    function getChatContext() {
        if (typeof activeGroupId === 'number' && activeGroupId) {
            return { type: 'group', groupId: activeGroupId };
        }
        if (typeof activeChatUsername === 'string' && activeChatUsername) {
            return { type: 'direct', partner: activeChatUsername };
        }
        return null;
    }

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

    function highlightMessage(messageId) {
        if (!messagesContainer || !messageId) {
            return;
        }
        var row = messagesContainer.querySelector('.message-row[data-message-id="' + messageId + '"]');
        if (!row) {
            return;
        }
        row.classList.add('is-highlighted');
        if (highlightTimeout) {
            clearTimeout(highlightTimeout);
        }
        highlightTimeout = setTimeout(function () {
            row.classList.remove('is-highlighted');
        }, HIGHLIGHT_DURATION);
    }

    function scrollToMessageInContainer(messageId) {
        if (!messagesContainer || !messageId) {
            return false;
        }
        var target = messagesContainer.querySelector('[data-message-id="' + messageId + '"]');
        if (!target) {
            return false;
        }
        target.scrollIntoView({ behavior: 'smooth', block: 'center' });
        highlightMessage(messageId);
        return true;
    }

    function renderContextMessages(messages) {
        if (!messagesContainer) {
            return;
        }
        messagesContainer.innerHTML = '';
        var builder = typeof buildMessageRow === 'function'
            ? buildMessageRow
            : (typeof buildGroupMessageRow === 'function' ? buildGroupMessageRow : null);
        if (!builder) {
            return;
        }
        if (typeof displayedMessageIds !== 'undefined') {
            displayedMessageIds.clear();
        }
        messages.forEach(function (m) {
            var row = builder(m);
            if (row) {
                messagesContainer.appendChild(row);
            }
        });
        if (typeof initLightbox === 'function') {
            initLightbox();
        }
        if (typeof ReactionsUI !== 'undefined' && ReactionsUI && messagesContainer) {
            messagesContainer.querySelectorAll('.message-row').forEach(function (row) {
                var id = row.getAttribute('data-message-id');
                if (id && initialReactions && initialReactions[String(id)]) {
                    ReactionsUI.renderBar(row, id, initialReactions[String(id)], currentUsername, function (emoji) {
                        if (typeof sendGroupReaction === 'function') {
                            sendGroupReaction(id, emoji);
                        } else if (typeof sendReaction === 'function') {
                            sendReaction(id, emoji);
                        }
                    });
                }
            });
        }
    }

    function loadContext(messageId, callback) {
        var ctx = getChatContext();
        if (!ctx) {
            return;
        }
        var params = 'type=' + encodeURIComponent(ctx.type) +
            '&id=' + encodeURIComponent(String(messageId)) +
            '&size=60';
        if (ctx.type === 'group') {
            params += '&groupId=' + encodeURIComponent(String(ctx.groupId));
        } else {
            params += '&partner=' + encodeURIComponent(ctx.partner);
        }

        fetch('/api/search/messages/' + encodeURIComponent(String(messageId)) + '/context?' + params, {
            headers: { 'Accept': 'application/json' }
        })
            .then(function (r) {
                if (!r.ok) {
                    throw new Error('HTTP ' + r.status);
                }
                return r.json();
            })
            .then(function (data) {
                if (data && Array.isArray(data.messages)) {
                    renderContextMessages(data.messages);
                    requestAnimationFrame(function () {
                        if (!scrollToMessageInContainer(messageId)) {
                            if (messagesContainer) {
                                messagesContainer.scrollTop = messagesContainer.scrollHeight;
                            }
                        }
                    });
                    if (callback) {
                        callback(true);
                    }
                } else if (callback) {
                    callback(false);
                }
            })
            .catch(function () {
                if (callback) {
                    callback(false);
                }
            });
    }

    function jumpToMessage(messageId) {
        if (scrollToMessageInContainer(messageId)) {
            return;
        }
        loadContext(messageId);
    }

    if (window.location.hash && /^#msg-\d+$/.test(window.location.hash)) {
        var hashId = window.location.hash.substring(4);
        setTimeout(function () {
            jumpToMessage(hashId);
            if (history.replaceState) {
                history.replaceState(null, '', window.location.pathname + window.location.search);
            }
        }, 60);
    }

    function openModal() {
        if (!modal || !modalInput) {
            return;
        }
        modal.hidden = false;
        document.body.classList.add('chat-search-modal-open');
        setTimeout(function () {
            modalInput.focus();
        }, 30);
    }

    function closeModal() {
        if (!modal) {
            return;
        }
        modal.hidden = true;
        document.body.classList.remove('chat-search-modal-open');
    }

    if (searchButton) {
        searchButton.addEventListener('click', function () {
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
    document.addEventListener('click', function (e) {
        var link = e.target.closest('.message-search-result a.conversation-body');
        if (!link) {
            return;
        }
        var item = link.closest('.message-search-result');
        if (!item) {
            return;
        }
        var msgId = item.getAttribute('data-msg-id');
        if (!msgId) {
            return;
        }
        var type = item.getAttribute('data-result-type');
        if (type === 'DIRECT') {
            var partner = item.getAttribute('data-partner');
            if (partner && typeof activeChatUsername === 'string' && activeChatUsername === partner) {
                e.preventDefault();
                jumpToMessage(msgId);
                return;
            }
        }
        link.href = link.href.split('#')[0] + '#msg-' + msgId;
    });

    document.addEventListener('keydown', function (e) {
        if (e.key === 'Escape' && modal && !modal.hidden) {
            closeModal();
        }
    });

    function renderResults(results) {
        if (!modalResults) {
            return;
        }
        if (!results || !results.length) {
            modalResults.innerHTML = '<div class="chat-search-empty">Ничего не найдено</div>';
            return;
        }
        var list = document.createElement('div');
        list.className = 'chat-search-result-list';
        results.forEach(function (r) {
            var item = document.createElement('button');
            item.type = 'button';
            item.className = 'chat-search-result-item';
            item.setAttribute('data-msg-id', r.messageId);
            item.setAttribute('data-type', r.type);

            var name = document.createElement('span');
            name.className = 'chat-search-result-name';
            name.textContent = r.senderUsername + (r.type === 'GROUP' ? ' → ' + r.groupName : '');

            var date = document.createElement('time');
            date.className = 'chat-search-result-date';
            date.textContent = r.date || '';

            var top = document.createElement('div');
            top.className = 'chat-search-result-top';
            top.appendChild(name);
            top.appendChild(date);

            var content = document.createElement('span');
            content.className = 'chat-search-result-content';
            content.textContent = r.content || '';

            item.appendChild(top);
            item.appendChild(content);
            list.appendChild(item);
        });
        modalResults.innerHTML = '';
        modalResults.appendChild(list);
    }

    if (modalForm) {
        modalForm.addEventListener('submit', function (e) {
            e.preventDefault();
            var query = (modalInput.value || '').trim();
            if (!query) {
                return;
            }
            var ctx = getChatContext();
            if (!ctx) {
                modalResults.innerHTML = '<div class="chat-search-empty">Сначала откройте чат</div>';
                return;
            }
            modalResults.innerHTML = '<div class="chat-search-loading">Поиск...</div>';
            var params = 'q=' + encodeURIComponent(query) + '&limit=50&scope=';
            if (ctx.type === 'group') {
                params += 'group&groupId=' + encodeURIComponent(String(ctx.groupId));
            } else {
                params += 'direct&partner=' + encodeURIComponent(ctx.partner);
            }
            fetch('/api/search/messages?' + params, {
                headers: { 'Accept': 'application/json' }
            })
                .then(function (r) {
                    return r.json().then(function (d) {
                        return { ok: r.ok, data: d };
                    });
                })
                .then(function (res) {
                    if (!res.ok || !res.data || !res.data.results) {
                        modalResults.innerHTML = '<div class="chat-search-empty">Ошибка поиска</div>';
                        return;
                    }
                    renderResults(res.data.results);
                })
                .catch(function () {
                    modalResults.innerHTML = '<div class="chat-search-empty">Ошибка сети</div>';
                });
        });
    }

    if (modalResults) {
        modalResults.addEventListener('click', function (e) {
            var item = e.target.closest('.chat-search-result-item');
            if (!item) {
                return;
            }
            var messageId = item.getAttribute('data-msg-id');
            if (!messageId) {
                return;
            }
            closeModal();
            jumpToMessage(messageId);
        });
    }

    window.MessageSearchUI = {
        jumpToMessage: jumpToMessage,
        openModal: openModal,
        closeModal: closeModal
    };
})();
