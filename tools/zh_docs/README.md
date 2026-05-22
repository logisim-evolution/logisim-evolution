# zh Documentation Rebuild Prototype

This directory contains a prototype workflow for rebuilding the Simplified
Chinese HTML documentation from the English source documentation.

The workflow is source-driven for content, but output-first for quality. The
English files provide the text to translate; they are not treated as perfect
HTML templates. The workflow prefers Translate Toolkit for initial extraction
and rendering:

1. Treat `src/main/resources/doc/en/html` as the canonical text source.
2. Normalize malformed source pages into a temporary template tree.
3. Extract translatable HTML fragments into PO files with `html2po`.
4. Translate the records with a provider such as DeepSeek.
5. Run PO-level QA on translated files with `pofilter`.
6. Render zh HTML drafts with `po2html`.
7. Clean up the zh HTML directly when the inherited structure is wrong,
   awkward, or unnecessarily constraining.
8. Lint the final zh HTML for terminology, links, images, security-sensitive
   markup, and cleanup problems.

The existing `src/main/resources/doc/zh/html` files are not used as source
truth. They can be consulted as translation memory later, but generated output
should be reproducible from English source plus the translation records.

## Quick Prototype Run

From the repository root:

```powershell
python tools/zh_docs/doc_i18n.py inventory
python tools/zh_docs/doc_i18n.py po-extract --sample
python tools/zh_docs/doc_i18n.py po-to-jsonl
python tools/zh_docs/doc_i18n.py translate --provider pseudo
python tools/zh_docs/doc_i18n.py po-apply
python tools/zh_docs/doc_i18n.py po-filter
python tools/zh_docs/doc_i18n.py po-render
python tools/zh_docs/doc_i18n.py lint --root build/zh-docs/po-preview/html
python tools/zh_docs/doc_i18n.py lint --root src/main/resources/doc/zh/html --include guide/tutorial --fail-on-warnings
python tools/zh_docs/doc_i18n.py check-javahelp --fail-on-warnings
python tools/zh_docs/doc_i18n.py check-translations --input build/zh-docs/translations.jsonl
```

Default outputs are written under `build/zh-docs/`, which is ignored by Git:

- `build/zh-docs/po`: extracted PO files
- `build/zh-docs/pot`: extracted POT templates when running `po-extract --pot`
- `build/zh-docs/source-clean/html`: lxml-normalized English templates
- `build/zh-docs/segments.jsonl`: source fragments converted from PO
- `build/zh-docs/translations.jsonl`: translated text records
- `build/zh-docs/po-translated`: translated PO files
- `build/zh-docs/po-qa`: PO entries flagged by `pofilter`
- `build/zh-docs/po-preview/html`: rendered zh preview

The `pseudo` provider is only a dry-run mode. It applies a few glossary terms
and prefixes text with `[待译]` so the extraction and rendering path can be
reviewed without spending API calls. The linter should report these TODO markers;
that is expected for pseudo output.

## Tool Dependency

Install Translate Toolkit before using the PO workflow:

```powershell
python -m pip install translate-toolkit
```

For isolated local testing without changing the Python environment:

```powershell
python -m pip install --target build/zh-docs/vendor translate-toolkit
```

The script automatically checks `PATH`, `TRANSLATE_TOOLKIT_BIN`, and
`build/zh-docs/vendor/bin` for `html2po` and `po2html`.

By default, `po-extract` first normalizes each English HTML file with lxml.
This is intentional: some upstream HTML pages contain mismatched tags that
strict localization parsers reject. The normalized source is used only as the
translation template under `build/zh-docs/source-clean/html`; the canonical
source remains `src/main/resources/doc/en/html`.

## DeepSeek Provider

Set the API key in the environment and run the translation step:

```powershell
$env:DEEPSEEK_API_KEY = "..."
python tools/zh_docs/doc_i18n.py translate --provider deepseek
```

Useful options:

```powershell
python tools/zh_docs/doc_i18n.py translate --provider deepseek --model deepseek-v4-pro --thinking disabled --batch-size 30 --limit 120
python tools/zh_docs/doc_i18n.py translate --provider deepseek --model deepseek-v4-flash --thinking disabled --batch-size 30 --limit 120
python tools/zh_docs/doc_i18n.py translate --provider deepseek --model deepseek-v4-pro --thinking enabled --reasoning-effort high --batch-size 10 --limit 30
python tools/zh_docs/doc_i18n.py po-extract --files guide/gui/gui-attrcomp.html libs/wiring/pin.html
python tools/zh_docs/doc_i18n.py po-to-jsonl
```

The provider sends JSON batches and expects JSON responses. Translation records
are append-only JSONL, so failed batches can be retried without rewriting the
source tree. The JSONL is then applied back into PO with `po-apply`.

