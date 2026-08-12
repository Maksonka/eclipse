function formatAttachmentSize(bytes) {
    if (bytes === null || bytes === undefined) {
        return '';
    }
    if (bytes < 1024) {
        return bytes + ' Б';
    }
    if (bytes < 1024 * 1024) {
        return (bytes / 1024).toFixed(1).replace('.', ',') + ' КБ';
    }
    return (bytes / (1024 * 1024)).toFixed(1).replace('.', ',') + ' МБ';
}

function escapeHtml(text) {
    var el = document.createElement('div');
    el.textContent = text == null ? '' : String(text);
    return el.innerHTML;
}

function linkifyText(text) {
    if (!text) {
        return '';
    }
    var escaped = escapeHtml(text);
    return escaped.replace(/(https?:\/\/[^\s<]+|www\.[^\s<]+)/g, function (match) {
        var url = match.replace(/[.,;:!?)\]}]+$/, '');
        var tail = match.substring(url.length);
        var href = url;
        if (href.indexOf('http') !== 0) {
            href = 'http://' + href;
        }
        return '<a href="' + href + '" target="_blank" rel="noopener noreferrer">' + url + '</a>' + tail;
    });
}

function linkifyContentElements() {
    var els = document.querySelectorAll ? document.querySelectorAll('.content, .chat-msg-content') : [];
    for (var i = 0; i < els.length; i++) {
        var el = els[i];
        if (el.getAttribute('data-linkified')) {
            continue;
        }
        el.innerHTML = linkifyText(el.textContent);
        el.setAttribute('data-linkified', '1');
    }
}

if (document.addEventListener) {
    document.addEventListener('DOMContentLoaded', linkifyContentElements);
}

function messagePreview(message) {
    if (message.stickerUrl) {
        return 'Стикер';
    }
    if (message.audioUrl) {
        return 'Голосовое сообщение';
    }
    if (message.content && message.content.trim()) {
        return message.content;
    }
    const type = message.attachmentType;
    if (type === 'image') {
        return 'Фото';
    }
    if (type === 'video') {
        return 'Видео';
    }
    if (type === 'audio') {
        return 'Аудио';
    }
    return 'Файл';
}

function truncatePreview(text) {
    if (!text) {
        return '';
    }
    return text.length > 48 ? text.substring(0, 48) + '\u2026' : text;
}

function createAttachmentElement(message) {
    if (!message.attachmentFilename) {
        return null;
    }
    var url = '/uploads/messages/' + encodeURIComponent(message.attachmentFilename);
    var box = document.createElement('span');
    box.className = 'attachment';

    var type = message.attachmentType || 'file';
    if (type === 'image') {
        var link = document.createElement('a');
        link.href = '#';
        link.className = 'attachment-image-link';
        link.title = 'Открыть фото';
        link.setAttribute('data-full-src', url);
        var img = document.createElement('img');
        img.src = url;
        img.alt = 'Фото';
        img.className = 'attachment-image';
        img.loading = 'lazy';
        link.appendChild(img);
        box.appendChild(link);
    } else if (type === 'video') {
        var video = document.createElement('video');
        video.src = url;
        video.controls = true;
        video.preload = 'metadata';
        video.className = 'attachment-video';
        box.appendChild(video);
    } else if (type === 'audio') {
        var audioWrap = document.createElement('span');
        audioWrap.className = 'attachment-audio-wrap';
        var audio = document.createElement('audio');
        audio.src = url;
        audio.controls = true;
        audio.preload = 'metadata';
        audio.className = 'attachment-audio';
        audioWrap.appendChild(audio);
        var audioMeta = document.createElement('span');
        audioMeta.className = 'attachment-file-meta';
        var audioName = document.createElement('span');
        audioName.className = 'attachment-file-name';
        audioName.textContent = message.attachmentOriginalName || message.attachmentFilename;
        var audioSize = document.createElement('span');
        audioSize.className = 'attachment-file-size';
        audioSize.textContent = formatAttachmentSize(message.attachmentSize);
        audioMeta.appendChild(audioName);
        audioMeta.appendChild(audioSize);
        audioWrap.appendChild(audioMeta);
        box.appendChild(audioWrap);
    } else {
        var fileLink = document.createElement('a');
        fileLink.href = url;
        fileLink.className = 'attachment-file';
        fileLink.title = 'Скачать файл';
        if (message.attachmentOriginalName) {
            fileLink.setAttribute('download', message.attachmentOriginalName);
        }
        var icon = document.createElement('span');
        icon.className = 'attachment-file-icon';
        icon.setAttribute('aria-hidden', 'true');
        icon.innerHTML = '<svg width="18" height="18" viewBox="0 0 24 24" fill="none">' +
            '<path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" stroke="currentColor" stroke-width="2" stroke-linejoin="round"/>' +
            '<path d="M14 2v6h6" stroke="currentColor" stroke-width="2" stroke-linejoin="round"/></svg>';
        var nameEl = document.createElement('span');
        nameEl.className = 'attachment-file-name';
        nameEl.textContent = message.attachmentOriginalName || message.attachmentFilename;
        var sizeEl = document.createElement('span');
        sizeEl.className = 'attachment-file-size';
        sizeEl.textContent = formatAttachmentSize(message.attachmentSize);
        fileLink.appendChild(icon);
        fileLink.appendChild(nameEl);
        fileLink.appendChild(sizeEl);
        box.appendChild(fileLink);
    }
    return box;
}

