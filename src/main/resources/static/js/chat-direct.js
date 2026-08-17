const conversationsList = document.getElementById('conversations-list');
const messagesContainer = document.getElementById('messages');
const messageForm = document.getElementById('message-form');
const messageInput = document.getElementById('message-input');
const sendButton = document.getElementById('send-button');
const chatStatus = document.getElementById('chat-status');
const typingIndicator = document.getElementById('typing-indicator');

function pinnedPreviewText(message) {
    if (!message) return '';
    if (message.stickerUrl) return 'Стикер';
    if (message.audioUrl) return 'Голосовое сообщение';
    if (message.content && message.content.trim()) return message.content.indexOf('e2e1:') === 0 ? '🔒 Сообщение' : message.content;
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
    if (!activeChatUsername || !isRelevantChat(message)) {
        return;
    }
    if (message.pinned) {
        addPinnedItem(message);
    } else {
        removePinnedItem(message.id);
    }
}

function unpinMessage(messageId) {
    if (!messageId || !stompClient || !stompClient.connected || !activeChatUsername) {
        return;
    }
    stompClient.send('/app/chat.pin', {}, JSON.stringify({ messageId: Number(messageId), pinned: false }));
}

function handlePinUpdate(message) {
    if (!activeChatUsername || !isRelevantChat(message)) {
        return;
    }
    var row = messagesContainer ? messagesContainer.querySelector('[data-message-id="' + message.id + '"]') : null;
    if (row) {
        if (message.deleted) {
            row.remove();
        } else {
            var updatedRow = buildMessageRow(message);
            row.replaceWith(updatedRow);
            forceLoadVoice(updatedRow);
        }
    }
    renderPinnedBar(message);
}

let typingStopTimeout = null;
let hideTypingTimeout = null;
let isSendingTyping = false;
let peerIsTyping = false;
let partnerOnline = typeof initialReceiverOnline === 'boolean' ? initialReceiverOnline : false;

const displayedMessageIds = new Set();
if (messagesContainer) {
    messagesContainer.querySelectorAll('[data-message-id]').forEach(function (el) {
        displayedMessageIds.add(el.getAttribute('data-message-id'));
    });
}

function getPartnerUsername(message) {
    return message.senderUsername === currentUsername
        ? message.receiverUsername
        : message.senderUsername;
}

function isRelevantChat(message) {
    if (!activeChatUsername) {
        return false;
    }
    return (message.senderUsername === currentUsername && message.receiverUsername === activeChatUsername) ||
        (message.senderUsername === activeChatUsername && message.receiverUsername === currentUsername);
}

function truncatePreview(text) {
    if (!text) {
        return '';
    }
    return text.length > 48 ? text.substring(0, 48) + '…' : text;
}

function hideConversationsEmpty() {
    const empty = document.getElementById('conversations-empty');
    if (empty) {
        empty.remove();
    }
}

function formatUnreadLabel(count) {
    return count > 99 ? '99+' : String(count);
}

function updateTotalUnreadBadge() {
    const badges = conversationsList ? conversationsList.querySelectorAll('.unread-badge') : [];
    let total = 0;
    badges.forEach(function (badge) {
        const value = parseInt(badge.textContent, 10);
        if (!isNaN(value)) {
            total += value;
        }
    });
    const totalEl = document.querySelector('.sidebar-unread-total');
    if (total > 0) {
        if (totalEl) {
            totalEl.textContent = formatUnreadLabel(total);
        } else {
            const title = document.querySelector('.sidebar-section-grow .sidebar-section-title');
            if (title) {
                const span = document.createElement('span');
                span.className = 'sidebar-unread-total';
                span.textContent = formatUnreadLabel(total);
                title.appendChild(span);
            }
        }
    } else if (totalEl) {
        totalEl.remove();
    }
}

