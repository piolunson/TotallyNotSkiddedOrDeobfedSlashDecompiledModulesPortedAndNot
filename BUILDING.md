# Building Hachimi 2.0

The sources in `Hachimi-2.0.jar_Decompiler.com.zip` are a CFR decompile in Minecraft's
**intermediary** namespace (`class_1234`, `field_1234`, `method_1234`), so they do not
compile against readable names. The pipeline below remaps them to Yarn 1.21.1 and repairs
the decompiler artifacts that javac rejects. Running it from a clean clone of this repo
produces `build/libs/hachimi-2.0.jar`.

## Requirements

- JDK 21
- Gradle 8.14+ (Fabric Loom 1.9.2 refuses anything older than 8.11)
- Network access on the first build: Loom downloads Minecraft 1.21.1 and its libraries

## Pipeline

```bash
# 1. Yarn 1.21.1 mappings (intermediary -> named)
bash tools/fetch_mappings.sh

# 2. unpack the decompiled jar
mkdir -p downloads
unzip -q Hachimi-2.0.jar_Decompiler.com.zip -d downloads/Hachimi-2.0.jar_Decompiler.com

# 3. remap sources and the access widener
python3 tools/remap_yarn.py --apply
python3 tools/aw_to_named.py

# 4. decompiler artifacts
python3 tools/fix_artifacts.py     # comp_* names, double FQNs, mixin self-casts
python3 tools/fix_syntax.py        # enum permits clauses, try without catch

# 5. runtime assets into the project
mkdir -p src/main/resources/assets
cp -r downloads/Hachimi-2.0.jar_Decompiler.com/assets/. src/main/resources/assets/

# 6. build, then finish the repairs that javac points at
gradle build > /tmp/build.log 2>&1; grep -c 'error:' /tmp/build.log
python3 tools/fix_artifacts2.py /tmp/build.log   # duplicated locals, generics
python3 tools/fix_artifacts3.py                   # explicit casts and renames
python3 tools/fix_artifacts4.py                   # remaining type-dependency fixes

# 7. final build
gradle build
# -> build/libs/hachimi-2.0.jar
```

## Notes

- `build.gradle` compiles the sources in place from `downloads/Hachimi-2.0.jar_Decompiler.com/`,
  the tree produced by step 2. Nothing is copied into `src/main/java`.
- `src/main/resources/hachimi.accesswidener` is the Yarn-named rewrite; the original in the
  zip is intermediary and Loom rejects it with `Namespace mismatch, expected named got intermediary`.
- Step 6 needs the log of the failed build, because the duplicated local variables come from javac.
- The scripts print what they changed and warn about anything they could not find, so a silent
  run means the sources did not match the expected decompiler output.
- Loom remaps the result back to intermediary, so the jar loads on a vanilla Fabric client.
- The produced jar is **not runtime-tested** here: nothing in this repo starts Minecraft.
