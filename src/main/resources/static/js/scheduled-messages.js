(function () {
    'use strict';

    var modal = document.getElementById('scheduled-modal');
    if (!modal) {
        return;
    }

    var modalClose = document.getElementById('scheduled-close');
    var sidebarToggle = document.getElementById('scheduled-sidebar-toggle');
    var contentEl = document.getElementById('scheduled-content');
    var datetimeEl = document.getElementById('scheduled-datetime');
    var submitBtn = document.getElementById('scheduled-submit');
    var errorEl = document.getElementById('scheduled-error');
    var targetNameEl = document.getElementById('scheduled-target-name');
    var listEl = document.getElementById('scheduled-list-items');
    var countEl = document.getElementById('scheduled-count');

    var receiverInput = document.getElementById('receiver-username');
    var groupIdInput = document.getElementById('group-id');
    var messageInput = document.getElementById('message-input');

    function currentTarget() {
        if (receiverInput && receiverInput.value) {
            return {
                type: 'DIRECT',
                name: '@' + receiverInput.value,
                receiverUsername: receiverInput.value
            };
        }
        if (groupIdInput && groupIdInput.value) {
            var activeName = document.querySelector('.group-item.is-active .conversation-name');
            return {
                type: 'GROUP',
                name: activeName ? activeName.textContent.trim() : 'Группа',
                groupId: groupIdInput.value
            };
        }
        return null;
    }

    function pad(n) {
        return (n < 10 ? '0' : '') + n;
    }

    function minDatetimeValue() {
        var d = new Date(Date.now() + 60 * 1000);
        return d.getFullYear() + '-' + pad(d.getMonth() + 1) + '-' + pad(d.getDate()) +
            'T' + pad(d.getHours()) + ':' + pad(d.getMinutes());
    }

    function injectClockButton() {
        if (document.getElementById('schedule-button')) {
            return;
        }
        if (!currentTarget()) {
            return;
        }
        var sendBtn = document.getElementById('send-button');
        if (!sendBtn) {
            return;
        }
        var btn = document.createElement('button');
        btn.type = 'button';
        btn.id = 'schedule-button';
        btn.className = 'composer-attach composer-schedule';
        btn.setAttribute('aria-label', 'Отложить сообщение');
        btn.setAttribute('title', 'Отложить сообщение');
        btn.innerHTML = '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" aria-hidden="true">' +
            '<circle cx="12" cy="12" r="9" stroke="currentColor" stroke-width="2"/>' +
            '<path d="M12 7v5l3 2" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>' +
            '</svg>';
        sendBtn.parentNode.insertBefore(btn, sendBtn);
        btn.addEventListener('click', openModal);
    }

    function openModal() {
        var target = currentTarget();
        targetNameEl.textContent = target ? target.name : '—';
        if (messageInput && messageInput.value.trim()) {
            contentEl.value = messageInput.value.trim();
        }
        datetimeEl.value = minDatetimeValue();
        datetimeEl.min = minDatetimeValue();
        errorEl.hidden = true;
        modal.hidden = false;
        document.body.classList.add('favorites-modal-open');
        loadList();
        contentEl.focus();
    }

    function closeModal() {
        modal.hidden = true;
        document.body.classList.remove('favorites-modal-open');
    }

    function showError(msg) {
        errorEl.textContent = msg;
        errorEl.hidden = false;
    }

    function submit() {
        var target = currentTarget();
        if (!target) {
            showError('Откройте чат, чтобы отложить сообщение');
            return;
        }
        var content = contentEl.value.trim();
        if (!content) {
            showError('Введите текст сообщения');
            return;
        }
        var raw = datetimeEl.value;
        if (!raw) {
            showError('Выберите время отправки');
            return;
        }
        var scheduleAt = new Date(raw).getTime();
        if (!(scheduleAt > Date.now())) {
            showError('Время должно быть в будущем');
            return;
        }

        var payload = {
            targetType: target.type,
            content: content,
            scheduleAt: scheduleAt
        };
        if (target.receiverUsername) {
            payload.receiverUsername = target.receiverUsername;
        } else {
            payload.groupId = target.groupId;
        }
        if (window.replyState && window.replyState.messageId) {
            payload.replyToMessageId = window.replyState.messageId;
        }

        submitBtn.disabled = true;
        fetch('/api/scheduled', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        }).then(function (r) {
            return r.json().catch(function () {
                return {};
            }).then(function (data) {
                return { ok: r.ok, data: data };
            });
        }).then(function (res) {
            if (!res.ok) {
                throw new Error(res.data.error || 'Не удалось отложить сообщение');
            }
            if (messageInput) {
                messageInput.value = '';
            }
            contentEl.value = '';
            errorEl.hidden = true;
            return loadList();
        }).catch(function (e) {
            showError(e.message || 'Ошибка');
        }).finally(function () {
            submitBtn.disabled = false;
        });
    }

    function formatTime(epoch) {
        var d = new Date(epoch);
        return pad(d.getDate()) + '.' + pad(d.getMonth() + 1) + '.' + d.getFullYear() +
            ' ' + pad(d.getHours()) + ':' + pad(d.getMinutes());
    }

    function escapeHtml(s) {
        var d = document.createElement('div');
        d.textContent = s == null ? '' : String(s);
        return d.innerHTML;
    }

    function renderList(items) {
        if (countEl) {
            countEl.textContent = items.length;
            countEl.hidden = items.length === 0;
        }
        if (!items.length) {
            listEl.innerHTML = '<div class="scheduled-empty">Нет отложенных сообщений</div>';
            return;
        }
        var html = '';
        for (var i = 0; i < items.length; i++) {
            var item = items[i];
            html += '<div class="scheduled-item">' +
                '<div class="scheduled-item-top">' +
                '<span class="scheduled-item-target">' + escapeHtml(item.targetName) + '</span>' +
                '<span class="scheduled-item-time">' + formatTime(item.scheduleAt) + '</span>' +
                '</div>' +
                '<div class="scheduled-item-content">' + escapeHtml(item.content) + '</div>' +
                '<button type="button" class="scheduled-item-cancel" data-id="' + item.id + '">Отменить</button>' +
                '</div>';
        }
        listEl.innerHTML = html;
    }

    function loadList() {
        return fetch('/api/scheduled').then(function (r) {
            return r.json();
        }).then(function (items) {
            renderList(items || []);
        }).catch(function () {
            listEl.innerHTML = '';
        });
    }

    function cancelItem(id) {
        fetch('/api/scheduled/' + id, { method: 'DELETE' })
            .then(function (r) {
                return r.json();
            })
            .then(function (data) {
                if (!data.ok) {
                    throw new Error('Не удалось отменить');
                }
                return loadList();
            }).catch(function (e) {
                showError(e.message);
            });
    }

    if (modalClose) {
        modalClose.addEventListener('click', closeModal);
    }
    if (sidebarToggle) {
        sidebarToggle.addEventListener('click', openModal);
    }
    if (submitBtn) {
        submitBtn.addEventListener('click', submit);
    }
    if (listEl) {
        listEl.addEventListener('click', function (e) {
            var btn = e.target.closest('.scheduled-item-cancel');
            if (btn) {
                cancelItem(btn.getAttribute('data-id'));
            }
        });
    }
    modal.addEventListener('click', function (e) {
        if (e.target === modal) {
            closeModal();
        }
    });
    document.addEventListener('keydown', function (e) {
        if (e.key === 'Escape' && !modal.hidden) {
            closeModal();
        }
    });

    injectClockButton();
    if (countEl) {
        loadList();
    }
})();
