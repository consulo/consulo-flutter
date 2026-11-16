# Consulo port

This repository is a fork of [flutter/flutter-intellij](https://github.com/flutter/flutter-intellij) ported to Consulo.
Upstream history is kept, upstream files stay at their original paths, so upstream changes can be merged.
It depends on the [consulo-dart](https://github.com/consulo/consulo-dart) plugin, ported the same way (see its `CONSULO.md`).

## Layout

- `pom.xml` - the Consulo plugin (Maven). Sources are upstream `src/`, resources are upstream `resources/` (as in
  upstream `build.gradle.kts`); `src/module-info.java` is the Consulo module descriptor.
- `src/consulo/flutter` - Consulo-only code.
- `tool/consulo/sync_upstream_resources.py` - regenerates `LOCALIZE-LIB` (from `src/io/flutter/FlutterBundle.properties`,
  used by `FlutterBundle`) and `ICON-LIB` (from `resources/icons`, used by `FlutterIcons`). Run after merging upstream.

## Decisions

- Java only: upstream Kotlin files are converted to Java, keeping the API Kotlin generates
  (`XxxKt` classes for top-level functions, `INSTANCE` for objects, `getX()` for properties).
- `org.jetbrains.annotations` nullability annotations of upstream are kept as is.
- Internal Consulo APIs (`*.internal.*` packages, `*.impl.*` modules other than `consulo.ide.impl`) are not used.
- Analytics is dropped (as in consulo-dart): `io.flutter.analytics` is disabled and its call sites are removed.
- JxBrowser is dropped (`io.flutter.jxbrowser` engine classes are `.java_`): it needs a commercial license key and
  downloads Chromium at runtime. DevTools views open in the external browser, the fallback upstream already has.
  `view/EmbeddedJcefBrowser` is disabled until it is ported to consulo-embedded-chrome.
- Android Studio integration is dropped (`studio-contribs.xml_`, Gradle sync, add-to-app Gradle editing, opening the
  `android/` module, the framework detector workaround). Emulators and devices still come from the `flutter` CLI.
- The AppCode action is dropped (AppCode is discontinued).
- `icons.FlutterIcons` is `io.flutter.icons.FlutterIcons`: the `icons` package belongs to the Dart module
  (`icons.DartIcons`) and JPMS does not allow split packages. Icons are `consulo.ui.image.Image`.
- `org.dartlang.analysis.server.protocol.ExtractWidgetOptions` / `RefactoringKind` are disabled (`.java_`):
  the Dart module has the same classes.

## Registrations

- Upstream `META-INF/plugin.xml` and the optional `*-contribs.xml` / `flutter-coverage.xml` are kept as `.xml_` as the list
  of upstream registrations; Consulo registrations are annotations (`@ExtensionImpl`, `@ActionImpl`, `@ServiceImpl`,
  `@TopicImpl`). After an upstream merge, compare its diff with the annotations.
- Upstream tests (`testSrc`, `testData`) use the IntelliJ test framework and are not built (`maven.test.skip`).

## Missing Consulo APIs

Places where upstream needs a platform API Consulo does not provide publicly; worked around in the plugin for now:

- maven-consulo-plugin `generate-icon` stops at the first resource directory without `ICON-LIB` (`return` instead of
  `continue`), so `src/` is not a resource root (it has no resources the plugin needs).
