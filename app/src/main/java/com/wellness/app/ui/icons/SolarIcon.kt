package com.wellness.app.ui.icons

import android.content.Context
import android.graphics.drawable.BitmapDrawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.SvgDecoder
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.ImageRequest
import coil.size.Size
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking

/**
 * Renders a Solar (Iconify) SVG icon from `assets/icons/<name>.svg`.
 *
 * Why two render paths?
 *
 * 1. **Synchronous (preferred).** [SolarIconLoader.prewarmAll] eagerly
 *    decodes every SVG into an [ImageBitmap] on a background thread at
 *    process start. Once the bitmap lands in [SolarIconLoader.bitmaps],
 *    SolarIcon paints with the standard, synchronous [Image] — no
 *    `AsyncImagePainter` state machine in the middle, no async decode
 *    hitch when the user opens the app or switches tabs. The bitmap is
 *    rasterised at a generous max-dimension and rendered with
 *    `ContentScale.Fit`, so it remains crisp at every call site size
 *    (22dp navbar, 72dp hero, etc.).
 *
 * 2. **Asynchronous fallback.** During the brief first-launch window
 *    *before* prewarm has populated the cache we drop back to
 *    [AsyncImage] so the slot doesn't show through empty. This path
 *    self-heals — once the prewarm finishes and writes to the snapshot
 *    map, the icon recomposes onto the synchronous branch.
 *
 * The navbar's 5 icons are decoded first by the prewarm worker and
 * the main thread briefly waits on a latch in MainActivity.onCreate
 * (see [SolarIconLoader.awaitNavbarReady]) so they're guaranteed to
 * paint instantly on the first frame.
 */
@Composable
fun SolarIcon(
    name: String,
    modifier: Modifier = Modifier,
    tint: Color = Color.Unspecified,
    size: Dp = 24.dp,
) {
    val bitmap = SolarIconLoader.bitmaps[name]
    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = null,
            modifier = modifier.size(size),
            contentScale = ContentScale.Fit,
            colorFilter = if (tint != Color.Unspecified) ColorFilter.tint(tint) else null,
        )
        return
    }

    val context = LocalContext.current
    val loader = remember(context.applicationContext) {
        SolarIconLoader.get(context.applicationContext)
    }
    val request = remember(name) {
        ImageRequest.Builder(context)
            .data("file:///android_asset/icons/$name.svg")
            .crossfade(false)
            .memoryCacheKey("solar:$name")
            .build()
    }
    AsyncImage(
        model = request,
        imageLoader = loader,
        contentDescription = null,
        contentScale = ContentScale.Fit,
        colorFilter = if (tint != Color.Unspecified) ColorFilter.tint(tint) else null,
        modifier = modifier.size(size),
    )
}

object SolarIconLoader {
    /**
     * Icon names used by the navbar. The prewarm worker decodes these
     * first and counts down [navbarReady]; MainActivity.onCreate waits
     * on that latch (up to a brief timeout) before calling setContent
     * so the first frame of the app already has every tab glyph in
     * cache. Keep in sync with Navbar.tabIcon — each decode is <10ms
     * so we list them by hand here rather than coupling the icons
     * module to the navbar module.
     */
    private val NAVBAR_ICONS = arrayOf(
        "home-2-bold-duotone",
        "fire-bold-duotone",
        "calendar-bold-duotone",
        "chart-2-bold-duotone",
        "user-bold-duotone",
    )

    /**
     * Max edge in pixels for the rasterised SVG. 256px keeps every icon
     * crisp at the largest in-app call site (the 72dp Telegram glyph on
     * the bindings screen ≈ 216px on a 3x device) while keeping decode
     * memory tiny — 85 icons × 256² × 4B ≈ 22MB worst case, but most
     * Solar SVGs decode to far less because they're white on transparent.
     */
    private const val RASTER_PX = 256

    @Volatile private var instance: ImageLoader? = null
    @Volatile private var prewarmStarted = false

    /**
     * Synchronous, snapshot-aware bitmap cache. SolarIcon reads this
     * directly during composition; writes happen on a worker thread
     * (snapshot state is thread-safe for writes).
     */
    val bitmaps: SnapshotStateMap<String, ImageBitmap> = mutableStateMapOf()

