#!/usr/bin/env python3
"""Naprawy skladni, ktorych nie da sie zrobic podmiana tokenu:

1. `enum X permits null, null, ... {` -> `enum X {`
   CFR dopisuje puste klauzule `permits` przy enumach, czego javac nie przyjmuje.
2. `try { ... }` bez catch/finally -> doklejenie `catch (Exception ignored) {}`
   CFR gubi catch, ktory w oryginale tylko polykal wyjatek.
"""
import glob
import re

ROOTS = [
    'downloads/Hachimi-2.0.jar_Decompiler.com/net',
    'downloads/Hachimi-2.0.jar_Decompiler.com/meteordevelopment',
]

PERMITS = re.compile(r'(\benum\s+[\w$]+)\s+permits\s+(?:null\s*,?\s*)+(?=\{)')


def match_brace(s, open_idx):
    depth = 0
    for i in range(open_idx, len(s)):
        if s[i] == '{':
            depth += 1
        elif s[i] == '}':
            depth -= 1
            if depth == 0:
                return i
    return -1


def try_points(src):
    points = []
    for m in re.finditer(r'\btry\s*\{', src):
        close = match_brace(src, m.end() - 1)
        if close < 0:
            continue
        rest = src[close + 1:].lstrip()
        if rest.startswith('catch') or rest.startswith('finally'):
            continue
        line_start = src.rfind('\n', 0, m.start()) + 1
        indent = re.match(r'\s*', src[line_start:]).group(0)
        points.append((close, indent))
    return points


stats = {'permits': 0, 'try': 0, 'pliki': 0}
for root in ROOTS:
    for f in sorted(glob.glob(root + '/**/*.java', recursive=True)):
        src = orig = open(f, encoding='utf-8').read()
        src, n = PERMITS.subn(r'\1 ', src)
        stats['permits'] += n
        points = try_points(src)
        for close, indent in sorted(points, reverse=True):
            src = (src[:close] + '} catch (Exception ignored) {\n' + indent + '}'
                   + src[close + 1:])
        stats['try'] += len(points)
        if src != orig:
            open(f, 'w', encoding='utf-8').write(src)
            stats['pliki'] += 1

print('naprawy skladni:', stats)
