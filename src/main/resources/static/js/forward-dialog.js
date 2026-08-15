(function () {
    'use strict';

    var overlay = null;

    function hide() {
        if (overlay) {
            overlay.remove();
            overlay = null;
        }
    }

    function loadLists() {
        return Promise.all([
            fetch('/api/conversations').then(function (r) { return r.ok ? r.json() : []; }).catch(function () { return []; }),
            fetch('/api/groups').then(function (r) { return r.ok ? r.json() : []; }).catch(function () { return []; })
        ]).then(function (results) {
            return { conversations: results[0], groups: results[1] };
        });
    }

    function escapeHtml(text) {
        var div = document.createElement('div');
        div.textContent = text == null ? '' : String(text);
        return div.innerHTML;
    }

    function showForwardDialog(onPick) {
        hide();

        overlay = document.createElement('div');
        overlay.className = 'forward-overlay';

        var dialog = document.createElement('div');
        dialog.className = 'forward-dialog';
        dialog.setAttribute('role', 'dialog');
        dialog.setAttribute('aria-modal', 'true');

        var header = document.createElement('div');
        header.className = 'forward-header';
        var title = document.createElement('div');
        title.className = 'forward-title';
        title.textContent = 'Переслать сообщение';
        var closeBtn = document.createElement('button');
        closeBtn.type = 'button';
        closeBtn.className = 'forward-close';
        closeBtn.setAttribute('aria-label', 'Закрыть');
        closeBtn.textContent = '×';
        header.appendChild(title);
        header.appendChild(closeBtn);
        dialog.appendChild(header);

        var body = document.createElement('div');
        body.className = 'forward-body';
        body.innerHTML = '<div class="forward-loading">Загрузка…</div>';
        dialog.appendChild(body);

        overlay.appendChild(dialog);
        document.body.appendChild(overlay);

        function cleanup() {
            closeBtn.removeEventListener('click', onClose);
            overlay.removeEventListener('click', onOverlayClick);
            document.removeEventListener('keydown', onKey);
            hide();
        }

        function onClose() {
            cleanup();
        }

        function onOverlayClick(e) {
            if (e.target === overlay) {
                cleanup();
            }
        }

        function onKey(e) {
            if (e.key === 'Escape') {
                cleanup();
            }
        }

        closeBtn.addEventListener('click', onClose);
        overlay.addEventListener('click', onOverlayClick);
        document.addEventListener('keydown', onKey);

        loadLists().then(function (lists) {
            if (!overlay) return;
            var html = '';

            var users = lists.conversations.filter(function (c) {
                return c && c.partnerUsername;
            });
            html += '<div class="forward-section">';
            html += '<div class="forward-section-title">Диалоги</div>';
            if (users.length === 0) {
                html += '<div class="forward-empty">Нет диалогов</div>';
            } else {
                html += '<div class="forward-list">';
                users.forEach(function (c) {
                    html += '<button type="button" class="forward-item" data-type="user" data-target="' + escapeHtml(c.partnerUsername) + '">' +
                        '<span class="forward-item-avatar">' + escapeHtml((c.partnerUsername || '?').charAt(0).toUpperCase()) + '</span>' +
                        '<span class="forward-item-name">' + escapeHtml(c.partnerUsername) + '</span>' +
                        '</button>';
                });
                html += '</div>';
            }
            html += '</div>';

            var groups = lists.groups.filter(function (g) {
                return g && g.groupName;
            });
            html += '<div class="forward-section">';
            html += '<div class="forward-section-title">Группы</div>';
            if (groups.length === 0) {
                html += '<div class="forward-empty">Нет групп</div>';
            } else {
                html += '<div class="forward-list">';
                groups.forEach(function (g) {
                    html += '<button type="button" class="forward-item" data-type="group" data-target="' + g.groupId + '">' +
                        '<span class="forward-item-avatar">' + escapeHtml((g.groupName || '?').charAt(0).toUpperCase()) + '</span>' +
                        '<span class="forward-item-name">' + escapeHtml(g.groupName) + '</span>' +
                        '</button>';
                });
                html += '</div>';
            }
            html += '</div>';

            body.innerHTML = html;

            body.querySelectorAll('.forward-item').forEach(function (item) {
                item.addEventListener('click', function () {
                    var type = item.getAttribute('data-type');
                    var target = item.getAttribute('data-target');
                    cleanup();
                    if (onPick) {
                        onPick(type, target);
                    }
                });
            });
        });
    }

    window.showForwardDialog = showForwardDialog;
})();
