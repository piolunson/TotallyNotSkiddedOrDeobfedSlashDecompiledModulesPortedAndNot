#!/usr/bin/env python3
"""Konwertuje access widener Hachimi z intermediary na named (Yarn 1.21.1)."""
import re

MAP = '.mappings/yarn/mappings/mappings.tiny'
SRC = 'downloads/Hachimi-2.0.jar_Decompiler.com/hachimi.accesswidener'
OUT = 'src/main/resources/hachimi.accesswidener'

cls = {}        # full intermediary path (with $) -> full named path (with $)
fld = {}
mth = {}
for line in open(MAP, encoding='utf-8'):
    line = line.rstrip('\n')
    if line.startswith('c\t'):
        _, inter, named = line.split('\t')
        cls[inter] = named
    elif line.startswith('\t'):
        p = line.split('\t')
        if p[1] == 'f':
            fld[p[3]] = p[4]
        elif p[1] == 'm':
            mth[p[3]] = p[4]

DESC = re.compile(r'L([^;]+);')


def map_class(path, warn):
    if path in cls:
        return cls[path]
    if 'class_' in path:
        warn.append(path)
    return path


def map_desc(d, warn):
    return DESC.sub(lambda m: 'L' + map_class(m.group(1), warn) + ';', d)


out = []
missing = []
for raw in open(SRC, encoding='utf-8'):
    line = raw.rstrip('\n')
    if not line.strip():
        continue
    parts = line.split('\t')
    if parts[0] == 'accessWidener':
        out.append('accessWidener\tv2\tnamed')
        continue
    kind = parts[1]
    if kind == 'class':
        out.append('%s\tclass\t%s' % (parts[0], map_class(parts[2], missing)))
    elif kind == 'method':
        owner, name, desc = parts[2], parts[3], parts[4]
        new_name = mth.get(name, name)
        if new_name == name and name.startswith('method_'):
            missing.append(name)
        out.append('%s\tmethod\t%s\t%s\t%s' % (parts[0], map_class(owner, missing),
                                               new_name, map_desc(desc, missing)))
    elif kind == 'field':
        owner, name, desc = parts[2], parts[3], parts[4]
        new_name = fld.get(name, name)
        if new_name == name and name.startswith('field_'):
            missing.append(name)
        out.append('%s\tfield\t%s\t%s\t%s' % (parts[0], map_class(owner, missing),
                                              new_name, map_desc(desc, missing)))
    else:
        out.append(line)

open(OUT, 'w', encoding='utf-8').write('\n'.join(out) + '\n')
print('linii:', len(out), 'niezmapowane:', sorted(set(missing)))
