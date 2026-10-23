# Better Rail Beetle

## Native source fixture safety

The source native fixture uses the actual ForgeGradle RunConfig, not the premature
JavaExec default. Preparation creates only absent comment-only server settings (native
defaults) and copies authored structures into the fresh fixture. A separate `nativeClear`
source set attaches only to `gameTestServer`, never ordinary client/server or shipping
runtime/sources archives. Its required arena-only mixin materializes old block entities at
`StructureUtils.clearBlock` entry, before the native state replacement. Native
`LevelChunk.setBlockState` replaces section state before old-block `onRemove` callbacks;
callbacks such as chest removal can otherwise promote lazy worldgen DUMMY data against
replacement air. Preparation uses the real native decoder once, never removes pending NBT,
and throws if an existing BE state cannot materialize. Corrupt native loads still fail.
The unsafe-excavation test additionally installs a genuinely pending DUMMY chest and runs
the real arena clear, checking that neither pending nor live data remains afterward.
Pure tests cover no-op, exactly-once decoding, null failure and original exception forwarding;
RunConfig and actual archive guards cover nonshipping scope. These tests do not identify
the former blocks at historical failed coordinates or establish fresh native acceptance. The gate requires actual
startup/shutdown, exactly 41 completed/passed required native tests and zero ERROR/FATAL
or failed-operation WARN/stack records in both actual latest/debug logs. With the required
cave runtime enabled, the debug log must also prove one actual application of the Alex JSON
serialization mixin. `verifyFast` exercises settings preservation and negative diagnostic/
population controls without Minecraft. This does not authorize the separate visual harness or prove
visual acceptance. Retain complete old builds, libraries, fixtures and evidence before
any independently authorized clean/reset.

### Bounded original-warning observer (not acceptance)

The same native-only `nativeClear` source set observes ordinal zero of the pinned
`LevelChunk.promotePendingBlockEntity(BlockPos, CompoundTag)`
`Logger.warn(String, Object, Object)` calls: the original DUMMY warning, not an inferred
precondition. It forwards the original receiver and arguments once **before** observation,
including when the logger throws; observer failures cannot replace that original exception.
It buffers at most four incident records using only supplied position and tag scalar
`id/x/y/z/keepPacked` arguments plus up to 24 actual `StackTraceElement`s per incident.
Stack truncation, capture failures and budget-dropped incidents remain explicit. No added
world/state/chunk/RNG query, pending mutation, loop logging or I/O is performed. Helpers
live outside the transformer's reserved `.mixin` package.

One independent `BC_RAIL_PROMOTION_WARNING_WITNESS` INFO marker flushes only at
`ServerStoppedEvent`, after setup/test loops, binding the fresh RunConfig UUID and optional
existing lifecycle-witness token. It does not filter, replace or excuse the original WARN;
strict diagnostics still fail. A missing shutdown marker is missing evidence, not a pass.
Pure compiled-hook checks pin the actual target ABI/ordinals and unchanged forwarding,
exception identity, privacy, budget and nonshipping exclusions, with negative controls.
They do not establish actual native Mixin application or identify an unseen historical caller.

### Bounded source-native producer witness (observation only)

The native-only source set also binds the original `StructureUtils.forceLoadChunks` and
`clearSpaceForStructure` arguments with exact-once MixinExtras 0.5.4 `WrapMethod` scopes,
balanced in `finally`. Scope records contain only original coordinates/bounds/floor,
thread/sequence/parent/depth and identity hashes (hashes can collide; no world/argument object
is retained). Caps are 256 scopes and depth16; overflow, unbound/cross-thread records and
dangling/imbalanced scopes are explicit, never inferred association.

Typed redirects in **both** `LevelChunk.setBlockState` and `ProtoChunk.setBlockState` forward
the exact original section receiver, primitive coordinates and requested state once, first,
return the actual old state, and leave original exceptions untouched. Observation only then
uses an exact typed accessor to the inherited pending Map and its supplied-position lookup.
Only changed-state calls with a currently present DUMMY tag are eligible. No terrain/state/chunk
read, block-entity load/factory, RNG, pending mutation or runtime reflection is added.

