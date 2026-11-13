#!/usr/bin/env python3
"""Czwarta tura napraw: poprawki zaleznosci typu, ktorych nie da sie opisac
podmiana jednego tokenu. Uruchamiac po fix_artifacts3.py.

- ModuleCommand: lokalna Color color koliduje z wczesniejsza zmienna
- CommandManager: surowy Predicate<Object> z brigadiera + brak importu
- ExplosionUtil: brak importu EntityType
- StreamUtils: rekord Intermediary wymaga jawnego typu
- HUDModule: surowy Stream zamiast Stream<Module> + kast w lambdzie
- ChatNotifierModule: trzy rozne paczki w jednej metodzie
"""
import re

ROOT = 'downloads/Hachimi-2.0.jar_Decompiler.com/'
# --- poprawki odporne na wciecia (regex) ---
REGEX_FIXES = [
    ('net/hachimi/client/impl/command/ModuleCommand.java', [
        (r'Color color = \(\(ColorConfig\)config\)\.parseColor\(value\);',
         'Color parsedColor = ((ColorConfig)config).parseColor(value);'),
        (r'(Color parsedColor = [^\n]*\n\s*)config\.setValue\(color\);',
         r'\1config.setValue(parsedColor);'),
    ]),
    ('net/hachimi/client/impl/manager/client/CommandManager.java', [
        (r'import java\.util\.List;\n',
         'import java.util.List;\nimport java.util.function.Predicate;\n'),
        (r'\.requires\(destination\.getRequirement\(\)\)',
         '.requires((Predicate)destination.getRequirement())'),
    ]),
    ('net/hachimi/client/util/world/ExplosionUtil.java', [
        (r'DefaultAttributeRegistry\.get\(\(EntityType\)entity\.getType\(\)\)',
         'DefaultAttributeRegistry.get((net.minecraft.entity.EntityType)entity.getType())'),
    ]),
    ('net/hachimi/client/util/StreamUtils.java', [
        (r'new Intermediary\(t, key\)', 'new Intermediary<T, U>(t, key)'),
    ]),
    ('net/hachimi/client/impl/module/client/HUDModule.java', [
        (r'Stream<Module> var10000 = modules\.stream\(\);',
         'Stream var10000 = modules.stream();'),
        (r'filter\(\(m\) -> !m\.isHidden\(\)\)',
         'filter((m) -> !((ToggleModule)m).isHidden())'),
    ]),
    ('net/hachimi/client/impl/module/misc/ChatNotifierModule.java', [
        (r'(?<![\w.])entity = event\.getPacket\(\);',
         'Packet entityPacket = event.getPacket();'),
        (r'if \(entity instanceof ', 'if (entityPacket instanceof '),
        (r'\)entity;', ')entityPacket;'),
        (r'\bentity_dup\b', 'poppedEntity'),
        (r'Packet entityPacket = event\.getPacket\(\);(\s*)if \(entityPacket instanceof PlayerRemoveS2CPacket\) \{(\s*)PlayerRemoveS2CPacket packet = \(PlayerRemoveS2CPacket\)entityPacket;',
         r'Packet leavePacket = event.getPacket();\1if (leavePacket instanceof PlayerRemoveS2CPacket) {\2PlayerRemoveS2CPacket packet = (PlayerRemoveS2CPacket)leavePacket;'),
    ]),
]

for path, subs in REGEX_FIXES:
    full = ROOT + path
    s = open(full, encoding='utf-8').read()
    for pat, rep in subs:
        s, n = re.subn(pat, rep, s)
        if n == 0:
            print('  NIE ZNALEZIONO (regex):', path, pat[:50])
    open(full, 'w', encoding='utf-8').write(s)
print('poprawki regex nalozone')
