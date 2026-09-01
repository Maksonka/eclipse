var messagesContainer = document.getElementById('messages');
var messageForm = document.getElementById('message-form');
var messageInput = document.getElementById('message-input');
var sendButton = document.getElementById('send-button');
var groupIdInput = document.getElementById('group-id');

var groupTypingIndicator = document.getElementById('group-typing-indicator');
var groupTypingLabel = document.getElementById('group-typing-label');
var groupTypingUsers = {};
var groupTypingTimers = {};
var groupTypingStopTimeout = null;
var groupIsSendingTyping = false;

function sendGroupTypingState(typing) {
    if (!stompClient || !stompClient.connected || !activeGroupId) {
        return;
    }
    stompClient.send('/app/group.typing', {}, JSON.stringify({
        groupId: activeGroupId,
        typing: typing
    }));
}

function notifyLocalGroupTyping() {
    if (!groupIsSendingTyping) {
        groupIsSendingTyping = true;
        sendGroupTypingState(true);
    }
    clearTimeout(groupTypingStopTimeout);
    groupTypingStopTimeout = setTimeout(stopLocalGroupTyping, 1500);
}

function stopLocalGroupTyping() {
    clearTimeout(groupTypingStopTimeout);
    if (groupIsSendingTyping) {
        groupIsSendingTyping = false;
        sendGroupTypingState(false);
    }
}

function updateGroupTypingIndicator() {
    if (!groupTypingIndicator) {
        return;
    }
    var names = Object.keys(groupTypingUsers);
    if (!names.length) {
        groupTypingIndicator.classList.remove('is-visible');
        return;
    }
    if (groupTypingLabel) {
        groupTypingLabel.textContent = names.length === 1
            ? names[0] + ' печатает…'
            : names[0] + ' и ещё ' + (names.length - 1) + ' печатают…';
    }
    groupTypingIndicator.classList.add('is-visible');
}

function handleGroupTyping(event) {
    if (!event || !event.senderUsername || event.senderUsername === currentUsername) {
        return;
    }
    var name = event.senderUsername;
    clearTimeout(groupTypingTimers[name]);
    if (event.typing) {
        groupTypingUsers[name] = true;
    } else {
        delete groupTypingUsers[name];
    }
    updateGroupTypingIndicator();
    if (event.typing) {
        groupTypingTimers[name] = setTimeout(function () {
            delete groupTypingUsers[name];
            updateGroupTypingIndicator();
        }, 2500);
    }
}

var displayedMessageIds = new Set();
if (messagesContainer) {
    messagesContainer.querySelectorAll('[data-message-id]').forEach(function (el) {
        displayedMessageIds.add(el.getAttribute('data-message-id'));
    });
}

function pinnedPreviewText(message) {
    if (!message) return '';
    if (message.stickerUrl) return 'Стикер';
    if (message.audioUrl) return 'Голосовое сообщение';
    if (message.content && message.content.trim()) return message.content;
    if (message.attachmentType) return messagePreview(message);
    return 'Сообщение';
}

function getPinnedBar() {
    var bar = document.getElementById('pinned-bar');
    if (bar) {
        return bar;
    }
    bar = document.createElement('div');
    bar.className = 'pinned-bar';
    bar.id = 'pinned-bar';

    var icon = document.createElement('span');
    icon.className = 'pinned-bar-icon';
    icon.setAttribute('aria-hidden', 'true');
    icon.textContent = '📌';

    var body = document.createElement('div');
    body.className = 'pinned-bar-body';
    var title = document.createElement('span');
    title.className = 'pinned-bar-title';
    title.textContent = 'Закреплённые сообщения';
    var list = document.createElement('div');
    list.className = 'pinned-list';
    list.id = 'pinned-list';
    body.appendChild(title);
    body.appendChild(list);

    bar.appendChild(icon);
    bar.appendChild(body);

    var messages = document.getElementById('messages');
    if (messages && messages.parentNode) {
        messages.parentNode.insertBefore(bar, messages);
    }
    return bar;
}

var pinnedItems = [];
var pinnedIndex = 0;

function findPinnedIndexById(messageId) {
    for (var i = 0; i < pinnedItems.length; i++) {
        if (pinnedItems[i].getAttribute('data-pinned-id') === String(messageId)) {
            return i;
        }
    }
    return -1;
}

function syncPinnedDisplay() {
    var bar = document.getElementById('pinned-bar');
    if (pinnedItems.length === 0) {
        if (bar) {
            bar.remove();
        }
        return;
    }
    if (pinnedIndex >= pinnedItems.length) {
        pinnedIndex = 0;
    }
    pinnedItems.forEach(function (el, i) {
        if (i === pinnedIndex) {
            el.classList.add('is-active');
        } else {
            el.classList.remove('is-active');
        }
    });
    var title = bar.querySelector('.pinned-bar-title');
    if (title) {
        title.textContent = 'Закреплённые: ' + (pinnedIndex + 1) + ' из ' + pinnedItems.length;
    }
}