function createReplyBlock(message) {
    if (!message.replyToMessageId) {
        return null;
    }
    var block = document.createElement('div');
    block.className = 'reply-block';
    block.setAttribute('data-reply-id', message.replyToMessageId);
    var sender = document.createElement('span');
    sender.className = 'reply-sender';
    sender.textContent = message.replyToSenderUsername || '';
    var text = document.createElement('span');
    text.className = 'reply-text';
    text.textContent = message.replyToContent || '';
    block.appendChild(sender);
    block.appendChild(text);
    return block;
}

function createDeletedBlock() {
    var el = document.createElement('span');
    el.className = 'content deleted-text';
    el.textContent = 'Сообщение удалено';
    return el;
}

function showComposerError(text) {
    var el = document.getElementById('composer-error');
    if (!el) {
        return;
    }
    el.textContent = text;
    el.hidden = false;
    clearTimeout(showComposerError._timeout);
    showComposerError._timeout = setTimeout(function () {
        el.hidden = true;
    }, 4000);
}

var pendingFile = null;

function clearPending() {
    pendingFile = null;
    var box = document.getElementById('attachment-pending');
    if (box) {
        box.hidden = true;
        box.innerHTML = '';
    }
}

function renderPendingPreview() {
    var box = document.getElementById('attachment-pending');
    if (!box) {
        return;
    }
    if (!pendingFile) {
        box.hidden = true;
        box.innerHTML = '';
        return;
    }

    box.hidden = false;
    box.innerHTML = '';

    var row = document.createElement('div');
    row.className = 'attachment-pending-row';

    var icon = document.createElement('span');
    icon.className = 'attachment-pending-icon';
    icon.setAttribute('aria-hidden', 'true');
    icon.innerHTML = '<svg width="20" height="20" viewBox="0 0 24 24" fill="none"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8l-6-6Z" stroke="currentColor" stroke-width="2" stroke-linejoin="round"/><path d="M14 2v6h6M9 13h6M9 17h6" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/></svg>';
    row.appendChild(icon);

    var meta = document.createElement('div');
    meta.className = 'attachment-pending-meta';
    var name = document.createElement('span');
    name.className = 'attachment-pending-name';
    name.textContent = pendingFile.name;
    var size = document.createElement('span');
    size.className = 'attachment-pending-size';
    size.textContent = formatAttachmentSize(pendingFile.size);
    meta.appendChild(name);
    meta.appendChild(size);
    row.appendChild(meta);

    var remove = document.createElement('button');
    remove.type = 'button';
    remove.className = 'attachment-pending-remove';
    remove.setAttribute('aria-label', 'Убрать файл');
    remove.title = 'Убрать файл';
    remove.innerHTML = '<svg width="16" height="16" viewBox="0 0 24 24" fill="none"><path d="M18 6L6 18M6 6l12 12" stroke="currentColor" stroke-width="2" stroke-linecap="round"/></svg>';
    remove.addEventListener('click', clearPending);
    row.appendChild(remove);

    box.appendChild(row);
}

