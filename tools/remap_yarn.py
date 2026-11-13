#!/usr/bin/env python3
"""Remap decompiled Hachimi sources from intermediary (class_/field_/method_)
to Yarn 1.21.1 names. Fully-qualified type references become named FQNs and
imports are rewritten, so the code stays internally consistent.

Usage: python3 remap_yarn.py [--apply]
"""
import glob
import os
import re
import sys
import collections

MAP = '.mappings/yarn/mappings/mappings.tiny'
ROOT = 'downloads/Hachimi-2.0.jar_Decompiler.com/net'

cls = {}                      # class_1234 -> net/minecraft/.../Name($Inner)
fld = collections.defaultdict(set)
mth = collections.defaultdict(set)

for line in open(MAP, encoding='utf-8'):
    line = line.rstrip('\n')
    if line.startswith('c\t'):
        _, inter, named = line.split('\t')
        cls[inter.split('/')[-1].split('$')[-1]] = named
    elif line.startswith('\t'):
        p = line.split('\t')
        if p[1] == 'f':
            fld[p[3]].add(p[4])
        elif p[1] == 'm':
            mth[p[3]].add(p[4])


def split_named(n):
    """net/minecraft/x/Outer$Inner -> (net.minecraft.x, Outer.Inner)"""
    pkg, _, last = n.rpartition('/')
    return pkg.replace('/', '.'), last.replace('$', '.')


def simple_of(n):
    return split_named(n)[1].split('.')[0]


def fqn_of(n):
    pkg, last = split_named(n)
    return (pkg + '.' + last) if pkg else last


DECL = re.compile(r'^\s*(?:public|private|protected|final|abstract|static|sealed|non-sealed|\s)*'
                  r'(?:class|interface|enum|record)\s+([A-Za-z_$][\w$]*)', re.M)
TOKEN = re.compile(r'class_\d+')
CHAIN = re.compile(r'\bnet\.minecraft(?:\.class_\d+)+\b')

stats = collections.Counter()
problems = []

files = sorted(glob.glob(ROOT + '/**/*.java', recursive=True))
out = {}

for f in files:
    src = open(f, encoding='utf-8').read()
    declared = set(DECL.findall(src))

    used = set(TOKEN.findall(src))
    missing = [c for c in used if c not in cls]
    if missing:
        problems.append((f, 'brak mapowania', missing))
        continue

    pkg = re.search(r'^package\s+([\w.]+);', src, re.M)
    sibling = set()
    if pkg:
        d = os.path.dirname(f)
        for g in glob.glob(os.path.join(d, '*.java')):
            for n in DECL.findall(open(g, encoding='utf-8').read()):
                sibling.add(n)

    name_count = collections.Counter(simple_of(cls[c]) for c in used)
    form = {}
    for c in used:
        named = cls[c]
        s = simple_of(named)
        # nested types are always written fully qualified: their simple name
        # alone is not a valid Java reference unless the outer type is in scope
        if '$' in named or name_count[s] > 1 or s in declared or s in sibling:
            form[c] = fqn_of(named)
        else:
            form[c] = s

    def repl_chain(m):
        # net.minecraft.class_310.class_8764 -> FQN of the innermost type
        return fqn_of(cls[m.group(0).split('.')[-1]])

    src = CHAIN.sub(repl_chain, src)
    src = TOKEN.sub(lambda m: form[m.group(0)], src)

    def r_field(m):
        name = m.group(0)
        names = fld.get(name)
        if not names:
            problems.append((f, 'brak pola', name))
            return name
        if len(names) > 1:
            problems.append((f, 'kolizja pola', name))
        return sorted(names)[0]

    def r_meth(m):
        name = m.group(0)
        names = mth.get(name)
        if not names:
            problems.append((f, 'brak metody', name))
            return name
        if len(names) > 1:
            problems.append((f, 'kolizja metody', name))
        return sorted(names)[0]

    src = re.sub(r'\bfield_\d+\b', r_field, src)
    src = re.sub(r'\bmethod_\d+\b', r_meth, src)

    lines = src.split('\n')
    seen = set()
    keep = []
    for ln in lines:
        s = ln.strip()
        if s.startswith('import ') and s.endswith(';'):
            if s in seen:
                stats['usuniety_duplikat_importu'] += 1
                continue
            seen.add(s)
        keep.append(ln)
    out[f] = '\n'.join(keep)
    stats['pliki'] += 1

leftover = collections.Counter()
for f, s in out.items():
    for tok, rx in (('class', r'\bclass_\d+\b'), ('field', r'\bfield_\d+\b'), ('method', r'\bmethod_\d+\b')):
        n = len(re.findall(rx, s))
        if n:
            leftover[tok] += n

apply = '--apply' in sys.argv
print('plikow przetworzonych:', stats['pliki'])
print('usunietych duplikatow importow:', stats['usuniety_duplikat_importu'])
print('pozostale tokeny intermediary:', dict(leftover) or 'brak')
print('problemy:', len(problems))
for p in problems[:20]:
    print('  ', p)

if apply:
    for f, s in out.items():
        open(f, 'w', encoding='utf-8').write(s)
    print('ZAPISANO')