function createPinnedItem(message) {
    var item = document.createElement('div');
    item.className = 'pinned-item';
    item.setAttribute('data-pinned-id', message.id);

    var sender = document.createElement('span');
    sender.className = 'pinned-bar-sender';
    sender.textContent = message.senderUsername;

    var preview = document.createElement('span');
    preview.className = 'pinned-bar-preview';
    preview.textContent = truncatePreview(pinnedPreviewText(message));

    var close = document.createElement('button');
    close.type = 'button';
    close.className = 'pinned-item-close';
    close.setAttribute('aria-label', 'Открепить сообщение');
    close.title = 'Открепить';
    close.innerHTML = '<svg width="12" height="12" viewBox="0 0 24 24" fill="none" aria-hidden="true"><path d="M6 6l12 12M18 6L6 18" stroke="currentColor" stroke-width="2.4" stroke-linecap="round"/></svg>';

    item.appendChild(sender);
    item.appendChild(preview);
    item.appendChild(close);
    return item;
}

function initPinnedBar() {
    var bar = document.getElementById('pinned-bar');
    if (!bar) {
        return;
    }
    var list = bar.querySelector('.pinned-list');
    pinnedItems = list ? Array.prototype.slice.call(list.querySelectorAll('.pinned-item')) : [];
    pinnedIndex = 0;
    syncPinnedDisplay();
}

function addPinnedItem(message) {
    var idx = findPinnedIndexById(message.id);
    if (idx >= 0) {
        var old = pinnedItems[idx];
        if (old.parentNode) {
            old.parentNode.removeChild(old);
        }
        pinnedItems.splice(idx, 1);
    }
    var bar = getPinnedBar();
    var list = bar.querySelector('.pinned-list');
    var el = createPinnedItem(message);
    pinnedItems.unshift(el);
    list.insertBefore(el, list.firstChild);
    pinnedIndex = 0;
    syncPinnedDisplay();
}

function removePinnedItem(messageId) {
    var idx = findPinnedIndexById(messageId);
    if (idx < 0) {
        return;
    }
    var el = pinnedItems[idx];
    if (el.parentNode) {
        el.parentNode.removeChild(el);
    }
    pinnedItems.splice(idx, 1);
    if (idx < pinnedIndex) {
        pinnedIndex--;
    }
    if (pinnedIndex >= pinnedItems.length) {
        pinnedIndex = Math.max(0, pinnedItems.length - 1);
    }
    syncPinnedDisplay();
}

function renderPinnedBar(message) {
    if (!messagesContainer || message.groupId !== activeGroupId) {
        return;
    }
    if (message.pinned) {
        addPinnedItem(message);
    } else {
        removePinnedItem(message.id);
    }
}

function unpinMessage(messageId) {
    if (!messageId || !stompClient || !stompClient.connected || !activeGroupId) {
        return;
    }
    stompClient.send('/app/group.pin', {}, JSON.stringify({ messageId: Number(messageId), groupId: Number(activeGroupId), pinned: false }));
}

function handlePinUpdate(message) {
    if (!messagesContainer || message.groupId !== activeGroupId) {
        return;
    }
    var isDeletedByMe = message.deletedByUserIds && message.deletedByUserIds.indexOf && message.deletedByUserIds.indexOf(currentUserId) !== -1;
    var isDeleted = isDeletedByMe || message.content === 'Сообщение удалено';
    var row = messagesContainer.querySelector('[data-message-id="' + message.id + '"]');
    if (row) {
        if (isDeleted) {
            row.remove();
        } else {
            row.replaceWith(buildGroupMessageRow(message));
        }
    }
    renderPinnedBar(message);
}

function hideEmptyState() {
    var emptyState = messagesContainer && messagesContainer.querySelector('.chat-empty');
    if (emptyState) {
        emptyState.remove();
    }
}

function scrollToBottom() {
    if (messagesContainer) {
        messagesContainer.scrollTop = messagesContainer.scrollHeight;
    }
}

function jumpToBottom() {
    if (!messagesContainer) {
        return;
    }
    var prev = messagesContainer.style.scrollBehavior;
    messagesContainer.style.scrollBehavior = 'auto';
    messagesContainer.scrollTop = messagesContainer.scrollHeight;
    messagesContainer.style.scrollBehavior = prev;
}

function scrollToMessage(messageId) {
    var target = messagesContainer.querySelector('[data-message-id="' + messageId + '"]');
    if (target) {
        target.scrollIntoView({ behavior: 'smooth', block: 'center' });
    } else {
        scrollToBottom();
    }
}

function focusReplyMessage() {
    if (replyState && replyState.messageId) {
        scrollToMessage(replyState.messageId);
    }
}

