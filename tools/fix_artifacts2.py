#!/usr/bin/env python3
"""Druga tura napraw artefaktow decompilacji (typy generyczne, duplikaty zmiennych).

Usage: python3 tools/fix_artifacts2.py <build.log>
"""
import glob
import re
import sys

ROOT = 'downloads/Hachimi-2.0.jar_Decompiler.com/net'
LOG = sys.argv[1] if len(sys.argv) > 1 else '/tmp/build.log'


def collapse_dup_fqn(src):
    """X.com.mojang.pkg.X.Y -> com.mojang.pkg.X.Y  (zdublowany prefiks po remapie)"""
    pat = re.compile(r'([A-Za-z_$][\w$]*)\.((?:com|net|org|io)\.[\w.$]*?\b\1\.[\w.$]+)')
    prev = None
    while prev != src:
        prev = src
        src = pat.sub(r'\2', src)
    src = re.sub(r'(@Nullable\s+)?([\w.$]+)\.@Nullable\s+\2\.', r'\1\2.', src)
    return src


def fix_generics(src):
    src = src.replace('.orElse((Object)null)', '.orElse(null)')
    src = src.replace('setReturnValue((Object)null)', 'setReturnValue(null)')
    src = re.sub(r',\s*\(Object\)null\)', ', null)', src)
    src = re.sub(r'\bnew (ArrayList|HashSet|LinkedHashSet|HashMap|LinkedHashMap|ConcurrentHashMap)'
                 r'(<[^;()]*>)?\(', r'new \1<>(', src)
    src = re.sub(r'\bnew (LinkedBlockingQueue|ArrayDeque|PriorityQueue)(<[^;()]*>)?\(',
                 r'new \1<>(', src)
    src = re.sub(r'for\s*\(\s*([A-Z][\w$.]*(?:<[^>]*>)?)\s+(\w+)\s*:\s*\(List\)',
                 r'for(\1 \2 : (List<\1>)', src)
    src = re.sub(r'\((Accessor\w+|IChatHudLineVisible)\)', r'(\1)(Object)', src)
    return src


def scope_end(lines, decl_idx):
    """Indeks pierwszej linii poza blokiem, w ktorym jest deklaracja."""
    depth = 0
    for i in range(decl_idx):
        depth += lines[i].count('{') - lines[i].count('}')
    indent = len(lines[decl_idx]) - len(lines[decl_idx].lstrip())
    d = depth
    for i in range(decl_idx, len(lines)):
        if i > decl_idx:
            stripped = lines[i].strip()
            if d <= depth and (stripped.startswith('case ') or stripped.startswith('default')):
                return i
        d += lines[i].count('{') - lines[i].count('}')
        if d < depth:
            return i
        if i > decl_idx and d <= depth and lines[i].strip() == '}' and \
                len(lines[i]) - len(lines[i].lstrip()) < indent:
            return i
    return len(lines)


def rename_dup(path, line_no, var):
    lines = open(path, encoding='utf-8').read().split('\n')
    idx = line_no - 1
    new = var + '_dup'
    n = 1
    existing = '\n'.join(lines)
    while re.search(r'\b%s\b' % re.escape(new), existing):
        n += 1
        new = '%s_dup%d' % (var, n)
    end = scope_end(lines, idx)
    for i in range(idx, min(end + 1, len(lines))):
        lines[i] = re.sub(r'\b%s\b' % re.escape(var), new, lines[i])
    open(path, 'w', encoding='utf-8').write('\n'.join(lines))
    return new


for f in sorted(glob.glob(ROOT + '/**/*.java', recursive=True)):
    s = open(f, encoding='utf-8').read()
    o = s
    s = collapse_dup_fqn(s)
    s = fix_generics(s)
    if s != o:
        open(f, 'w', encoding='utf-8').write(s)

dups = []
for m in re.finditer(r'^(/\S+\.java):(\d+): error: variable (\w+) is already defined',
                     open(LOG, encoding='utf-8', errors='replace').read(), re.M):
    dups.append((m.group(1), int(m.group(2)), m.group(3)))
seen = set()
for path, line, var in dups:
    if (path, line) in seen:
        continue
    seen.add((path, line))
    print('rename', path.split('Decompiler.com/')[-1], line, var, '->',
          rename_dup(path, line, var))
