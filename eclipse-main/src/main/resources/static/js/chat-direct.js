const conversationsList = document.getElementById('conversations-list');
const messagesContainer = document.getElementById('messages');
const messageForm = document.getElementById('message-form');
const messageInput = document.getElementById('message-input');
const sendButton = document.getElementById('send-button');
const chatStatus = document.getElementById('chat-status');
const typingIndicator = document.getElementById('typing-indicator');

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

function upsertConversation(message, incrementUnread) {
    if (!conversationsList) {
        return;
    }

    const partner = getPartnerUsername(message);
    const isOutgoing = message.senderUsername === currentUsername;
    const preview = truncatePreview(message.content);
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

function createReadReceiptElement(isRead) {
    const receipt = document.createElement('span');
    receipt.className = 'read-receipt' + (isRead ? ' is-read' : '');
    receipt.textContent = '✓✓';
    receipt.title = isRead ? 'Прочитано' : 'Доставлено';
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

function appendMessage(message) {
    if (!messagesContainer || !isRelevantChat(message)) {
        return;
    }

    if (message.id && displayedMessageIds.has(String(message.id))) {
        return;
    }
    if (message.id) {
        displayedMessageIds.add(String(message.id));
    }

    hideEmptyState();
    hidePeerTyping();

    const isOutgoing = message.senderUsername === currentUsername;
    const rowEl = document.createElement('div');
    rowEl.className = 'message-row ' + (isOutgoing ? 'outgoing' : 'incoming');
    if (message.id) {
        rowEl.setAttribute('data-message-id', message.id);
    }

    const bubbleEl = document.createElement('div');
    bubbleEl.className = 'message-bubble';

    if (!isOutgoing) {
        const senderEl = document.createElement('span');
        senderEl.className = 'sender';
        senderEl.textContent = message.senderUsername;
        bubbleEl.appendChild(senderEl);
    }

    const contentEl = document.createElement('span');
    contentEl.className = 'content';
    contentEl.textContent = message.content;

    const metaEl = document.createElement('span');
    metaEl.className = 'message-meta';

    const timeEl = document.createElement('time');
    timeEl.className = 'message-time';
    timeEl.textContent = message.timestamp || '';
    metaEl.appendChild(timeEl);

    if (isOutgoing) {
        metaEl.appendChild(createReadReceiptElement(!!message.read));
    }

    bubbleEl.appendChild(contentEl);
    bubbleEl.appendChild(metaEl);
    rowEl.appendChild(bubbleEl);
    messagesContainer.appendChild(rowEl);
    scrollToBottom();
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

    upsertConversation(message, isIncoming && !inActiveChat);

    if (inActiveChat) {
        appendMessage(message);
        if (isIncoming) {
            sendMarkAsRead();
        }
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

function handlePresence(event) {
    if (!event || !event.username) {
        return;
    }
    setPartnerOnline(event.username, !!event.online);
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
}

if (messageForm) {
    setComposerEnabled(false);
}

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
        const content = messageInput.value.trim();
        if (!content || !stompClient.connected || !activeChatUsername) {
            return;
        }

        stopLocalTyping();

        stompClient.send('/app/chat.send', {}, JSON.stringify({
            receiverUsername: activeChatUsername,
            content: content
        }));
        messageInput.value = '';
        messageInput.focus();
    });

    messageInput.addEventListener('input', notifyLocalTyping);
    messageInput.addEventListener('blur', stopLocalTyping);
    window.addEventListener('beforeunload', stopLocalTyping);

    hidePeerTyping();
    scrollToBottom();
    messageInput.focus();
}