function setupAttachmentComposer(config) {
    var attachButton = document.getElementById('attach-button');
    var attachmentInput = document.getElementById('attachment-input');
    if (!attachButton || !attachmentInput) {
        return;
    }

    var uploading = false;

    attachButton.addEventListener('click', function () {
        if (!uploading) {
            attachmentInput.click();
        }
    });

    attachmentInput.addEventListener('change', function () {
        var file = attachmentInput.files && attachmentInput.files[0];
        if (!file || uploading) {
            return;
        }
        pendingFile = file;
        attachmentInput.value = '';
        renderPendingPreview();
        if (config.onPendingChange) {
            config.onPendingChange();
        }
    });

    window.AttachmentPending = {
        getFile: function () {
            return pendingFile;
        },
        hasFile: function () {
            return pendingFile != null;
        },
        clear: clearPending,
        send: function (options) {
            if (!pendingFile || uploading) {
                return Promise.resolve(false);
            }
            uploading = true;
            if (config.onUploadStart) {
                config.onUploadStart();
            }

            var formData = new FormData();
            formData.append('file', pendingFile);
            if (options && options.content) {
                formData.append('content', options.content);
            }
            if (options && options.replyToMessageId) {
                formData.append('replyToMessageId', String(options.replyToMessageId));
            }

            return fetch(config.uploadUrl(), {
                method: 'POST',
                body: formData,
                headers: { 'X-Requested-With': 'XMLHttpRequest' }
            }).then(function (response) {
                if (!response.ok) {
                    return response.json().then(function (err) {
                        throw new Error(err && err.error ? err.error : '\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u043e\u0442\u043f\u0440\u0430\u0432\u0438\u0442\u044c \u0444\u0430\u0439\u043b');
                    });
                }
                return response.json();
            }).then(function () {
                clearPending();
                return true;
            }).catch(function (err) {
                if (config.onError) {
                    config.onError(err && err.message ? err.message : '\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u043e\u0442\u043f\u0440\u0430\u0432\u0438\u0442\u044c \u0444\u0430\u0439\u043b');
                }
                throw err;
            }).finally(function () {
                uploading = false;
                if (config.onUploadEnd) {
                    config.onUploadEnd();
                }
            });
        }
    };
}

function initLightbox() {
    var lightbox = document.getElementById('lightbox');
    if (!lightbox) return;
    var content = lightbox.querySelector('.lightbox-content');
    var closeBtn = lightbox.querySelector('.lightbox-close');

    function openLightbox(html) {
        content.innerHTML = html;
        lightbox.hidden = false;
        document.body.style.overflow = 'hidden';
    }

    function closeLightboxFn() {
        lightbox.hidden = true;
        content.innerHTML = '';
        document.body.style.overflow = '';
    }

    document.addEventListener('click', function (e) {
        var link = e.target.closest('.attachment-image-link');
        if (link) {
            e.preventDefault();
            var src = link.getAttribute('data-full-src') || link.href;
            openLightbox('<img src="' + src + '" alt="Фото" class="lightbox-img"/>');
            return;
        }
        var vid = e.target.closest('.attachment-video');
        if (vid) {
            e.preventDefault();
            openLightbox('<video src="' + vid.src + '" controls autoplay class="lightbox-video"/>');
            return;
        }
    }, true);

    closeBtn.addEventListener('click', closeLightboxFn);
    lightbox.addEventListener('click', function (e) {
        if (e.target === lightbox) closeLightboxFn();
    });
    document.addEventListener('keydown', function (e) {
        if (e.key === 'Escape' && !lightbox.hidden) closeLightboxFn();
    });
}
