(function () {
    'use strict';

    var wrapper = document.getElementById('user-search');
    var input = document.getElementById('user-search-input');
    var clearBtn = document.getElementById('user-search-clear');
    var results = document.getElementById('user-search-results');

    if (!wrapper || !input || !results) {
        return;
    }

    var debounceTimer = null;
    var abortController = null;
    var requestSeq = 0;

    function buildAvatar(user) {
        var avatar = document.createElement('span');
        avatar.className = 'avatar-with-status';

        var box = document.createElement('span');
        box.className = 'user-avatar user-search-avatar';
        if (user.avatarFilename) {
            var img = document.createElement('img');
            img.className = 'user-avatar-img';
            img.src = '/uploads/avatars/' + encodeURIComponent(user.avatarFilename);
            img.alt = user.username;
            img.loading = 'lazy';
            box.appendChild(img);
        } else {
            var letter = document.createElement('span');
            letter.className = 'user-avatar-letter';
            letter.textContent = (user.username || '?').charAt(0).toUpperCase();
            box.appendChild(letter);
        }
        avatar.appendChild(box);

        var dot = document.createElement('span');
        dot.className = 'online-dot' + (user.online ? ' is-online' : '');
        avatar.appendChild(dot);
        return avatar;
    }

    function buildItem(user) {
        var link = document.createElement('a');
        link.className = 'user-search-item';
        link.href = '/chat/' + encodeURIComponent(user.username);

        link.appendChild(buildAvatar(user));

        var body = document.createElement('span');
        body.className = 'user-search-item-body';
        var name = document.createElement('span');
        name.className = 'user-search-item-name';
        name.textContent = user.username;
        body.appendChild(name);

        if (user.about) {
            var about = document.createElement('span');
            about.className = 'user-search-item-about';
            about.textContent = user.about;
            body.appendChild(about);
        }
        link.appendChild(body);

        var status = document.createElement('span');
        status.className = 'user-search-item-status' + (user.online ? ' is-online' : '');
        status.textContent = user.online ? 'в сети' : 'не в сети';
        link.appendChild(status);

        return link;
    }

    function setState(node) {
        results.hidden = false;
        results.replaceChildren(node);
    }

    function messageNode(className, text) {
        var div = document.createElement('div');
        div.className = className;
        div.textContent = text;
        return div;
    }

    function hideResults() {
        results.hidden = true;
        results.replaceChildren();
    }

    function render(users, q) {
        if (!q || input.value.trim() !== q) {
            return;
        }
        if (!users.length) {
            setState(messageNode('sidebar-user-empty', 'Никого не нашли'));
            return;
        }
        var frag = document.createDocumentFragment();
        users.forEach(function (user) {
            frag.appendChild(buildItem(user));
        });
        results.hidden = false;
        results.replaceChildren(frag);
    }

    function search(q) {
        var seq = ++requestSeq;
        if (abortController) {
            abortController.abort();
        }
        abortController = new AbortController();

        fetch('/api/users/search?q=' + encodeURIComponent(q), {
            headers: { 'Accept': 'application/json' },
            signal: abortController.signal
        })
            .then(function (r) {
                if (r.status === 401 || r.redirected || (r.headers.get('Content-Type') || '').indexOf('text/html') !== -1) {
                    location.href = '/login';
                    throw new Error('aborted-redirect');
                }
                if (!r.ok) {
                    throw new Error('HTTP ' + r.status);
                }
                return r.json();
            })
            .then(function (users) {
                if (seq === requestSeq) {
                    render(Array.isArray(users) ? users : [], q);
                }
            })
            .catch(function (err) {
                if (err && (err.name === 'AbortError' || err.message === 'aborted-redirect')) {
                    return;
                }
                if (seq === requestSeq && input.value.trim()) {
                    setState(messageNode('sidebar-user-empty', 'Ошибка поиска'));
                }
            });
    }

    input.addEventListener('input', function () {
        var q = input.value.trim();
        clearBtn.hidden = !q;
        if (debounceTimer) {
            clearTimeout(debounceTimer);
        }
        if (!q) {
            hideResults();
            return;
        }
        debounceTimer = setTimeout(function () {
            search(q);
        }, 250);
    });

    input.addEventListener('keydown', function (e) {
        if (e.key === 'Escape') {
            input.value = '';
            clearBtn.hidden = true;
            hideResults();
            input.blur();
        }
    });

    clearBtn.addEventListener('click', function () {
        input.value = '';
        clearBtn.hidden = true;
        hideResults();
        input.focus();
    });

    results.addEventListener('click', function (e) {
        if (e.target.closest('.user-search-item')) {
            input.value = '';
            clearBtn.hidden = true;
            hideResults();
        }
    });

    document.addEventListener('click', function (e) {
        if (!wrapper.contains(e.target)) {
            hideResults();
        }
    });
})();
