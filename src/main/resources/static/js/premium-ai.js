(function () {
    'use strict';

    var button = document.getElementById('ai-assistant-button');
    var panel = document.getElementById('ai-assistant-panel');
    var panelClose = document.getElementById('ai-assistant-close');
    var messagesEl = document.getElementById('ai-assistant-messages');
    var form = document.getElementById('ai-assistant-form');
    var input = document.getElementById('ai-assistant-input');
    var translateToggle = document.getElementById('ai-auto-translate');
    var chatMessages = document.getElementById('messages');
    if (!button || !panel || !messagesEl || !form || !input) {
        return;
    }

    function getChatContext() {
        if (typeof activeGroupId === 'number' && activeGroupId) {
            return { type: 'group', target: String(activeGroupId), key: 'group-' + activeGroupId };
        }
        if (typeof activeChatUsername === 'string' && activeChatUsername) {
            return { type: 'direct', target: activeChatUsername, key: 'direct-' + activeChatUsername };
        }
        return null;
    }

    function escapeHtml(text) {
        return String(text == null ? '' : text)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;');
    }

    function addBubble(text, kind) {
        var row = document.createElement('div');
        row.className = 'ai-msg ai-msg-' + (kind || 'ai');
        var b = document.createElement('div');
        b.className = 'ai-msg-bubble';
        b.textContent = text;
        row.appendChild(b);
        messagesEl.appendChild(row);
        messagesEl.scrollTop = messagesEl.scrollHeight;
        return b;
    }

    function openPanel() {
        panel.hidden = false;
        var app = panel.closest('.chat-app');
        if (app) {
            app.classList.add('ai-assistant-open');
        }
        if (!panel.getAttribute('data-welcomed')) {
            panel.setAttribute('data-welcomed', '1');
            addBubble('Я — AI-ассистент. Попробуй: «найди про файлы», «переведи hello», «напиши ответ», «расшифруй голосовые».', 'ai');
        }
        setTimeout(function () { input.focus(); }, 30);
    }

    function closePanel() {
        panel.hidden = true;
        var app = panel.closest('.chat-app');
        if (app) {
            app.classList.remove('ai-assistant-open');
        }
    }

    button.addEventListener('click', function () {
        panel.hidden ? openPanel() : closePanel();
    });
    if (panelClose) {
        panelClose.addEventListener('click', closePanel);
    }

    form.addEventListener('submit', function (e) {
        e.preventDefault();
        var q = (input.value || '').trim();
        if (!q) {
            return;
        }
        var ctx = getChatContext();
        if (!ctx) {
            addBubble('Сначала откройте чат.', 'ai');
            return;
        }
        input.value = '';
        addBubble(q, 'me');
        var pending = addBubble('Думаю…', 'ai');
        fetch('/api/ai/ask', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ chatType: ctx.type, chatWith: ctx.target, query: q })
        })
            .then(function (r) {
                return r.json().then(function (d) {
                    return { ok: r.ok, data: d };
                });
            })
            .then(function (res) {
                if (!res.ok || !res.data) {
                    pending.textContent = 'Нужен Premium: ' + (res.data && res.data.error ? res.data.error : 'оформите в /premium');
                    pending.innerHTML = escapeHtml(res.data && res.data.error ? res.data.error : 'Нужен Premium') +
                        ' <a href="/premium" class="ai-msg-link">Оформить</a>';
                    return;
                }
                pending.textContent = res.data.reply || '';
            })
            .catch(function () {
                pending.textContent = 'Ошибка сети. Попробуйте ещё раз.';
            });
    });

    var translateKey = (getChatContext() || {}).key || 'translate';

    function storedTranslate() {
        try {
            return localStorage.getItem('ai_translate_' + translateKey) === '1';
        } catch (e) {
            return false;
        }
    }

    function addTag(b) {
        if (b.querySelector('.ai-translated-tag')) {
            return;
        }
        var tag = document.createElement('span');
        tag.className = 'ai-translated-tag';
        tag.textContent = 'переведено автоматически';
        b.appendChild(tag);
    }

    function bubbleOriginal(b) {
        var c = b.querySelector ? b.querySelector('.content') : null;
        var target = c || b;
        var original = target.getAttribute('data-original');
        if (original == null) {
            original = target.textContent.trim();
            target.setAttribute('data-original', original);
        }
        return original;
    }

    function translateBubble(b) {
        if (!b || b.getAttribute('data-translated')) {
            return;
        }
        var c = b.querySelector ? b.querySelector('.content') : null;
        var target = c || b;
        if (target.classList && target.classList.contains('e2e-pending')) {
            return;
        }
        if (b.querySelector('.ai-translated-tag')) {
            return;
        }
        var original = bubbleOriginal(b);
        if (!original || original === '🔒 Зашифрованное сообщение') {
            return;
        }
        b.setAttribute('data-translated', 'pending');
        fetch('/api/ai/translate', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ text: original, to: 'ru' })
        })
            .then(function (r) {
                return r.json().catch(function () { return {}; });
            })
            .then(function (d) {
                if (d && d.translated && d.translated.trim() && d.translated.trim() !== original.trim()) {
                    b.setAttribute('data-translated', '1');
                    target.textContent = d.translated.trim();
                    addTag(b);
                } else {
                    b.removeAttribute('data-translated');
                }
            })
            .catch(function () {
                b.removeAttribute('data-translated');
            });
    }

    function setTranslateOn(on) {
        try {
            localStorage.setItem('ai_translate_' + translateKey, on ? '1' : '0');
        } catch (e) {
        }
        translateToggle.checked = on;
        if (chatMessages) {
            chatMessages.classList.toggle('ai-translate-on', on);
        }
        if (on && chatMessages) {
            chatMessages.querySelectorAll('.message-row.incoming .message-bubble').forEach(function (b) {
                translateBubble(b);
            });
        }
    }

    if (translateToggle) {
        translateToggle.addEventListener('change', function () {
            setTranslateOn(translateToggle.checked);
        });
        setTranslateOn(storedTranslate());
    }

    if (chatMessages && translateToggle) {
        var observer = new MutationObserver(function (mutations) {
            if (!translateToggle.checked) {
                return;
            }
            mutations.forEach(function (m) {
                m.addedNodes.forEach(function (node) {
                    if (node.nodeType !== 1) {
                        return;
                    }
                    var rows = node.classList && node.classList.contains('message-row')
                        ? [node]
                        : node.querySelectorAll ? node.querySelectorAll('.message-row') : [];
                    rows.forEach(function (row) {
                        if (!row.classList.contains('incoming')) {
                            return;
                        }
                        var b = row.querySelector('.message-bubble');
                        if (b) {
                            translateBubble(b);
                        }
                    });
                });
            });
        });
        observer.observe(chatMessages, { childList: true, subtree: true });
    }
})();