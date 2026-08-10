var hub = document.getElementById('watch-hub');
var roomView = document.getElementById('watch-room-view');
var roomTitle = document.getElementById('room-title');
var roomMeta = document.getElementById('room-meta');
var roomsList = document.getElementById('rooms-list');
var createForm = document.getElementById('create-room-form');
var roomNameInput = document.getElementById('room-name');
var joinForm = document.getElementById('join-room-form');
var roomCodeInput = document.getElementById('room-code');
var leaveBtn = document.getElementById('leave-room-btn');
var inviteBtn = document.getElementById('invite-btn');
var videoEl = document.getElementById('watch-video');
var ytWrap = document.getElementById('watch-yt');
var embedNote = document.getElementById('watch-embed-note');
var playerEmpty = document.getElementById('player-empty');
var playerEmptySub = document.getElementById('player-empty-sub');
var playerOverlay = document.getElementById('player-overlay');
var playerOverlayBtn = document.getElementById('player-overlay-btn');
var playBtn = document.getElementById('play-btn');
var pauseBtn = document.getElementById('pause-btn');
var nextBtn = document.getElementById('next-btn');
var videoUrlInput = document.getElementById('video-url-input');
var setUrlBtn = document.getElementById('set-url-btn');
var hostHint = document.getElementById('host-hint');
var playlistList = document.getElementById('playlist-list');
var playlistAddForm = document.getElementById('playlist-add-form');
var playlistUrlInput = document.getElementById('playlist-url-input');
var playlistTitleInput = document.getElementById('playlist-title-input');
var playlistHostHint = document.getElementById('playlist-host-hint');
var videoSearchBtn = document.getElementById('video-search-btn');
var videoSearchOverlay = document.getElementById('video-search-overlay');
var videoSearchClose = document.getElementById('video-search-close');
var videoSearchForm = document.getElementById('video-search-form');
var videoSearchInput = document.getElementById('video-search-input');
var videoSearchStatus = document.getElementById('video-search-status');
var videoSearchResults = document.getElementById('video-search-results');
var chatMessages = document.getElementById('watch-chat-messages');
var chatForm = document.getElementById('watch-chat-form');
var chatInput = document.getElementById('watch-chat-input');
var toast = document.getElementById('watch-toast');
var seekRow = document.getElementById('seek-row');
var seekBar = document.getElementById('seek-bar');
var seekTime = document.getElementById('seek-time');
var hostScrubbing = false;
var lastSeekTime = 0;
var lastSendControlTs = 0;

var socket = new SockJS('/ws');
var stompClient = Stomp.over(socket);
stompClient.debug = null;

var activeRoom = null;
var isHost = false;
var currentVideoUrl = null;
var applyingSync = false;
var syncSub = null;
var chatSub = null;
var playlistSub = null;
var reactionSub = null;
var reactionLayer = document.getElementById('reaction-layer');
var reactionBar = document.getElementById('reaction-bar');
var liveBadge = document.getElementById('room-live-badge');
var fullscreenBtn = document.getElementById('fullscreen-btn');
var playlist = { currentItemId: null, items: [] };
var didInitialJoin = false;
var connectErrorShown = false;

/* Player adapter: html5 | youtube | embed */
var playerMode = null;
var currentMeta = null;
var ytPlayer = null;
var ytApiReady = false;
var ytPlayAttempt = 0;
var ytAutoplayResolved = false;
var ytControlsForHost = -1;
var pendingYt = null;
var embedStarted = false;
var embedTiming = { posMs: 0, startedAt: 0, playing: false };
var hlsPlayer = null;
var directSrc = null;
var pendingHtml5Snap = null;

function embedTimeMs() {
    if (!embedTiming.playing) return embedTiming.posMs;
    return embedTiming.posMs + (Date.now() - embedTiming.startedAt);
}

function driftTargetMs(posMs, updatedAtMs) {
    var t = posMs || 0;
    if (updatedAtMs) {
        var elapsed = Date.now() - updatedAtMs;
        if (elapsed > 0 && elapsed < 60000) t += elapsed;
    }
    return t;
}

function parseVideoUrl(url) {
    if (!url) return null;
    var yt = url.match(/(?:youtube\.com\/(?:watch\?(?:.*&)?v=|shorts\/|embed\/|live\/)|youtu\.be\/)([\w-]{11})/);
    if (yt) return { type: 'youtube', id: yt[1] };
    var vk = url.match(/(?:vk\.com\/video|vkvideo\.ru\/(?:video|play))(-?\d+)_(\d+)/i);
    if (vk) return { type: 'vk', oid: vk[1], id: vk[2] };
    var rt = url.match(/rutube\.ru\/(?:video|play\/embed)\/([\w-]+)/i);
    if (rt) return { type: 'rutube', id: rt[1] };
    var vim = url.match(/vimeo\.com\/(\d+)/i);
    if (vim) return { type: 'vimeo', id: vim[1] };
    if (/\.(mp4|webm|ogv|ogg|mov)(\?.*)?$/i.test(url)) return { type: 'html5', url: url };
    return null;
}

function isSupportedVideoUrl(url) {
    return parseVideoUrl(url) !== null;
}

function embedUrl(meta, autoplay, startSec) {
    if (meta.type === 'vk') {
        var u = 'https://vk.com/video_ext.php?oid=' + meta.oid + '&id=' + meta.id;
        if (autoplay) u += '&autoplay=1';
        if (startSec) u += '&start=' + Math.floor(startSec);
        return u;
    }
    if (meta.type === 'rutube') {
        var r = 'https://rutube.ru/play/embed/' + meta.id;
        var q = [];
        if (autoplay) q.push('autoplay=1');
        if (startSec) q.push('start=' + Math.floor(startSec));
        return q.length ? r + '?' + q.join('&') : r;
    }
    if (meta.type === 'vimeo') {
        var v = 'https://player.vimeo.com/video/' + meta.id;
        if (autoplay) v += '?autoplay=1';
        if (startSec) v += '#t=' + Math.floor(startSec) + 's';
        return v;
    }
    return '';
}

function mountEmbed(meta, autoplay, startSec) {
    if (!ytWrap || !meta) return;
    ytWrap.innerHTML = '<iframe src="' + embedUrl(meta, autoplay, startSec) + '" allow="autoplay; fullscreen; encrypted-media; picture-in-picture; clipboard-write" frameborder="0" scrolling="no" allowfullscreen></iframe>';
}

function destroyHls() {
    if (hlsPlayer) { try { hlsPlayer.destroy(); } catch (e) {} hlsPlayer = null; }
    directSrc = null;
}

function playHls(el, src) {
    destroyHls();
    directSrc = src;
    if (window.Hls && Hls.isSupported()) {
        var hls = new Hls({ maxBufferLength: 30 });
        hlsPlayer = hls;
        hls.on(Hls.Events.ERROR, function (evt, data) {
            if (!data.fatal) return;
            if (data.type === Hls.ErrorTypes.MEDIA_ERROR) {
                try { hls.recoverMediaError(); } catch (e) {}
            } else if (data.type === Hls.ErrorTypes.NETWORK_ERROR) {
                try { hls.startLoad(); } catch (e) {}
            } else {
                fallbackToEmbed();
            }
        });
        hls.loadSource(src);
        hls.attachMedia(el);
    } else if (el.canPlayType('application/vnd.apple.mpegurl')) {
        el.src = src;
        el.load();
    } else {
        fallbackToEmbed();
    }
}