Before native level loading, startup selects all Minecraft blocks plus all `EntityBlock`
blocks **before** enumerating their full-property states. The immutable identity dictionary
is capped at131072 entries; schema and full enumerated-palette SHA256 digests bind it.
Unknown actual states or a truncated dictionary mark observation incomplete. Eight retained
candidate attempts at most capture original position, dictionary state descriptions, typed
`id/x/y/z/keepPacked`, up to32 tag key/type names (not payload values) and24 stack frames.
Metadata failures, unknowns, budget drops and truncated stacks remain explicit. The ordinary
noncandidate mutation path has no added allocations; the first bounded anomaly captures do
allocate metadata/stack records, and outer setup `WrapMethod` forwarding allocates varargs.
This is diagnostic perturbation, not a zero-allocation or performance-acceptance claim.

One separate nonce-bound `BC_RAIL_PRODUCER_WITNESS` INFO summary flushes only at
`ServerStoppedEvent`. The existing original-warning observer and fatal native log audit remain
unchanged. Completeness describes only the declared hook/selection scope: direct section
bypasses, producers earlier than observation, calls without a returned old state, unselected
states and missing native injection evidence cannot establish absence or causation. Pure JDK
controls execute actual compiled handlers in an inert ABI-stub realm, not Minecraft; native
injection composition and the unchanged41-test run remain the separately serialized gate.

### Source-native pre-clear TICKING readiness candidate

The retained `rail-producer-16` diagnostic completed all41 tests but failed the unchanged
strict warning audit. Two of its eight captured transitions match later warnings: campfire→AIR
at `(246,-47,36)` and spawner→AIR at `(5,4,181)`, both original `clearBlock` stacks with
pending DUMMY. Six other captures were unbound ProtoChunk structure-marker changes, and six
candidate attempts were dropped; this is **not** identification of all four warning producers.
Pinned native ABI shows `LevelChunk.getBlockEntity` can return a live BE without removing its
pending tag, unlike ProtoChunk removal of both maps. The existing per-block pre-materializer
therefore does not establish completion of native `postProcessGeneration`, which clears the
pending Map after its natural shape-update/materialization loop.

`ClearReadinessMixin` is a separate **SOURCE-native fixture candidate**, before the original
`clearSpaceForStructure`, enabled only by the native RunConfig's `readiness=ticking-v1` and
actual `GameTestServer`. It derives the exact native inclusive inflation (X−2/+3, Y−3/+20,
Z−3/+3), capped at64 intersecting chunks. The original 5×5 forcing calls remain untouched;
only missing footprint chunks receive additional native forcing. Those extra tickets are
journaled before admission and released through the original API in a finally-balanced lease,
including partial admission/original-clear failures; existing forced tickets are not removed.
Cleanup failures stay loud, with the original failure primary and cleanup suppressed.

The actual cache distance-manager update propagates tickets and invokes holder `updateFutures`
before selecting each actual BLOCK_TICKING future. It then pumps **the cache's actual**
`BlockableEventLoop<Runnable>` (typed `ChunkMap.mainThreadExecutor`, not server `managedBlock`
or a main-thread `join`). Initial completed Right/UNLOADED, exceptional/cancelled futures,
wrong thread, unsupported/reentrant calls, stale holder/future/status or wrong chunk/world
fail; there is no retry-until-green. Success requires every original native future's Left result,
including the natural native postprocess/start-ticking continuation. No manual postprocess,
manual preparation, pending purge, AIR guard, fake BE or log exemption is added.

A single monotonic30s readiness budget starts before extra forcing; it is not reset per chunk.
The wait predicate and between-operation checks are bounded. **Synchronous native FULL loading
inside `setChunkForced`, or an individual native task, cannot be preempted by this predicate**;
the outer bounded native runner remains necessary. This finite fixture preparation can change
terrain loading, callback/RNG order and startup timing; it is not observationally neutral.
Original clear arguments, geometry, floor, state operations, seed/defaults, observer budget8,
41 tests and strict warning audit remain unchanged. Standalone policy/compiled-ABI/AP controls
are not native injection or acceptance; one separately preregistered native run must qualify it.

## Opt-in nonshipping lifecycle witness (not acceptance)