The default DeepSeek configuration is `deepseek-v4-pro` with thinking disabled.
Use this for normal translation batches. Use `deepseek-v4-flash` only for cheap
drafts or exploratory pages, and reserve thinking mode for small review batches
where extra deliberation is worth the slower runtime and reasoning-token cost.

## Incremental Updates

For a one-off rebuild, translating from freshly extracted PO files is enough.
For long-term maintenance, prefer the standard Gettext-style update loop:

1. Generate fresh POT templates with `html2po -P`.
2. Merge existing zh translations into the new templates with `pot2po` or
   `msgmerge`.
3. Translate only untranslated or fuzzy entries.
4. Render and lint the final zh HTML.

This prototype currently implements the first full rebuild path and keeps
translation JSONL records keyed by source path, context, and source text. It
also includes the standard update path:

```powershell
python tools/zh_docs/doc_i18n.py po-extract --sample --pot
python tools/zh_docs/doc_i18n.py po-update --input-root build/zh-docs/pot --template-root previous-zh-po --output-root build/zh-docs/po
```

## Design Notes

- The earlier hand-written text-node extractor is still present for experiments,
  but the intended workflow is `html2po` / `po2html`. Translate Toolkit extracts
  paragraph-level HTML fragments, so inline markup such as
  `<b class="propertie">Output?</b>` stays inside one translation unit.
- `po2html` inserts translated element content as trusted HTML fragments. Review
  PO output before publishing generated pages. The final lint checks for
  security-sensitive tags, inline event handler attributes, and unsafe URL
  schemes that were newly introduced by the translated output. Existing source
  markup is allowed by default and can be reviewed separately.
- `po-filter` wraps Translate Toolkit's `pofilter` so PO-level checks can catch
  common translation-file problems before rendering. Its default test set is
  intentionally conservative for Chinese: URLs, variables, numbers, paths,
  escapes, blank/untranslated entries, and similar mechanical
  checks. The exact `xmltags` pofilter test is not enabled by default because it
  reports legitimate inline-tag reordering; use `check-translations
  --flag-tag-reorder` or `po-filter --test xmltags` when you want that review
  list. Use `po-filter --all-tests` only for exploratory review because the full
  pofilter suite produces many natural-language false positives for zh-CN.
  `po-filter` clears its output directory before running when that directory is
  under `build/zh-docs`, so stale QA files do not pollute the result.
- Translation JSONL QA is a preflight check, not the final authority. The final
  authority is the rendered zh HTML: it must parse, link correctly, show the
  right images, use consistent terminology, and read naturally in Chinese.
- `check-translations` enforces glossary consistency rules, such as `Pin` ->
  `引脚`, `poke`/`push` -> `手形`, and `component` -> `组件`. These checks are
  only one QA signal; rendered HTML structure, links, images, English-ratio
  checks, terminology coverage, and human review decide whether a page is ready.
- The generated zh HTML may intentionally differ from the English structure.
  If the inherited structure is malformed or makes the Chinese awkward, fix the
  zh output directly rather than preserving the old mistake.
- Inline HTML tag order is not a default failure condition. Use
  `check-translations --flag-tag-reorder` only when you want an explicit review
  list for placeholder movement.
- Preview lint resolves links against the preview directory first, then against
  `src/main/resources/doc/zh/html` so CSS and images do not need to be copied
  into every scratch preview. Run preview lint with the preview `html` directory
  as `--root`, not a nested section directory, so fallback paths mirror the
  final documentation tree. For focused checks on committed files, keep
  `--root` at the final `zh/html` directory and add one or more `--include`
  filters, such as `--include guide/tutorial`; this avoids false missing-image
  warnings from linting a nested directory as though it were the root.
- Link lint ignores links inside HTML comments because comments are reported
  separately and are not part of the rendered page. The template-only
  `libs/_modelempty` directory is excluded by default; pass
  `--no-default-excludes` if you explicitly want to inspect it.
- Link lint also warns on suspicious whitespace in `href` and `src` values,
  because legacy pages have contained typos such as a space inside a relative
  path.
- Link lint checks exact path casing, which matters once the documentation is
  packaged into a JAR even if the local checkout is on Windows.
- `check-javahelp` validates the JavaHelp map and table-of-contents targets
  used by the in-application help viewer, including TOC icons and the default
  `top`, `guide`, `tutorial`, and `libs` entry points.
- English-ratio checks ignore expected technical tokens such as product names,
  keyboard shortcuts, all-caps acronyms, and version labels before warning.
- Comments from the English source are not emitted by the renderer.
- The renderer rewrites the document language metadata from English to Chinese.
- Glossary enforcement is handled both in the prompt and by lint checks.

## Suggested PR Strategy

1. PR 1: Add this tooling and generate a very small sample set.
2. PR 2: Rebuild `guide/gui`.
3. PR 3: Rebuild `libs/wiring`.
4. Continue by section, keeping generated diffs reviewable.
