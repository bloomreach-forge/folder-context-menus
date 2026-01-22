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
