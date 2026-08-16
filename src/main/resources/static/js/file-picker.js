(function () {
    'use strict';

    function bind(picker) {
        var input = picker.querySelector('.file-picker-input');
        if (!input) {
            return;
        }
        var nameEl = picker.querySelector('.file-picker-name');
        var previewEl = picker.querySelector('.file-picker-preview');
        input.addEventListener('change', function () {
            var file = input.files && input.files[0];
            if (file) {
                picker.classList.add('has-file');
                if (nameEl) {
                    nameEl.textContent = file.name;
                }
                if (previewEl && file.type.indexOf('image/') === 0) {
                    if (previewEl.dataset.objectUrl) {
                        URL.revokeObjectURL(previewEl.dataset.objectUrl);
                    }
                    var url = URL.createObjectURL(file);
                    previewEl.dataset.objectUrl = url;
                    previewEl.src = url;
                }
            } else {
                picker.classList.remove('has-file');
                if (nameEl) {
                    nameEl.textContent = '';
                }
            }
        });
    }

    function init() {
        document.querySelectorAll('.file-picker').forEach(bind);
    }

    document.addEventListener('DOMContentLoaded', init);
})();
