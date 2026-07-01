# Feature: Reference Viewer (equations & figures, hover-to-view)

## Goal

When viewing a page, let the user trigger a multi-modal LLM analysis of that page which:

1. **Extracts equations and figures** on the page, each tagged with its identifying number/label
   (e.g. "Equation 3", "Figure 2a").
2. **Locates in-text references** to those equations/figures elsewhere on the page (e.g. "as shown
   in Eq. 3", "see Fig. 2a") and links them to the extracted item.
3. Lets the user **hold/tap a reference** in the text and see the referenced equation or figure
   rendered in a **hover-over popup**, without navigating away from the current page.

## Why

PDFs (especially papers) constantly force readers to flip back and forth to check what "Eq. 3" or
"Fig. 2" refers to. MJ PDF already has the plumbing for page text extraction, page bitmap
rendering, and tap-based interaction (see `CLAUDE.md`); this feature closes the loop with an LLM
doing the semantic work of identifying figures/equations and the reference mentions pointing at
them.

## Rough shape of the feature (not a locked-in design)

- Trigger: a new action (menu item / long-press) that runs analysis on the **current page** (and
  possibly a small window of neighboring pages, since references can point to a figure/equation on
  a previous page).
- Input to the LLM: a rendered image of the page (via `PdfiumCore.renderPageBitmap`) plus the
  extracted page text (via `PdfExtractor.getPageText`), sent to a multi-modal model.
- Output: structured data — a list of `{ label, type: equation|figure, bounding box or page
  region }` and a list of `{ reference text span, resolved label, source page }`.
- Once results come back, use `PdfiumCore.createHighlightText`-style bounds lookups (or bounds
  returned by the model) to map reference text spans to on-screen regions.
- Reuse the existing tap-hit-testing pattern (`DragPinchManager` / `OnDrawListener` in
  `AndroidPdfViewer`) to detect when a user is holding a reference span, and show a popup rendering
  the corresponding equation/figure region (cropped page bitmap, or the model's own rendering).
- Results should probably be cached per document/page (Room DB, see `repository/`) so re-opening a
  page doesn't require re-running the LLM call every time.
- API key / model config belongs in `data/Preferences.kt` + a new section in `ui/settings/`.

## Open questions for whoever picks this up

- Which multi-modal provider/model, and how is the API key obtained/stored securely?
- Cost/latency: analyze automatically per page, or only on explicit user request?
- How to handle cross-page references (a reference on page 5 to a figure on page 2)?
- Exact popup UI/UX: full render of the figure, or a cropped snippet? Tap vs. long-press vs. both?

Start here, but treat this as a starting brief, not a spec — expect the design to evolve once
someone digs into how well an LLM can actually localize figures/equations from a rendered page
image.
