(function (global) {
    'use strict';

    var players = [];

    function fmt(seconds) {
        if (!isFinite(seconds) || seconds < 0) seconds = 0;
        var s = Math.floor(seconds);
        var m = Math.floor(s / 60);
        s = s % 60;
        return m + ':' + (s < 10 ? '0' : '') + s;
    }

    function initPlayer(audio) {
        if (!audio || !audio.classList.contains('attachment-audio')) return;
        if (audio.closest('.audio-player-root')) return;

        var src = audio.getAttribute('src');
        if (!src) return;

        var root = document.createElement('div');
        root.className = 'audio-player-root';

        var playBtn = document.createElement('button');
        playBtn.type = 'button';
        playBtn.className = 'audio-player-btn';
        playBtn.innerHTML = '<svg class="audio-ic-play" width="16" height="16" viewBox="0 0 24 24" fill="currentColor"><path d="M8 5v14l11-7z"/></svg><svg class="audio-ic-pause" width="16" height="16" viewBox="0 0 24 24" fill="currentColor"><path d="M6 5h4v14H6zM14 5h4v14h-4z"/></svg>';

        var bar = document.createElement('div');
        bar.className = 'audio-player-bar';
        var fill = document.createElement('div');
        fill.className = 'audio-player-fill';
        bar.appendChild(fill);

        var timeEl = document.createElement('span');
        timeEl.className = 'audio-player-time';
        timeEl.textContent = '0:00';

        var audioEl;
        if (audio.parentNode) {
            var copy = audio.cloneNode(false);
            copy.controls = false;
            copy.preload = 'metadata';
            audio.parentNode.replaceChild(root, audio);
            root.appendChild(copy);
            audioEl = copy;
        } else {
            audio.controls = false;
            audio.preload = 'metadata';
            root.appendChild(audio);
            audioEl = audio;
        }

        root.appendChild(playBtn);
        root.appendChild(bar);
        root.appendChild(timeEl);

        var state = { audio: audioEl, root: root, fill: fill, timeEl: timeEl, total: 0 };
        players.push(state);

        audioEl.addEventListener('loadedmetadata', function () {
            if (isFinite(audioEl.duration)) state.total = audioEl.duration;
            timeEl.textContent = fmt(state.total);
        });
        audioEl.addEventListener('timeupdate', function () {
            timeEl.textContent = fmt(audioEl.currentTime);
            fill.style.width = state.total ? (audioEl.currentTime / state.total * 100) + '%' : '0%';
        });
        audioEl.addEventListener('play', function () { root.classList.add('is-playing'); });
        audioEl.addEventListener('pause', function () { root.classList.remove('is-playing'); });
        audioEl.addEventListener('ended', function () {
            root.classList.remove('is-playing');
            fill.style.width = '0%';
            timeEl.textContent = fmt(state.total);
        });

        playBtn.addEventListener('click', function () {
            if (audioEl.paused) {
                players.forEach(function (p) {
                    if (p.audio !== audioEl && !p.audio.paused) {
                        try { p.audio.pause(); } catch (e) {}
                    }
                });
                if (global.VoicePlayer) global.VoicePlayer.pauseAll();
                audioEl.play().catch(function () {});
            } else {
                audioEl.pause();
            }
        });

        bar.addEventListener('click', function (e) {
            if (!state.total) return;
            var rect = bar.getBoundingClientRect();
            var ratio = Math.max(0, Math.min(1, (e.clientX - rect.left) / rect.width));
            audioEl.currentTime = ratio * state.total;
        });
    }

    function initAll(scope) {
        var els = (scope || document).querySelectorAll
            ? (scope || document).querySelectorAll('audio.attachment-audio')
            : [];
        for (var i = 0; i < els.length; i++) initPlayer(els[i]);
    }

    function start() {
        initAll(document);
        var obs = new MutationObserver(function (mutations) {
            for (var i = 0; i < mutations.length; i++) {
                var nodes = mutations[i].addedNodes;
                for (var j = 0; j < nodes.length; j++) {
                    var node = nodes[j];
                    if (!node.querySelectorAll) continue;
                    var audios = node.querySelectorAll('audio.attachment-audio');
                    for (var k = 0; k < audios.length; k++) initPlayer(audios[k]);
                }
            }
        });
        obs.observe(document.body, { childList: true, subtree: true });
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', start);
    } else {
        start();
    }

    global.AudioPlayer = {
        initAll: initAll,
        pauseAll: function () {
            players.forEach(function (p) {
                if (p.audio && !p.audio.paused) {
                    try { p.audio.pause(); } catch (e) {}
                }
            });
        }
    };
})(window);
