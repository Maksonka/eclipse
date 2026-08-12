/* Реакции-эмодзи на сообщениях (ShadowVibe)
   Общий модуль: палитра по клику на пузырь + лента бейджей реакций.
   Интеграция: chat-direct.js / chat-group.js / watch.js. */
(function () {
    var BASE_EMOJIS = ['👍', '❤️', '😂', '😮', '😢', '🔥', '🎉', '👏'];

    function normReactions(reactions) {
        if (!reactions || typeof reactions !== 'object') {
            return {};
        }
        var out = {};
        Object.keys(reactions).forEach(function (emoji) {
            var users = reactions[emoji];
            if (Array.isArray(users) && users.length) {
                out[emoji] = users.slice();
            }
        });
        return out;
    }

    function containsUser(users, username) {
        if (!Array.isArray(users)) {
            return false;
        }
        return users.indexOf(username) !== -1;
    }

    /* Рендер ленты бейджей реакций под пузырём. */
    function renderBar(row, messageId, reactions, username, onToggle) {
        var bar = row.querySelector(':scope > .message-reactions');
        if (bar) {
            bar.remove();
        }
        var norm = normReactions(reactions);
        var keys = Object.keys(norm);
        if (!keys.length) {
            return;
        }
        bar = document.createElement('div');
        bar.className = 'message-reactions';
        keys.forEach(function (emoji) {
            var users = norm[emoji];
            var badge = document.createElement('button');
            badge.type = 'button';
            badge.className = 'reaction-badge' + (containsUser(users, username) ? ' is-mine' : '');
            badge.setAttribute('data-emoji', emoji);
            badge.setAttribute('data-users', users.join(','));
            badge.setAttribute('title', users.join(', '));
            var emojiSpan = document.createElement('span');
            emojiSpan.className = 'reaction-badge-emoji';
            emojiSpan.textContent = emoji;
            var countSpan = document.createElement('span');
            countSpan.className = 'reaction-badge-count';
            countSpan.textContent = String(users.length);
            badge.appendChild(emojiSpan);
            badge.appendChild(countSpan);
            badge.addEventListener('click', function (e) {
                e.stopPropagation();
                if (onToggle) {
                    onToggle(emoji);
                }
            });
            bar.appendChild(badge);
        });
        row.appendChild(bar);
    }

    function hideAllPalettes() {
        document.querySelectorAll('.reaction-palette').forEach(function (p) {
            p.remove();
        });
    }

    /* Открыть палитру рядом с пузырём сообщения. */
    function openPalette(bubble, messageId, reactions, username, onPick) {
        hideAllPalettes();
        var row = bubble.closest('.message-row') || bubble;
        var norm = normReactions(reactions);
        var emojis = BASE_EMOJIS.slice();
        Object.keys(norm).forEach(function (e) {
            if (emojis.indexOf(e) === -1) {
                emojis.push(e);
            }
        });

        var palette = document.createElement('div');
        palette.className = 'reaction-palette';
        emojis.forEach(function (emoji) {
            var btn = document.createElement('button');
            btn.type = 'button';
            btn.className = 'reaction-palette-btn';
            btn.setAttribute('data-emoji', emoji);
            btn.textContent = emoji;
            btn.title = emoji;
            btn.addEventListener('click', function (e) {
                e.stopPropagation();
                hideAllPalettes();
                if (onPick) {
                    onPick(emoji);
                }
            });
            palette.appendChild(btn);
        });

        var bubbleRect = bubble.getBoundingClientRect();
        palette.style.position = 'fixed';
        palette.style.left = Math.max(8, Math.min(bubbleRect.left, window.innerWidth - 320)) + 'px';
        palette.style.top = Math.max(8, bubbleRect.top - 46) + 'px';
        document.body.appendChild(palette);
        return palette;
    }

    function handleBubbleClick(e, opts) {
        var target = e.target;
        if (target.closest('a, button, audio, .reaction-palette, .message-reactions, input, textarea, .voice-player, .sticker-image, .attachment-image-link, .attachment-video, .reply-block')) {
            return;
        }
        var bubbleSelector = opts.bubbleSelector || '.message-bubble';
        var bubble = target.closest(bubbleSelector);
        if (!bubble) {
            return;
        }
        var row = bubble.closest('.message-row') || bubble;
        var messageId = opts.getMessageId(row);
        if (messageId == null) {
            return;
        }
        openPalette(bubble, messageId, opts.getReactions(messageId), opts.username, function (emoji) {
            if (opts.onPick) {
                opts.onPick(messageId, emoji);
            }
        });
    }

    /* Привязать поведение к контейнеру с сообщениями. */
    function initContainer(container, opts) {
        if (!container) {
            return;
        }
        container.addEventListener('click', function (e) {
            handleBubbleClick(e, opts);
        });
        document.addEventListener('click', function (e) {
            if (!e.target.closest('.reaction-palette')) {
                hideAllPalettes();
            }
        });
        document.addEventListener('keydown', function (e) {
            if (e.key === 'Escape') {
                hideAllPalettes();
            }
        });
    }

    window.ReactionsUI = {
        initContainer: initContainer,
        renderBar: renderBar,
        hideAllPalettes: hideAllPalettes,
        BASE_EMOJIS: BASE_EMOJIS
    };
})();
