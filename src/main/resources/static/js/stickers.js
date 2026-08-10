window.StickerUI = (function () {
    var opts = null;
    var packs = [];
    var activePackId = null;
    var panel = null;
    var tabsEl = null;
    var gridEl = null;
    var addForm = null;
    var btn = null;
    var nameInput = null;
    var stickerFileInput = null;
    var panelErrEl = null;
    var pressTimer = null;
    var pressShown = false;
    var isPressing = false;
    var zoomPinned = false;
    var pressStartedAt = 0;
    var lastReleaseAt = 0;
    var zoomOverlay = null;
    var zoomImg = null;
    var zoomHandlerRegistered = false;

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
        gridEl.addEventListener('mouseleave', finishStickerPress);

        addForm = buildAddForm();

        panel.appendChild(header);
        panel.appendChild(gridEl);
        panel.appendChild(addForm);

        panelErrEl = document.createElement('div');
        panelErrEl.className = 'sticker-error';
        panelErrEl.hidden = true;
        panel.appendChild(panelErrEl);

        stickerFileInput = document.createElement('input');
        stickerFileInput.type = 'file';
        stickerFileInput.accept = 'image/png,image/jpeg,image/webp,image/gif';
        stickerFileInput.multiple = true;
        stickerFileInput.hidden = true;
        document.body.appendChild(stickerFileInput);
        stickerFileInput.addEventListener('change', function () {
            addStickersToActivePack(stickerFileInput.files);
        });

        document.body.appendChild(panel);
    }

    function buildAddForm() {
        var form = document.createElement('div');
        form.className = 'sticker-add';
        form.hidden = true;

        var title = document.createElement('div');
        title.className = 'sticker-add-title';
        title.textContent = 'Создать новый набор';
        form.appendChild(title);

        var nameRow = document.createElement('div');
        nameRow.className = 'sticker-add-row';

        nameInput = document.createElement('input');
        nameInput.type = 'text';
        nameInput.className = 'sticker-add-input';
        nameInput.placeholder = 'Название набора...';
        nameInput.maxLength = 60;

        var createBtn = document.createElement('button');
        createBtn.type = 'button';
        createBtn.className = 'sticker-add-btn';
        createBtn.textContent = 'Создать';
        createBtn.addEventListener('click', createPack);

        nameRow.appendChild(nameInput);
        nameRow.appendChild(createBtn);
        form.appendChild(nameRow);

        var hint = document.createElement('div');
        hint.className = 'sticker-add-hint';
        hint.textContent = 'Стикеры в новый набор добавляйте из самого набора';
        form.appendChild(hint);

        return form;
    }

    function setError(text) {
        if (!panelErrEl) {
            return;
        }
        if (text) {
            panelErrEl.textContent = text;
            panelErrEl.hidden = false;
        } else {
            panelErrEl.hidden = true;
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
        addTab.title = 'Создать новый набор';
        addTab.textContent = '+';
        addTab.addEventListener('click', function () {
            showAdd(addForm.hidden);
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
            renderAddCell(pack);
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
            cell.addEventListener('mousedown', function () {
                startStickerZoom(sticker.url);
            });
            cell.addEventListener('mouseup', finishStickerPress);
            cell.addEventListener('touchstart', function () {
                startStickerZoom(sticker.url);
            }, { passive: true });
            cell.addEventListener('touchend', finishStickerPress);
            cell.addEventListener('touchcancel', finishStickerPress);
            cell.addEventListener('click', function () {
                if (pressShown) {
                    pressShown = false;
                    return;
                }
                if (opts && typeof opts.onPick === 'function') {
                    opts.onPick(sticker.code, sticker);
                }
            });
            gridEl.appendChild(cell);
        });
        renderAddCell(pack);
    }

    function renderAddCell(pack) {
        if (!pack.mine) {
            return;
        }
        var addTile = document.createElement('button');
        addTile.type = 'button';
        addTile.className = 'sticker-add-cell';
        addTile.title = 'Добавить стикеры в этот набор';
        var plus = document.createElement('span');
        plus.className = 'sticker-add-cell-plus';
        plus.textContent = '+';
        var label = document.createElement('span');
        label.textContent = 'Добавить стикер';
        addTile.appendChild(plus);
        addTile.appendChild(label);
        addTile.addEventListener('click', function () {
            setError('');
            if (stickerFileInput) {
                stickerFileInput.value = '';
                stickerFileInput.click();
            }
        });
        gridEl.appendChild(addTile);
    }

    function createPack() {
        var name = nameInput.value.trim();
        if (!name) {
            setError('Введите название набора');
            return;
        }
        setError('');
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
                showAdd(false);
                return loadPacks(res.data.pack.id);
            })
            .catch(function (e) {
                setError(e.message || 'Не удалось создать набор');
            });
    }

    function addStickersToActivePack(files) {
        if (!activePackId) {
            return;
        }
        if (!files || !files.length) {
            return;
        }
        setError('');
        var packId = activePackId;
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
                if (stickerFileInput) {
                    stickerFileInput.value = '';
                }
                return loadPacks(packId);
            })
            .catch(function (e) {
                setError(e.message || 'Не удалось загрузить стикеры');
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
        if (zoomPinned) {
            zoomPinned = false;
        }
        if (zoomOverlay) {
            zoomOverlay.hidden = true;
        }
    }

    function showAdd(show) {
        addForm.hidden = !show;
        renderTabs();
    }

    function ensureZoomOverlay() {
        if (zoomOverlay) {
            return;
        }
        zoomOverlay = document.createElement('div');
        zoomOverlay.className = 'sticker-zoom';
        zoomOverlay.hidden = true;
        zoomImg = document.createElement('img');
        zoomImg.alt = '';
        zoomOverlay.appendChild(zoomImg);
        document.body.appendChild(zoomOverlay);
    }

    function resetZoomState() {
        isPressing = false;
        zoomPinned = false;
        if (pressTimer) {
            clearTimeout(pressTimer);
            pressTimer = null;
        }
        if (zoomOverlay) {
            zoomOverlay.hidden = true;
        }
        pressShown = false;
    }

    function startStickerZoom(stickerUrl) {
        resetZoomState();
        isPressing = true;
        pressStartedAt = Date.now();
        pressTimer = setTimeout(function () {
            pressTimer = null;
            pressShown = true;
            ensureZoomOverlay();
            zoomImg.src = stickerUrl;
            zoomOverlay.hidden = false;
        }, 350);
    }

    function finishStickerPress() {
        if (!isPressing) {
            return;
        }
        isPressing = false;
        lastReleaseAt = Date.now();
        if (pressTimer) {
            clearTimeout(pressTimer);
            pressTimer = null;
        }
        if (zoomOverlay && !zoomOverlay.hidden && (lastReleaseAt - pressStartedAt) >= 200) {
            zoomPinned = true;
        } else {
            if (zoomOverlay) {
                zoomOverlay.hidden = true;
            }
        }
        if (pressShown) {
            setTimeout(function () {
                pressShown = false;
            }, 100);
        }
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
        registerStickerClickHandler();
        registerZoomHandlers();

        document.addEventListener('click', function (e) {
            if (zoomPinned && (Date.now() - lastReleaseAt) > 200) {
                zoomPinned = false;
                if (zoomOverlay) {
                    zoomOverlay.hidden = true;
                }
            }
            if (!panel.hidden) {
                if (pressShown) {
                    return;
                }
                if (e.target && e.target.closest && e.target.closest('.sticker-zoom')) {
                    return;
                }
                var path = e.composedPath ? e.composedPath() : [];
                if (path.indexOf(panel) === -1 && path.indexOf(btn) === -1) {
                    closePanel();
                }
            }
        });
        document.addEventListener('keydown', function (e) {
            if (e.key === 'Escape') {
                if (zoomOverlay && !zoomOverlay.hidden) {
                    zoomPinned = false;
                    zoomOverlay.hidden = true;
                    return;
                }
                if (!panel.hidden) {
                    closePanel();
                }
            }
        });
        window.addEventListener('resize', function () {
            if (!panel.hidden) {
                positionPanel();
            }
        });
    }

    function createStickerImage(stickerUrl, stickerCode, outgoing) {
        var img = document.createElement('img');
        img.className = 'sticker-image';
        img.src = stickerUrl;
        img.alt = 'Стикер';
        if (stickerCode) {
            img.setAttribute('data-sticker-code', stickerCode);
        }
        if (outgoing) {
            img.setAttribute('data-outgoing', '1');
        }
        return img;
    }

    var previewModal = null;
    var previewHandlerRegistered = false;

    function registerZoomHandlers() {
        if (zoomHandlerRegistered) {
            return;
        }
        zoomHandlerRegistered = true;
        document.addEventListener('mouseup', finishStickerPress);
        document.addEventListener('touchend', finishStickerPress);
        document.addEventListener('touchcancel', finishStickerPress);
    }

    function registerStickerClickHandler() {
        if (previewHandlerRegistered) {
            return;
        }
        previewHandlerRegistered = true;
        document.addEventListener('click', function (e) {
            var target = e.target;
            if (!target || !target.closest) {
                return;
            }
            if (target.closest('.sticker-preview')) {
                return;
            }
            var img = target.closest('.sticker-image[data-sticker-code]');
            if (!img || img.hasAttribute('data-outgoing')) {
                return;
            }
            previewStickerPack(img.getAttribute('data-sticker-code'));
        });
    }

    function previewStickerPack(code) {
        fetch('/api/sticker-packs/by-code/' + encodeURIComponent(code))
            .then(function (r) {
                return r.json().then(function (data) { return { ok: r.ok, data: data }; });
            })
            .then(function (res) {
                if (!res.ok || !res.data || !res.data.id || res.data.added) {
                    return;
                }
                showPreviewModal(res.data);
            })
            .catch(function () {
            });
    }

    function showPreviewModal(pack) {
        if (previewModal) {
            previewModal.remove();
            previewModal = null;
        }

        var overlay = document.createElement('div');
        overlay.className = 'sticker-preview-overlay';

        var modal = document.createElement('div');
        modal.className = 'sticker-preview';

        var header = document.createElement('div');
        header.className = 'sticker-preview-header';

        var title = document.createElement('div');
        title.className = 'sticker-preview-title';
        title.textContent = pack.name || 'Набор стикеров';

        var closeBtn = document.createElement('button');
        closeBtn.type = 'button';
        closeBtn.className = 'sticker-close';
        closeBtn.setAttribute('aria-label', 'Закрыть');
        closeBtn.textContent = '\u00d7';
        closeBtn.addEventListener('click', function () {
            closePreviewModal();
        });

        header.appendChild(title);
        header.appendChild(closeBtn);

        var grid = document.createElement('div');
        grid.className = 'sticker-preview-grid';
        if (pack.stickers && pack.stickers.length) {
            pack.stickers.forEach(function (sticker) {
                var img = document.createElement('img');
                img.src = sticker.url;
                img.alt = '';
                grid.appendChild(img);
            });
        } else {
            var empty = document.createElement('div');
            empty.className = 'sticker-empty';
            empty.textContent = 'В этом наборе пока нет стикеров';
            grid.appendChild(empty);
        }

        var addBtn = document.createElement('button');
        addBtn.type = 'button';
        addBtn.className = 'btn-primary sticker-preview-add';
        addBtn.textContent = 'Добавить набор';
        addBtn.addEventListener('click', function () {
            fetch('/api/sticker-packs/' + pack.id + '/subscribe', { method: 'POST' })
                .then(function (r) {
                    return r.json().then(function (data) { return { ok: r.ok, data: data }; });
                })
                .then(function (res) {
                    if (!res.ok || !res.data || !res.data.success || !res.data.pack || !res.data.pack.added) {
                        throw new Error(res.data && res.data.error ? res.data.error : 'Не удалось добавить набор');
                    }
                    addBtn.textContent = '\u2713 Набор добавлен';
                    addBtn.disabled = true;
                    if (panel && !panel.hidden) {
                        loadPacks(pack.id);
                    }
                    setTimeout(closePreviewModal, 900);
                })
                .catch(function (e) {
                    addBtn.textContent = e.message || 'Не удалось добавить набор';
                    addBtn.disabled = false;
                });
        });

        modal.appendChild(header);
        modal.appendChild(grid);
        modal.appendChild(addBtn);
        overlay.appendChild(modal);
        document.body.appendChild(overlay);

        overlay.addEventListener('click', function (e) {
            if (e.target === overlay) {
                closePreviewModal();
            }
        });

        previewModal = overlay;
    }

    function closePreviewModal() {
        if (previewModal) {
            previewModal.remove();
            previewModal = null;
        }
    }

    return {
        init: init,
        createStickerImage: createStickerImage
    };
})();
