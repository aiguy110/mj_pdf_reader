# MJ PDF — Repo Map

MJ PDF is an Android PDF viewer app (package `com.gitlab.mudlej.MjPdfReader`). This repo is a
GitHub mirror (`aiguy110/mj_pdf_reader`) of the canonical source hosted on GitLab
(`gitlab.com/mudlej_android/mj_pdf_reader`, remote `upstream-gitlab`). Origin `origin` points at
the GitHub fork.

## Module layout

Standard multi-module Gradle project. `AndroidPdfViewer/` and `PdfiumAndroid/` are vendored forks
committed directly as real module folders (not git submodules), wired via root `settings.gradle`:

```
include ':AndroidPdfViewer:android-pdf-viewer'
include ':app'
include ':PdfiumAndroid'
```

- **`app/`** — the actual MJ PDF application (Kotlin-first: ~62 `.kt` vs 3 `.java` files).
- **`AndroidPdfViewer/`** — forked from `barteksc/AndroidPdfViewer` (package
  `com.github.barteksc.pdfviewer`). Pure Java. Renders PDF pages into a scrollable/zoomable
  `PDFView`, handles gestures, links, and drawing overlays.
- **`PdfiumAndroid/`** — forked from `barteksc/PdfiumAndroid` (package `com.shockwave.pdfium`).
  JNI wrapper around Google's PDFium C++ library (native code in `src/main/jni`). Exposes text
  extraction, bounding boxes, links, bookmarks, and page rendering to Java/Kotlin.

Build: `./gradlew assembleDebug` (or `assembleRelease`), per-module tasks like `:app:assembleDebug`.
`minSdkVersion 21`, `targetSdkVersion 34`, Java/Kotlin 17. No `src/test` or `src/androidTest`
directories currently exist anywhere in the repo — there is no automated test suite; changes are
verified by building and manual testing on device/emulator.

## app/ structure (package `com.gitlab.mudlej.MjPdfReader`)

- **`ui/main/MainActivity.kt`** — the main PDF viewer screen. Owns the `PDFView`, configures it via
  `Configurator`, wires `.onTap { ... }`. This is the entry point for any feature that needs to
  react to taps/gestures on the rendered page.
- **`ui/link/`** (`LinksActivity.kt`, `LinkAdapter.kt`, `LinkFunctions.kt`, `LinkComparator.kt`,
  `LinkViewHolder.kt`) + **`data/Link.kt`** — "view all embedded links" feature. Pattern:
  background-coroutine extraction (`PdfExtractor.getAllLinks()`) → filterable RecyclerView list →
  tap either opens URL or returns a page number via `setResult(PDF.LINK_RESULT_OK, ...)` to
  `MainActivity`, which jumps to that page. This extractor → list-UI → jump-to-page pattern is the
  template to follow for any new "browse extracted items" feature.
- **`ui/bookmark/`** (`BookmarksActivity.kt` + Adapter/Comparator/ViewHolder/Functions) +
  **`data/Bookmark.kt`** — Table of Contents, sourced from `PdfExtractor.getAllBookmarks()`.
- **`ui/search/`** (`SearchActivity.kt` + Adapter/Comparator/ViewHolder/Functions) +
  **`data/SearchResult.kt`** — searches page text, highlights results using Pdfium's
  `createHighlightText`.
- **`ui/text_mode/TextModeActivity.kt`** — renders extracted page text as plain text via
  `PdfExtractor.getPageText(pageNumber)`.
- **`manager/extractor/PdfExtractor.kt`** (interface) / **`PdfExtractorImpl.kt`** — the central
  abstraction wrapping `PdfiumCore`/`PdfDocument`: `getPageText`, `getPageCount`, `getPageLinks`,
  `getAllBookmarks`, `getAllLinks`. New page-content extraction (e.g. images, structured metadata)
  belongs here.
- **`data/Preferences.kt`** — typed wrapper around `SharedPreferences`
  (`PreferenceManager.getDefaultSharedPreferences`). **`ui/settings/`**
  (`SettingsActivity.kt`, `SettingsFragment.kt`) is the existing AndroidX Preference-based settings
  screen — extend both for any new user-facing config (e.g. an API key).
- **`repository/`** (`AppDatabase.kt`, `PdfRecord.kt`, `PdfRecordDao.kt`) — Room DB, currently used
  for per-document state (e.g. last page). Natural place to cache expensive derived data
  per-document/per-page.

## AndroidPdfViewer/ (package `com.github.barteksc.pdfviewer`)