function updateUnreadBadge(partner, count) {
    if (!conversationsList || !partner) {
        return;
    }
    const item = conversationsList.querySelector('[data-partner="' + partner + '"]');
    if (!item) {
        return;
    }

    let badge = item.querySelector('.unread-badge');
    if (count > 0) {
        item.classList.add('has-unread');
        if (!badge) {
            badge = document.createElement('span');
            badge.className = 'unread-badge';
            item.appendChild(badge);
        }
        badge.textContent = formatUnreadLabel(count);
    } else {
        item.classList.remove('has-unread');
        if (badge) {
            badge.remove();
        }
    }
    updateTotalUnreadBadge();
}

function setPartnerOnline(username, online) {
    if (!username) {
        return;
    }

    const dot = document.querySelector('.conversation-item[data-partner="' + username + '"] .online-dot');
    if (dot) {
        dot.classList.toggle('is-online', online);
    }

    if (activeChatUsername === username && chatStatus && !peerIsTyping) {
        partnerOnline = online;
        chatStatus.textContent = online ? 'В сети' : 'Не в сети';
        chatStatus.classList.toggle('is-online', online);
        chatStatus.classList.toggle('is-offline', !online);
        chatStatus.classList.remove('is-typing');
    }
}

function createAvatarElement(username) {
    const avatar = document.createElement('div');
    avatar.className = 'user-avatar conversation-avatar';
    const letter = document.createElement('span');
    letter.className = 'user-avatar-letter';
    letter.textContent = username.charAt(0).toUpperCase();
    avatar.appendChild(letter);
    return avatar;
}

function createConversationDelete(partner) {
    const form = document.createElement('form');
    form.method = 'post';
    form.action = '/chat/' + encodeURIComponent(partner) + '/delete';
    form.setAttribute('data-confirm-message', 'Удалить переписку с «' + partner + '»? Это действие нельзя отменить.');
    const button = document.createElement('button');
    button.type = 'submit';
    button.className = 'conversation-delete';
    button.title = 'Удалить переписку';
    button.setAttribute('aria-label', 'Удалить переписку');
    button.innerHTML = '<svg width="14" height="14" viewBox="0 0 24 24" fill="none" aria-hidden="true">' +
        '<path d="M3 6h18M8 6V4a1 1 0 0 1 1-1h6a1 1 0 0 1 1 1v2m3 0v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>' +
        '<path d="M10 11v6M14 11v6" stroke="currentColor" stroke-width="2" stroke-linecap="round"/></svg>';
    form.appendChild(button);
    return form;
}