`rail_native_witness` defaults to exactly `false`. Only a separately admitted forensic
GameTest invocation may set it to `true`; this is not a new test profile or a Native grant.
The separate `nativeFixture` source set joins the same owning mod **only** in that explicit
`gameTestServer` RunConfig. Neither ordinary client/server/default GameTests nor either
shipping runtime/sources JAR includes its classes or mixin resource. `verifyFast` checks
actual RunConfig attachment/default absence, both actual archives, and pure privacy/bounds
contracts before an enabled witness can start its unchanged 41 required native tests.
Actual Gradle/Native qualification remains separately scheduled; standalone javac/JUnit
readiness does not establish RunConfig execution, new archive identity or Mixin application.

The observer reads pending scalar `id/x/y/z/keepPacked`, existing public state descriptions,
chunk status and up to eight caller class/method/line entries. It never serializes/copies a
CompoundTag, inventory, loot seed, payload, private path or player name. The actual expanded
`StructureUtils` boundary binds a synchronous thread/world-identity scope, released in finally.
Within that scope, `Level.setBlock` already acquired the real chunk and old state: the witness
uses those original values before its existing mutation, consulting only the public pending
Map.get accessor for old-BE-state candidates. Proposed new state is an existing argument,
not a committed-outcome claim. No getChunkNow, added state read or position/voxel scan remains.
Promotion observes the ORIGINAL state-read return and original pending-tag argument, scoped
to the same clear operation. Its public getLevel getter compares only existing owner identity.
No diagnostic load, materialization, native mutation, seed/default or timing change is added.
Original arguments/results/exceptions are forwarded once. No old consumed NBT is reconstructed.

Bounds remain 128 clear scopes, 64 distinct supplied chunks per scope, 4096 eligible candidate
calls in total (repeated positions consume budget), 1024 records and a 1MiB artifact. Non-BE
old states/no-op paths and out-of-scope promotion are not pending-map absence proof. Aborted or
unbound scopes, omitted caller tails, field/record/byte/candidate/chunk truncation are explicit, never
converted into proof about unseen terrain or data. Scalars are buffered; serialization
and one create-new `native-witness.json` write happen only at `ServerStoppedEvent` in the
owning fresh UUID fixture. An abort before that event can leave no witness: that is not
successful observation. Failed native loads remain FAIL even if all tests pass or a
witness exists; the owning guard also rejects the exact logger-scoped reverse-worded
LevelChunk failures. Do not discard pending data or infer a global DUMMY workaround.

## Optional Alex's Caves configuration serialization

When both Alex's Caves and Citadel bytecode are present, a soft-target production mixin
serializes Alex's existing JSON read/create/read/validate/write transaction. It forwards
all original arguments and calls the original once, preserving results, original failure
handling and terrain/default values. A reentrant lock releases in `finally`; no NightConfig
getters/events, native world reads, worker threads or delayed work are added inside it.
The narrower file transaction is locked, not the surrounding Forge config callbacks or
whole world/config reload. Missing optional targets skip safely; genuine bytecode lookup IO
failures are not treated as absence. Pure contention/recursion/failure/absence contracts
run without cave classes; they do not establish native race acceptance.

## Playtest diagnostics

`bc.playtest.better_rail_beetle.beetle` reports control/context rejections, authored governor
requirements, route-selection reasons and departure admission, plus completed/recovered
route observations. Native checks keep their original short-circuit order and run once.
Placement/support and rollback-invocation counts accumulate on the existing vehicle and
are reported at control/route boundaries, never per rail or motion tick. Rollback
invocation is not independently observed restoration; departure admission is not route
completion. Existing operation/Beetle UUIDs identify causal native work, not players.

INFO startup supplies enabled/version/integration presence and exact own-artifact source
metadata when available (UNKNOWN otherwise). The repository-local Java17 helper shares
120 DEBUG/TRACE details/minute, burst20,256 keys and2048-character records. Native END ticks
emit30-second summaries; stop clears aggregation without rearming TRACE or the allowance.
Standalone remains native INFO; pack profile chooses scoped DEBUG/`bc.playtest.level`.
No route dumps, inventory/NBT, names, payloads, extra terrain probes or RNG draws are added.
Common six-case and work/rollback projection tests run alongside the existing native gates.

## Scope and authority

This repository owns its mod-specific behavior and authoring inputs. Read [local instructions](AGENTS.md)
and the [shared documentation/policy index](../../better-content-modpack/docs/README.md).