- **`PDFView.java`** — main view. Renders visible `PagePart`s in `onDraw`, then calls
  `drawWithListener(canvas, page, callbacks.getOnDraw())` / `getOnDrawAll()` — an existing
  **`OnDrawListener`** hook (`listener/OnDrawListener.java`) for drawing custom overlays on top of
  the rendered page.
- **`DragPinchManager.java`** — implements `GestureDetector.OnGestureListener`.
  `onSingleTapConfirmed` calls `pdfView.callbacks.callOnTap(e)` and separately hit-tests tapped
  coordinates against `pdfFile.getPageLinks(page)` bounds, then invokes
  `callbacks.callLinkHandler(new LinkTapEvent(...))`. This tap → bounds-hit-test → callback pattern
  is the mechanism to reuse for tap-to-reveal features anchored to specific page regions.
- **`link/LinkHandler.java`** / **`DefaultLinkHandler.java`**, **`model/LinkTapEvent.java`** — link
  tap event plumbing, carrying both view-space and page-space coordinates plus the underlying
  `PdfDocument.Link`.
- **`PdfFile.java`** — per-document page geometry and link lookups, coordinate mapping helpers
  between page-space and view/device-space.

## PdfiumAndroid/ (package `com.shockwave.pdfium`)

`PdfiumCore.java` is the public API surface:
- **Text**: `getPageText(doc, pageIndex)`, `getPagesText(doc, start, end)`.
- **Bounding boxes**: `createHighlightText(doc, pageIndex, start, end, padding)` → `Rect[]`,
  backed by native `nativeGetPageTextBounds`. Currently used only for search highlighting, but
  works for arbitrary text ranges — usable to locate any text span (e.g. a figure/equation label
  or an in-text reference mention) on the page.
- **Links**: `getPageLinks(doc, pageIndex)` → `List<PdfDocument.Link>`
  (`bounds: RectF`, `destPageIdx`, `uri`); `mapPageCoordsToDevice`/`mapRectToDevice` for coordinate
  transforms.
- **Bookmarks/TOC**: `getTableOfContents(doc)` → `List<PdfDocument.Bookmark>`.
- **Rendering**: `renderPageBitmap(doc, bitmap, pageIndex, ...)` renders a page to an Android
  `Bitmap` at arbitrary DPI — the natural way to produce page images (e.g. to send to a
  multimodal model, or to crop a sub-region once its bounds are known).
- There is **no existing JNI method to extract embedded images directly** — only whole-page bitmap
  rendering. Extracting an individual figure as an image means either adding new native code under
  `PdfiumAndroid/src/main/jni`, or cropping a rendered page bitmap using bounds obtained elsewhere
  (e.g. from text bounding boxes or a bounding box computed by an external analysis step).

## Networking / external services

There is currently **no HTTP client dependency** in `app/build.gradle` beyond
`ch.acra:acra-http` (crash reporting). The `INTERNET` permission is already declared in
`AndroidManifest.xml`. `com.google.code.gson:gson` is already a dependency, useful for
request/response (de)serialization. `kotlinx-coroutines-android` is used throughout for async work
(extraction, page loads) — new async work (e.g. a network call) should follow the same
`lifecycleScope.launch { withContext(Dispatchers.IO) { ... } }` pattern seen in `LinksActivity`.
Any feature that calls an external API will need to add its own HTTP client and manage credentials
via `data/Preferences.kt` + `ui/settings/`.

## Where things live, at a glance

| Concern                          | File(s)                                                        |
|-----------------------------------|-----------------------------------------------------------------|
| Main viewer screen / tap handling | `app/.../ui/main/MainActivity.kt`                                |
| Page content extraction           | `app/.../manager/extractor/PdfExtractor(Impl).kt`                |
| "Browse extracted items" UI       | `app/.../ui/link/`, `ui/bookmark/`, `ui/search/` (pick a template) |
| User preferences / settings UI    | `app/.../data/Preferences.kt`, `ui/settings/`                    |
| Per-document persistence          | `app/.../repository/` (Room)                                     |
| Page rendering overlay hook       | `AndroidPdfViewer/.../PDFView.java` (`OnDrawListener`)           |
| Tap → page-region hit testing     | `AndroidPdfViewer/.../DragPinchManager.java`                     |
| Text bounds / page bitmap / links | `PdfiumAndroid/.../PdfiumCore.java`                              |

## Feature work in flight

See `refview.md` for a spec of the next planned feature (LLM-assisted equation/figure extraction
and reference hover popups).
