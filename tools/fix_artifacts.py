#!/usr/bin/env python3
"""Poprawki artefaktow decompilacji potrzebne do kompilacji:
1. nazwy comp_NNN (intermediary) -> Yarn
2. zdublowane lancuchy FQN (X.X -> X)
3. (Typ)this -> (Typ)(Object)this w klasach mixin
4. this == x -> (Object)this == x
"""
import glob
import re

MAP = '.mappings/yarn/mappings/mappings.tiny'
ROOT = 'downloads/Hachimi-2.0.jar_Decompiler.com/net'

mth, fld = {}, {}
for line in open(MAP, encoding='utf-8'):
    if line.startswith('\t'):
        p = line.rstrip('\n').split('\t')
        if p[1] == 'm':
            mth[p[3]] = p[4]
        elif p[1] == 'f':
            fld[p[3]] = p[4]


def fix_comp(src):
    def r(m):
        name = m.group(0)
        after = src[m.end():m.end() + 1]
        if after == '(':
            return mth.get(name, name)
        return fld.get(name, mth.get(name, name))
    return re.sub(r'\bcomp_\d+\b', r, src)


DUP = re.compile(r'((?:[A-Za-z_$][\w$]*\.)+[A-Za-z_$][\w$]*)\.\1\b')


def fix_dup(src):
    while True:
        new = DUP.sub(r'\1', src)
        if new == src:
            return src
        src = new


CAST = re.compile(r'\((?!float\b|double\b|int\b|long\b|short\b|byte\b|char\b|boolean\b|void\b)'
                  r'([A-Za-z][\w.$]*)\)this\b')


def fix_cast(src):
    return CAST.sub(r'(\1)(Object)this', src)


def fix_cmp(src):
    src = re.sub(r'\bthis\b(\s*)(==|!=)(\s*)', r'(Object)this\1\2\3', src)
    src = re.sub(r'([\w.$\)\]])(\s*)(==|!=)(\s*)this\b(?!\.)', r'\1\2\3\4(Object)this', src)
    return src


stats = {'comp': 0, 'dup': 0, 'pkg': 0, 'cast': 0, 'cmp': 0}
for f in sorted(glob.glob(ROOT + '/**/*.java', recursive=True)):
    src = open(f, encoding='utf-8').read()
    orig = src
    n = len(re.findall(r'\bcomp_\d+\b', src))
    if n:
        src = fix_comp(src)
        stats['comp'] += n
    if 'com.com.mojang' in src:
        src = src.replace('com.com.mojang', 'com.mojang')
        stats['pkg'] += 1
    if re.search(DUP, src):
        before = src
        src = fix_dup(src)
        stats['dup'] += (before != src)
    if 'org.spongepowered.asm.mixin.Mixin;' in src:
        c = len(CAST.findall(src))
        if c:
            src = fix_cast(src)
            stats['cast'] += c
        m = len(re.findall(r'\bthis\b\s*(?:==|!=)', src)) + len(re.findall(r'(?:==|!=)\s*this\b', src))
        if m:
            src = fix_cmp(src)
            stats['cmp'] += m
    if src != orig:
        open(f, 'w', encoding='utf-8').write(src)

print('poprawki:', stats)
