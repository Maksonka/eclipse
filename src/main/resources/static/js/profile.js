(function () {
    'use strict';

    var username = typeof profileUsername !== 'undefined' ? profileUsername : '';
    if (!username) return;

    function loadCommonGroups() {
        fetch('/api/profile/' + encodeURIComponent(username) + '/common-groups', {
            headers: { 'Accept': 'application/json' }
        })
            .then(function (r) { return r.json(); })
            .then(function (data) {
                if (data.count > 0) {
                    var section = document.getElementById('profile-common-section');
                    var countEl = document.getElementById('profile-common-count');
                    var label = data.count === 1 ? 'совместная группа' : 'совместных групп';
                    countEl.textContent = data.count;
                    section.querySelector('.profile-common-label').textContent = label;
                    section.hidden = false;

                    if (data.groups && data.groups.length > 0) {
                        var groupList = document.createElement('div');
                        groupList.className = 'profile-common-list';
                        data.groups.forEach(function (g) {
                            var link = document.createElement('a');
                            link.className = 'profile-common-group';
                            link.href = '/chat/group/' + g.id;
                            link.textContent = g.name;
                            groupList.appendChild(link);
                        });
                        section.appendChild(groupList);
                    }
                }
            })
            .catch(function () {});
    }

    function loadSharedMedia() {
        fetch('/api/profile/' + encodeURIComponent(username) + '/shared-media', {
            headers: { 'Accept': 'application/json' }
        })
            .then(function (r) { return r.json(); })
            .then(function (data) {
                if (data.media && data.media.length > 0) {
                    var section = document.getElementById('profile-media-section');
                    var grid = document.getElementById('profile-media-grid');
                    data.media.forEach(function (item) {
                        var cell = document.createElement('div');
                        cell.className = 'profile-media-cell';
                        cell.setAttribute('data-id', item.id);

                        if (item.type === 'image') {
                            var img = document.createElement('img');
                            img.className = 'profile-media-img';
                            img.src = '/uploads/messages/' + encodeURIComponent(item.filename);
                            img.alt = item.name || 'Фото';
                            img.loading = 'lazy';
                            cell.appendChild(img);
                        } else {
                            var fileIcon = document.createElement('span');
                            fileIcon.className = 'profile-media-file';
                            fileIcon.textContent = item.name || 'Файл';
                            cell.appendChild(fileIcon);
                        }

                        grid.appendChild(cell);
                    });
                    section.hidden = false;

                    grid.addEventListener('click', function (e) {
                        var cell = e.target.closest('.profile-media-cell');
                        if (!cell) return;
                        var img = cell.querySelector('.profile-media-img');
                        if (!img) return;
                        e.preventDefault();
                        var lb = document.getElementById('lightbox');
                        if (lb) {
                            var content = lb.querySelector('.lightbox-content');
                            content.innerHTML = '<img src="' + img.src + '" alt="Фото" class="lightbox-img"/>';
                            lb.classList.add('is-open');
                            document.body.style.overflow = 'hidden';
                        }
                    });
                }
            })
            .catch(function () {});
    }

    function closeLightbox() {
        var lb = document.getElementById('lightbox');
        if (lb) {
            lb.classList.remove('is-open');
            lb.querySelector('.lightbox-content').innerHTML = '';
            document.body.style.overflow = '';
        }
    }

    document.addEventListener('DOMContentLoaded', function () {
        loadCommonGroups();
        loadSharedMedia();

        var lb = document.getElementById('lightbox');
        if (lb) {
            lb.querySelector('.lightbox-close').addEventListener('click', closeLightbox);
            lb.addEventListener('click', function (e) {
                if (e.target === lb) closeLightbox();
            });
        }
        document.addEventListener('keydown', function (e) {
            if (e.key === 'Escape') closeLightbox();
        });
    });
})();