Rail Beetle is a server-authoritative Forge 1.20.1 mod. At the end of an
existing railway, a Beetle surveys nearby cave floors once per second, presents
three core terrain-following rail routes plus up to four more when their
destinations are well separated. Up to three nearby Route Beacons add shortest
legal routes ending beside those blocks. The Beetle builds the route selected with
the crosshair and the `G` key. A Beetle placed in the middle of a railway stays
in normal driving mode instead of trying to plan from that location.

The vehicle is an `AbstractMinecart`, so Create's normal minecart coupling works
without replacing vanilla rail physics. Its industrial automaton renderer is
larger than its standard minecart hitbox: black steel and aged brass enclose a
timber cargo bed, while rail-setting jaws, survey booms, split machinery covers,
and six working tool assemblies form the Beetle silhouette. Installed modules
and the selected engine alter visible machinery. Create and every alternate
power mod are optional.

## Underground discoveries

With Create loaded, new Overworld terrain can contain a cave siding: one baseline
Beetle, an empty passenger cart, and a chest minecart already coupled in that order.
The Beetle's amber nose faces out of an excavated ruined work-camp alcove into the
natural cave, with both trailing carts deeper inside. Its working inventory contains 64 rails,
64 cobblestone and 16 coal; the chest carries the same quantities as reserves.
Freight does not automatically feed the working inventory. No advanced modules or
alternate engine are supplied.

The consist stays braked and does no surveying until a player opens the Beetle (or
issues an authorized control). This parked state survives saving. Open it, inspect
the supplies, select a projected route with `G`, and ride in the passenger cart.

Discoveries use post-carving feature placement at `underground_decoration`, not a
structure grid. `data/better_rail_beetle/worldgen/placed_feature/cave_siding.json`
selects an average of one chunk in sixteen for a bounded search across its columns
and floor heights (Y=-48 through Y=64, at least 16 blocks under the local surface).
Both normal air and cave air are accepted. Selection is not guaranteed placement.

The search finds a dry natural cave mouth with excavatable rock behind it, then
chooses the heading from that rock toward the cave. It carves an irregular alcove
for the eight-block siding and a small lateral camp, with rough/mossy foundations,
a broken timber canopy, a lantern, a workbench, rubble and an extinguished hearth.
No extra loot container is added. Only the two immediate departure steps must
already be open cave; the rest of the route is left to the normal terrain planner.
A pre-existing straight 13-block corridor is no longer required.

The camp and its untouched safety shell stay in one chunk. Excavation is limited
to ordinary Overworld stone/soil/gravel; ores in the excavation, fluids, block
entities, manufactured blocks, deep unsupported foundations, insufficient roof
cover and intersecting structure starts veto the entire site before any writes.
At most one camp is placed per chunk. New discoveries remain features, not
`/locate structure` targets. Legacy structure/piece registrations and definitions
remain readable for existing saves; old empty candidate records are not repaired
or erased. Full-pack natural discovery frequency and visual acceptance remain
separate from the local native noise-column probe and authored GameTests.

A cobblestone-looking foundation block initializes the vehicles on the server,
using deterministic per-site UUIDs and ordinary Create couplings. It records
completion permanently, never restocks existing carts, and never replaces a
previously spawned missing cart. Initialization has bounded retries, loads no
extra chunks, and leaves player-disturbed sites alone. Completed sites remain
completed after the vehicles leave. This is not transactional recovery from
arbitrary disk corruption or independently rolled-back chunk/entity files.

Existing chest-item starter packages remain a separate acquisition route. New
worldgen does not retrofit already-generated chunks.

## Use

- Place the Beetle on a rail. Its amber front lamp shows the persistent forward
  direction; repeated forward commands never infer a new direction from yaw.
- Open it to load tagged rails, furnace fuel, and solid support blocks into its
  unchanged 27-slot cargo hold, use the compact controls, or service its one
  engine cradle and six module bays while stopped with the brakes applied.
- At a rail terminus, aim near any colored ghost route and press `G` from anywhere
  the server tracks the Beetle. The highlighted route and prompt identify the
  selection. Press `G` again to stop; while stopped, press it to clear the route
  and choose another. Missing rails, supports, or startup fuel are shown in red
  on the prompt without preventing selection.