function buildGroupMessageRow(message) {
    var isOutgoing = message.senderUsername === currentUsername;
    var rowEl = document.createElement('div');
    rowEl.className = 'message-row ' + (isOutgoing ? 'outgoing' : 'incoming');
    if (message.id) {
        rowEl.setAttribute('data-message-id', message.id);
    }

    var bubbleEl = document.createElement('div');
    bubbleEl.className = 'message-bubble';
    if (isOutgoing) {
        bubbleEl.setAttribute('data-sender', currentUsername);
    }
    if (message.stickerUrl && !message.content) {
        bubbleEl.classList.add('bubble-none');
    }

    if (!isOutgoing) {
        var senderEl = document.createElement('a');
        senderEl.className = 'sender sender-link';
        senderEl.href = '/profile/' + encodeURIComponent(message.senderUsername);
        senderEl.textContent = message.senderUsername;
        senderEl.addEventListener('click', function (e) {
            e.stopPropagation();
        });
        bubbleEl.appendChild(senderEl);
    }

    var replyBlock = createReplyBlock(message);
    if (replyBlock) {
        bubbleEl.appendChild(replyBlock);
    }

    if (message.forwardedFrom) {
        var fwdEl = document.createElement('div');
        fwdEl.className = 'forward-badge';
        fwdEl.textContent = 'Переслано от ' + message.forwardedFrom;
        bubbleEl.appendChild(fwdEl);
    }

    var attachmentEl = createAttachmentElement(message);
    if (attachmentEl) {
        bubbleEl.appendChild(attachmentEl);
    }

    if (message.audioUrl) {
        try {
            var voiceEl = window.VoicePlayer
                ? VoicePlayer.create(message.audioUrl, message.audioDurationMs)
                : (function () {
                    var a = document.createElement('audio');
                    a.src = message.audioUrl;
                    a.controls = true;
                    a.preload = 'metadata';
                    return a;
                })();
            if (voiceEl) {
                bubbleEl.appendChild(voiceEl);
            }
        } catch (e) {}
    }

    if (message.audioUrl && message.transcript) {
        var transcriptEl = document.createElement('div');
        transcriptEl.className = 'voice-transcript';
        transcriptEl.textContent = message.transcript;
        bubbleEl.appendChild(transcriptEl);
    } else if (message.audioUrl) {
        var trBtn = document.createElement('button');
        trBtn.type = 'button';
        trBtn.className = 'voice-transcribe-btn';
        trBtn.setAttribute('data-action', 'transcribe-row');
        trBtn.textContent = 'Расшифровать';
        bubbleEl.appendChild(trBtn);
    }

    if (message.stickerUrl) {
        var stickerEl = window.StickerUI
            ? StickerUI.createStickerImage(message.stickerUrl, message.stickerCode, isOutgoing)
            : (function () {
                var img = document.createElement('img');
                img.className = 'sticker-image';
                img.src = message.stickerUrl;
                img.alt = 'Стикер';
                if (message.stickerCode) {
                    img.setAttribute('data-sticker-code', message.stickerCode);
                }
                if (isOutgoing) {
                    img.setAttribute('data-outgoing', '1');
                }
                return img;
            })();
        bubbleEl.appendChild(stickerEl);
    }

    if (message.content) {
        var contentEl = document.createElement('span');
        contentEl.className = 'content';
        contentEl.innerHTML = linkifyText(message.content);
        bubbleEl.appendChild(contentEl);
    }

    var metaEl = document.createElement('span');
    metaEl.className = 'message-meta';
    var timeEl = document.createElement('time');
    timeEl.className = 'message-time';
    timeEl.textContent = message.timestamp || '';
    metaEl.appendChild(timeEl);

    if (message.edited) {
        var editedEl = document.createElement('span');
        editedEl.className = 'message-edited';
        editedEl.textContent = 'изменено';
        metaEl.appendChild(editedEl);
    }

    if (message.pinned) {
        var pinEl = document.createElement('span');
        pinEl.className = 'pin-badge';
        pinEl.title = 'Закреплённое сообщение';
        pinEl.setAttribute('aria-label', 'Закреплённое сообщение');
        pinEl.textContent = '📌';
        bubbleEl.appendChild(pinEl);
    }

    bubbleEl.appendChild(metaEl);
    rowEl.appendChild(bubbleEl);
    if (window.ReactionsUI) {
        ReactionsUI.renderBar(rowEl, message.id, message.reactions || (initialReactions && initialReactions[String(message.id)]), currentUsername, function (emoji) {
            sendGroupReaction(message.id, emoji);
        });
    }
    if (window.Favorites) {
        window.Favorites.markRow(rowEl, message.id);
    }
    return rowEl;
}

function bumpGroup(message) {
    if (!message || message.groupId == null) {
        return;
    }
    var groupsList = document.getElementById('groups-list');
    var item = groupsList ? groupsList.querySelector('[data-group-id="' + message.groupId + '"]') : null;
    if (!item) {
        return;
    }

    var timeEl = item.querySelector('.conversation-time');
    if (timeEl) {
        timeEl.textContent = message.timestamp || '';
    }

    var previewEl = item.querySelector('.conversation-preview');
    if (previewEl) {
        previewEl.textContent = '';
        var isMine = message.senderUsername === currentUsername;
        if (isMine) {
            previewEl.appendChild(document.createTextNode('Вы: '));
        } else {
            var name = message.senderUsername;
            if (name) {
                previewEl.appendChild(document.createTextNode(name + ': '));
            }
        }
        previewEl.appendChild(document.createTextNode(messagePreview(message)));
    }

    var incomingUnread = !message.senderUsername || message.senderUsername !== currentUsername;
    if (incomingUnread && groupsList) {
        var gid = item.getAttribute('data-group-id');
        var isActiveHere = String(activeGroupId) === String(gid);
        var badge = item.querySelector('.unread-badge');
        if (badge) {
            var n = (parseInt(badge.textContent, 10) || 0) + 1;
            badge.textContent = n > 99 ? '99+' : String(n);
        } else if (!isActiveHere || document.hidden) {
            var b = document.createElement('span');
            b.className = 'unread-badge';
            b.textContent = '1';
            item.appendChild(b);
        }
    }

    if (groupsList) {
        groupsList.prepend(item);
    }
}

