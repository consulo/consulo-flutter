#!/usr/bin/env python3
# Converts upstream resources into the Consulo layout:
#  - src/io/flutter/FlutterBundle.properties -> LOCALIZE-LIB (FlutterLocalize), so FlutterBundle keys stay valid
#  - resources/icons/**/*.{svg,png} -> ICON-LIB (FlutterIconGroup), used by icons.FlutterIcons
#  - resources/liveTemplates/*.xml -> consulo.flutter.sdk.FlutterLiveTemplateContributor
# Run after merging upstream changes.
import json, os, shutil
import xml.etree.ElementTree as ET

BASE = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', '..')
ROOT = os.path.join(BASE, 'resources')
BUNDLE = os.path.join(BASE, 'src', 'io', 'flutter', 'FlutterBundle.properties')
OUT = 'LOCALIZE-LIB/en_US/consulo.flutter.FlutterLocalize.yaml'
ICONS = 'icons'
ICON_LIB = 'ICON-LIB/{theme}/consulo.flutter.FlutterIconGroup'
LIVE_TEMPLATES = 'liveTemplates'
LIVE_TEMPLATE_CONTRIBUTOR = os.path.join(BASE, 'src', 'consulo', 'flutter', 'sdk', 'FlutterLiveTemplateContributor.java')
# contextId (upstream plugin.xml liveTemplateContext) -> Consulo context class
LIVE_TEMPLATE_CONTEXTS = {'DART': 'DartTemplateContextType.Generic', 'DART_STATEMENT': 'DartTemplateContextType.Statement',
                          'DART_TOPLEVEL': 'DartToplevelTemplateContextType'}


def read_properties(path):
    result = {}
    with open(path, encoding='utf-8') as f:
        lines = f.read().split('\n')
    i = 0
    while i < len(lines):
        line = lines[i].lstrip()
        i += 1
        if not line or line[0] in '#!':
            continue
        while line.endswith('\\') and not line.endswith('\\\\'):
            line = line[:-1] + (lines[i].lstrip() if i < len(lines) else '')
            i += 1
        key, sep, value = '', '', ''
        j = 0
        while j < len(line):
            c = line[j]
            if c == '\\':
                key += line[j:j + 2]
                j += 2
                continue
            if c in '=: \t':
                break
            key += c
            j += 1
        rest = line[j:].lstrip(' \t')
        if rest[:1] in ('=', ':'):
            rest = rest[1:].lstrip(' \t')
        result[unescape(key)] = unescape(rest)
    return result


def unescape(s):
    out, j = '', 0
    while j < len(s):
        c = s[j]
        if c == '\\' and j + 1 < len(s):
            n = s[j + 1]
            if n == 'u':
                out += chr(int(s[j + 2:j + 6], 16))
                j += 6
                continue
            out += {'n': '\n', 't': '\t', 'r': '\r', 'f': '\f'}.get(n, n)
            j += 2
            continue
        out += c
        j += 1
    return out


def sync_icons():
    src = os.path.join(ROOT, ICONS)
    for theme in ('light', 'dark'):
        shutil.rmtree(os.path.join(ROOT, ICON_LIB.format(theme=theme)), ignore_errors=True)
    count = 0
    for dirpath, dirnames, filenames in os.walk(src):
        dirnames.sort()
        for name in sorted(filenames):
            if not name.endswith(('.svg', '.png')):
                continue
            if '@20x20' in name:
                # new UI tool window size variants; Consulo scales the 16x16 icon
                continue
            rel = os.path.relpath(dirpath, src)
            dark = name.endswith(('_dark.svg', '_dark.png'))
            target = name.replace('_dark.', '.') if dark else name
            out = os.path.normpath(os.path.join(ROOT, ICON_LIB.format(theme='dark' if dark else 'light'), rel))
            os.makedirs(out, exist_ok=True)
            shutil.copyfile(os.path.join(dirpath, name), os.path.join(out, target))
            count += 1
    print(f'{count} icons -> {ICON_LIB}')


def java_string(s):
    return json.dumps(s, ensure_ascii=False)


def sync_live_templates():
    src = os.path.join(ROOT, LIVE_TEMPLATES)
    body = []
    count = 0
    for name in sorted(os.listdir(src)):
        if not name.endswith('.xml'):
            continue
        for t in ET.parse(os.path.join(src, name)).getroot().findall('template'):
            abbreviation = t.get('name')
            if t.get('key'):
                description = f'FlutterBundle.messageValue({java_string(t.get("key"))})'
            else:
                description = f'LocalizeValue.localizeTODO({java_string(t.get("description") or abbreviation)})'
            body.append(f'    try (Builder builder = factory.newBuilder({java_string("flutter" + abbreviation[0].upper() + abbreviation[1:])}, '
                        f'{java_string(abbreviation)}, {java_string(t.get("value"))}, {description})) {{')
            if t.get('toReformat') == 'true':
                body.append('      builder.withReformat();')
            for v in t.findall('variable'):
                body.append(f'      builder.withVariable({java_string(v.get("name"))}, {java_string(v.get("expression") or "")}, '
                            f'{java_string(v.get("defaultValue") or "")}, {str(v.get("alwaysStopAt") == "true").lower()});')
            for o in t.findall('context/option'):
                body.append(f'      builder.withContext({LIVE_TEMPLATE_CONTEXTS[o.get("name")]}.class, {o.get("value")});')
            body.append('    }')
            body.append('')
            count += 1
    if body:
        body.pop()
    java = """// Generated by tool/consulo/sync_upstream_resources.py from resources/liveTemplates/*.xml - do not edit
package consulo.flutter.sdk;

import com.jetbrains.lang.dart.ide.template.DartTemplateContextType;
import consulo.annotation.component.ExtensionImpl;
import consulo.language.editor.template.LiveTemplateContributor;
import consulo.localize.LocalizeValue;
import io.flutter.FlutterBundle;
import io.flutter.template.DartToplevelTemplateContextType;
import org.jetbrains.annotations.NotNull;

/**
 * Replaces upstream {@code io.flutter.template.FlutterLiveTemplatesProvider}.
 */
@ExtensionImpl
public class FlutterLiveTemplateContributor implements LiveTemplateContributor {
  @Override
  public @NotNull String groupId() {
    return "flutter";
  }

  @Override
  public @NotNull LocalizeValue groupName() {
    return LocalizeValue.localizeTODO("Flutter");
  }

  @Override
  public void contribute(@NotNull Factory factory) {
""" + '\n'.join(body) + """
  }
}
"""
    os.makedirs(os.path.dirname(LIVE_TEMPLATE_CONTRIBUTOR), exist_ok=True)
    with open(LIVE_TEMPLATE_CONTRIBUTOR, 'w', encoding='utf-8') as f:
        f.write(java)
    print(f'{count} live templates -> FlutterLiveTemplateContributor')


def main():
    messages = read_properties(BUNDLE)
    path = os.path.join(ROOT, OUT)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w', encoding='utf-8') as f:
        f.write('# Generated by tool/consulo/sync_upstream_resources.py from src/io/flutter/FlutterBundle.properties - do not edit\n')
        for key in sorted(messages):
            f.write(f'{key}:\n    text: {json.dumps(messages[key], ensure_ascii=False)}\n')
    print(f'{len(messages)} messages -> {OUT}')
    sync_icons()
    sync_live_templates()


if __name__ == '__main__':
    main()