function upsertConversation(message, incrementUnread) {
    if (!conversationsList) {
        return;
    }

    const partner = getPartnerUsername(message);
    if (partner === currentUsername) {
        return;
    }
    const isOutgoing = message.senderUsername === currentUsername;
    const preview = truncatePreview(messagePreview(message));
    let item = conversationsList.querySelector('[data-partner="' + partner + '"]');

    hideConversationsEmpty();

    if (item) {
        const timeEl = item.querySelector('.conversation-time');
        const previewEl = item.querySelector('.conversation-preview');
        if (timeEl) {
            timeEl.textContent = message.timestamp || '';
        }
        if (previewEl) {
            previewEl.textContent = '';
            if (isOutgoing) {
                previewEl.appendChild(document.createTextNode('Вы: '));
            }
            previewEl.appendChild(document.createTextNode(preview));
        }
        if (incrementUnread) {
            const badge = item.querySelector('.unread-badge');
            const current = badge ? (parseInt(badge.textContent, 10) || 0) : 0;
            updateUnreadBadge(partner, current + 1);
        }
        conversationsList.prepend(item);
        return;
    }

    item = document.createElement('div');
    item.className = 'conversation-item' + (incrementUnread && !isOutgoing ? ' has-unread' : '');
    item.setAttribute('data-partner', partner);
    if (activeChatUsername === partner) {
        item.classList.add('is-active');
    }

    const avatarLink = document.createElement('a');
    avatarLink.className = 'conversation-avatar-link avatar-with-status';
    avatarLink.href = '/profile/' + encodeURIComponent(partner);
    avatarLink.title = 'Профиль';
    avatarLink.appendChild(createAvatarElement(partner));
    const onlineDot = document.createElement('span');
    onlineDot.className = 'online-dot';
    avatarLink.appendChild(onlineDot);

    const bodyLink = document.createElement('a');
    bodyLink.className = 'conversation-body';
    bodyLink.href = '/chat/' + encodeURIComponent(partner);

    const top = document.createElement('div');
    top.className = 'conversation-top';

    const nameEl = document.createElement('span');
    nameEl.className = 'conversation-name';
    nameEl.textContent = partner;

    const timeEl = document.createElement('time');
    timeEl.className = 'conversation-time';
    timeEl.textContent = message.timestamp || '';

    top.appendChild(nameEl);
    top.appendChild(timeEl);

    const previewEl = document.createElement('span');
    previewEl.className = 'conversation-preview';
    if (isOutgoing) {
        previewEl.appendChild(document.createTextNode('Вы: '));
    }
    previewEl.appendChild(document.createTextNode(preview));

    bodyLink.appendChild(top);
    bodyLink.appendChild(previewEl);
    item.appendChild(avatarLink);
    item.appendChild(bodyLink);

    if (incrementUnread && !isOutgoing && partner !== activeChatUsername) {
        const badge = document.createElement('span');
        badge.className = 'unread-badge';
        badge.textContent = '1';
        item.appendChild(badge);
        updateTotalUnreadBadge();
    }

    item.appendChild(createConversationDelete(partner));

    conversationsList.prepend(item);
}

