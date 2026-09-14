package eu.kanade.tachiyomi.source

/**
 * Marker interface for novel (text-based) sources. Mirrors the compileOnly stub extensions
 * compile against in extensions-lib.
 *
 * Detection is via the [Source.isNovelSource] property and the text API is [Source.fetchPageText];
 * neither requires this interface. It is kept only for source compatibility with existing
 * extensions that declare `: HttpSource(), NovelSource`. New sources just set
 * `isNovelSource = true` and override [Source.fetchPageText].
 *
 * Do not add `is NovelSource` checks here or elsewhere in the app: this interface has no real
 * implementors in app code (only extensions, loaded from a separate dex R8 never sees), so R8
 * can prove any direct call through it only ever reaches this interface's own body and
 * devirtualizes it away in release builds. That's what broke [SourceTracker] - see its history.
 */
@Deprecated("Detection is via Source.isNovelSource; fetchPageText is on Source")
interface NovelSource : Source

/**
 * Checks if this source is a novel source. Backed solely by the [Source.isNovelSource] property,
 * which lives on the shared [Source] interface and is virtual-dispatched across classloaders.
 */
fun Source.isNovelSource(): Boolean = isNovelSource
