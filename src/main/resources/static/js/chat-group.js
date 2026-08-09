var messagesContainer = document.getElementById('messages');
var messageForm = document.getElementById('message-form');
var messageInput = document.getElementById('message-input');
var sendButton = document.getElementById('send-button');
var groupIdInput = document.getElementById('group-id');

var displayedMessageIds = new Set();
if (messagesContainer) {
    messagesContainer.querySelectorAll('[data-message-id]').forEach(function (el) {
        displayedMessageIds.add(el.getAttribute('data-message-id'));
    });
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
        var senderEl = document.createElement('span');
        senderEl.className = 'sender';
        senderEl.textContent = message.senderUsername;
        bubbleEl.appendChild(senderEl);
    }

    var replyBlock = createReplyBlock(message);
    if (replyBlock) {
        bubbleEl.appendChild(replyBlock);
    }

    var attachmentEl = createAttachmentElement(message);
    if (attachmentEl) {
        bubbleEl.appendChild(attachmentEl);
    }

    if (message.stickerUrl) {
        var stickerEl = window.StickerUI
            ? StickerUI.createStickerImage(message.stickerUrl)
            : (function () {
                var img = document.createElement('img');
                img.className = 'sticker-image';
                img.src = message.stickerUrl;
                img.alt = 'Стикер';
                return img;
            })();
        bubbleEl.appendChild(stickerEl);
    }

    if (message.content) {
        var contentEl = document.createElement('span');
        contentEl.className = 'content';
        contentEl.textContent = message.content;
        bubbleEl.appendChild(contentEl);
    }

    var metaEl = document.createElement('span');
    metaEl.className = 'message-meta';
    var timeEl = document.createElement('time');
    timeEl.className = 'message-time';
    timeEl.textContent = message.timestamp || '';
    metaEl.appendChild(timeEl);

    bubbleEl.appendChild(metaEl);
    rowEl.appendChild(bubbleEl);
    return rowEl;
}

function appendGroupMessage(message) {
    if (!messagesContainer || message.groupId !== activeGroupId) {
        return;
    }

    hideEmptyState();

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

    if (message.senderUsername !== currentUsername && document.hidden && window.MessageNotifications) {
        MessageNotifications.show({
            sender: message.senderUsername,
            title: message.senderUsername,
            text: message.content || messagePreview(message) || 'Сообщение',
            href: '/chat/group/' + activeGroupId
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

if (messagesContainer) {
    messagesContainer.addEventListener('contextmenu', function (e) {
        var bubble = e.target.closest('.message-bubble');
        if (!bubble) return;
        e.preventDefault();
        var row = bubble.closest('.message-row');
        var messageId = row ? row.getAttribute('data-message-id') : null;
        if (!messageId) return;
        var sender = bubble.getAttribute('data-sender');
        var isOwn = sender === currentUsername;
        contextMenu.setAttribute('data-message-id', messageId);
        contextMenu.setAttribute('data-is-own', isOwn ? '1' : '0');
        var deleteAllBtn = contextMenu.querySelector('[data-action="delete-all"]');
        if (deleteAllBtn) deleteAllBtn.style.display = isOwn ? '' : 'none';
        contextMenu.hidden = false;
        contextMenu.style.left = e.clientX + 'px';
        contextMenu.style.top = e.clientY + 'px';
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
        } else if (action === 'delete-me') {
            stompClient.send('/app/group.delete', {}, JSON.stringify({ messageId: Number(messageId), groupId: activeGroupId, mode: 'me' }));
        } else if (action === 'delete-all') {
            stompClient.send('/app/group.delete', {}, JSON.stringify({ messageId: Number(messageId), groupId: activeGroupId, mode: 'everyone' }));
        }
    });
}

if (replyPreviewEl) {
    replyPreviewEl.querySelector('.reply-preview-close').addEventListener('click', clearReplyPreview);
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
        if (!content || !stompClient.connected || !groupId) {
            return;
        }

        var payload = { groupId: groupId, content: content };
        if (replyState && replyState.messageId) {
            payload.replyToMessageId = replyState.messageId;
        }
        stompClient.send('/app/group.send', {}, JSON.stringify(payload));
        messageInput.value = '';
        messageInput.focus();
        clearReplyPreview();
    });

    scrollToBottom();
    messageInput.focus();
}

initLightbox();

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