function hideEmptyState() {
    if (!messagesContainer) {
        return;
    }
    const emptyState = messagesContainer.querySelector('.chat-empty');
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

function createReadReceiptElement(isRead) {
    const receipt = document.createElement('span');
    receipt.className = 'read-receipt' + (isRead ? ' is-read' : '');
    receipt.title = isRead ? 'Прочитано' : 'Доставлено';
    receipt.innerHTML = '<svg width="16" height="16" viewBox="0 0 24 24" fill="none" aria-hidden="true">' +
        '<path d="M2.5 12.5l4.5 4.5L16.5 7" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"/>' +
        '<path d="M10.5 17l2 2L22 8" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"/>' +
        '</svg>';
    return receipt;
}

function markMessagesAsRead(messageIds) {
    if (!messagesContainer || !messageIds || !messageIds.length) {
        return;
    }
    messageIds.forEach(function (id) {
        const row = messagesContainer.querySelector('[data-message-id="' + id + '"]');
        if (row) {
            const receipt = row.querySelector('.read-receipt');
            if (receipt) {
                receipt.classList.add('is-read');
                receipt.title = 'Прочитано';
            }
        }
    });
}

function buildMessageRow(message) {
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
    if ((message.stickerUrl || message.audioUrl) && !message.content) {
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

    if (message.audioUrl) {
        var voiceEl = window.VoicePlayer ? VoicePlayer.create(message.audioUrl, message.audioDurationMs) : null;
        if (voiceEl) {
            bubbleEl.appendChild(voiceEl);
        }
    }

    if (message.content) {
        var contentEl = document.createElement('span');
        contentEl.className = 'content';
        if (window.E2E && E2E.isEncrypted(message.content)) {
            contentEl.textContent = '🔒 Зашифрованное сообщение';
            contentEl.classList.add('e2e-pending');
            bubbleEl.appendChild(contentEl);
            E2E.decryptMessage(message.content, getPartnerUsername(message)).then(function (plain) {
                if (plain && contentEl.parentNode) {
                    contentEl.classList.remove('e2e-pending');
                    contentEl.innerHTML = linkifyText(plain);
                }
            });
        } else {
            contentEl.innerHTML = linkifyText(message.content);
            bubbleEl.appendChild(contentEl);
        }
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

    if (isOutgoing) {
        metaEl.appendChild(createReadReceiptElement(!!message.read));
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
            sendReaction(message.id, emoji);
        });
    }
    if (window.Favorites) {
        window.Favorites.markRow(rowEl, message.id);
    }
    return rowEl;
}

function appendMessage(message) {
    if (!messagesContainer || !isRelevantChat(message)) {
        return;
    }

    hideEmptyState();
    hidePeerTyping();

    var isDeleted = message.deleted || (message.content === 'Сообщение удалено');
    if (message.id) {
        var existingRow = messagesContainer.querySelector('[data-message-id="' + message.id + '"]');
        if (isDeleted) {
            if (existingRow) {
                existingRow.remove();
            }
            return;
        }
        if (existingRow) {
            var updatedRow = buildMessageRow(message);
            existingRow.replaceWith(updatedRow);
            forceLoadVoice(updatedRow);
            scrollToBottom();
            return;
        }
        displayedMessageIds.add(String(message.id));
    }

    var row = buildMessageRow(message);
    messagesContainer.appendChild(row);
    forceLoadVoice(row);
    scrollToBottom();
}

function forceLoadVoice(row) {
    if (!row) return;
    requestAnimationFrame(function () {
        row.querySelectorAll('.voice-player').forEach(function (vp) {
            var a = vp.querySelector('audio');
            if (a && a.src) {
                try { a.load(); } catch (e) {}
            }
        });
    });
}

function sendMarkAsRead() {
    if (!stompClient || !stompClient.connected || !activeChatUsername) {
        return;
    }
    stompClient.send('/app/chat.read', {}, JSON.stringify({
        partnerUsername: activeChatUsername
    }));
    updateUnreadBadge(activeChatUsername, 0);
}

function handleIncomingMessage(message) {
    const partner = getPartnerUsername(message);
    const isIncoming = message.senderUsername !== currentUsername;
    const inActiveChat = partner === activeChatUsername;

    if (message.pinUpdate) {
        handlePinUpdate(message);
        return;
    }

    upsertConversation(message, isIncoming && !inActiveChat);

    if (inActiveChat) {
        appendMessage(message);
        if (isIncoming) {
            sendMarkAsRead();
        }
    }

    if (isIncoming && window.MessageNotifications
            && (!window.MuteManager || !MuteManager.isDirectMuted(partner))
            && (!inActiveChat || document.hidden)) {
        MessageNotifications.show({
            sender: message.senderUsername,
            title: message.senderUsername,
            text: truncatePreview(messagePreview(message)),
            href: '/chat/' + encodeURIComponent(partner),
            tag: 'direct-' + partner
        });
    }
}

function sendTypingState(typing) {
    if (!stompClient.connected || !activeChatUsername) {
        return;
    }
    stompClient.send('/app/chat.typing', {}, JSON.stringify({
        receiverUsername: activeChatUsername,
        typing: typing
    }));
}

function notifyLocalTyping() {
    if (!messageInput || !messageInput.value.trim()) {
        stopLocalTyping();
        return;
    }

    if (!isSendingTyping) {
        isSendingTyping = true;
        sendTypingState(true);
    }

    clearTimeout(typingStopTimeout);
    typingStopTimeout = setTimeout(stopLocalTyping, 1500);
}

function stopLocalTyping() {
    clearTimeout(typingStopTimeout);
    if (isSendingTyping) {
        isSendingTyping = false;
        sendTypingState(false);
    }
}

function showPeerTyping() {
    if (!typingIndicator || !chatStatus || peerIsTyping) {
        return;
    }
    peerIsTyping = true;
    typingIndicator.classList.add('is-visible');
    chatStatus.textContent = 'печатает...';
    chatStatus.classList.add('is-typing');
    chatStatus.classList.remove('is-online', 'is-offline');
}

function hidePeerTyping() {
    if (!typingIndicator || !chatStatus) {
        return;
    }
    clearTimeout(hideTypingTimeout);
    peerIsTyping = false;
    typingIndicator.classList.remove('is-visible');
    chatStatus.textContent = partnerOnline ? 'В сети' : 'Не в сети';
    chatStatus.classList.toggle('is-online', partnerOnline);
    chatStatus.classList.toggle('is-offline', !partnerOnline);
    chatStatus.classList.remove('is-typing');
}

function handlePeerTyping(event) {
    if (!activeChatUsername || event.senderUsername !== activeChatUsername) {
        return;
    }

    if (event.typing === true) {
        showPeerTyping();
        clearTimeout(hideTypingTimeout);
        hideTypingTimeout = setTimeout(hidePeerTyping, 2000);
    } else {
        hidePeerTyping();
    }
}

function handleReadReceipt(receipt) {
    if (!receipt || !receipt.messageIds) {
        return;
    }
    if (activeChatUsername && receipt.readerUsername === activeChatUsername) {
        markMessagesAsRead(receipt.messageIds);
    }
}

function handleUnreadUpdate(update) {
    if (!update || !update.partnerUsername) {
        return;
    }
    updateUnreadBadge(update.partnerUsername, update.unreadCount || 0);
}

function sendReaction(messageId, emoji) {
    if (!stompClient || !stompClient.connected || !activeChatUsername) {
        return;
    }
    stompClient.send('/app/chat.react', {}, JSON.stringify({ messageId: Number(messageId), emoji: emoji }));
}

function handleReactionEvent(event) {
    if (!event || event.messageId == null || !event.reactions) {
        return;
    }
    if (!messagesContainer || !activeChatUsername) {
        return;
    }
    var row = messagesContainer.querySelector('[data-message-id="' + event.messageId + '"]');
    if (!row) {
        return;
    }
    if (window.ReactionsUI) {
        ReactionsUI.renderBar(row, event.messageId, event.reactions, currentUsername, function (emoji) {
            sendReaction(event.messageId, emoji);
        });
    }
}

function handlePresence(event) {
    if (!event || !event.username) {
        return;
    }
    setPartnerOnline(event.username, !!event.online);
}

function ensureConversationsEmpty() {
    if (!conversationsList || conversationsList.querySelector('.conversation-item')) {
        return;
    }
    if (!document.getElementById('conversations-empty')) {
        const empty = document.createElement('div');
        empty.className = 'sidebar-empty';
        empty.id = 'conversations-empty';
        empty.innerHTML = 'Пока нет переписок.<br/>Найдите пользователя через поиск.';
        conversationsList.appendChild(empty);
    }
}

function handleConversationDeleted(event) {
    if (!event || !event.deletedByUsername) {
        return;
    }
    const partner = event.deletedByUsername;
    if (conversationsList) {
        const item = conversationsList.querySelector('[data-partner="' + partner + '"]');
        if (item) {
            item.remove();
        }
    }
    updateTotalUnreadBadge();
    ensureConversationsEmpty();
    if (activeChatUsername === partner) {
        window.location.href = '/chat';
    }
}

const socket = new SockJS('/ws');
const stompClient = Stomp.over(socket);
stompClient.debug = null;

function setComposerEnabled(enabled) {
    if (messageInput) {
        messageInput.disabled = !enabled;
    }
    if (sendButton) {
        sendButton.disabled = !enabled;
    }
    if (voiceButton) {
        voiceButton.disabled = !enabled;
    }
}

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
    if (!activeChatUsername) {
        showComposerError('Сначала откройте чат');
        return;
    }
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
            if (!activeChatUsername || !stompClient.connected) {
                showComposerError('Нет соединения — голосовое не отправлено');
                return;
            }
            var payload = {
                receiverUsername: activeChatUsername,
                audioUrl: res.data.url
            };
            if (replyState && replyState.messageId) {
                payload.replyToMessageId = replyState.messageId;
            }
            stompClient.send('/app/chat.send', {}, JSON.stringify(payload));
            clearReplyPreview();
        })
        .catch(function () {
            showComposerError('Ошибка сети при отправке голосового');
        });
}

