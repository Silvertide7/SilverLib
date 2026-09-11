# SilverLib

Shared library code for Silvertide's Minecraft mods.

- Minecraft 1.21.1
- NeoForge 21.1.249
- Java 21

## Build & Run

```
./gradlew build          # build the mod jar
./gradlew runClient      # launch the client with the mod loaded
./gradlew runServer      # launch a dedicated server
./gradlew runGameTestServer   # run registered gametests, then exit (needs at least one)
```

The built jar lands in `build/libs/silver_lib-<version>.jar`.

Gametests are enabled for the `silver_lib` namespace in every run config and can be invoked
in-game with `/test`. There are none yet, so `runGameTestServer` will fail until one exists —
`runClient` is the smoke test for now: look for `SilverLib loaded` in the log.

## License

All Rights Reserved — see `LICENSE.md`.

`TEMPLATE_LICENSE.txt` is a separate, narrower notice: it is the NeoForged MDK's own MIT license
and covers only the template files still in this repo (`build.gradle`, the Gradle wrapper,
`.gitignore`, the CI workflow). It does not apply to this mod's own source.