function fallbackToEmbed() {
    if (!currentMeta || !directSrc || !currentVideoUrl) return;
    var wasPlaying = !!activeRoom && activeRoom.status === 'PLAYING';
    var pos = activeRoom ? (activeRoom.positionMs || 0) : 0;
    var meta = currentMeta;
    destroyHls();
    playerMode = meta.type;
    videoEl.pause();
    videoEl.removeAttribute('src');
    videoEl.load();
    videoEl.hidden = true;
    ytWrap.hidden = false;
    embedNote.hidden = false;
    mountEmbed(meta, wasPlaying, pos / 1000);
    embedStarted = wasPlaying;
    embedTiming.posMs = pos;
    embedTiming.startedAt = Date.now();
    embedTiming.playing = wasPlaying;
    hidePlayerOverlay();
    showToast('Прямое воспроизведение недоступно — включена вставка');
}

function loadYouTubeApi() {
    if (window.YT && YT.Player) { ytApiReady = true; maybeCreateYt(); return; }
    var tag = document.createElement('script');
    tag.src = 'https://www.youtube.com/iframe_api';
    tag.async = true;
    document.head.appendChild(tag);
}

function maybeCreateYt() {
    if (!ytApiReady || !pendingYt) return;
    var p = pendingYt;
    pendingYt = null;
    ensureYtPlayer(p.id, function () {
        syncPlayback(p.status, p.posMs, p.updatedAtMs, false);
    });
}

function ensureYtPlayer(videoId, cb) {
    if (ytPlayer) {
        if (ytPlayer.videoId === videoId) { if (cb) cb(); return; }
        try { ytPlayer.destroy(); } catch (e) {}
        ytPlayer = null;
    }
    ytPlayer = new YT.Player('watch-yt', {
        width: '100%',
        height: '100%',
        videoId: videoId,
        playerVars: {
            autoplay: 0,
            controls: isHost ? 1 : 0,
            disablekb: isHost ? 0 : 1,
            modestbranding: 1,
            rel: 0,
            playsinline: 1
        },
        events: {
            onReady: function () { if (cb) cb(); },
            onStateChange: function (e) { onYtStateChange(e.data); }
        }
    });
    ytControlsForHost = isHost ? 1 : 0;
    ytPlayer.videoId = videoId;
}

function onYtStateChange(state) {
    if (state === 1) {
        ytAutoplayResolved = true;
        hidePlayerOverlay();
    }
    if (!activeRoom || !ytPlayer || applyingSync || !isHost) return;
    var pos = (ytPlayer.getCurrentTime() || 0) * 1000;
    if (state === 1) {
        sendControl('PLAYING', pos);
    } else if (state === 2) {
        sendControl('PAUSED', pos);
    } else if (state === 0) {
        if (hasNextInPlaylist()) {
            stompClient.send('/app/room.playlist.next', {}, JSON.stringify({ roomId: activeRoom.roomId }));
        } else {
            sendControl('PAUSED', pos);
        }
    }
}

function checkYtAutoplay() {
    if (!ytPlayer || playerMode !== 'youtube' || ytPlayAttempt === 0 || ytAutoplayResolved) return;
    var st;
    try { st = ytPlayer.getPlayerState(); } catch (e) { return; }
    if (st === 1) {
        ytAutoplayResolved = true;
        hidePlayerOverlay();
        return;
    }
    if (st === 2 || st === 0) return;
    if (Date.now() - ytPlayAttempt > 1500) showPlayerOverlay();
}

function getCurrentTimeMs() {
    if (playerMode === 'youtube' && ytPlayer) return (ytPlayer.getCurrentTime() || 0) * 1000;
    if (playerMode === 'html5') {
        if (activeRoom && typeof activeRoom.positionMs === 'number') return activeRoom.positionMs;
        return videoEl && videoEl.currentTime ? videoEl.currentTime * 1000 : 0;
    }
    return embedTimeMs();
}

function showToast(text) {
    if (!toast) return;
    toast.textContent = text;
    toast.hidden = false;
    clearTimeout(showToast._t);
    showToast._t = setTimeout(function () {
        toast.hidden = true;
    }, 3500);
}

function saveRoom(room) {
    try {
        localStorage.setItem('eclipseWatchRoom', JSON.stringify({ roomId: room.roomId, roomCode: room.roomCode }));
    } catch (e) {}
}

function getSavedRoom() {
    try {
        var raw = localStorage.getItem('eclipseWatchRoom');
        return raw ? JSON.parse(raw) : null;
    } catch (e) {
        return null;
    }
}

function clearSavedRoom() {
    try {
        localStorage.removeItem('eclipseWatchRoom');
    } catch (e) {}
}

function copyInviteLink() {
    if (!activeRoom) return;
    var code = activeRoom.roomCode;
    function fallback() {
        var ta = document.createElement('textarea');
        ta.value = code;
        ta.style.position = 'fixed';
        ta.style.opacity = '0';
        document.body.appendChild(ta);
        ta.select();
        try { document.execCommand('copy'); } catch (e) {}
        document.body.removeChild(ta);
        showToast('Код скопирован: ' + code);
    }
    if (navigator.clipboard && navigator.clipboard.writeText) {
        navigator.clipboard.writeText(code).then(function () {
            showToast('Код скопирован: ' + code);
        }, fallback);
    } else {
        fallback();
    }
}

function scrollChat() {
    chatMessages.scrollTop = chatMessages.scrollHeight;
}

function sendControl(status, positionMs, videoUrl, restart) {
    if (!stompClient.connected || !activeRoom) return;
    var payload = { roomId: activeRoom.roomId };
    if (status) payload.status = status;
    if (typeof positionMs === 'number') payload.positionMs = Math.round(positionMs);
    if (videoUrl) payload.videoUrl = videoUrl;
    if (restart) payload.restart = true;
    lastSendControlTs = Date.now();
    stompClient.send('/app/room.control', {}, JSON.stringify(payload));
}

/* ===== Room view ===== */

function showRoomView() {
    hub.hidden = true;
    roomView.hidden = false;
    chatMessages.innerHTML = '<div class="sidebar-empty">Загрузка…</div>';
    resetPlayer();
}

function resetRoomView() {
    if (syncSub) { try { syncSub.unsubscribe(); } catch (e) {} syncSub = null; }
    if (chatSub) { try { chatSub.unsubscribe(); } catch (e) {} chatSub = null; }
    if (playlistSub) { try { playlistSub.unsubscribe(); } catch (e) {} playlistSub = null; }
    if (reactionSub) { try { reactionSub.unsubscribe(); } catch (e) {} reactionSub = null; }
    clearReactions();
    resetPlayer();
    activeRoom = null;
    isHost = false;
    currentVideoUrl = null;
    playlist = { currentItemId: null, items: [] };
    chatMessages.innerHTML = '';
    roomView.hidden = true;
    hub.hidden = false;
    videoUrlInput.disabled = true;
    setUrlBtn.disabled = true;
    playBtn.disabled = true;
    pauseBtn.disabled = true;
    nextBtn.disabled = true;
    hostHint.hidden = true;
    playlistHostHint.hidden = true;
    if (liveBadge) liveBadge.hidden = true;
    var visBadge = document.getElementById('room-visibility-badge');
    if (visBadge) visBadge.hidden = true;
    renderPlaylist();
    hidePlayerOverlay();
}

function resetPlayer() {
    currentVideoUrl = null;
    playerMode = null;
    currentMeta = null;
    if (ytPlayer) { try { ytPlayer.destroy(); } catch (e) {} ytPlayer = null; }
    ytPlayAttempt = 0;
    ytAutoplayResolved = false;
    embedStarted = false;
    embedTiming = { posMs: 0, startedAt: 0, playing: false };
    pendingHtml5Snap = null;
    destroyHls();
    videoEl.pause();
    videoEl.removeAttribute('src');
    videoEl.load();
    ytWrap.hidden = true;
    embedNote.hidden = true;
    playerEmpty.hidden = false;
    playerEmptySub.textContent = isHost ? 'Вставьте ссылку на видео в поле ниже' : 'Хост ещё не добавил видео';
    if (seekRow) seekRow.hidden = true;
    hidePlayerOverlay();
}