function messagePreview(message) {
    if (!message) return '';
    if (message.content) return message.content;
    if (message.stickerUrl) return 'Стикер';
    if (message.audioUrl) return 'Голосовое сообщение';
    if (message.attachmentFilename) return '📎 Вложение';
    return '';
}

function transcribeGroupVoiceMessage(messageId) {
    if (!currentUserPremium) {
        if (window.location) window.location.href = '/premium';
        return;
    }
    var groupId = groupIdInput ? parseInt(groupIdInput.value, 10) : activeGroupId;
    var row = messagesContainer ? messagesContainer.querySelector('[data-message-id="' + messageId + '"]') : null;
    var btn = row ? row.querySelector('.voice-transcribe-btn') : null;
    if (btn) {
        btn.disabled = true;
        btn.textContent = 'Расшифровка…';
    }
    if (!stompClient || !stompClient.connected) {
        if (btn) {
            btn.disabled = false;
            btn.textContent = 'Расшифровать';
        }
        showComposerError('Нет соединения');
        return;
    }
    stompClient.send('/app/group.transcribe', {}, JSON.stringify({ messageId: Number(messageId), groupId: groupId }));
}var groupChatErrorToastTimer = null;
function handleGroupChatError(data) {
    var message = data && data.error ? data.error : 'Не удалось выполнить операцию';
    var toast = document.getElementById('chat-error-toast');
    if (!toast) {
        toast = document.createElement('div');
        toast.id = 'chat-error-toast';
        toast.className = 'chat-error-toast';
        document.body.appendChild(toast);
    }
    toast.textContent = message;
    toast.classList.add('visible');
    if (groupChatErrorToastTimer) {
        clearTimeout(groupChatErrorToastTimer);
    }
    groupChatErrorToastTimer = setTimeout(function () {
        toast.classList.remove('visible');
    }, 3500);
}

function appendGroupMessage(message) {
    if (!messagesContainer || message.groupId !== activeGroupId) {
        return;
    }

    if (message.pinUpdate) {
        handlePinUpdate(message);
        return;
    }

    hideEmptyState();
    bumpGroup(message);

    var isDeletedByMe = message.deletedByUserIds && message.deletedByUserIds.indexOf && message.deletedByUserIds.indexOf(currentUserId) !== -1;
    var isDeletedContent = message.content === 'Сообщение удалено';
    var isDeleted = isDeletedByMe || isDeletedContent;
    if (message.id) {
        var existingRow = messagesContainer.querySelector('[data-message-id="' + message.id + '"]');
        if (isDeleted) {
            if (existingRow) {
                existingRow.remove();
            }
            return;
        }
        if (existingRow) {
            existingRow.replaceWith(buildGroupMessageRow(message));
            scrollToBottom();
            return;
        }
        displayedMessageIds.add(String(message.id));
    }

    messagesContainer.appendChild(buildGroupMessageRow(message));
    scrollToBottom();

    if (message.senderUsername !== currentUsername && document.hidden && window.MessageNotifications
            && (!window.MuteManager || !MuteManager.isGroupMuted(activeGroupId))) {
        MessageNotifications.show({
            sender: message.senderUsername,
            title: message.senderUsername,
            text: message.content || messagePreview(message) || 'Сообщение',
            href: '/chat/group/' + activeGroupId,
            tag: 'group-' + activeGroupId
        });
    }
}

var replyState = null;

var contextMenu = document.getElementById('context-menu');
var replyPreviewEl = document.getElementById('reply-preview');

function hideContextMenu() {
    if (contextMenu) contextMenu.hidden = true;
}

function showReplyPreview(messageId, senderUsername, content) {
    replyState = { messageId: messageId, sender: senderUsername, content: content };
    if (!replyPreviewEl) return;
    replyPreviewEl.querySelector('.reply-preview-sender').textContent = senderUsername;
    replyPreviewEl.querySelector('.reply-preview-text').textContent = truncatePreview(content);
    replyPreviewEl.classList.add('is-open');
    if (messageInput) messageInput.focus();
}

function clearReplyPreview() {
    replyState = null;
    if (replyPreviewEl) replyPreviewEl.classList.remove('is-open');
}

function openContextMenuAt(clientX, clientY, bubble) {
    if (!contextMenu || !bubble) return;
    var row = bubble.closest('.message-row');
    var messageId = row ? row.getAttribute('data-message-id') : null;
    if (!messageId) return;
    var sender = bubble.getAttribute('data-sender');
    var isOwn = sender === currentUsername;
    contextMenu.setAttribute('data-message-id', messageId);
    contextMenu.setAttribute('data-is-own', isOwn ? '1' : '0');
    var pinBtn = contextMenu.querySelector('[data-action="pin"]');
    if (pinBtn) {
        pinBtn.textContent = row.querySelector('.pin-badge') ? 'Открепить' : 'Закрепить';
    }
    var favBtn = contextMenu.querySelector('[data-action="favorite"]');
    if (favBtn) {
        favBtn.textContent = window.Favorites && Favorites.isFavorited(messageId)
            ? 'Убрать из избранного'
            : 'В избранное';
    }
    var deleteAllBtn = contextMenu.querySelector('[data-action="delete-all"]');
    if (deleteAllBtn) deleteAllBtn.style.display = isOwn ? '' : 'none';
    var editBtn = contextMenu.querySelector('[data-action="edit"]');
    if (editBtn) {
        var editable = isOwn && !bubble.querySelector('.sticker-image')
            && !bubble.querySelector('.voice-player')
            && !bubble.querySelector('.attachment-image')
            && !bubble.querySelector('.attachment-video')
            && !bubble.querySelector('.attachment-audio-wrap');
        editBtn.style.display = editable ? '' : 'none';
    }
    contextMenu.hidden = false;
    contextMenu.style.left = clientX + 'px';
    contextMenu.style.top = clientY + 'px';
}

