const messagesContainer = document.getElementById('messages');
const messageForm = document.getElementById('message-form');
const messageInput = document.getElementById('message-input');
const sendButton = document.getElementById('send-button');
const groupIdInput = document.getElementById('group-id');

const displayedMessageIds = new Set();
if (messagesContainer) {
    messagesContainer.querySelectorAll('[data-message-id]').forEach(function (el) {
        displayedMessageIds.add(el.getAttribute('data-message-id'));
    });
}

function hideEmptyState() {
    const emptyState = messagesContainer && messagesContainer.querySelector('.chat-empty');
    if (emptyState) {
        emptyState.remove();
    }
}

function scrollToBottom() {
    if (messagesContainer) {
        messagesContainer.scrollTop = messagesContainer.scrollHeight;
    }
}

function appendGroupMessage(message) {
    if (!messagesContainer || message.groupId !== activeGroupId) {
        return;
    }

    if (message.id && displayedMessageIds.has(String(message.id))) {
        return;
    }
    if (message.id) {
        displayedMessageIds.add(String(message.id));
    }

    hideEmptyState();

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

    bubbleEl.appendChild(contentEl);
    bubbleEl.appendChild(metaEl);
    rowEl.appendChild(bubbleEl);
    messagesContainer.appendChild(rowEl);
    scrollToBottom();
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

if (messageForm) {
    messageForm.addEventListener('submit', function (event) {
        event.preventDefault();
        const content = messageInput.value.trim();
        const groupId = groupIdInput ? parseInt(groupIdInput.value, 10) : activeGroupId;
        if (!content || !stompClient.connected || !groupId) {
            return;
        }

        stompClient.send('/app/group.send', {}, JSON.stringify({
            groupId: groupId,
            content: content
        }));
        messageInput.value = '';
        messageInput.focus();
    });

    scrollToBottom();
    messageInput.focus();
}
