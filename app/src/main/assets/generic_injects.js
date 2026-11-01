//download blobs support
if (!window.tvBroClicksListener) {
    window.tvBroClicksListener = function(e) {
        if (e.target.tagName.toUpperCase() == "A" && e.target.attributes.href.value.toLowerCase().startsWith("blob:")) {
            var fileName = e.target.download;
            var url = e.target.attributes.href.value;
            var xhr=new XMLHttpRequest();
            xhr.open('GET', e.target.attributes.href.value, true);
            xhr.responseType = 'blob';
            xhr.onload = function(e) {
                if (this.status == 200) {
                    var blob = this.response;
                    var reader = new FileReader();
                    reader.readAsDataURL(blob);
                    reader.onloadend = function() {
                        base64data = reader.result;
                        SimpleBrowser.takeBlobDownloadData(base64data, fileName, url, blob.type);
                    }
                }
            };
            xhr.send();
            e.stopPropagation();
            e.preventDefault();
        }
    };
    document.addEventListener("click", window.tvBroClicksListener);
}

// context menu support
window.addEventListener("touchstart", function(e) {
    window.TVBRO_activeElement = e.target;
    window.TVBRO_touchStartX = e.touches[0].clientX;
    window.TVBRO_touchStartY = e.touches[0].clientY;
});
// the virtual cursor is a mouse for pages in the desktop view: its long press touches nothing
window.addEventListener("mousemove", function(e) {
    window.TVBRO_activeElement = e.target;
}, {capture: true, passive: true});

// Virtual keyboard and IME support for TV remote navigation
if (!window.tvBroImeListener) {
    window.tvBroImeListener = true;
    function isEditableElement(el) {
        if (!el) return false;
        var tag = el.tagName ? el.tagName.toUpperCase() : "";
        if (tag === "INPUT") {
            var type = (el.type || "").toLowerCase();
            return type !== "button" && type !== "submit" && type !== "reset" &&
                   type !== "checkbox" && type !== "radio" && type !== "image" &&
                   type !== "file" && type !== "range" && type !== "color" && type !== "hidden";
        }
        if (tag === "TEXTAREA") return true;
        if (el.isContentEditable) return true;
        if (el.getAttribute && el.getAttribute("contenteditable") === "true") return true;
        return false;
    }

    function checkAndRequestInput(el) {
        if (isEditableElement(el) && window.SimpleBrowser && window.SimpleBrowser.requestSoftInput) {
            window.SimpleBrowser.requestSoftInput();
        }
    }

    document.addEventListener("focusin", function(e) {
        checkAndRequestInput(e.target);
    }, true);

    document.addEventListener("click", function(e) {
        checkAndRequestInput(e.target);
    }, true);

    document.addEventListener("focusout", function(e) {
        setTimeout(function() {
            if (!isEditableElement(document.activeElement) && window.SimpleBrowser && window.SimpleBrowser.onInputBlurred) {
                window.SimpleBrowser.onInputBlurred();
            }
        }, 100);
    }, true);
}