function loadVideo(url, posMs, status, updatedAtMs) {
    var meta = parseVideoUrl(url);
    currentVideoUrl = url;
    if (!meta) {
        resetPlayer();
        return;
    }
    var idChanged = !currentMeta || currentMeta.type !== meta.type
        || (meta.type === 'youtube' && currentMeta.id !== meta.id)
        || (meta.type !== 'html5' && currentMeta.type === meta.type && currentMeta.id !== meta.id);
    currentMeta = meta;
    if (idChanged) {
        if (ytPlayer) { try { ytPlayer.destroy(); } catch (e) {} ytPlayer = null; }
        ytPlayAttempt = 0;
        ytAutoplayResolved = false;
        pendingHtml5Snap = null;
        videoEl.pause();
        videoEl.removeAttribute('src');
        videoEl.load();
        videoEl.hidden = true;
        ytWrap.hidden = true;
        embedNote.hidden = true;
    }
    playerMode = meta.type;
    playerEmpty.hidden = true;
    updateSeekControls();
    if (meta.type === 'html5') {
        videoEl.hidden = false;
        if (videoEl.src !== meta.url) {
            videoEl.src = meta.url;
            videoEl.load();
        }
        syncPlayback(status, posMs, updatedAtMs, false);
    } else if (meta.type === 'youtube') {
        ytWrap.hidden = false;
        if (idChanged) {
            pendingYt = { id: meta.id, status: status, posMs: posMs, updatedAtMs: updatedAtMs };
            loadYouTubeApi();
        } else {
            syncPlayback(status, posMs, updatedAtMs, false);
        }
    } else if (meta.type === 'rutube') {
        playerMode = 'html5';
        videoEl.hidden = false;
        var playSrc = '/api/video/play/rutube?id=' + encodeURIComponent(meta.id);
        if (idChanged || directSrc !== playSrc) {
            videoEl.removeAttribute('src');
            videoEl.load();
            playHls(videoEl, playSrc);
        }
        syncPlayback(status, posMs, updatedAtMs, false);
    } else if (meta.type === 'vk') {
        playerMode = 'html5';
        videoEl.hidden = false;
        videoEl.muted = false;
        var vkSrc = '/api/video/play/vk?id=' + encodeURIComponent(meta.oid + '_' + meta.id);
        if (idChanged || directSrc !== vkSrc) {
            videoEl.removeAttribute('src');
            videoEl.load();
            destroyHls();
            directSrc = vkSrc;
            fetch(vkSrc).then(function (r) { return r.ok ? r.text() : null; }).then(function (t) {
                if (!t) { fallbackToEmbed(); return; }
                if (!currentMeta || currentMeta.id !== meta.id) return;
                var sep = t.indexOf('|');
                var u = sep > 0 ? t.substring(sep + 1) : t;
                if (sep > 0 && t.substring(0, sep) === 'hls') {
                    playHls(videoEl, u);
                } else {
                    videoEl.src = u;
                    videoEl.load();
                }
                syncPlayback(status, posMs, updatedAtMs, false);
            });
        } else {
            syncPlayback(status, posMs, updatedAtMs, false);
        }
    } else {
        ytWrap.hidden = false;
        embedNote.hidden = false;
        if (idChanged || ytWrap.querySelector('iframe') === null) {
            mountEmbed(meta, status === 'PLAYING', (posMs || 0) / 1000);
            embedStarted = status === 'PLAYING';
        }
        if (status === 'PLAYING') {
            embedTiming.posMs = posMs || 0;
            embedTiming.startedAt = Date.now();
            embedTiming.playing = true;
        } else {
            embedTiming.posMs = posMs || 0;
            embedTiming.playing = false;
        }
        hidePlayerOverlay();
    }
}

function syncPlayback(status, posMs, updatedAtMs, restart) {
    if (!status) return;
    if (playerMode === 'html5') {
        if (!videoEl.src) {
            if (restart && !isHost) {
                pendingHtml5Snap = { target: driftTargetMs(posMs, updatedAtMs) / 1000, play: status === 'PLAYING', ts: Date.now() };
            }
            return;
        }
        if (status === 'PLAYING') {
            var target = driftTargetMs(posMs, updatedAtMs) / 1000;
            if (restart) {
                pendingHtml5Snap = { target: target, play: true, ts: Date.now() };
                showToast('[sync] прыжок на ' + target.toFixed(1) + 'с (было ' + videoEl.currentTime.toFixed(1) + 'с)');
            }
            if (restart || (!isHost && Math.abs(videoEl.currentTime - target) > 2)) {
                applyingSync = true;
                try { videoEl.currentTime = target; } catch (e) {}
                applyingSync = false;
            }
            var p = videoEl.play();
            if (p && p.catch) p.catch(function () { showPlayerOverlay(); });
        } else {
            applyingSync = true;
            videoEl.pause();
            if (!isHost) {
                var pv = (posMs || 0) / 1000;
                if (Math.abs(videoEl.currentTime - pv) > 2) {
                    try { videoEl.currentTime = pv; } catch (e) {}
                }
            }
            applyingSync = false;
            hidePlayerOverlay();
        }
    } else if (playerMode === 'youtube') {
        if (!ytApiReady || !ytPlayer) {
            if (!pendingYt) {
                pendingYt = { id: currentMeta ? currentMeta.id : null, status: status, posMs: posMs, updatedAtMs: updatedAtMs };
                loadYouTubeApi();
            }
            return;
        }
        var t = driftTargetMs(posMs, updatedAtMs) / 1000;
        var cur = 0;
        try { cur = ytPlayer.getCurrentTime() || 0; } catch (e) {}
        var st = -1;
        try { st = ytPlayer.getPlayerState(); } catch (e) {}
        if (restart || (!isHost && Math.abs(cur - t) > 2)) {
            applyingSync = true;
            try { ytPlayer.seekTo(t, true); } catch (e) {}
            applyingSync = false;
        }
        if (status === 'PLAYING') {
            if (st !== 1) {
                applyingSync = true;
                try {
                    ytPlayAttempt = Date.now();
                    ytPlayer.playVideo();
                } catch (e) {}
                applyingSync = false;
            }
        } else {
            if (st !== 2) {
                applyingSync = true;
                try { ytPlayer.pauseVideo(); } catch (e) {}
                applyingSync = false;
            }
            hidePlayerOverlay();
        }
    } else {
        if (status === 'PLAYING') {
            if (((restart && !isHost) || !embedStarted) && currentMeta) {
                embedStarted = true;
                mountEmbed(currentMeta, true, (posMs || 0) / 1000);
            }
            embedTiming.posMs = posMs || 0;
            embedTiming.startedAt = Date.now();
            embedTiming.playing = true;
            hidePlayerOverlay();
        } else {
            var wasStarted = embedStarted;
            embedStarted = false;
            embedTiming.playing = false;
            if (posMs != null) embedTiming.posMs = posMs || 0;
            if (wasStarted && currentMeta) mountEmbed(currentMeta, false, embedTiming.posMs / 1000);
        }
    }
}

function updateHeader() {
    roomTitle.textContent = activeRoom.name || 'Комната';
    var members = activeRoom.members ? activeRoom.members.length : 0;
    roomMeta.textContent = 'Код: ' + activeRoom.roomCode + ' · ' + activeRoom.hostUsername + ' · ' + members + ' чел.';
    if (liveBadge) {
        liveBadge.hidden = false;
        liveBadge.setAttribute('title', 'Вы уже в комнате ' + activeRoom.roomCode);
    }
    var visBadge = document.getElementById('room-visibility-badge');
    if (visBadge) {
        var isPrivate = activeRoom.visibility === 'PRIVATE';
        visBadge.hidden = false;
        visBadge.className = 'room-visibility-badge' + (isPrivate ? ' is-private' : ' is-public');
        visBadge.textContent = isPrivate ? '🔒 Приватная' : '🌍 Публичная';
        visBadge.setAttribute('title', isPrivate
            ? 'Комната по приглашению: вход только по коду'
            : 'Публичная комната: видна в ленте активных комнат');
    }
}

