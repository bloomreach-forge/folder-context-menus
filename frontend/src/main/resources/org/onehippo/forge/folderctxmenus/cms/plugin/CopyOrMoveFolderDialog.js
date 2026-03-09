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

FolderContextMenus._activeIntervalId = null;

FolderContextMenus.startProgressPolling = function(options) {
    if (!options || !options.url || !options.panelId) {
        return;
    }

    var panel = document.getElementById(options.panelId);
    if (!panel) {
        return;
    }

    // Cancel any globally-tracked interval before starting a new one.
    // Storing the handle only on the DOM element is unreliable: Wicket can
    // reuse the same markup-id for a replacement ProgressPanel, causing the
    // DOM guard to see the *new* element and never stop the *old* closure.
    if (FolderContextMenus._activeIntervalId !== null) {
        clearInterval(FolderContextMenus._activeIntervalId);
        FolderContextMenus._activeIntervalId = null;
    }

    var intervalMs = options.intervalMs || 200;

    var statusEl = document.getElementById(options.statusId);
    var progressBarEl = document.getElementById(options.progressBarId);
    var progressLabelEl = document.getElementById(options.progressLabelId);
    var pathEl = document.getElementById(options.pathId);
    var cancelEl = document.getElementById(options.cancelId);
    var closeEl = document.getElementById(options.closeId);

    var getFilename = function(path) {
        if (!path) {
            return '';
        }
        var slash = path.lastIndexOf('/');
        return slash >= 0 ? path.substring(slash + 1) : path;
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

        if (data.finalizing) {
            if (statusEl) {
                statusEl.textContent = 'Finalizing... (' + (data.finalizingCount || 0) + ' items)';
            }
            if (progressBarEl) {
                progressBarEl.className = 'progress-bar progress-bar-finalizing';
                progressBarEl.style.width = '100%';
            }
            if (progressLabelEl) {
                progressLabelEl.textContent = '';
            }
            if (pathEl) {
                pathEl.style.display = 'none';
            }
            return false;
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
            pathEl.textContent = getFilename(data.path || '');
        }

        if (data.cancelled && cancelEl) {
            cancelEl.setAttribute('disabled', 'disabled');
        }

        return false;
    };

    var pollInFlight = false;
    var intervalId = null;

    var stopPolling = function() {
        if (intervalId !== null) {
            clearInterval(intervalId);
            intervalId = null;
            FolderContextMenus._activeIntervalId = null;
        }
    };

    var poll = function() {
        if (!document.getElementById(options.panelId)) {
            stopPolling();
            return;
        }
        if (pollInFlight) {
            return;
        }
        pollInFlight = true;
        fetch(options.url, { credentials: 'same-origin', cache: 'no-store' })
            .then(function(response) {
                if (!response.ok) {
                    stopPolling();
                    return null;
                }
                return response.json();
            })
            .then(function(data) {
                pollInFlight = false;
                if (!data) {
                    return;
                }
                var done = updateProgress(data);
                if (done) {
                    stopPolling();
                }
            })
            .catch(function() {
                pollInFlight = false;
                // Ignore transient network errors; next interval will retry.
            });
    };

    poll();
    intervalId = setInterval(poll, intervalMs);
    FolderContextMenus._activeIntervalId = intervalId;
};