if (messagesContainer) {
    messagesContainer.addEventListener('contextmenu', function (e) {
        var bubble = e.target.closest('.message-bubble');
        if (!bubble) return;
        e.preventDefault();
        openContextMenuAt(e.clientX, e.clientY, bubble);
    });

    // Долгое нажатие для мобильных устройств
    var longPressTimer = null;
    var longPressBubble = null;

    messagesContainer.addEventListener('touchstart', function (e) {
        longPressBubble = e.target.closest('.message-bubble');
        if (!longPressBubble) return;
        clearTimeout(longPressTimer);
        longPressTimer = setTimeout(function () {
            var touch = e.touches[0] || e.changedTouches[0];
            var x = touch ? touch.clientX : e.clientX;
            var y = touch ? touch.clientY : e.clientY;
            if (navigator.vibrate) { try { navigator.vibrate(50); } catch (_) {} }
            openContextMenuAt(x, y, longPressBubble);
        }, 450);
    }, { passive: true });

    messagesContainer.addEventListener('touchend', function () {
        clearTimeout(longPressTimer);
        longPressTimer = null;
        longPressBubble = null;
    });
    messagesContainer.addEventListener('touchmove', function () {
        clearTimeout(longPressTimer);
        longPressTimer = null;
    }, { passive: true });
    messagesContainer.addEventListener('touchcancel', function () {
        clearTimeout(longPressTimer);
        longPressTimer = null;
        longPressBubble = null;
    });
}

document.addEventListener('click', function (e) {
    if (contextMenu && !contextMenu.hidden && !contextMenu.contains(e.target)) {
        hideContextMenu();
    }
});

if (contextMenu) {
    contextMenu.addEventListener('click', function (e) {
        var btn = e.target.closest('.context-menu-item');
        if (!btn) return;
        var action = btn.getAttribute('data-action');
        var messageId = contextMenu.getAttribute('data-message-id');
        hideContextMenu();
        if (!messageId) return;

        if (action === 'reply') {
            var row = messagesContainer.querySelector('[data-message-id="' + messageId + '"]');
            if (row) {
                var contentText = '';
                var contentEl = row.querySelector('.content');
                if (contentEl) contentText = contentEl.textContent;
                var senderEl = row.querySelector('.sender');
                var senderName = senderEl ? senderEl.textContent : currentUsername;
                showReplyPreview(messageId, senderName, contentText);
            }
        } else if (action === 'pin') {
            var pinRow = messagesContainer.querySelector('[data-message-id="' + messageId + '"]');
            var isPinned = pinRow ? !!pinRow.querySelector('.pin-badge') : false;
            stompClient.send('/app/group.pin', {}, JSON.stringify({ messageId: Number(messageId), groupId: Number(activeGroupId), pinned: !isPinned }));
        } else if (action === 'favorite') {
            if (window.Favorites) {
                window.Favorites.toggle(messageId, 'GROUP');
            }
        } else if (action === 'edit') {
            startGroupInlineEdit(messageId);
        } else if (action === 'forward') {
            if (window.showForwardDialog) {
                showForwardDialog(function (targetType, target) {
                    if (targetType === 'group') {
                        stompClient.send('/app/group.forward', {}, JSON.stringify({
                            sourceType: 'GROUP',
                            sourceMessageId: Number(messageId),
                            groupId: Number(target)
                        }));
                    } else {
                        stompClient.send('/app/chat.forward', {}, JSON.stringify({
                            sourceType: 'GROUP',
                            sourceMessageId: Number(messageId),
                            targetUsername: target
                        }));
                    }
                });
            }
        } else if (action === 'delete-me') {
            var rowMe = messagesContainer.querySelector('[data-message-id="' + messageId + '"]');
            var labelMe = rowMe ? getGroupMessageLabel(rowMe) : 'сообщение';
            showConfirmDialog('Вы точно хотите удалить «' + labelMe + '» для себя?', function () {
                stompClient.send('/app/group.delete', {}, JSON.stringify({ messageId: Number(messageId), groupId: activeGroupId, mode: 'me' }));
            });
        } else if (action === 'delete-all') {
            var rowAll = messagesContainer.querySelector('[data-message-id="' + messageId + '"]');
            var labelAll = rowAll ? getGroupMessageLabel(rowAll) : 'сообщение';
            showConfirmDialog('Вы точно хотите удалить «' + labelAll + '» для всех?', function () {
                stompClient.send('/app/group.delete', {}, JSON.stringify({ messageId: Number(messageId), groupId: activeGroupId, mode: 'everyone' }));
            });
        }
    });
}

