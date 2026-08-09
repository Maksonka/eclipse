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

function messagePreview(message) {
    if (message.stickerUrl) {
        return 'Стикер';
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
        var audio = document.createElement('audio');
        audio.src = url;
        audio.controls = true;
        audio.preload = 'metadata';
        audio.className = 'attachment-audio';
        box.appendChild(audio);
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
        uploading = true;
        attachButton.disabled = true;
        if (config.onUploadStart) {
            config.onUploadStart();
        }

        var formData = new FormData();
        formData.append('file', file);

        fetch(config.uploadUrl(), {
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
        }).catch(function (err) {
            if (config.onError) {
                config.onError(err && err.message ? err.message : '\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u043e\u0442\u043f\u0440\u0430\u0432\u0438\u0442\u044c \u0444\u0430\u0439\u043b');
            }
        }).finally(function () {
            uploading = false;
            attachButton.disabled = false;
            attachmentInput.value = '';
            if (config.onUploadEnd) {
                config.onUploadEnd();
            }
        });
    });
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
