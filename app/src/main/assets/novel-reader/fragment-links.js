// In-page "#id" links within a chapter. Installed once per load via
// NovelWebViewStyler.injectFragmentLinks().
//
// Replaces:
//   __TSUNDOKU_OBJECT_NAME__: global runtime object, for actions.goToElement (paged-aware)
//   __CHAPTER_TAG_NAME__: per-chapter wrapper element, so ids repeated across appended chapters
//   resolve inside the chapter the link sits in

(function () {
    if (window.__tdFragmentLinksInstalled) return;
    window.__tdFragmentLinksInstalled = true;

    function cssEscape(value) {
        if (window.CSS && CSS.escape) return CSS.escape(value);
        return String(value).replace(/["\\]/g, '\\$&');
    }

    function decode(value) {
        try { return decodeURIComponent(value); } catch (e) { return value; }
    }

    document.addEventListener('click', function (e) {
        var anchor = e.target && e.target.closest ? e.target.closest('a[href^="#"]') : null;
        if (!anchor) return;
        var id = decode(anchor.getAttribute('href').substring(1));
        if (!id) return;
        e.preventDefault();
        var selector = '[id="' + cssEscape(id) + '"], a[name="' + cssEscape(id) + '"]';
        var chapter = anchor.closest('__CHAPTER_TAG_NAME__');
        var target = (chapter && chapter.querySelector(selector)) || document.querySelector(selector);
        if (!target) return;
        e.stopPropagation();
        var t = window.__TSUNDOKU_OBJECT_NAME__;
        if (t && t.actions && t.actions.goToElement) t.actions.goToElement(target);
        else target.scrollIntoView({ block: 'start' });
    }, true);
})();
