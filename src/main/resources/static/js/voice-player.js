(function (global) {
    'use strict';

    var BAR_COUNT = 28;

    var players = [];

    function fmt(seconds) {
        if (!isFinite(seconds) || seconds < 0) {
            seconds = 0;
        }
        var s = Math.floor(seconds);
        var m = Math.floor(s / 60);
        s = s % 60;
        return m + ':' + (s < 10 ? '0' : '') + s;
    }

    function barHeight(i) {
        var x = Math.abs(Math.sin((i + 0.5) * 1.1));
        return 22 + Math.round(x * 78);
    }

    function buildBars(waveEl) {
        for (var i = 0; i < BAR_COUNT; i++) {
            var bar = document.createElement('span');
            bar.className = 'voice-player-bar';
            bar.style.height = barHeight(i) + '%';
            waveEl.appendChild(bar);
        }
    }

    function setProgress(waveEl, fraction) {
        if (!waveEl) {
            return;
        }
        var bars = waveEl.children;
        var playedCount = Math.round(Math.min(1, Math.max(0, fraction)) * BAR_COUNT);
        for (var i = 0; i < bars.length; i++) {
            bars[i].classList.toggle('is-played', i < playedCount);
        }
    }

    function setPlaying(stateEl, playing) {
        if (stateEl) {
            stateEl.classList.toggle('is-playing', playing);
        }
    }

    function pauseOthers(audio) {
        players.forEach(function (p) {
            if (p.audio !== audio && p.audio && !p.audio.paused) {
                try {
                    p.audio.pause();
                } catch (e) {}
            }
        });
    }

    function initPlayer(audio) {
        if (!audio || !audio.hasAttribute('data-voice-player')) {
            return;
        }
        if (audio.closest && audio.closest('.voice-player')) {
            return;
        }

        var src = audio.getAttribute('src') || audio.getAttribute('data-src');
        if (!src) {
            return;
        }

        var root = document.createElement('div');
        root.className = 'voice-player';
        root.setAttribute('data-voice-src', src);

        var playBtn = document.createElement('button');
        playBtn.type = 'button';
        playBtn.className = 'voice-player-play';
        playBtn.setAttribute('aria-label', 'Воспроизвести');
        playBtn.innerHTML = '<svg class="voice-player-ic-play" width="16" height="16" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true"><path d="M8 5v14l11-7z"/></svg>' +
            '<svg class="voice-player-ic-pause" width="16" height="16" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true"><path d="M6 5h4v14H6zM14 5h4v14h-4z"/></svg>';

        var wave = document.createElement('div');
        wave.className = 'voice-player-wave';
        wave.setAttribute('data-voice-wave', '1');
        buildBars(wave);

        var timeEl = document.createElement('span');
        timeEl.className = 'voice-player-time';
        timeEl.textContent = '0:00';

        var audioEl;
        if (audio.parentNode) {
            var audioCopy = audio.cloneNode(false);
            audioCopy.setAttribute('data-voice-player', '');
            audioCopy.preload = 'metadata';
            audioCopy.controls = false;
            audioCopy.hidden = true;
            audio.parentNode.replaceChild(root, audio);
            root.appendChild(audioCopy);
            audioEl = audioCopy;
        } else {
            audio.preload = 'metadata';
            audio.controls = false;
            audio.hidden = true;
            root.appendChild(audio);
            audioEl = audio;
        }

        root.appendChild(playBtn);
        root.appendChild(wave);
        root.appendChild(timeEl);

        var state = {
            audio: audioEl,
            root: root,
            playBtn: playBtn,
            wave: wave,
            timeEl: timeEl,
            total: 0
        };
        players.push(state);

        audioEl.addEventListener('loadedmetadata', function () {
            if (isFinite(audioEl.duration)) {
                state.total = audioEl.duration;
            }
            if (audioEl.paused && audioEl.currentTime === 0) {
                timeEl.textContent = fmt(state.total);
                setProgress(wave, 1);
            }
        });
        audioEl.addEventListener('timeupdate', function () {
            timeEl.textContent = fmt(audioEl.currentTime);
            setProgress(wave, state.total ? audioEl.currentTime / state.total : 0);
        });
        if (audioEl.readyState >= 1 && isFinite(audioEl.duration) && audioEl.duration > 0) {
            state.total = audioEl.duration;
            timeEl.textContent = fmt(state.total);
            setProgress(wave, 1);
        }
        audioEl.addEventListener('play', function () {
            setPlaying(root, true);
            playBtn.setAttribute('aria-label', 'Пауза');
        });
        audioEl.addEventListener('pause', function () {
            setPlaying(root, false);
            playBtn.setAttribute('aria-label', 'Воспроизвести');
        });
        audioEl.addEventListener('ended', function () {
            setPlaying(root, false);
            timeEl.textContent = fmt(state.total);
            setProgress(wave, 1);
        });
        audioEl.addEventListener('error', function () {
            timeEl.textContent = '0:00';
        });

        playBtn.addEventListener('click', function () {
            if (audioEl.paused) {
                pauseOthers(audioEl);
                var p = audioEl.play();
                if (p && p.catch) {
                    p.catch(function () {});
                }
            } else {
                audioEl.pause();
            }
        });

        var fallback = audioEl.getAttribute('data-duration-ms');
        if (fallback && parseInt(fallback, 10) > 0) {
            state.total = parseInt(fallback, 10) / 1000;
            timeEl.textContent = fmt(state.total);
        }
    }

    function create(src, durationMs) {
        var audio = document.createElement('audio');
        audio.setAttribute('data-voice-player', '');
        audio.src = src;
        if (durationMs) {
            audio.setAttribute('data-duration-ms', String(durationMs));
        }
        initPlayer(audio);
        var found = players[players.length - 1];
        return found && found.audio === audio ? found.root : null;
    }

    function initAll(root) {
        var scope = root || document;
        var els = scope.querySelectorAll ? scope.querySelectorAll('audio[data-voice-player]') : [];
        for (var i = 0; i < els.length; i++) {
            initPlayer(els[i]);
        }
    }

    global.VoicePlayer = {
        create: create,
        initAll: initAll,
        pauseAll: function () {
            players.forEach(function (p) {
                if (p.audio && !p.audio.paused) {
                    try {
                        p.audio.pause();
                    } catch (e) {}
                }
            });
        }
    };
})(window);
