(function () {
    'use strict';

    var chatPartners = Object.prototype.toString.call(window.__chatPartners) === '[object Array]'
        ? window.__chatPartners.filter(function (u) { return u && u.username; })
        : [];
    var existingMembers = Object.prototype.toString.call(window.__existingMembers) === '[object Array]'
        ? window.__existingMembers.map(function (u) { return (u && u.username) ? u.username : u; })
        : [];

    var pickers = document.querySelectorAll('.member-picker');
    pickers.forEach(initPicker);

    function normalizeList(list) {
        if (Object.prototype.toString.call(list) !== '[object Array]') return [];
        return list.map(function (u) {
            if (typeof u === 'string') return { username: u, avatarFilename: null };
            return { username: u && u.username, avatarFilename: u && u.avatarFilename };
        });
    }

    function initPicker(picker) {
        var hidden = picker.querySelector('input[name="members"]');
        var input = picker.querySelector('.member-picker-input');
        var searchBox = picker.querySelector('.member-picker-search');
        var clearBtn = picker.querySelector('.member-picker-clear');
        var results = picker.querySelector('.member-picker-results');
        var chips = picker.querySelector('.member-picker-chips');
        var trigger = picker.querySelector('.member-picker-trigger');
        var countEl = picker.querySelector('.member-picker-trigger-count');
        var selected = [];

        function isOpen() {
            return picker.classList.contains('is-open');
        }
        function setOpen(open) {
            if (open) {
                picker.classList.add('is-open');
                renderDefault();
            } else {
                picker.classList.remove('is-open');
            }
            input.value = '';
            syncClear();
            results.hidden = true;
        }
        function toggle() { setOpen(!isOpen()); }

        function syncClear() {
            if (clearBtn) clearBtn.classList.toggle('is-visible', !!input.value);
        }
        function toggle() { setOpen(!isOpen()); }

        function selectedSet() {
            var s = {};
            selected.forEach(function (u) { s[u.username] = true; });
            return s;
        }

        function selectedContains(username) {
            return selected.some(function (u) { return u.username === username; });
        }

        function buildAvatar(user) {
            var avatar = document.createElement('span');
            avatar.className = 'user-avatar member-picker-avatar';
            if (user.avatarFilename) {
                var img = document.createElement('img');
                img.className = 'user-avatar-img';
                img.src = '/uploads/avatars/' + encodeURIComponent(user.avatarFilename);
                img.alt = user.username;
                img.loading = 'lazy';
                avatar.appendChild(img);
            } else {
                var letter = document.createElement('span');
                letter.className = 'user-avatar-letter';
                letter.textContent = (user.username || '?').charAt(0).toUpperCase();
                avatar.appendChild(letter);
            }
            return avatar;
        }

        function buildRow(user) {
            var row = document.createElement('button');
            row.type = 'button';
            row.className = 'member-picker-item';
            row.appendChild(buildAvatar(user));
            var name = document.createElement('span');
            name.className = 'member-picker-name';
            name.textContent = user.username;
            row.appendChild(name);
            row.addEventListener('click', function () {
                addUser(user);
            });
            return row;
        }

        function renderChips() {
            chips.innerHTML = '';
            selected.forEach(function (u) {
                var chip = document.createElement('span');
                chip.className = 'member-picker-chip';
                var name = document.createElement('span');
                name.textContent = u.username;
                chip.appendChild(name);
                var x = document.createElement('button');
                x.type = 'button';
                x.className = 'member-picker-chip-x';
                x.setAttribute('aria-label', 'Удалить');
                x.textContent = '✕';
                x.addEventListener('click', function () { removeUser(u.username); });
                chip.appendChild(x);
                chips.appendChild(chip);
            });
            hidden.value = selected.map(function (u) { return u.username; }).join(', ');
            hidden.dispatchEvent(new Event('change', { bubbles: true }));
            if (countEl) {
                countEl.textContent = selected.length ? String(selected.length) : '';
            }
        }

        function addUser(user) {
            if (selectedContains(user.username)) return;
            selected.push(user);
            renderChips();
            input.value = '';
            syncClear();
            if (isOpen()) renderDefault();
        }

        function removeUser(username) {
            selected = selected.filter(function (u) { return u.username !== username; });
            renderChips();
            if (isOpen() && !input.value.trim()) renderDefault();
        }

        function renderDefault() {
            var base = normalizeList(chatPartners).filter(function (u) {
                return !selectedContains(u.username) && existingMembers.indexOf(u.username) === -1;
            });
            render(base, '_default');
        }

        function render(users, q) {
            if (q !== '_default' && input.value.trim() !== q) return;
            if (q === '_default' && input.value.trim()) return;
            var visible = users.filter(function (u) {
                return !selectedContains(u.username) && existingMembers.indexOf(u.username) === -1;
            });
            results.innerHTML = '';
            if (!visible.length) {
                var empty = document.createElement('div');
                empty.className = 'member-picker-empty';
                if (q === '_default') {
                    empty.textContent = 'Нет чатов для выбора';
                } else {
                    empty.textContent = 'Никого не нашли';
                }
                results.appendChild(empty);
            } else {
                var frag = document.createDocumentFragment();
                visible.forEach(function (u) { frag.appendChild(buildRow(u)); });
                results.appendChild(frag);
            }
            results.hidden = false;
        }

        function hideResults() {
            results.hidden = true;
            results.innerHTML = '';
        }

        function search(q) {
            fetch('/api/users/search?q=' + encodeURIComponent(q), {
                headers: { 'Accept': 'application/json' }
            })
                .then(function (r) {
                    if (r.status === 401 || (r.headers.get('Content-Type') || '').indexOf('text/html') !== -1) {
                        window.location.href = '/login';
                        throw new Error('aborted');
                    }
                    if (!r.ok) throw new Error('HTTP ' + r.status);
                    return r.json();
                })
                .then(function (users) {
                    if (input.value.trim() === q) {
                        render(normalizeList(users), q);
                    }
                })
                .catch(function (err) {
                    if (err && err.message === 'aborted') return;
                    if (input.value.trim() === q) {
                        results.innerHTML = '';
                        var e = document.createElement('div');
                        e.className = 'member-picker-empty';
                        e.textContent = 'Ошибка поиска';
                        results.appendChild(e);
                        results.hidden = false;
                    }
                });
        }

        var debounceTimer = null;
        if (searchBox) {
            searchBox.addEventListener('click', function (e) { if (e.target === searchBox) input.focus(); });
        }
        if (clearBtn) {
            clearBtn.addEventListener('click', function (e) {
                e.preventDefault();
                e.stopPropagation();
                input.value = '';
                syncClear();
                renderDefault();
                input.focus();
            });
        }
        input.addEventListener('input', function () {
            syncClear();
            var q = input.value.trim();
            if (debounceTimer) clearTimeout(debounceTimer);
            if (!q) { renderDefault(); return; }
            debounceTimer = setTimeout(function () { search(q); }, 200);
        });
        input.addEventListener('keydown', function (e) {
            if (e.key === 'Escape') { setOpen(false); input.blur(); }
        });
        if (trigger) {
            trigger.addEventListener('click', function (e) {
                e.preventDefault();
                toggle();
            });
        }
        document.addEventListener('click', function (e) {
            if (!picker.contains(e.target)) setOpen(false);
        });
    }
})();