function startGroupInlineEdit(messageId) {
    var row = messagesContainer.querySelector('[data-message-id="' + messageId + '"]');
    if (!row) return;
    var bubble = row.querySelector('.message-bubble');
    var contentEl = bubble ? bubble.querySelector('.content') : null;
    if (!contentEl) return;
    var oldText = contentEl.textContent;

    var wrap = document.createElement('div');
    wrap.className = 'inline-edit';

    var input = document.createElement('textarea');
    input.className = 'inline-edit-input';
    input.value = oldText;
    input.maxLength = 2000;
    wrap.appendChild(input);

    var actions = document.createElement('div');
    actions.className = 'inline-edit-actions';
    var saveBtn = document.createElement('button');
    saveBtn.type = 'button';
    saveBtn.className = 'inline-edit-save';
    saveBtn.textContent = 'Сохранить';
    var cancelBtn = document.createElement('button');
    cancelBtn.type = 'button';
    cancelBtn.className = 'inline-edit-cancel';
    cancelBtn.textContent = 'Отмена';
    actions.appendChild(saveBtn);
    actions.appendChild(cancelBtn);
    wrap.appendChild(actions);

    contentEl.replaceWith(wrap);
    input.focus();
    input.setSelectionRange(input.value.length, input.value.length);

    var finished = false;
    function revert() {
        if (finished) return;
        finished = true;
        var span = document.createElement('span');
        span.className = 'content';
        span.textContent = oldText;
        if (wrap.isConnected) wrap.replaceWith(span);
    }
    function commit() {
        if (finished) return;
        var val = input.value.trim();
        if (!val) { revert(); return; }
        finished = true;
        var span = document.createElement('span');
        span.className = 'content';
        span.innerHTML = linkifyText(val);
        if (wrap.isConnected) wrap.replaceWith(span);
        stompClient.send('/app/group.edit', {}, JSON.stringify({
            messageId: Number(messageId),
            groupId: Number(activeGroupId),
            content: val
        }));
    }

    input.addEventListener('keydown', function (e) {
        if (e.key === 'Enter' && !e.shiftKey) {
            e.preventDefault();
            commit();
        } else if (e.key === 'Escape') {
            e.preventDefault();
            revert();
        }
    });
    saveBtn.addEventListener('click', commit);
    cancelBtn.addEventListener('click', revert);
}

function getGroupMessageLabel(row) {
    var contentEl = row.querySelector('.content');
    if (contentEl && contentEl.textContent && contentEl.textContent.trim()) {
        return contentEl.textContent.trim();
    }
    if (row.querySelector('.voice-player')) {
        return 'голосовое сообщение';
    }
    if (row.querySelector('.sticker-image')) {
        return 'стикер';
    }
    if (row.querySelector('.attachment-audio-wrap')) {
        return 'аудиофайл';
    }
    if (row.querySelector('.attachment-image')) {
        return 'изображение';
    }
    if (row.querySelector('.attachment-video')) {
        return 'видео';
    }
    return 'сообщение';
}

if (replyPreviewEl) {
    replyPreviewEl.querySelector('.reply-preview-close').addEventListener('click', clearReplyPreview);
    replyPreviewEl.querySelector('.reply-preview-content').addEventListener('click', focusReplyMessage);
}

document.addEventListener('click', function (e) {
    var item;
    if (e.target.closest && (item = e.target.closest('.pinned-item'))) {
        if (e.target.closest('.pinned-item-close')) {
            unpinMessage(item.getAttribute('data-pinned-id'));
        } else {
            scrollToMessage(item.getAttribute('data-pinned-id'));
            pinnedIndex = (pinnedIndex + 1) % pinnedItems.length;
            syncPinnedDisplay();
        }
    }
});

initPinnedBar();

if (messagesContainer) {
    messagesContainer.addEventListener('click', function (e) {
        var replyBlock = e.target.closest('.reply-block[data-reply-id]');
        if (replyBlock) {
            scrollToMessage(replyBlock.getAttribute('data-reply-id'));
        }
        var trBtn = e.target.closest('[data-action="transcribe-row"]');
        if (trBtn) {
            var row = trBtn.closest('.message-row');
            if (row && row.getAttribute('data-message-id')) {
                transcribeGroupVoiceMessage(row.getAttribute('data-message-id'));
            }
        }
    });
}

document.addEventListener('keydown', function (e) {
    if (e.key === 'Escape') {
        hideContextMenu();
        clearReplyPreview();
    }
});

var socket = new SockJS('/ws');
var stompClient = Stomp.over(socket);
stompClient.debug = null;

var voiceButton = document.getElementById('voice-button');
var voiceRecBar = document.getElementById('voice-rec-bar');
var voiceRecTimer = document.getElementById('voice-rec-timer');
var voiceRecSend = document.getElementById('voice-rec-send');
var voiceRecCancel = document.getElementById('voice-rec-cancel');

var voiceRecorder = null;
var voiceChunks = [];
var voiceRecTimerInterval = null;
var voiceRecStarted = 0;
var voiceRecording = false;
var VOICE_REC_MAX = 60 * 1000;

function formatVoiceTime(ms) {
    var s = Math.max(0, Math.floor(ms / 1000));
    var m = Math.floor(s / 60);
    s = s % 60;
    return m + ':' + (s < 10 ? '0' : '') + s;
}