if (voiceButton) {
    voiceButton.addEventListener('click', function () {
        if (voiceRecording) {
            return;
        }
        startVoiceRecording();
    });
}
if (voiceRecSend) {
    voiceRecSend.addEventListener('click', function () {
        if (voiceRecording) {
            finishVoiceRecording();
        }
    });
}
if (voiceRecCancel) {
    voiceRecCancel.addEventListener('click', cancelVoiceRecording);
}
window.addEventListener('beforeunload', function () {
    if (voiceRecording) {
        cancelVoiceRecording();
    }
});

if (messageForm) {
    setComposerEnabled(false);
}

setupAttachmentComposer({
    uploadUrl: function () {
        return '/chat/' + encodeURIComponent(activeChatUsername) + '/attachment';
    },
    onUploadStart: function () {
        setComposerEnabled(false);
    },
    onUploadEnd: function () {
        setComposerEnabled(true);
    },
    onError: showComposerError
});

stompClient.connect({}, function () {
    if (messageForm) {
        setComposerEnabled(true);
    }
    stompClient.subscribe('/user/queue/messages', function (payload) {
        handleIncomingMessage(JSON.parse(payload.body));
    });
    stompClient.subscribe('/user/queue/typing', function (payload) {
        handlePeerTyping(JSON.parse(payload.body));
    });
    stompClient.subscribe('/user/queue/read-receipts', function (payload) {
        handleReadReceipt(JSON.parse(payload.body));
    });
    stompClient.subscribe('/user/queue/unread-update', function (payload) {
        handleUnreadUpdate(JSON.parse(payload.body));
    });
    stompClient.subscribe('/user/queue/presence', function (payload) {
        handlePresence(JSON.parse(payload.body));
    });
    stompClient.subscribe('/user/queue/conversation-deleted', function (payload) {
        handleConversationDeleted(JSON.parse(payload.body));
    });
    stompClient.subscribe('/user/queue/reactions', function (payload) {
        handleReactionEvent(JSON.parse(payload.body));
    });

    if (activeChatUsername) {
        sendMarkAsRead();
    }
}, function (error) {
    console.error('WebSocket connection error:', error);
    if (messageForm) {
        setComposerEnabled(false);
    }
});

