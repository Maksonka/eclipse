(function (global) {
    'use strict';

    var players = [];

    function fmt(seconds) {
        if (!isFinite(seconds) || seconds < 0) {
            seconds = 0;
        }
        var s = Math.floor(seconds);
        var m = Math.floor(s / 60);
        return m + ':' + (s < 10 ? '0' : '') + s;
    }

    function pauseAll() {
        players.forEach(function (p) {
            if (p.audio && !p.audio.paused) {
                try {
                    p.audio.pause();
                } catch (e) {}
            }
        });
    }

    function initPlayer(audio) {
        if (!audio || audio.hasAttribute('data-audio-player')) {
            return;
        }
        if (audio.closest && audio.closest('.audio-attachment-player')) {
            return;
        }

        var src = audio.getAttribute('src') || audio.getAttribute('data-src');
        if (!src) {
            return;
        }
        audio.setAttribute('data-audio-player', '');

        var root = document.createElement('div');
        root.className = 'audio-attachment-player';

        var playBtn = document.createElement('button');
        playBtn.type = 'button';
        playBtn.className = 'audio-attachment-play';
        playBtn.setAttribute('aria-label', 'Воспроизвести');
        playBtn.innerHTML = '<svg class="audio-attachment-ic-play" width="16" height="16" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true"><path d="M8 5v14l11-7z"/></svg>' +
            '<svg class="audio-attachment-ic-pause" width="16" height="16" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true"><path d="M6 5h4v14H6zM14 5h4v14h-4z"/></svg>';

        var track = document.createElement('div');
        track.className = 'audio-attachment-track';
        track.setAttribute('role', 'slider');
        track.setAttribute('aria-label', 'Прогресс воспроизведения');

        var fill = document.createElement('div');
        fill.className = 'audio-attachment-fill';
        track.appendChild(fill);

        var timeEl = document.createElement('span');
        timeEl.className = 'audio-attachment-time';
        timeEl.textContent = '0:00 / 0:00';

        var audioEl;
        if (audio.parentNode) {
            var audioCopy = audio.cloneNode(false);
            audioCopy.controls = false;
            audioCopy.preload = 'metadata';
            audio.parentNode.replaceChild(root, audio);
            root.appendChild(audioCopy);
            audioEl = audioCopy;
        } else {
            audio.controls = false;
            root.appendChild(audio);
            audioEl = audio;
        }

        root.appendChild(playBtn);
        root.appendChild(track);
        root.appendChild(timeEl);

        var state = {
            audio: audioEl,
            root: root,
            playBtn: playBtn,
            track: track,
            fill: fill,
            timeEl: timeEl,
            total: 0
        };
        players.push(state);

        function updateTime() {
            timeEl.textContent = fmt(audioEl.currentTime) + ' / ' + fmt(state.total);
        }

        function updateFill() {
            var f = state.total ? audioEl.currentTime / state.total : 0;
            fill.style.width = (Math.min(1, Math.max(0, f)) * 100) + '%';
        }

        function seekFraction(clientX) {
            var rect = track.getBoundingClientRect();
            if (!rect.width) {
                return 0;
            }
            return Math.min(1, Math.max(0, (clientX - rect.left) / rect.width));
        }

        function seekTo(clientX) {
            if (!state.total) {
                return;
            }
            var f = seekFraction(clientX);
            try {
                audioEl.currentTime = f * state.total;
            } catch (e) {}
            updateFill();
        }

        audioEl.addEventListener('loadedmetadata', function () {
            if (isFinite(audioEl.duration)) {
                state.total = audioEl.duration;
            }
            updateTime();
            updateFill();
        });
        audioEl.addEventListener('timeupdate', function () {
            updateTime();
            updateFill();
        });
        audioEl.addEventListener('play', function () {
            root.classList.add('is-playing');
            playBtn.setAttribute('aria-label', 'Пауза');
        });
        audioEl.addEventListener('pause', function () {
            root.classList.remove('is-playing');
            playBtn.setAttribute('aria-label', 'Воспроизвести');
        });
        audioEl.addEventListener('ended', function () {
            root.classList.remove('is-playing');
            playBtn.setAttribute('aria-label', 'Воспроизвести');
            timeEl.textContent = fmt(state.total) + ' / ' + fmt(state.total);
            fill.style.width = '0%';
        });
        audioEl.addEventListener('error', function () {
            timeEl.textContent = '0:00 / 0:00';
        });
        if (audioEl.readyState >= 1 && isFinite(audioEl.duration) && audioEl.duration > 0) {
            state.total = audioEl.duration;
            updateTime();
        }

        playBtn.addEventListener('click', function () {
            if (audioEl.paused) {
                pauseAll();
                if (global.VoicePlayer) {
                    global.VoicePlayer.pauseAll();
                }
                var p = audioEl.play();
                if (p && p.catch) {
                    p.catch(function () {});
                }
            } else {
                audioEl.pause();
            }
        });

        var dragging = false;

        function onMove(e) {
            if (dragging) {
                seekTo(e.clientX);
            }
        }

        function onUp() {
            dragging = false;
            document.removeEventListener('mousemove', onMove);
            document.removeEventListener('mouseup', onUp);
        }

        track.addEventListener('mousedown', function (e) {
            e.preventDefault();
            dragging = true;
            seekTo(e.clientX);
            document.addEventListener('mousemove', onMove);
            document.addEventListener('mouseup', onUp);
        });
        track.addEventListener('touchstart', function (e) {
            if (e.touches && e.touches.length) {
                seekTo(e.touches[0].clientX);
            }
        }, { passive: true });
        track.addEventListener('touchmove', function (e) {
            if (e.touches && e.touches.length) {
                seekTo(e.touches[0].clientX);
            }
        }, { passive: true });
    }

    function initAll(root) {
        var scope = root || document;
        var els = scope.querySelectorAll ? scope.querySelectorAll('audio.attachment-audio') : [];
        for (var i = 0; i < els.length; i++) {
            initPlayer(els[i]);
        }
    }

    function start() {
        initAll(document);
        if (global.MutationObserver) {
            var mo = new MutationObserver(function (mutations) {
                for (var i = 0; i < mutations.length; i++) {
                    var nodes = mutations[i].addedNodes;
                    if (!nodes) {
                        continue;
                    }
                    for (var j = 0; j < nodes.length; j++) {
                        var n = nodes[j];
                        if (n.nodeType !== 1) {
                            continue;
                        }
                        var found = n.tagName === 'AUDIO' ? n : (n.querySelector ? n.querySelector('audio.attachment-audio') : null);
                        if (found) {
                            initPlayer(found);
                        }
                    }
                }
            });
            mo.observe(document.body, { childList: true, subtree: true });
        }
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', start);
    } else {
        start();
    }

    global.AudioPlayer = {
        initAll: initAll,
        pauseAll: pauseAll
    };
})(window);