function updateVoiceRecTimer() {
    if (!voiceRecTimer) {
        return;
    }
    var elapsed = Date.now() - voiceRecStarted;
    voiceRecTimer.textContent = formatVoiceTime(elapsed);
    if (elapsed >= VOICE_REC_MAX) {
        finishVoiceRecording();
    }
}

function setVoiceRecordingUI(recording) {
    voiceRecording = recording;
    if (voiceRecBar) {
        voiceRecBar.hidden = !recording;
    }
    if (voiceButton) {
        voiceButton.disabled = recording;
    }
    if (sendButton) {
        sendButton.disabled = recording || !stompClient || !stompClient.connected;
    }
}

function startVoiceRecording() {
    if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia || !window.MediaRecorder) {
        showComposerError('Запись голоса не поддерживается этим браузером');
        return;
    }
    navigator.mediaDevices.getUserMedia({ audio: true }).then(function (stream) {
        var mime = 'audio/webm';
        if (!window.MediaRecorder.isTypeSupported(mime)) {
            mime = '';
        }
        try {
            voiceRecorder = new MediaRecorder(stream, mime ? { mimeType: mime } : undefined);
        } catch (e) {
            stream.getTracks().forEach(function (t) { t.stop(); });
            showComposerError('Не удалось запустить запись');
            return;
        }
        voiceChunks = [];
        voiceRecorder.ondataavailable = function (e) {
            if (e.data && e.data.size) {
                voiceChunks.push(e.data);
            }
        };
        voiceRecorder.onstop = function () {
            stream.getTracks().forEach(function (t) { t.stop(); });
            setVoiceRecordingUI(false);
            uploadVoiceRecording();
        };
        voiceRecorder.onerror = function () {
            showComposerError('Ошибка записи');
            voiceChunks = [];
            voiceRecorder = null;
            setVoiceRecordingUI(false);
        };
        voiceRecorder.start();
        voiceRecStarted = Date.now();
        setVoiceRecordingUI(true);
        updateVoiceRecTimer();
        voiceRecTimerInterval = setInterval(updateVoiceRecTimer, 250);
    }).catch(function () {
        showComposerError('Нет доступа к микрофону');
    });
}

function cancelVoiceRecording() {
    voiceChunks = [];
    if (voiceRecTimerInterval) {
        clearInterval(voiceRecTimerInterval);
        voiceRecTimerInterval = null;
    }
    if (voiceRecorder) {
        try {
            voiceRecorder.onstop = null;
            if (voiceRecorder.state !== 'inactive') {
                voiceRecorder.stop();
            }
        } catch (e) {}
        voiceRecorder = null;
    }
    setVoiceRecordingUI(false);
}

function finishVoiceRecording() {
    if (voiceRecTimerInterval) {
        clearInterval(voiceRecTimerInterval);
        voiceRecTimerInterval = null;
    }
    if (voiceRecorder && voiceRecorder.state !== 'inactive') {
        try {
            voiceRecorder.stop();
        } catch (e) {}
    } else {
        setVoiceRecordingUI(false);
    }
}

function uploadVoiceRecording() {
    var chunks = voiceChunks;
    var durationMs = Date.now() - voiceRecStarted;
    voiceChunks = [];
    if (!chunks.length) {
        showComposerError('Запись пустая — ничего не отправлено');
        return;
    }
    var blob = new Blob(chunks, { type: 'audio/webm' });
    var fd = new FormData();
    fd.append('file', blob, 'voice.webm');
    fd.append('durationMs', String(durationMs));
    fetch('/api/voice/upload', { method: 'POST', body: fd })
        .then(function (r) {
            return r.json().then(function (d) { return { ok: r.ok, data: d }; });
        })
        .then(function (res) {
            if (!res.ok || !res.data || !res.data.url) {
                showComposerError('Не удалось отправить голосовое сообщение');
                return;
            }
            if (!activeGroupId || !stompClient.connected) {
                showComposerError('Нет соединения — голосовое не отправлено');
                return;
            }
            var payload = {
                groupId: activeGroupId,
                audioUrl: res.data.url,
                audioDurationMs: durationMs
            };
            if (replyState && replyState.messageId) {
                payload.replyToMessageId = replyState.messageId;
            }
            stompClient.send('/app/group.send', {}, JSON.stringify(payload));
            clearReplyPreview();
        })
        .catch(function () {
            showComposerError('Ошибка сети при отправке голосового');
        });
}

if (voiceButton) {
    voiceButton.addEventListener('click', function (e) {
        e.preventDefault();
        if (voiceRecording) {
            finishVoiceRecording();
        } else {
            startVoiceRecording();
        }
    });
}
if (voiceRecSend) {
    voiceRecSend.addEventListener('click', function () {
        finishVoiceRecording();
    });
}
if (voiceRecCancel) {
    voiceRecCancel.addEventListener('click', function () {
        cancelVoiceRecording();
    });
}

function sendGroupReaction(messageId, emoji) {
    if (!stompClient || !stompClient.connected || !activeGroupId) {
        return;
    }
    stompClient.send('/app/group.react', {}, JSON.stringify({
        messageId: Number(messageId),
        groupId: Number(activeGroupId),
        emoji: emoji
    }));
}

