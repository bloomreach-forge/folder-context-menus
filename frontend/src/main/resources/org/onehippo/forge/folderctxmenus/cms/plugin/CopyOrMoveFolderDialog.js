var FolderContextMenus = FolderContextMenus || {};

FolderContextMenus.resizeDialog = function(width) {
    var modal = document.querySelector('.wicket-modal');
    if (!modal) {
        return;
    }

    modal.style.width = width + 'px';
    modal.style.height = 'auto';
    modal.style.left = ((window.innerWidth - width) / 2) + 'px';
    modal.style.top = '100px';

    // Reset height on all potential containers
    var selectors = [
        '.w_content_container',
        '.w_content',
        '.w_content_1',
        '.w_content_2',
        '.w_content_3',
        '.wicket-modal-wrap',
        '.hippo-dialog',
        '.hippo-dialog-body',
        '.hippo-window-body'
    ];

    selectors.forEach(function(selector) {
        var elements = modal.querySelectorAll(selector);
        elements.forEach(function(el) {
            el.style.height = 'auto';
            el.style.minHeight = '0';
        });
    });
};

FolderContextMenus.startProgressPolling = function(options) {
    if (!options || !options.url || !options.panelId) {
        return;
    }

    var panel = document.getElementById(options.panelId);
    if (!panel) {
        return;
    }

    if (panel._progressPollInterval) {
        clearInterval(panel._progressPollInterval);
        panel._progressPollInterval = null;
    }

    var intervalMs = options.intervalMs || 200;
    var maxPathLength = options.maxPathLength || 60;

    var statusEl = document.getElementById(options.statusId);
    var progressBarEl = document.getElementById(options.progressBarId);
    var progressLabelEl = document.getElementById(options.progressLabelId);
    var pathEl = document.getElementById(options.pathId);
    var cancelEl = document.getElementById(options.cancelId);
    var closeEl = document.getElementById(options.closeId);

    var truncatePath = function(path) {
        if (!path || path.length <= maxPathLength) {
            return path || '';
        }
        return '...' + path.substring(path.length - maxPathLength + 3);
    };

    var applySummaryToElements = function(summary, elements) {
        if (elements.statusEl) {
            elements.statusEl.textContent = summary.message || '';
        }

        if (elements.progressBarEl) {
            if (summary.error) {
                elements.progressBarEl.className = 'progress-bar progress-bar-error';
            } else {
                elements.progressBarEl.className = 'progress-bar';
            }
            elements.progressBarEl.style.width = '100%';
        }

        if (elements.progressLabelEl) {
            elements.progressLabelEl.textContent = summary.error ? '' : '100%';
        }

        if (elements.pathEl) {
            elements.pathEl.style.display = 'none';
        }

        if (elements.cancelEl) {
            elements.cancelEl.className = 'cancel-btn hidden-btn';
            elements.cancelEl.setAttribute('disabled', 'disabled');
        }

        if (elements.closeEl) {
            elements.closeEl.style.display = '';
        }
    };

    var updateProgress = function(data) {
        if (data.summary) {
            applySummaryToElements(data.summary, {
                statusEl: statusEl,
                progressBarEl: progressBarEl,
                progressLabelEl: progressLabelEl,
                pathEl: pathEl,
                cancelEl: cancelEl,
                closeEl: closeEl
            });
            return true;
        }

        var total = data.total || 0;
        var current = data.current || 0;
        var eta = data.eta || '';
        var percent = data.percent || 0;

        if (statusEl) {
            if (total > 0) {
                var status = 'Processing ' + current + '/' + total + ' items';
                if (eta) {
                    status += ' - ' + eta;
                }
                statusEl.textContent = status;
            } else {
                statusEl.textContent = 'Initializing...';
            }
        }

        if (progressBarEl) {
            progressBarEl.className = 'progress-bar';
            progressBarEl.style.width = percent + '%';
        }

        if (progressLabelEl) {
            progressLabelEl.textContent = percent + '%';
        }

        if (pathEl) {
            pathEl.style.display = '';
            pathEl.textContent = truncatePath(data.path || '');
        }

        if (data.cancelled && cancelEl) {
            cancelEl.setAttribute('disabled', 'disabled');
        }

        return false;
    };

    var poll = function() {
        fetch(options.url, { credentials: 'same-origin', cache: 'no-store' })
            .then(function(response) {
                if (!response.ok) {
                    return null;
                }
                return response.json();
            })
            .then(function(data) {
                if (!data) {
                    return;
                }
                var done = updateProgress(data);
                if (done) {
                    clearInterval(panel._progressPollInterval);
                    panel._progressPollInterval = null;
                }
            })
            .catch(function() {
                // Ignore polling errors; next interval will retry.
            });
    };

    poll();
    panel._progressPollInterval = setInterval(poll, intervalMs);
};