function formatTime(sec) {
    if (!isFinite(sec) || sec < 0) return '0:00';
    var s = Math.floor(sec);
    var m = Math.floor(s / 60);
    s = s % 60;
    var h = Math.floor(m / 60);
    m = m % 60;
    var out = (h > 0 ? h + ':' + (m < 10 ? '0' : '') : '') + m + ':' + (s < 10 ? '0' : '') + s;
    return out;
}

function seekPosSec() {
    if (playerMode === 'html5') return videoEl && isFinite(videoEl.currentTime) ? videoEl.currentTime : 0;
    if (playerMode === 'youtube' && ytPlayer && ytApiReady) {
        try { return ytPlayer.getCurrentTime() || 0; } catch (e) {}
    }
    return 0;
}

function seekDurationSec() {
    if (playerMode === 'html5') return videoEl && isFinite(videoEl.duration) ? videoEl.duration : 0;
    if (playerMode === 'youtube' && ytPlayer && ytApiReady) {
        try { return ytPlayer.getDuration() || 0; } catch (e) {}
    }
    return 0;
}

function updateSeekBar() {
    if (!seekBar || !seekTime) return;
    if (playerMode !== 'html5' && playerMode !== 'youtube') {
        seekRow.hidden = true;
        return;
    }
    seekRow.hidden = false;
    var dur = seekDurationSec();
    var maxV = dur > 0 && isFinite(dur) ? Math.round(dur * 1000) : 1000;
    if (maxV > 0 && seekBar.max !== String(maxV)) seekBar.max = String(maxV);
    var pos;
    if (isHost && activeRoom) {
        pos = (activeRoom.positionMs || 0) / 1000;
    } else {
        pos = seekPosSec();
    }
    var val = Math.min(Math.max(pos * 1000, 0), maxV);
    if (!hostScrubbing) seekBar.value = String(val);
    seekTime.textContent = formatTime(pos) + ' / ' + formatTime(dur);
    seekBar.disabled = !isHost;
}

function updateSeekControls() {
    if (!seekRow) return;
    if (!currentVideoUrl || (playerMode !== 'html5' && playerMode !== 'youtube')) {
        seekRow.hidden = true;
        return;
    }
    updateSeekBar();
}

function updateControls() {
    var hostNow = isHost;
    var hasUrl = !!currentVideoUrl;
    playBtn.disabled = !hostNow || !hasUrl;
    pauseBtn.disabled = !hostNow || !hasUrl;
    nextBtn.disabled = !hostNow || !(playlist.items || []).length;
    setUrlBtn.disabled = !hostNow;
    videoUrlInput.disabled = !hostNow;
    hostHint.hidden = !hostNow;
    playlistHostHint.hidden = !hostNow;
    if (!hostNow && !currentVideoUrl) {
        playerEmptySub.textContent = 'Синхронизация с хостом…';
    }
    updateSeekControls();
    syncNativeControls();
    renderPlaylist();
}

/* Только хост управляет воспроизведением: гостям скрываем нативные контролы.
   HTML5 — переключаем атрибут; YouTube — controls задаётся при создании,
   поэтому при смене хоста пересоздаём плеер. */
function syncNativeControls() {
    if (playerMode === 'html5') {
        videoEl.controls = false;
    } else if (playerMode === 'youtube' && ytPlayer && ytApiReady) {
        var want = isHost ? 1 : 0;
        if (ytControlsForHost !== want) {
            var vid = currentMeta ? currentMeta.id : null;
            if (vid) {
                var r = activeRoom || {};
                pendingYt = { id: vid, status: r.status, posMs: r.positionMs || 0, updatedAtMs: r.updatedAtMs || 0 };
                if (ytPlayer) { try { ytPlayer.destroy(); } catch (e) {} ytPlayer = null; }
                maybeCreateYt();
            }
        }
    }
}

function showPlayerOverlay() {
    playerOverlay.hidden = false;
}

function hidePlayerOverlay() {
    playerOverlay.hidden = true;
}

function isEmbedMode() {
    return playerMode && playerMode !== 'html5' && playerMode !== 'youtube';
}

function unlockEmbedAutoplay() {
    if (!isEmbedMode() || !currentMeta || !activeRoom) return;
    if (activeRoom.status !== 'PLAYING') return;
    embedStarted = true;
    var pos = activeRoom.positionMs || 0;
    embedTiming.posMs = pos;
    embedTiming.startedAt = Date.now();
    embedTiming.playing = true;
    mountEmbed(currentMeta, true, pos / 1000);
    hidePlayerOverlay();
}

document.addEventListener('pointerdown', unlockEmbedAutoplay, { once: true, capture: true });

function setVideoSource(url, posMs, status, updatedAtMs) {
    loadVideo(url, posMs, status, updatedAtMs);
}

function handleRoomDeleted() {
    clearSavedRoom();
    if (activeRoom) {
        showToast('Комната «' + activeRoom.name + '» закрыта');
    }
    resetRoomView();
}

function applyRoomUpdate(update) {
    if (!activeRoom || update.roomId !== activeRoom.roomId) return;

    if (update.deleted) {
        handleRoomDeleted();
        return;
    }

    if (update.hostUsername) {
        activeRoom.hostUsername = update.hostUsername;
        isHost = update.hostUsername === currentUsername;
    }
    if (update.members) activeRoom.members = update.members;
    if (update.name) activeRoom.name = update.name;
    if (update.roomCode) activeRoom.roomCode = update.roomCode;
    if (update.status) activeRoom.status = update.status;
    if (update.visibility) activeRoom.visibility = update.visibility;
    if (typeof update.positionMs === 'number') activeRoom.positionMs = update.positionMs;
    if (typeof update.updatedAtMs === 'number') activeRoom.updatedAtMs = update.updatedAtMs;
    if (typeof update.videoUrl === 'string') activeRoom.videoUrl = update.videoUrl;

    updateHeader();

    if (update.videoUrl !== currentVideoUrl) {
        setVideoSource(update.videoUrl, update.positionMs, update.status, update.updatedAtMs);
    } else if (isHost && update.lastControlBy === currentUsername) {
        if (playerMode === 'html5' && videoEl.src) {
            if (update.status === 'PLAYING') { var p = videoEl.play(); if (p && p.catch) p.catch(function(){}); }
            else { videoEl.pause(); }
        } else if (playerMode === 'youtube' && ytPlayer && ytApiReady) {
            if (update.status === 'PLAYING') { try { ytPlayer.playVideo(); } catch(e){} }
            else { try { ytPlayer.pauseVideo(); } catch(e){} }
        }
    } else {
        syncPlayback(update.status, update.positionMs, update.updatedAtMs, !!update.restart);
    }
    updateControls();
}

