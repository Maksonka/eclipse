(function () {
    "use strict";

    var players = [];

    function fmt(t) {
        if (!isFinite(t) || t < 0) t = 0;
        var s = Math.floor(t % 60);
        var m = Math.floor(t / 60) % 60;
        var h = Math.floor(t / 3600);
        var mm = (m < 10 ? '0' : '') + m;
        var ss = (s < 10 ? '0' : '') + s;
        return h > 0 ? h + ':' + mm + ':' + ss : m + ':' + ss;
    }

    function build(video) {
        var root = video.closest('.video-player-root');
        if (!root || root.classList.contains('is-built')) return;
        root.classList.add('is-built');
        video.controls = false;

        var icPlay = '<svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor"><path d="M8 5v14l11-7z"/></svg>';
        var icPause = '<svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor"><path d="M6 5h4v14H6zM14 5h4v14h-4z"/></svg>';
        var icFull = '<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M8 3H3v5M16 3h5v5M8 21H3v-5M16 21h5v-5"/></svg>';

        var overlay = document.createElement('button');
        overlay.type = 'button';
        overlay.className = 'video-player-overlay';
        overlay.innerHTML = icPlay;
        root.appendChild(overlay);

        var controls = document.createElement('div');
        controls.className = 'video-player-controls';

        var playBtn = document.createElement('button');
        playBtn.type = 'button';
        playBtn.className = 'video-player-btn video-player-playbtn';
        playBtn.innerHTML = icPlay;
        controls.appendChild(playBtn);

        var barWrap = document.createElement('div');
        barWrap.className = 'video-player-seek';
        var bar = document.createElement('div');
        bar.className = 'video-player-bar';
        var fill = document.createElement('div');
        fill.className = 'video-player-fill';
        var buf = document.createElement('div');
        buf.className = 'video-player-buffer';
        fill.innerHTML = '';
        bar.appendChild(buf);
        bar.appendChild(fill);
        barWrap.appendChild(bar);
        controls.appendChild(barWrap);

        var timeEl = document.createElement('span');
        timeEl.className = 'video-player-time';
        timeEl.textContent = '0:00 / 0:00';
        controls.appendChild(timeEl);

        var fsBtn = document.createElement('button');
        fsBtn.type = 'button';
        fsBtn.className = 'video-player-btn video-player-fsbtn';
        fsBtn.innerHTML = icFull;
        controls.appendChild(fsBtn);

        root.appendChild(controls);

        var st = {
            video: video, root: root, overlay: overlay, playBtn: playBtn,
            bar: bar, fill: fill, buf: buf, timeEl: timeEl, fsBtn: fsBtn,
            controls: controls, total: 0
        };
        players.push(st);

        function updateTime() {
            var cur = video.currentTime || 0;
            timeEl.textContent = fmt(cur) + ' / ' + fmt(st.total);
            fill.style.width = st.total ? (cur / st.total * 100) + '%' : '0%';
        }

        function toggle() {
            if (video.paused) {
                video.play().catch(function () {});
            } else {
                video.pause();
            }
        }

        function setPlaying(playing) {
            root.classList.toggle('is-playing', !!playing);
            if (playing) {
                playBtn.innerHTML = icPause;
                overlay.innerHTML = icPause;
                overlay.classList.add('hide-on-play');
            } else {
                playBtn.innerHTML = icPlay;
                overlay.innerHTML = icPlay;
                overlay.classList.remove('hide-on-play');
            }
        }

        video.addEventListener('loadedmetadata', function () {
            if (isFinite(video.duration)) st.total = video.duration;
            updateTime();
        });
        video.addEventListener('durationchange', function () {
            if (isFinite(video.duration)) st.total = video.duration;
            updateTime();
        });
        video.addEventListener('timeupdate', updateTime);
        video.addEventListener('progress', function () {
            try {
                if (video.buffered && video.buffered.length) {
                    var end = video.buffered.end(video.buffered.length - 1);
                    buf.style.width = st.total ? (end / st.total * 100) + '%' : '0%';
                }
            } catch (e) {}
        });
        video.addEventListener('play', function () { setPlaying(true); });
        video.addEventListener('pause', function () { setPlaying(false); });
        video.addEventListener('ended', function () {
            setPlaying(false);
            fill.style.width = '0%';
            buf.style.width = '0%';
            timeEl.textContent = fmt(st.total) + ' / ' + fmt(st.total);
        });

        playBtn.addEventListener('click', toggle);
        overlay.addEventListener('click', toggle);

        function seekFrom(e) {
            if (!st.total) return;
            var rect = bar.getBoundingClientRect();
            var ratio = Math.max(0, Math.min(1, (e.clientX - rect.left) / rect.width));
            video.currentTime = ratio * st.total;
        }
        bar.addEventListener('click', seekFrom);

        st.fsBtn.addEventListener('click', function () {
            if (document.fullscreenElement) {
                document.exitFullscreen();
            } else if (root.requestFullscreen) {
                root.requestFullscreen();
            }
        });

        var hideTimer = null;
        function showControls() {
            controls.style.opacity = '1';
            overlay.style.opacity = '1';
            if (hideTimer) clearTimeout(hideTimer);
            if (!video.paused) {
                hideTimer = setTimeout(function () {
                    controls.style.opacity = '0';
                    overlay.style.opacity = '0';
                }, 2500);
            }
        }
        root.addEventListener('mousemove', showControls);
        root.addEventListener('touchstart', showControls, { passive: true });
        root.addEventListener('mouseleave', function () {
            if (!video.paused) {
                controls.style.opacity = '0';
                overlay.style.opacity = '0';
            }
        });
        showControls();
    }

    function initRoots(scope) {
        var roots = (scope || document).querySelectorAll('.video-player-root video');
        for (var i = 0; i < roots.length; i++) build(roots[i]);
    }

    function start() {
        initRoots(document);
        var obs = new MutationObserver(function (mutations) {
            for (var i = 0; i < mutations.length; i++) {
                var nodes = mutations[i].addedNodes;
                for (var j = 0; j < nodes.length; j++) {
                    var n = nodes[j];
                    if (!n.querySelectorAll) continue;
                    var vids = n.querySelectorAll('.video-player-root video');
                    for (var k = 0; k < vids.length; k++) build(vids[k]);
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
})();