- Drive at reverse `0.5`, `0.5x` (`2` blocks/second), or `1x` (`4`
  blocks/second) immediately. High-Speed Governors add `2x` (`8`) and `3x`
  (`12`) without withholding useful baseline movement.
- The Beetle automatically applies its hand brake between automatic phases and
  on slopes. Its `0.4` blocks/tick² traction is shared across a Create-coupled
  consist so trailing carts keep climbing without pulling the Beetle backward.
- Outside Neutral, collisions cannot slow or displace the Beetle; it shoves
  movable entities clear and clamps itself to terminal rail centers. Neutral
  clears routes, releases the brake, and restores ordinary pushing and towing.
- The baseline route cap is 64 rails; survey modules raise it to 128 and 192.
  The server config is an upper safety limit, not an upgrade bypass.
- When choosing supports, it preserves fuel by using non-burnable blocks first,
  then blocks with lower furnace burn time before more valuable fuel blocks.

Every active operation consumes work: motion scales with speed squared and adds
grade/consist penalties, while surveying, rail/support placement, clearing,
remote commands, and a lit searchlight have explicit costs. UI use, Stop, and
fail-safe braking are free. With no engine—or an empty installed engine—the
built-in firebox burns cargo fuel. Optional drives store their native resource
on the engine item: Create steam, Forge Energy, Ars Source, Blood Magic life
essence, PneumaticCraft air, Goety soul energy, or Malum spirits. The stopped
Beetle exposes cargo plus Forge Energy/fluid docking where applicable.

Ten module families cover speed, efficiency, brakes, adhesion, acceleration,
drawgear, survey range, dispatch control, trestle construction, and a dynamic
searchlight. Duplicate families cannot be installed. Dispatch I works within
128 blocks; Dispatch II reaches any already-loaded Beetle in the same dimension
and never adds chunk tickets. Sodium/Embeddium Dynamic Lights supplies true
light when present; the bright searchlight model remains as the fallback.

The planner only traverses loaded terrain, never excavates solid blocks or routes
underwater, and clears tagged dry vegetation, leaves, and snow layers with normal
block drops as it builds. Baseline trestles span one rail and build supports up
to four blocks deep. Trestle modules raise those limits to 5/12 and 12/32.
Columns are placed bottom-up. If real block physics later removes a support, the
Beetle halts without stabilizing or refunding the player's failed structure. It
never substitutes a down/up slope. It requires
two straight rails between 90-degree turns and never places a route immediately
beside unrelated existing rails. It accepts vanilla/Forge rail tags plus Rail
Beetle's explicit compatibility tag and rejects stale or changed routes before
movement begins.

Validation:

```sh
./gradlew verifyFast
./gradlew verifyFull
```

`verifyFull` runs the native GameTests, including all four excavated-camp headings
and departures into the natural cave, fluid/structure/roof vetoes, a one-search
cave-air placement test, a fixed-seed native Overworld noise-column probe,
parked-state saving, and no replacement after removal. A production placed-feature test searches an
authored off-centre cave pocket, checks single-site placement and preserves a solid
centre column. This is not a natural-terrain frequency test. Its fresh-world
preparation now uses a new `build/gametest/<UUID>/` ForgeGradle RunConfig and leaves
complete prior `run-gametest/` and `run-gametest-history/` trees untouched. These isolated tests do not establish natural-terrain frequency or
visual acceptance in the full pack.

## Minimal visual harness

The development-only visual harness opens the production Rail Beetle screens
without launching the Better Content modpack. Start these in separate terminals:

```sh
./gradlew runVisualServer -PincludeCaveIntegrationRuntime=false
./gradlew runVisualClient -PincludeCaveIntegrationRuntime=false
```

After the client joins as `Dev`, enter one of these commands in the server
terminal. The harness creates the requested state, opens the real menu, and
writes a 1280×720 image to `run-visual-client/screenshots/`:

```text
rbvisual main Dev baseline main-baseline
rbvisual main Dev upgraded main-upgraded
rbvisual main Dev locked main-service-locked
rbvisual remote Dev baseline remote-baseline
rbvisual remote Dev upgraded remote-upgraded
```

`baseline` uses the built-in firebox and no optional drive upgrades; a remote
baseline adds only the receiver required to open that screen. `upgraded` fills
the engine and module slots, while `locked` puts the machinery bays into their
in-use lock state.