function onRoomJoined(room) {
    if (!room || !room.roomId) return;
    activeRoom = room;
    isHost = room.hostUsername === currentUsername;
    saveRoom(room);
    showRoomView();

    if (syncSub) { try { syncSub.unsubscribe(); } catch (e) {} }
    if (chatSub) { try { chatSub.unsubscribe(); } catch (e) {} }
    if (playlistSub) { try { playlistSub.unsubscribe(); } catch (e) {} }
    if (reactionSub) { try { reactionSub.unsubscribe(); } catch (e) {} }
    syncSub = stompClient.subscribe('/topic/room.' + room.roomId, function (payload) {
        applyRoomUpdate(JSON.parse(payload.body));
    });
    chatSub = stompClient.subscribe('/topic/room.' + room.roomId + '.chat', function (payload) {
        appendChatMessage(JSON.parse(payload.body));
    });
    playlistSub = stompClient.subscribe('/topic/room.' + room.roomId + '.playlist', function (payload) {
        onPlaylistUpdate(JSON.parse(payload.body));
    });
    reactionSub = stompClient.subscribe('/topic/room.' + room.roomId + '.reactions', function (payload) {
        var r = JSON.parse(payload.body);
        if (r && r.emoji) spawnReaction(r.emoji, r.username);
    });

    loadChatHistory(room.roomId);
    loadPlaylist(room.roomId);
    applyRoomUpdate(room);
}

/* ===== Chat ===== */

function loadChatHistory(roomId) {
    fetch('/watch/' + roomId + '/messages').then(function (r) {
        return r.json();
    }).then(function (messages) {
        chatMessages.innerHTML = '';
        if (Array.isArray(messages) && messages.length) {
            messages.forEach(function (m) {
                appendChatMessage(m, false);
            });
            scrollChat();
        } else {
            chatMessages.innerHTML = '<div class="sidebar-empty">Сообщений пока нет</div>';
        }
    }).catch(function () {
        chatMessages.innerHTML = '<div class="sidebar-empty">Не удалось загрузить чат</div>';
    });
}

function appendChatMessage(msg, notify) {
    var empty = chatMessages.querySelector('.sidebar-empty');
    if (empty) empty.remove();

    var row = document.createElement('div');
    row.className = 'chat-msg ' + (msg.senderUsername === currentUsername ? 'outgoing' : 'incoming');
    if (msg.messageId) row.setAttribute('data-msg-id', msg.messageId);

    var sender = document.createElement('span');
    sender.className = 'chat-msg-sender';
    sender.textContent = msg.senderUsername;

    if (msg.stickerUrl) {
        row.classList.add('chat-msg-sticker');
        var stickerImg = document.createElement('img');
        stickerImg.className = 'chat-msg-content sticker-image';
        stickerImg.src = msg.stickerUrl;
        stickerImg.alt = 'Стикер';
        if (msg.stickerCode) {
            stickerImg.setAttribute('data-sticker-code', msg.stickerCode);
        }
        if (msg.senderUsername === currentUsername) {
            stickerImg.setAttribute('data-outgoing', '1');
        }
        row.appendChild(sender);
        row.appendChild(stickerImg);
    } else {
        var content = document.createElement('span');
        content.className = 'chat-msg-content';
        content.textContent = msg.content;
        row.appendChild(sender);
        row.appendChild(content);
    }

    var meta = document.createElement('time');
    meta.className = 'chat-msg-time';
    meta.textContent = msg.timestamp || '';

    row.appendChild(meta);
    chatMessages.appendChild(row);
    scrollChat();

    if (msg.senderUsername !== currentUsername && activeRoom && notify !== false && window.MessageNotifications) {
        MessageNotifications.show({
            sender: msg.senderUsername,
            title: msg.senderUsername,
            text: msg.stickerUrl ? 'Стикер' : (msg.content || 'Файл'),
            href: '/watch?room=' + activeRoom.roomId
        });
    }
}

/* ===== Reactions ===== */

var REACTION_EMOJIS = ['🔥', '😂', '😮', '❤️', '👍', '👏'];

function sendReaction(emoji) {
    if (!activeRoom || !stompClient.connected) return;
    stompClient.send('/app/room.react', {}, JSON.stringify({ roomId: activeRoom.roomId, emoji: emoji }));
    spawnReaction(emoji, currentUsername);
}

function spawnReaction(emoji, username) {
    if (!reactionLayer) return;
    var el = document.createElement('span');
    el.className = 'reaction-float';
    el.textContent = emoji;
    var drift = (Math.random() * 120 - 60).toFixed(0);
    el.style.left = (18 + Math.random() * 64) + '%';
    el.style.setProperty('--drift', drift + 'px');
    if (username) el.setAttribute('data-user', username);
    reactionLayer.appendChild(el);
    setTimeout(function () {
        if (el.parentNode) el.parentNode.removeChild(el);
    }, 2200);
}

function clearReactions() {
    if (reactionLayer) reactionLayer.innerHTML = '';
}

/* ===== Playlist ===== */

function loadPlaylist(roomId) {
    fetch('/watch/' + roomId + '/playlist').then(function (r) {
        return r.json();
    }).then(function (data) {
        if (data && Array.isArray(data.items)) {
            playlist = data;
            renderPlaylist();
            updateControls();
        }
    }).catch(function () {});
}

function onPlaylistUpdate(data) {
    if (!data || !activeRoom || data.roomId !== activeRoom.roomId) return;
    playlist = data;
    renderPlaylist();
    updateControls();
}

function hasNextInPlaylist() {
    var items = playlist.items || [];
    if (!items.length || !playlist.currentItemId) return false;
    for (var i = 0; i < items.length - 1; i++) {
        if (items[i].itemId === playlist.currentItemId) return true;
    }
    return false;
}

function renderPlaylist() {
    if (!playlistList) return;
    var items = playlist.items || [];
    playlistList.innerHTML = '';
    if (!items.length) {
        var empty = document.createElement('div');
        empty.className = 'sidebar-empty';
        empty.textContent = 'Очередь пуста';
        playlistList.appendChild(empty);
        return;
    }
    items.forEach(function (item, idx) {
        var row = document.createElement('div');
        row.className = 'watch-playlist-item';
        if (playlist.currentItemId && item.itemId === playlist.currentItemId) {
            row.classList.add('is-current');
        }
        row.setAttribute('data-item-id', item.itemId);

        var info = document.createElement('div');
        info.className = 'watch-playlist-item-info';

        var title = document.createElement('span');
        title.className = 'watch-playlist-item-title';
        title.textContent = item.title || item.videoUrl;
        title.title = item.videoUrl;

        var meta = document.createElement('span');
        meta.className = 'watch-playlist-item-meta';
        meta.textContent = (idx + 1) + '. ' + item.addedBy;

        info.appendChild(title);
        info.appendChild(meta);
        row.appendChild(info);

        if (isHost) {
            var play = document.createElement('button');
            play.type = 'button';
            play.className = 'watch-btn small playlist-play-btn';
            play.textContent = '▶';
            play.title = 'Запустить это видео';

            var remove = document.createElement('button');
            remove.type = 'button';
            remove.className = 'watch-btn small danger playlist-remove-btn';
            remove.textContent = '✕';
            remove.title = 'Убрать из очереди';

            row.appendChild(play);
            row.appendChild(remove);
        }
        playlistList.appendChild(row);
    });
}

/* ===== Hub ===== */