if (messageForm) {
    messageForm.addEventListener('submit', function (event) {
        event.preventDefault();
        var content = messageInput.value.trim();
        var pendingFile = window.AttachmentPending ? AttachmentPending.getFile() : null;
        if (!stompClient.connected || !activeChatUsername || voiceRecording) {
            return;
        }
        if (!content && !pendingFile) {
            return;
        }

        stopLocalTyping();

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

        var payload = {
            receiverUsername: activeChatUsername,
            content: content
        };
        if (window.E2E && E2E.isReadyFor(activeChatUsername)) {
            payload.content = E2E.encryptFor(activeChatUsername, content);
        }
        if (replyState && replyState.messageId) {
            payload.replyToMessageId = replyState.messageId;
        }
        stompClient.send('/app/chat.send', {}, JSON.stringify(payload));
        messageInput.value = '';
        messageInput.focus();
        clearReplyPreview();
    });

    messageInput.addEventListener('input', notifyLocalTyping);
    messageInput.addEventListener('blur', stopLocalTyping);
    window.addEventListener('beforeunload', stopLocalTyping);

    hidePeerTyping();
    requestAnimationFrame(function () { jumpToBottom(); });
    if (window.addEventListener) {
        window.addEventListener('load', function () {
            requestAnimationFrame(function () { jumpToBottom(); });
        });
    }
    messageInput.focus();
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
                if (contentEl) {
                    contentText = contentEl.textContent;
                } else if (row.querySelector('.voice-player')) {
                    contentText = 'Голосовое сообщение';
                }
                var senderEl = row.querySelector('.sender');
                var senderName = senderEl ? senderEl.textContent : currentUsername;
                showReplyPreview(messageId, senderName, contentText);
            }
        } else if (action === 'pin') {
            var pinRow = messagesContainer.querySelector('[data-message-id="' + messageId + '"]');
            var isPinned = pinRow ? !!pinRow.querySelector('.pin-badge') : false;
            stompClient.send('/app/chat.pin', {}, JSON.stringify({ messageId: Number(messageId), pinned: !isPinned }));
        } else if (action === 'favorite') {
            if (window.Favorites) {
                window.Favorites.toggle(messageId, 'DIRECT');
            }
        } else if (action === 'edit') {
            startInlineEdit(messageId);
        } else if (action === 'forward') {
            if (window.showForwardDialog) {
                showForwardDialog(function (targetType, target) {
                    if (targetType === 'group') {
                        stompClient.send('/app/group.forward', {}, JSON.stringify({
                            sourceType: 'DIRECT',
                            sourceMessageId: Number(messageId),
                            groupId: Number(target)
                        }));
                    } else {
                        stompClient.send('/app/chat.forward', {}, JSON.stringify({
                            sourceType: 'DIRECT',
                            sourceMessageId: Number(messageId),
                            targetUsername: target
                        }));
                    }
                });
            }
        } else if (action === 'delete-me') {
            var rowMe = messagesContainer.querySelector('[data-message-id="' + messageId + '"]');
            var labelMe = rowMe ? getMessageLabel(rowMe) : 'сообщение';
            showConfirmDialog('Вы точно хотите удалить «' + labelMe + '» для себя?', function () {
                stompClient.send('/app/chat.delete', {}, JSON.stringify({ messageId: Number(messageId), mode: 'me' }));
                var msgRow = messagesContainer.querySelector('[data-message-id="' + messageId + '"]');
                if (msgRow) {
                    msgRow.remove();
                }
            });
        } else if (action === 'delete-all') {
            var rowAll = messagesContainer.querySelector('[data-message-id="' + messageId + '"]');
            var labelAll = rowAll ? getMessageLabel(rowAll) : 'сообщение';
            showConfirmDialog('Вы точно хотите удалить «' + labelAll + '» для всех?', function () {
                stompClient.send('/app/chat.delete', {}, JSON.stringify({ messageId: Number(messageId), mode: 'everyone' }));
                var delRow = messagesContainer.querySelector('[data-message-id="' + messageId + '"]');
                if (delRow) {
                    delRow.remove();
                }
            });
        }
    });
}

function startInlineEdit(messageId) {
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
        stompClient.send('/app/chat.edit', {}, JSON.stringify({
            messageId: Number(messageId),
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

function getMessageLabel(row) {
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
    });
}

document.addEventListener('keydown', function (e) {
    if (e.key === 'Escape') {
        hideContextMenu();
        clearReplyPreview();
    }
});

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
            sendReaction(messageId, emoji);
        }
    });
    messagesContainer.querySelectorAll('.message-row').forEach(function (row) {
        var id = row.getAttribute('data-message-id');
        if (id && initialReactions && initialReactions[String(id)]) {
            ReactionsUI.renderBar(row, id, initialReactions[String(id)], currentUsername, function (emoji) {
                sendReaction(id, emoji);
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
            if (!stickerCode || !stompClient.connected || !activeChatUsername || voiceRecording) {
                return;
            }
            var payload = { receiverUsername: activeChatUsername, stickerCode: stickerCode };
            stompClient.send('/app/chat.send', {}, JSON.stringify(payload));
            hidePeerTyping();
            clearReplyPreview();
        }
    });
}
