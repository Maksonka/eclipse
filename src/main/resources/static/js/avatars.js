(function () {
    "use strict";

    var palettes = [
        ["#8b5cf6", "#6d28d9"],
        ["#3b82f6", "#1d4ed8"],
        ["#06b6d4", "#0e7490"],
        ["#ec4899", "#be185d"],
        ["#f59e0b", "#b45309"],
        ["#10b981", "#047857"],
        ["#ef4444", "#b91c1c"],
        ["#8b5cf6", "#db2777"],
        ["#14b8a6", "#0f766e"],
        ["#a855f7", "#6b21a8"],
        ["#f97316", "#c2410c"],
        ["#6366f1", "#4338ca"]
    ];

    function hashString(str) {
        var h = 0;
        for (var i = 0; i < str.length; i++) {
            h = ((h << 5) - h + str.charCodeAt(i)) | 0;
        }
        return Math.abs(h);
    }

    function apply() {
        var nodes = document.querySelectorAll(".user-avatar[data-name]");
        for (var i = 0; i < nodes.length; i++) {
            var el = nodes[i];
            if (!el.querySelector(".user-avatar-letter")) continue;
            var name = el.getAttribute("data-name") || "";
            if (!name) continue;
            var pal = palettes[hashString(name) % palettes.length];
            el.style.background =
                "linear-gradient(135deg, " + pal[0] + ", " + pal[1] + ")";
        }
    }

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", apply);
    } else {
        apply();
    }
})();