function renderRooms(rooms) {
    if (!roomsList) return;
    roomsList.innerHTML = '';
    if (!rooms || rooms.length === 0) {
        var empty = document.createElement('div');
        empty.className = 'sidebar-empty';
        empty.textContent = 'Сейчас нет активных комнат';
        roomsList.appendChild(empty);
        return;
    }
    rooms.forEach(function (room) {
        var item = document.createElement('div');
        item.className = 'watch-room-item';
        item.setAttribute('data-room-id', room.roomId);

        if (room.videoThumb) {
            var thumb = document.createElement('div');
            thumb.className = 'watch-room-thumb';
            var img = document.createElement('img');
            img.src = room.videoThumb;
            img.alt = '';
            img.loading = 'lazy';
            img.referrerPolicy = 'no-referrer';
            thumb.appendChild(img);
            item.appendChild(thumb);
        }

        var info = document.createElement('div');
        info.className = 'watch-room-info';

        var name = document.createElement('span');
        name.className = 'watch-room-name';
        name.textContent = (room.videoTitle && room.videoTitle.trim()) ? room.videoTitle : room.name;

        var meta = document.createElement('span');
        meta.className = 'watch-room-meta';
        meta.textContent = room.hostUsername + ' · ' + room.memberCount + ' чел.';

        info.appendChild(name);
        info.appendChild(meta);

        var members = room.members || [];
        if (members.length > 0) {
            var membersRow = document.createElement('span');
            membersRow.className = 'watch-room-members';
            members.slice(0, 5).forEach(function (m) {
                var avatar = document.createElement('span');
                avatar.className = 'watch-room-member';
                avatar.title = m.username;
                if (m.avatarFilename) {
                    var img = document.createElement('img');
                    img.className = 'watch-room-member-img';
                    img.src = '/uploads/avatars/' + encodeURIComponent(m.avatarFilename);
                    img.alt = m.username;
                    avatar.appendChild(img);
                } else {
                    var letter = document.createElement('span');
                    letter.className = 'watch-room-member-letter';
                    letter.textContent = (m.username || '?').charAt(0).toUpperCase();
                    avatar.appendChild(letter);
                }
                membersRow.appendChild(avatar);
            });
            if (room.memberCount > 5) {
                var more = document.createElement('span');
                more.className = 'watch-room-members-more';
                more.title = room.memberCount + ' зрителей';
                more.textContent = '…';
                membersRow.appendChild(more);
            }
            info.appendChild(membersRow);
        }

        if (room.status === 'PLAYING' && room.videoTitle) {
            var nowPlaying = document.createElement('span');
            nowPlaying.className = 'watch-room-nowplaying';
            var dot = document.createElement('span');
            dot.className = 'watch-nowplaying-dot';
            var title = document.createElement('span');
            title.textContent = 'Сейчас играет';
            nowPlaying.appendChild(dot);
            nowPlaying.appendChild(title);
            info.appendChild(nowPlaying);
        }

        var status = document.createElement('span');
        status.className = 'watch-room-status';
        if (room.status === 'PLAYING') {
            status.classList.add('is-playing');
            status.textContent = 'Сейчас играет';
        } else if (room.status === 'PAUSED') {
            status.classList.add('is-paused');
            status.textContent = 'Пауза';
        } else {
            status.textContent = 'Ожидание';
        }
        info.appendChild(status);

        var btn = document.createElement('button');
        btn.type = 'button';
        btn.className = 'watch-btn small join-room-btn';
        btn.setAttribute('data-room-code', room.roomCode);
        btn.textContent = 'Смотреть';

        item.appendChild(info);
        item.appendChild(btn);
        roomsList.appendChild(item);
    });
}

/* ===== Events ===== */

if (createForm) {
    createForm.addEventListener('submit', function (e) {
        e.preventDefault();
        if (!stompClient.connected) { showToast('Нет соединения с сервером'); return; }
        var name = roomNameInput.value.trim();
        var visibility = selectedVisibility;
        stompClient.send('/app/room.create', {}, JSON.stringify({ name: name, visibility: visibility }));
        roomNameInput.value = '';
    });
}

var visibilityToggle = document.getElementById('room-visibility-toggle');
var selectedVisibility = 'PUBLIC';
if (visibilityToggle) {
    visibilityToggle.addEventListener('click', function (e) {
        var option = e.target.closest('.watch-visibility-option');
        if (!option) return;
        selectedVisibility = option.getAttribute('data-visibility');
        var all = visibilityToggle.querySelectorAll('.watch-visibility-option');
        for (var i = 0; i < all.length; i++) {
            all[i].classList.toggle('is-active', all[i] === option);
        }
        var hint = document.getElementById('create-hint');
        if (hint) {
            hint.textContent = selectedVisibility === 'PRIVATE'
                ? 'Приватная комната видна только по приглашению — код придёт друзьям.'
                : 'Вы станете хостом и будете управлять воспроизведением. Публичные комнаты видны в ленте.';
        }
    });
}

if (joinForm) {
    joinForm.addEventListener('submit', function (e) {
        e.preventDefault();
        if (!stompClient.connected) { showToast('Нет соединения с сервером'); return; }
        var code = roomCodeInput.value.trim().toUpperCase();
        if (!code) return;
        stompClient.send('/app/room.join', {}, JSON.stringify({ roomCode: code }));
        roomCodeInput.value = '';
    });
}

if (roomsList) {
    roomsList.addEventListener('click', function (e) {
        var btn = e.target.closest('.join-room-btn');
        if (!btn) return;
        var code = btn.getAttribute('data-room-code');
        if (code) stompClient.send('/app/room.join', {}, JSON.stringify({ roomCode: code }));
    });
}

if (leaveBtn) {
    leaveBtn.addEventListener('click', function () {
        if (!activeRoom) return;
        if (stompClient.connected) {
            try {
                stompClient.send('/app/room.leave', {}, JSON.stringify({ roomId: activeRoom.roomId }));
            } catch (e) {}
        }
        clearSavedRoom();
        resetRoomView();
        closeVideoSearch();
    });
}

if (inviteBtn) {
    inviteBtn.addEventListener('click', function () {
        if (!activeRoom) return;
        copyInviteLink();
    });
}

if (setUrlBtn) {
    setUrlBtn.addEventListener('click', function () {
        if (!isHost || !activeRoom) return;
        var url = videoUrlInput.value.trim();
        if (!url) { showToast('Вставьте ссылку на видео'); return; }
        if (!isSupportedVideoUrl(url)) { showToast('Неподдерживаемая ссылка: используйте YouTube, VK, Rutube, Vimeo или прямой файл .mp4/.webm'); return; }
        sendControl('PLAYING', 0, url);
        videoUrlInput.value = '';
    });
    videoUrlInput.addEventListener('keydown', function (e) {
        if (e.key === 'Enter') setUrlBtn.click();
    });
}

if (playBtn) {
    playBtn.addEventListener('click', function () {
        if (!isHost || !currentVideoUrl) return;
        if (playerMode && playerMode !== 'html5' && playerMode !== 'youtube') {
            var startPos = embedTiming.playing
                ? 0
                : (activeRoom && activeRoom.status === 'PAUSED' ? (activeRoom.positionMs || 0) : 0);
            embedTiming.posMs = startPos;
            embedTiming.startedAt = Date.now();
            embedTiming.playing = true;
            embedStarted = true;
            if (currentMeta) mountEmbed(currentMeta, true, startPos / 1000);
            sendControl('PLAYING', getCurrentTimeMs(), null, true);
        } else {
            if (playerMode === 'html5') {
                var pp = videoEl.play();
                if (pp && pp.catch) pp.catch(function () { showPlayerOverlay(); });
            }
            sendControl('PLAYING', getCurrentTimeMs(), null, true);
        }
    });
}

if (pauseBtn) {
    pauseBtn.addEventListener('click', function () {
        if (!isHost || !currentVideoUrl) return;
        if (playerMode && playerMode !== 'html5' && playerMode !== 'youtube') {
            var t = embedTimeMs();
            embedTiming.playing = false;
            embedTiming.posMs = t;
            embedStarted = false;
            if (currentMeta) mountEmbed(currentMeta, false, t / 1000);
        } else if (playerMode === 'html5') {
            videoEl.pause();
        }
        sendControl('PAUSED', getCurrentTimeMs());
    });
}

function applyPendingHtml5Snap() {
    if (!pendingHtml5Snap || playerMode !== 'html5') return;
    var snap = pendingHtml5Snap;
    if (!videoEl.src) return;
    if (Date.now() - snap.ts > 15000) {
        pendingHtml5Snap = null;
        return;
    }
    if (Math.abs(videoEl.currentTime - snap.target) > 0.5) {
        applyingSync = true;
        try { videoEl.currentTime = snap.target; } catch (e) {}
        applyingSync = false;
    } else {
        pendingHtml5Snap = null;
        showToast('[sync] гость на ' + snap.target.toFixed(1) + 'с');
        if (snap.play) {
            var p = videoEl.play();
            if (p && p.catch) p.catch(function () {});
        }
    }
}

