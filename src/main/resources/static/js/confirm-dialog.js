(function () {
    'use strict';

    var overlay = null;

    function hide() {
        if (overlay) {
            overlay.remove();
            overlay = null;
        }
    }

    function showConfirm(message, onConfirm) {
        hide();

        overlay = document.createElement('div');
        overlay.className = 'confirm-overlay';

        var dialog = document.createElement('div');
        dialog.className = 'confirm-dialog';
        dialog.setAttribute('role', 'alertdialog');
        dialog.setAttribute('aria-modal', 'true');

        var text = document.createElement('p');
        text.className = 'confirm-text';
        text.textContent = message;
        dialog.appendChild(text);

        var actions = document.createElement('div');
        actions.className = 'confirm-actions';

        var cancelBtn = document.createElement('button');
        cancelBtn.type = 'button';
        cancelBtn.className = 'confirm-btn confirm-cancel';
        cancelBtn.textContent = 'Отмена';
        actions.appendChild(cancelBtn);

        var okBtn = document.createElement('button');
        okBtn.type = 'button';
        okBtn.className = 'confirm-btn confirm-ok';
        okBtn.textContent = 'Удалить';
        actions.appendChild(okBtn);

        dialog.appendChild(actions);
        overlay.appendChild(dialog);
        document.body.appendChild(overlay);

        function cleanup() {
            cancelBtn.removeEventListener('click', onCancel);
            okBtn.removeEventListener('click', onOk);
            overlay.removeEventListener('click', onOverlayClick);
            document.removeEventListener('keydown', onKey);
            hide();
        }

        function onCancel() {
            cleanup();
        }

        function onOk() {
            cleanup();
            if (onConfirm) {
                onConfirm();
            }
        }

        function onOverlayClick(e) {
            if (e.target === overlay) {
                cleanup();
            }
        }

        function onKey(e) {
            if (e.key === 'Escape') {
                cleanup();
            } else if (e.key === 'Enter') {
                onOk();
            }
        }

        cancelBtn.addEventListener('click', onCancel);
        okBtn.addEventListener('click', onOk);
        overlay.addEventListener('click', onOverlayClick);
        document.addEventListener('keydown', onKey);

        setTimeout(function () {
            okBtn.focus();
        }, 0);
    }

    document.addEventListener('submit', function (event) {
        var form = event.target;
        if (form && form.matches && form.matches('form[data-confirm-message]')) {
            var message = form.getAttribute('data-confirm-message');
            if (!message) {
                return;
            }
            event.preventDefault();
            showConfirm(message, function () {
                form.submit();
            });
        }
    });

    window.showConfirmDialog = showConfirm;
})();
