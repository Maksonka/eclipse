window.StickerUI = (function () {
    var opts = null;
    var packs = [];
    var activePackId = null;
    var panel = null;
    var tabsEl = null;
    var gridEl = null;
    var addForm = null;
    var btn = null;
    var packSelect = null;
    var nameInput = null;

    function createButton(insertBeforeEl) {
        btn = document.createElement('button');
        btn.type = 'button';
        btn.className = 'composer-sticker';
        btn.title = 'Стикеры';
        btn.setAttribute('aria-label', 'Стикеры');
        btn.innerHTML =
            '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" aria-hidden="true">' +
            '<circle cx="12" cy="12" r="10" stroke="currentColor" stroke-width="1.8"/>' +
            '<path d="M8 14.5c1.2 1.3 2.6 2 4 2s2.8-.7 4-2" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/>' +
            '<circle cx="9" cy="9.5" r="1.2" fill="currentColor"/>' +
            '<circle cx="15" cy="9.5" r="1.2" fill="currentColor"/>' +
            '</svg>';
        btn.addEventListener('click', function (e) {
            e.preventDefault();
            togglePanel();
        });
        insertBeforeEl.parentNode.insertBefore(btn, insertBeforeEl);
    }

    function buildPanel() {
        panel = document.createElement('div');
        panel.className = 'sticker-panel';
        panel.hidden = true;

        var header = document.createElement('div');
        header.className = 'sticker-panel-header';

        tabsEl = document.createElement('div');
        tabsEl.className = 'sticker-tabs';

        var closeBtn = document.createElement('button');
        closeBtn.type = 'button';
        closeBtn.className = 'sticker-close';
        closeBtn.setAttribute('aria-label', 'Закрыть');
        closeBtn.textContent = '\u00d7';
        closeBtn.addEventListener('click', closePanel);

        header.appendChild(tabsEl);
        header.appendChild(closeBtn);

        gridEl = document.createElement('div');
        gridEl.className = 'sticker-grid';

        addForm = buildAddForm();

        panel.appendChild(header);
        panel.appendChild(gridEl);
        panel.appendChild(addForm);
        document.body.appendChild(panel);
    }

    function buildAddForm() {
        var form = document.createElement('div');
        form.className = 'sticker-add';
        form.hidden = true;

        var title = document.createElement('div');
        title.className = 'sticker-add-title';
        title.textContent = 'Добавить стикеры';
        form.appendChild(title);

        var nameRow = document.createElement('div');
        nameRow.className = 'sticker-add-row';

        nameInput = document.createElement('input');
        nameInput.type = 'text';
        nameInput.className = 'sticker-add-input';
        nameInput.placeholder = 'Название нового набора...';
        nameInput.maxLength = 60;

        var createBtn = document.createElement('button');
        createBtn.type = 'button';
        createBtn.className = 'sticker-add-btn';
        createBtn.textContent = 'Создать набор';
        createBtn.addEventListener('click', createPack);

        nameRow.appendChild(nameInput);
        nameRow.appendChild(createBtn);
        form.appendChild(nameRow);

        var orRow = document.createElement('div');
        orRow.className = 'sticker-add-or';
        orRow.textContent = 'или добавьте в существующий:';
        form.appendChild(orRow);

        packSelect = document.createElement('select');
        packSelect.className = 'sticker-add-select';
        form.appendChild(packSelect);

        var fileRow = document.createElement('div');
        fileRow.className = 'sticker-add-row';

        var fileInput = document.createElement('input');
        fileInput.type = 'file';
        fileInput.id = 'sticker-file-input';
        fileInput.accept = 'image/png,image/jpeg,image/webp,image/gif';
        fileInput.multiple = true;

        var uploadBtn = document.createElement('button');
        uploadBtn.type = 'button';
        uploadBtn.className = 'sticker-add-btn';
        uploadBtn.textContent = 'Загрузить';
        uploadBtn.addEventListener('click', function () {
            uploadStickers(fileInput.files);
        });

        fileRow.appendChild(fileInput);
        fileRow.appendChild(uploadBtn);
        form.appendChild(fileRow);

        var hint = document.createElement('div');
        hint.className = 'sticker-add-hint';
        hint.textContent = 'Формат JPG, PNG, WEBP или GIF, до 3 МБ за стикер';
        form.appendChild(hint);

        var err = document.createElement('div');
        err.className = 'sticker-add-error';
        err.hidden = true;
        form.appendChild(err);

        return form;
    }

    function setAddError(text) {
        var err = addForm.querySelector('.sticker-add-error');
        if (text) {
            err.textContent = text;
            err.hidden = false;
        } else {
            err.hidden = true;
        }
    }

    function renderTabs() {
        tabsEl.innerHTML = '';
        packs.forEach(function (pack) {
            var tab = document.createElement('button');
            tab.type = 'button';
            tab.className = 'sticker-tab' + (pack.id === activePackId ? ' is-active' : '');
            tab.title = pack.name;
            if (pack.stickers && pack.stickers.length) {
                var img = document.createElement('img');
                img.src = pack.stickers[0].url;
                img.alt = '';
                tab.appendChild(img);
            } else {
                var span = document.createElement('span');
                span.className = 'sticker-tab-letter';
                span.textContent = pack.name.charAt(0).toUpperCase();
                tab.appendChild(span);
            }
            tab.appendChild(document.createTextNode(pack.name));
            tab.addEventListener('click', function () {
                activePackId = pack.id;
                renderTabs();
                renderGrid();
                showAdd(false);
            });
            tabsEl.appendChild(tab);
        });

        var addTab = document.createElement('button');
        addTab.type = 'button';
        addTab.className = 'sticker-tab sticker-tab-add' + (addForm.hidden ? '' : ' is-active');
        addTab.title = 'Добавить стикеры';
        addTab.textContent = '+';
        addTab.addEventListener('click', function () {
            var willShow = addForm.hidden;
            showAdd(willShow);
            if (willShow) {
                populatePackSelect();
            }
        });
        tabsEl.appendChild(addTab);
    }

    function renderGrid() {
        gridEl.innerHTML = '';
        var pack = null;
        for (var i = 0; i < packs.length; i++) {
            if (packs[i].id === activePackId) {
                pack = packs[i];
                break;
            }
        }
        if (!pack) {
            gridEl.innerHTML = '<div class="sticker-empty">Выберите набор стикеров</div>';
            return;
        }
        if (!pack.stickers || !pack.stickers.length) {
            gridEl.innerHTML = '<div class="sticker-empty">В этом наборе пока нет стикеров</div>';
            return;
        }
        pack.stickers.forEach(function (sticker) {
            var cell = document.createElement('button');
            cell.type = 'button';
            cell.className = 'sticker-cell';
            cell.title = 'Отправить стикер';
            var img = document.createElement('img');
            img.src = sticker.url;
            img.alt = '';
            img.loading = 'lazy';
            cell.appendChild(img);
            cell.addEventListener('click', function () {
                if (opts && typeof opts.onPick === 'function') {
                    opts.onPick(sticker.code, sticker);
                }
            });
            gridEl.appendChild(cell);
        });
    }

    function populatePackSelect() {
        packSelect.innerHTML = '';
        packs.forEach(function (pack) {
            var option = document.createElement('option');
            option.value = String(pack.id);
            option.textContent = pack.name + (pack.authorUsername ? ' (автор: ' + pack.authorUsername + ')' : '');
            if (pack.id === activePackId) {
                option.selected = true;
            }
            packSelect.appendChild(option);
        });
        if (!packs.length) {
            var emptyOption = document.createElement('option');
            emptyOption.value = '';
            emptyOption.textContent = 'Нет наборов';
            packSelect.appendChild(emptyOption);
            packSelect.disabled = true;
        } else {
            packSelect.disabled = false;
        }
    }

    function createPack() {
        var name = nameInput.value.trim();
        if (!name) {
            setAddError('Введите название набора');
            return;
        }
        setAddError('');
        var form = new FormData();
        form.append('name', name);
        fetch('/api/sticker-packs', { method: 'POST', body: form })
            .then(function (r) {
                return r.json().then(function (data) { return { ok: r.ok, data: data }; });
            })
            .then(function (res) {
                if (!res.ok) {
                    throw new Error(res.data.error || 'Не удалось создать набор');
                }
                nameInput.value = '';
                return loadPacks(res.data.pack.id);
            })
            .catch(function (e) {
                setAddError(e.message || 'Не удалось создать набор');
            });
    }

    function uploadStickers(files) {
        var packId = packSelect.value;
        if (!packId) {
            setAddError('Сначала создайте или выберите набор');
            return;
        }
        if (!files || !files.length) {
            setAddError('Выберите файлы стикеров');
            return;
        }
        setAddError('');
        var form = new FormData();
        for (var i = 0; i < files.length; i++) {
            form.append('files', files[i]);
        }
        fetch('/api/sticker-packs/' + packId + '/stickers', { method: 'POST', body: form })
            .then(function (r) {
                return r.json().then(function (data) { return { ok: r.ok, data: data }; });
            })
            .then(function (res) {
                if (!res.ok) {
                    throw new Error(res.data.error || 'Не удалось загрузить стикеры');
                }
                var fileInput = document.getElementById('sticker-file-input');
                if (fileInput) {
                    fileInput.value = '';
                }
                return loadPacks(Number(packId));
            })
            .catch(function (e) {
                setAddError(e.message || 'Не удалось загрузить стикеры');
            });
    }

    function loadPacks(selectId) {
        return fetch('/api/stickers')
            .then(function (r) { return r.json(); })
            .then(function (data) {
                packs = Array.isArray(data) ? data : [];
                if (activePackId === null && packs.length) {
                    activePackId = packs[0].id;
                }
                if (selectId !== undefined && selectId !== null) {
                    var found = packs.some(function (p) { return p.id === selectId; });
                    if (found) {
                        activePackId = selectId;
                    }
                }
                renderTabs();
                renderGrid();
                populatePackSelect();
            });
    }

    function togglePanel() {
        if (panel.hidden) {
            openPanel();
        } else {
            closePanel();
        }
    }

    function openPanel() {
        panel.hidden = false;
        positionPanel();
        loadPacks();
    }

    function positionPanel() {
        var bottomOffset = 72;
        var composer = null;
        if (opts && opts.composerSelector) {
            composer = document.querySelector(opts.composerSelector);
        }
        if (!composer && btn) {
            composer = btn.closest('form');
        }
        if (composer) {
            var rect = composer.getBoundingClientRect();
            if (rect && rect.height > 0 && rect.top > 0 && rect.top <= window.innerHeight) {
                bottomOffset = Math.max(8, window.innerHeight - rect.top + 8);
            }
        }
        panel.style.bottom = bottomOffset + 'px';
        panel.style.top = 'auto';
        panel.style.left = '50%';
        panel.style.transform = 'translateX(-50%)';
    }

    function closePanel() {
        panel.hidden = true;
    }

    function showAdd(show) {
        addForm.hidden = !show;
        renderTabs();
    }

    function init(options) {
        opts = options || {};
        var insertBeforeEl = null;
        if (opts.attachSelector) {
            insertBeforeEl = document.querySelector(opts.attachSelector);
        }
        if (!insertBeforeEl) {
            return;
        }
        buildPanel();
        createButton(insertBeforeEl);

        document.addEventListener('click', function (e) {
            if (!panel.hidden) {
                var path = e.composedPath ? e.composedPath() : [];
                if (path.indexOf(panel) === -1 && path.indexOf(btn) === -1) {
                    closePanel();
                }
            }
        });
        document.addEventListener('keydown', function (e) {
            if (e.key === 'Escape' && !panel.hidden) {
                closePanel();
            }
        });
        window.addEventListener('resize', function () {
            if (!panel.hidden) {
                positionPanel();
            }
        });
    }

    function createStickerImage(stickerUrl) {
        var img = document.createElement('img');
        img.className = 'sticker-image';
        img.src = stickerUrl;
        img.alt = 'Стикер';
        return img;
    }

    return {
        init: init,
        createStickerImage: createStickerImage
    };
})();