if (videoEl) {
    videoEl.addEventListener('seeked', applyPendingHtml5Snap);
    videoEl.addEventListener('canplay', applyPendingHtml5Snap);
    videoEl.addEventListener('loadeddata', applyPendingHtml5Snap);
    videoEl.addEventListener('timeupdate', applyPendingHtml5Snap);
    videoEl.addEventListener('play', function () {
        if (!isHost || applyingSync || playerMode !== 'html5') return;
        if (Date.now() - lastSendControlTs < 1500) return;
        sendControl('PLAYING', getCurrentTimeMs());
    });
    videoEl.addEventListener('pause', function () {
        if (!isHost || applyingSync || playerMode !== 'html5') return;
        if (Date.now() - lastSendControlTs < 1500) return;
        sendControl('PAUSED', getCurrentTimeMs());
    });
    videoEl.addEventListener('seeked', function () {
        if (hostScrubbing) { hostScrubbing = false; return; }
        if (!isHost || applyingSync || !activeRoom || playerMode !== 'html5') return;
        if (Date.now() - lastSendControlTs < 1500) return;
        sendControl(activeRoom.status === 'PLAYING' ? 'PLAYING' : 'PAUSED', getCurrentTimeMs());
    });
    videoEl.addEventListener('timeupdate', updateSeekBar);
    videoEl.addEventListener('durationchange', updateSeekBar);
    videoEl.addEventListener('ended', function () {
        if (!isHost || applyingSync || playerMode !== 'html5') return;
        if (Date.now() - lastSendControlTs < 1500) return;
        if (hasNextInPlaylist()) {
            stompClient.send('/app/room.playlist.next', {}, JSON.stringify({ roomId: activeRoom.roomId }));
        } else {
            sendControl('PAUSED', getCurrentTimeMs());
        }
    });
}

if (seekBar) {
    seekBar.addEventListener('input', function () {
        if (!isHost) return;
        hostScrubbing = true;
        var val = (Number(seekBar.value) || 0) / 1000;
        if (playerMode === 'html5') {
            try { videoEl.currentTime = val; } catch (e) {}
        } else if (playerMode === 'youtube' && ytPlayer && ytApiReady) {
            try { ytPlayer.seekTo(val, true); } catch (e) {}
        }
        seekTime.textContent = formatTime(val) + ' / ' + formatTime(seekDurationSec());
    });
    seekBar.addEventListener('change', function () {
        if (!isHost) return;
        var val = (Number(seekBar.value) || 0) / 1000;
        var playing = false;
        if (playerMode === 'html5') {
            playing = videoEl && !videoEl.paused;
            try { videoEl.currentTime = val; } catch (e) {}
        } else if (playerMode === 'youtube' && ytPlayer && ytApiReady) {
            try { playing = ytPlayer.getPlayerState() === 1; } catch (e) {}
            try { ytPlayer.seekTo(val, true); } catch (e) {}
        }
        lastSeekTime = Date.now();
        if (activeRoom) activeRoom.positionMs = Math.round(val * 1000);
        sendControl(playing ? 'PLAYING' : 'PAUSED', Math.round(val * 1000), null, true);
    });
}

if (playlistAddForm) {
    playlistAddForm.addEventListener('submit', function (e) {
        e.preventDefault();
        if (!activeRoom || !stompClient.connected) return;
        var url = playlistUrlInput.value.trim();
        if (!url) { showToast('Вставьте ссылку на видео'); return; }
        if (!isSupportedVideoUrl(url)) { showToast('Неподдерживаемая ссылка: используйте YouTube, VK, Rutube, Vimeo или прямой файл .mp4/.webm'); return; }
        var title = playlistTitleInput.value.trim();
        stompClient.send('/app/room.playlist.add', {}, JSON.stringify({ roomId: activeRoom.roomId, url: url, title: title }));
        playlistUrlInput.value = '';
        playlistTitleInput.value = '';
    });
}

/* ===== Video search ===== */

function openVideoSearch() {
    if (!videoSearchOverlay) return;
    videoSearchOverlay.hidden = false;
    videoSearchResults.innerHTML = '';
    setVideoSearchStatus('');
    if (videoSearchInput) {
        videoSearchInput.value = '';
        setTimeout(function () { videoSearchInput.focus(); }, 50);
    }
}

function closeVideoSearch() {
    if (videoSearchOverlay) videoSearchOverlay.hidden = true;
}

function setVideoSearchStatus(text) {
    if (!videoSearchStatus) return;
    if (text) {
        videoSearchStatus.textContent = text;
        videoSearchStatus.hidden = false;
    } else {
        videoSearchStatus.textContent = '';
        videoSearchStatus.hidden = true;
    }
}

function runVideoSearch(query) {
    if (!videoSearchResults) return;
    videoSearchResults.innerHTML = '<div class="sidebar-empty">Поиск...</div>';
    setVideoSearchStatus('');
    fetch('/api/video/search?q=' + encodeURIComponent(query) + '&limit=12')
        .then(function (r) {
            return r.json().then(function (d) { return { ok: r.ok, data: d }; });
        })
        .then(function (res) {
            if (!res.ok || !res.data) {
                videoSearchResults.innerHTML = '';
                setVideoSearchStatus('Не удалось выполнить поиск. Попробуйте ещё раз.');
                return;
            }
            var results = res.data.results || [];
            if (!results.length) {
                videoSearchResults.innerHTML = '';
                setVideoSearchStatus('Ничего не найдено по запросу «' + query + '».');
                return;
            }
            videoSearchResults.innerHTML = '';
            results.forEach(function (item) {
                videoSearchResults.appendChild(buildVideoSearchResult(item));
            });
        })
        .catch(function () {
            videoSearchResults.innerHTML = '';
            setVideoSearchStatus('Ошибка сети. Попробуйте ещё раз.');
        });
}

function buildVideoSearchResult(item) {
    var row = document.createElement('div');
    row.className = 'video-search-result';
    row.setAttribute('role', 'button');
    row.setAttribute('tabindex', '0');
    row.title = item.title || item.url || '';
    var thumb = document.createElement('img');
    thumb.className = 'video-search-result-thumb';
    thumb.src = item.thumb || '';
    thumb.alt = '';
    thumb.loading = 'lazy';
    thumb.referrerPolicy = 'no-referrer';
    var body = document.createElement('div');
    body.className = 'video-search-result-body';
    var title = document.createElement('span');
    title.className = 'video-search-result-title';
    title.textContent = item.title || item.url || '';
    var badge = document.createElement('span');
    badge.className = 'video-search-result-badge';
    badge.textContent = item.source === 'youtube' ? 'YouTube' : (item.source || '');
    body.appendChild(title);
    body.appendChild(badge);
    row.appendChild(thumb);
    row.appendChild(body);
    row.addEventListener('click', function () {
        addVideoSearchResult(item);
    });
    row.addEventListener('keydown', function (e) {
        if (e.key === 'Enter' || e.key === ' ') {
            e.preventDefault();
            addVideoSearchResult(item);
        }
    });
    return row;
}

function addVideoSearchResult(item) {
    if (!activeRoom) {
        closeVideoSearch();
        showToast('Сначала войдите в комнату');
        return;
    }
    if (!stompClient || !stompClient.connected) {
        showToast('Нет соединения с сервером');
        return;
    }
    var url = item.url || '';
    var title = item.title || '';
    if (!isSupportedVideoUrl(url)) {
        showToast('Не удалось добавить это видео');
        return;
    }
    stompClient.send('/app/room.playlist.add', {}, JSON.stringify({ roomId: activeRoom.roomId, url: url, title: title }));
    closeVideoSearch();
    showToast('Добавлено в очередь');
}

