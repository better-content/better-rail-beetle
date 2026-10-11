# Better Rail Beetle

Forge 1.20.1 / Java 17; mod ID `better_rail_beetle`;
base package `com.bettercontent.betterrailbeetle`. Optional Create 6.0.8 integration.
This clean implementation must not import, inspect, migrate or add compatibility for earlier
Rail Crawler implementations/artifacts.
Deterministic/staged JAR: `./gradlew verifyFast`.
Runtime: `./gradlew verifyFull` (Create, Alex's Caves and YUNG's Better Caves loaded).
Stage: `./gradlew stageRuntimeJar`, `build/libs/better-rail-beetle-<version>.jar`.

Read [shared workspace policy](../../better-content-modpack/docs/policies/workspace.md)
and its linked testing/disposal policies. Docs-only changes use the shared document check
and `git diff --check`, not unrelated runtime builds.