    fun get(appContext: Context): ImageLoader =
        instance ?: synchronized(this) {
            instance ?: ImageLoader.Builder(appContext)
                .components { add(SvgDecoder.Factory()) }
                .memoryCache {
                    MemoryCache.Builder(appContext)
                        .maxSizePercent(0.15)
                        .build()
                }
                .diskCache(null as DiskCache?)
                .build()
                .also { instance = it }
        }

    /** Latch released when [prewarmAll] has decoded all 5 navbar icons. */
    private val navbarReady = CountDownLatch(NAVBAR_ICONS.size)

    /**
     * Block the calling (main) thread until every navbar glyph is in
     * [bitmaps] — or until [timeoutMs] elapses, whichever comes first.
     *
     * Why this exists: Coil's [ImageLoader.execute] internally dispatches
     * onto `Dispatchers.Main.immediate`. Calling
     * `runBlocking { loader.execute(...) }` from the main thread therefore
     * deadlocks — the outer `runBlocking` parks Main, and Coil's coroutine
     * needs Main to make progress. The previous build hit exactly this
     * deadlock and froze the app on the splash forever.
     *
     * The safe pattern is: do the decode on a worker thread (where
     * `runBlocking` is fine), and have the main thread wait on a Java
     * concurrency primitive that does NOT route through Main looper.
     * That's what [navbarReady] does — every successful navbar decode in
     * [prewarmAll]'s worker counts down the latch, and we await it with
     * a generous-but-bounded timeout so even a catastrophic decode stall
     * cannot strand the user on the splash.
     */
    fun awaitNavbarReady(timeoutMs: Long = 800L) {
        if (NAVBAR_ICONS.all { bitmaps.containsKey(it) }) return
        runCatching { navbarReady.await(timeoutMs, TimeUnit.MILLISECONDS) }
    }

    /**
     * Decode every SVG in `assets/icons/` on a background thread and put
     * the resulting bitmaps into [bitmaps]. Navbar icons are decoded
     * **first** so [awaitNavbarReady] can release the main thread as
     * early as possible. Safe to call multiple times — the second call
     * is a no-op.
     */
    fun prewarmAll(appContext: Context) {
        if (prewarmStarted) return
        synchronized(this) {
            if (prewarmStarted) return
            prewarmStarted = true
        }
        val loader = get(appContext)
        Thread(
            {
                // Decode navbar glyphs first so main can unblock ASAP.
                for (name in NAVBAR_ICONS) {
                    if (!bitmaps.containsKey(name)) {
                        decodeInto(loader, appContext, "$name.svg")
                    }
                    navbarReady.countDown()
                }
                // Now the rest. Sorted for determinism — easier to
                // reproduce ordering bugs in profiles.
                val assetManager = appContext.assets
                val all = runCatching { assetManager.list("icons") }
                    .getOrNull()
                    ?.filter { it.endsWith(".svg") }
                    ?.sorted()
                    ?: return@Thread
                val navbarSet = NAVBAR_ICONS.mapTo(HashSet()) { "$it.svg" }
                for (file in all) {
                    if (file in navbarSet) continue
                    val key = file.removeSuffix(".svg")
                    if (bitmaps.containsKey(key)) continue
                    decodeInto(loader, appContext, file)
                }
            },
            "solar-icon-prewarm",
        ).apply {
            isDaemon = true
            // One step below default — keeps the UI thread free of
            // scheduler contention during the heavy first-frame burst,
            // but high enough that the 5 navbar decodes still finish
            // in well under our 800ms main-thread wait budget.
            priority = Thread.NORM_PRIORITY - 1
        }.start()
    }

    private fun decodeInto(loader: ImageLoader, appContext: Context, file: String) {
        val key = file.removeSuffix(".svg")
        val request = ImageRequest.Builder(appContext)
            .data("file:///android_asset/icons/$file")
            .size(Size(RASTER_PX, RASTER_PX))
            // The bitmaps live in our own SnapshotStateMap and may be
            // drawn from any thread — hardware bitmaps don't play nice
            // with cross-thread upload, so force software.
            .allowHardware(false)
            .memoryCacheKey("solar:$key")
            .build()
        // Safe here: we're already on the worker thread spawned by
        // prewarmAll, NOT the main thread, so Coil's internal dispatch
        // to Dispatchers.Main.immediate can actually make progress while
        // this runBlocking parks the worker.
        val result = runCatching { runBlocking { loader.execute(request) } }
            .getOrNull() ?: return
        val drawable = result.drawable as? BitmapDrawable ?: return
        bitmaps[key] = drawable.bitmap.asImageBitmap()
    }
}