function handleGroupReactionEvent(event) {
    if (!event || event.messageId == null || !event.reactions) {
        return;
    }
    if (!messagesContainer) {
        return;
    }
    var row = messagesContainer.querySelector('[data-message-id="' + event.messageId + '"]');
    if (!row) {
        return;
    }
    if (window.ReactionsUI) {
        ReactionsUI.renderBar(row, event.messageId, event.reactions, currentUsername, function (emoji) {
            sendGroupReaction(event.messageId, emoji);
        });
    }
}

function setComposerEnabled(enabled) {
    if (messageInput) {
        messageInput.disabled = !enabled;
    }
    if (sendButton) {
        sendButton.disabled = !enabled;
    }
}

setComposerEnabled(false);

stompClient.connect({}, function () {
    setComposerEnabled(true);
    stompClient.subscribe('/topic/group.' + activeGroupId, function (payload) {
        appendGroupMessage(JSON.parse(payload.body));
    });
    stompClient.subscribe('/topic/group.' + activeGroupId + '.reactions', function (payload) {
        handleGroupReactionEvent(JSON.parse(payload.body));
    });
    stompClient.subscribe('/topic/group.' + activeGroupId + '.typing', function (payload) {
        handleGroupTyping(JSON.parse(payload.body));
    });
    stompClient.subscribe('/user/queue/chat-error', function (payload) {
        handleGroupChatError(JSON.parse(payload.body));
    });
}, function (error) {
    console.error('WebSocket connection error:', error);
    setComposerEnabled(false);
});

setupAttachmentComposer({
    uploadUrl: function () {
        var groupId = groupIdInput ? parseInt(groupIdInput.value, 10) : activeGroupId;
        return '/group/' + groupId + '/attachment';
    },
    onUploadStart: function () {
        setComposerEnabled(false);
    },
    onUploadEnd: function () {
        setComposerEnabled(true);
    },
    onError: showComposerError
});

if (messageForm) {
    messageForm.addEventListener('submit', function (event) {
        event.preventDefault();
        var content = messageInput.value.trim();
        var groupId = groupIdInput ? parseInt(groupIdInput.value, 10) : activeGroupId;
        var pendingFile = window.AttachmentPending ? AttachmentPending.getFile() : null;
        if (!stompClient.connected || !groupId) {
            return;
        }
        if (!content && !pendingFile) {
            return;
        }

        if (pendingFile) {
            AttachmentPending.send({
                content: content,
                replyToMessageId: replyState && replyState.messageId
            }).then(function () {
                messageInput.value = '';
                messageInput.focus();
                clearReplyPreview();
            }).catch(function () {
            });
            return;
        }

        var payload = { groupId: groupId, content: content };
        if (replyState && replyState.messageId) {
            payload.replyToMessageId = replyState.messageId;
        }
        stompClient.send('/app/group.send', {}, JSON.stringify(payload));
        messageInput.value = '';
        stopLocalGroupTyping();
        messageInput.focus();
        clearReplyPreview();
    });

    jumpToBottom();
    messageInput.focus();

    if (window.addEventListener) {
        window.addEventListener('load', function () {
            requestAnimationFrame(function () { jumpToBottom(); });
        });
    }

    messageInput.addEventListener('input', notifyLocalGroupTyping);
    messageInput.addEventListener('blur', stopLocalGroupTyping);
    window.addEventListener('beforeunload', stopLocalGroupTyping);
}

initLightbox();

if (window.ReactionsUI && messagesContainer) {
    ReactionsUI.initContainer(messagesContainer, {
        username: currentUsername,
        getMessageId: function (row) {
            return row.getAttribute('data-message-id');
        },
        getReactions: function (messageId) {
            var row = messagesContainer.querySelector('[data-message-id="' + messageId + '"]');
            if (row) {
                var bar = row.querySelector(':scope > .message-reactions');
                if (bar) {
                    var map = {};
                    bar.querySelectorAll('.reaction-badge').forEach(function (b) {
                        map[b.getAttribute('data-emoji')] = b.getAttribute('data-users') ? b.getAttribute('data-users').split(',') : [];
                    });
                    return map;
                }
            }
            return initialReactions && initialReactions[String(messageId)];
        },
        onPick: function (messageId, emoji) {
            sendGroupReaction(messageId, emoji);
        }
    });
    messagesContainer.querySelectorAll('.message-row').forEach(function (row) {
        var id = row.getAttribute('data-message-id');
        if (id && initialReactions && initialReactions[String(id)]) {
            ReactionsUI.renderBar(row, id, initialReactions[String(id)], currentUsername, function (emoji) {
                sendGroupReaction(id, emoji);
            });
        }
    });
}

if (window.VoicePlayer && messagesContainer) {
    VoicePlayer.initAll(messagesContainer);
}

if (window.StickerUI) {
    StickerUI.init({
        attachSelector: '#attach-button',
        composerSelector: '.chat-composer',
        onPick: function (stickerCode) {
            var groupId = groupIdInput ? parseInt(groupIdInput.value, 10) : activeGroupId;
            if (!stickerCode || !stompClient.connected || !groupId) {
                return;
            }
            stompClient.send('/app/group.send', {}, JSON.stringify({ groupId: groupId, stickerCode: stickerCode }));
            clearReplyPreview();
        }
    });
}
