(function () {
    'use strict';

    var PRIV_KEY = 'shadow_e2e_privkey';
    var pubKeyCache = {};
    var peerKeyPending = {};

    function toBytes(str) {
        return new TextEncoder().encode(str);
    }

    function fromBytes(bytes) {
        return new TextDecoder().decode(bytes);
    }

    function b64(bytes) {
        var s = '';
        for (var i = 0; i < bytes.length; i++) {
            s += String.fromCharCode(bytes[i]);
        }
        return window.btoa(s);
    }

    function unb64(s) {
        var bin = window.atob(s);
        var out = new Uint8Array(bin.length);
        for (var i = 0; i < bin.length; i++) {
            out[i] = bin.charCodeAt(i);
        }
        return out;
    }

    function getStoredPriv() {
        try {
            return localStorage.getItem(PRIV_KEY);
        } catch (e) {
            return null;
        }
    }

    function ensureKeys() {
        if (!window.nacl) {
            return Promise.resolve(false);
        }
        var priv = getStoredPriv();
        if (priv) {
            try {
                var bytes = unb64(priv);
                if (bytes.length === 32) {
                    nacl.box.keyPair.fromSecretKey(bytes);
                    return Promise.resolve(true);
                }
            } catch (e) {
            }
        }
        var keyPair = nacl.box.keyPair();
        try {
            localStorage.setItem(PRIV_KEY, b64(keyPair.secretKey));
        } catch (e) {
        }
        return fetch('/api/e2e/key', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ publicKey: b64(keyPair.publicKey) })
        }).then(function (r) {
            return r.ok;
        }).catch(function () {
            return false;
        });
    }

    function hasOwnKey() {
        return !!getStoredPriv();
    }

    function ownPublicKey() {
        var priv = getStoredPriv();
        if (!priv || !window.nacl) {
            return null;
        }
        try {
            return b64(nacl.box.keyPair.fromSecretKey(unb64(priv)).publicKey);
        } catch (e) {
            return null;
        }
    }

    function getPeerKey(username, force) {
        if (!force && Object.prototype.hasOwnProperty.call(pubKeyCache, username)) {
            return Promise.resolve(pubKeyCache[username]);
        }
        if (!force && peerKeyPending[username]) {
            return peerKeyPending[username];
        }
        var p = fetch('/api/e2e/key/' + encodeURIComponent(username), {
            headers: { 'Accept': 'application/json' }
        }).then(function (r) {
            if (!r.ok) {
                return null;
            }
            return r.json().then(function (d) {
                return d && d.publicKey ? d.publicKey : null;
            });
        }).catch(function () {
            return null;
        }).then(function (key) {
            pubKeyCache[username] = key;
            delete peerKeyPending[username];
            return key;
        });
        if (!force) {
            peerKeyPending[username] = p;
        }
        return p;
    }

    function sharedKey(peerPubB64) {
        var priv = getStoredPriv();
        if (!priv || !window.nacl) {
            return null;
        }
        try {
            return nacl.box.before(unb64(peerPubB64), unb64(priv));
        } catch (e) {
            return null;
        }
    }

    function isEncrypted(content) {
        return typeof content === 'string' && content.indexOf('e2e1:') === 0;
    }

    function encrypt(peerPubB64, text) {
        var shared = sharedKey(peerPubB64);
        if (!shared || !window.nacl) {
            return null;
        }
        var nonce = nacl.randomBytes(nacl.box.nonceLength);
        var box = nacl.box.after(toBytes(text), nonce, shared);
        var combined = new Uint8Array(nonce.length + box.length);
        combined.set(nonce, 0);
        combined.set(box, nonce.length);
        return 'e2e1:' + b64(combined);
    }

    function decrypt(peerPubB64, content) {
        var shared = sharedKey(peerPubB64);
        if (!shared || !window.nacl) {
            return null;
        }
        var raw;
        try {
            raw = unb64(content.slice(5));
        } catch (e) {
            return null;
        }
        if (raw.length < nacl.box.nonceLength) {
            return null;
        }
        var nonce = raw.slice(0, nacl.box.nonceLength);
        var box = raw.slice(nacl.box.nonceLength);
        var out = nacl.box.open.after(box, nonce, shared);
        if (!out) {
            return null;
        }
        return fromBytes(out);
    }

    function decryptMessage(content, peerUsername) {
        if (!isEncrypted(content)) {
            return Promise.resolve(content);
        }
        var cached = pubKeyCache[peerUsername];
        var force = !cached;
        return getPeerKey(peerUsername, force).then(function (key) {
            if (!key) {
                return null;
            }
            return decrypt(key, content);
        });
    }

    function decryptElements(peerUsername) {
        var nodes = document.querySelectorAll('[data-cipher]');
        if (!nodes.length) {
            return;
        }
        var force = !pubKeyCache[peerUsername];
        getPeerKey(peerUsername, force).then(function (key) {
            nodes.forEach(function (el) {
                if (el.getAttribute('data-decrypted') === '1') {
                    return;
                }
                var ct = el.getAttribute('data-cipher');
                var plain = (key && decrypt(key, ct)) || null;
                if (plain) {
                    el.setAttribute('data-decrypted', '1');
                    el.textContent = plain;
                    el.classList.remove('e2e-pending');
                }
            });
        });
    }

    function updateIndicator(peerUsername, btn) {
        getPeerKey(peerUsername).then(function (key) {
            if (!btn) {
                return;
            }
            var active = !!key && hasOwnKey();
            if (active) {
                btn.classList.add('is-active');
                btn.setAttribute('title', 'Сквозное шифрование включено');
                btn.setAttribute('aria-label', 'Сквозное шифрование включено');
            } else {
                btn.classList.remove('is-active');
                btn.setAttribute('title', 'Сквозное шифрование недоступно — собеседник не настроил E2E');
            }
        });
    }

    function init() {
        ensureKeys();
        var btn = document.getElementById('e2e-header-btn');
        var peer = btn && btn.getAttribute('data-peer');
        if (peer) {
            getPeerKey(peer);
            updateIndicator(peer, btn);
            decryptElements(peer);
        }
    }

    window.E2E = {
        ensureKeys: ensureKeys,
        hasOwnKey: hasOwnKey,
        ownPublicKey: ownPublicKey,
        getPeerKey: getPeerKey,
        isEncrypted: isEncrypted,
        encrypt: encrypt,
        decrypt: decrypt,
        decryptMessage: decryptMessage,
        decryptElements: decryptElements,
        updateIndicator: updateIndicator,
        isReadyFor: function (username) {
            return hasOwnKey() && Object.prototype.hasOwnProperty.call(pubKeyCache, username) && !!pubKeyCache[username];
        },
        encryptFor: function (username, text) {
            var key = pubKeyCache[username];
            if (!key || !window.nacl) {
                return text;
            }
            var ct = encrypt(key, text);
            return ct || text;
        }
    };

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
})();