if (videoSearchBtn) {
    videoSearchBtn.addEventListener('click', openVideoSearch);
}
if (videoSearchClose) {
    videoSearchClose.addEventListener('click', closeVideoSearch);
}
if (videoSearchOverlay) {
    videoSearchOverlay.addEventListener('click', function (e) {
        if (e.target === videoSearchOverlay) {
            closeVideoSearch();
        }
    });
}
if (videoSearchForm) {
    videoSearchForm.addEventListener('submit', function (e) {
        e.preventDefault();
        if (!videoSearchInput) return;
        var q = videoSearchInput.value.trim();
        if (!q) {
            setVideoSearchStatus('Введите название видео');
            return;
        }
        runVideoSearch(q);
    });
}
document.addEventListener('keydown', function (e) {
    if (e.key === 'Escape' && videoSearchOverlay && !videoSearchOverlay.hidden) {
        closeVideoSearch();
    }
});

if (nextBtn) {
    nextBtn.addEventListener('click', function () {
        if (!isHost || !activeRoom) return;
        stompClient.send('/app/room.playlist.next', {}, JSON.stringify({ roomId: activeRoom.roomId }));
    });
}

if (playlistList) {
    playlistList.addEventListener('click', function (e) {
        if (!isHost || !activeRoom) return;
        var itemRow = e.target.closest('.watch-playlist-item');
        if (!itemRow) return;
        var itemId = Number(itemRow.getAttribute('data-item-id'));
        if (e.target.closest('.playlist-play-btn')) {
            stompClient.send('/app/room.playlist.play', {}, JSON.stringify({ roomId: activeRoom.roomId, itemId: itemId }));
        } else if (e.target.closest('.playlist-remove-btn')) {
            stompClient.send('/app/room.playlist.remove', {}, JSON.stringify({ roomId: activeRoom.roomId, itemId: itemId }));
        }
    });
}

if (reactionBar) {
    reactionBar.addEventListener('click', function (e) {
        var btn = e.target.closest('.reaction-btn');
        if (!btn) return;
        sendReaction(btn.getAttribute('data-emoji') || btn.textContent);
    });
}

if (playerOverlayBtn) {
    playerOverlayBtn.addEventListener('click', function () {
        hidePlayerOverlay();
        if (playerMode === 'youtube') {
            if (!ytPlayer) return;
            ytAutoplayResolved = true;
            applyingSync = true;
            try {
                ytPlayAttempt = 0;
                ytPlayer.playVideo();
            } catch (e) {}
            applyingSync = false;
        } else {
            var p = videoEl.play();
            if (p && p.catch) p.catch(function () {});
        }
    });
}

if (chatForm) {
    chatForm.addEventListener('submit', function (e) {
        e.preventDefault();
        if (!activeRoom || !stompClient.connected) return;
        var content = chatInput.value.trim();
        if (!content) return;
        stompClient.send('/app/room.message', {}, JSON.stringify({ roomId: activeRoom.roomId, content: content }));
        chatInput.value = '';
        chatInput.focus();
    });
}

/* ===== Connect ===== */

stompClient.connect({}, function () {
    stompClient.subscribe('/topic/rooms', function (payload) {
        renderRooms(JSON.parse(payload.body));
    });
    stompClient.subscribe('/user/queue/room', function (payload) {
        onRoomJoined(JSON.parse(payload.body));
    });
    stompClient.subscribe('/user/queue/room-state', function (payload) {
        onRoomJoined(JSON.parse(payload.body));
    });
    stompClient.subscribe('/user/queue/room-error', function (payload) {
        var data = JSON.parse(payload.body);
        showToast(String(data.error || 'Произошла ошибка'));
    });

    if (!didInitialJoin) {
        didInitialJoin = true;
        var saved = getSavedRoom();
        var code = inviteRoom || (saved && saved.roomCode);
        if (code) {
            stompClient.send('/app/room.join', {}, JSON.stringify({ roomCode: code }));
        }
    }
}, function () {
    if (!connectErrorShown) {
        connectErrorShown = true;
        showToast('Не удалось установить соединение с сервером');
    }
});

stompClient.onerror = function (frame) {
    var text = (frame && frame.headers && frame.headers.message)
        ? frame.headers.message
        : 'Произошла ошибка';
    showToast(String(text));
};

/* ===== YouTube IFrame API ===== */

window.onYouTubeIframeAPIReady = function () {
    ytApiReady = true;
    maybeCreateYt();
};

setInterval(function () {
    if (playerMode === 'youtube') {
        checkYtAutoplay();
    }
    if ((playerMode === 'html5' || playerMode === 'youtube') && !hostScrubbing) {
        if (isHost && activeRoom && videoEl && isFinite(videoEl.currentTime) && videoEl.currentTime > 0) {
            activeRoom.positionMs = Math.round(videoEl.currentTime * 1000);
        }
        updateSeekBar();
    }
}, 500);

/* ===== FULLSCREEN (available to ALL users) ===== */
(function () {
    if (!fullscreenBtn) return;

    var playerWrap = document.querySelector('.watch-player-wrap');

    function isFullscreen() {
        return !!(document.fullscreenElement || document.webkitFullscreenElement || document.mozFullScreenElement);
    }

    function toggleFullscreen() {
        if (isFullscreen()) {
            if (document.exitFullscreen) document.exitFullscreen();
            else if (document.webkitExitFullscreen) document.webkitExitFullscreen();
            else if (document.mozCancelFullScreen) document.mozCancelFullScreen();
        } else {
            var el = playerWrap || document.documentElement;
            if (el.requestFullscreen) el.requestFullscreen();
            else if (el.webkitRequestFullscreen) el.webkitRequestFullscreen();
            else if (el.mozRequestFullScreen) el.mozRequestFullScreen();
        }
    }

    fullscreenBtn.addEventListener('click', toggleFullscreen);

    document.addEventListener('fullscreenchange', updateFsIcon);
    document.addEventListener('webkitfullscreenchange', updateFsIcon);
    document.addEventListener('mozfullscreenchange', updateFsIcon);

    function updateFsIcon() {
        if (isFullscreen()) {
            fullscreenBtn.innerHTML = '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" aria-hidden="true"><path d="M8 3v3a2 2 0 0 1-2 2H3m18 0h-3a2 2 0 0 1-2-2V3m0 18v-3a2 2 0 0 1 2-2h3M3 16h3a2 2 0 0 1 2 2v3" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/></svg>';
            fullscreenBtn.title = 'Выйти из полноэкранного режима';
        } else {
            fullscreenBtn.innerHTML = '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" aria-hidden="true"><path d="M8 3H5a2 2 0 0 0-2 2v3m18 0V5a2 2 0 0 0-2-2h-3m0 18h3a2 2 0 0 0 2-2v-3M3 16v3a2 2 0 0 0 2 2h3" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/></svg>';
            fullscreenBtn.title = 'На весь экран';
        }
    }

    /* Show button on touch devices (no hover) */
    if ('ontouchstart' in window) {
        fullscreenBtn.style.opacity = '1';
    }
})();

/* ===== Stickers ===== */
if (window.StickerUI) {
    StickerUI.init({
        attachSelector: '#watch-chat-input',
        composerSelector: '#watch-chat-form',
        onPick: function (stickerCode) {
            if (!activeRoom || !stompClient.connected) return;
            stompClient.send('/app/room.message', {}, JSON.stringify({ roomId: activeRoom.roomId, stickerCode: stickerCode }));
        }
    });
}
