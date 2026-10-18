# Better Rail Beetle

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
The Beetle faces the forward rail terminus. Its working inventory contains 64 rails,
64 cobblestone and 16 coal; the chest carries the same quantities as reserves.
Freight does not automatically feed the working inventory. No advanced modules or
alternate engine are supplied.

The consist stays braked and does no surveying until a player opens the Beetle (or
issues an authorized control). This parked state survives saving. Open it, inspect
the supplies, select a projected route with `G`, and ride in the passenger cart.

Discoveries use post-carving cave-floor feature placement, similar to Artifacts'
underground campsites, rather than a structure-candidate grid.
`data/better_rail_beetle/worldgen/placed_feature/cave_siding.json` controls 16 attempts
per chunk: random X/Z and a random height from Y=-48 to Y=64, then a downward scan
through air for solid ground within eight blocks and an offset onto the floor.
The Overworld biome modifier adds this feature at `underground_decoration`.
Each floor position tries four orientations and must remain at least 16 blocks
beneath the local surface. Actual sites require a clear 3-wide, 13-long, 3-high
footprint, ground within three blocks, and an unobstructed forward departure runway.
No cave is excavated; flooded or deep-ravine sites are rejected, and the entire
site fits in one chunk. At most one siding is placed per chunk. Failed attempts
leave no structure starts, and new discoveries are not `/locate structure` targets.
Legacy structure/piece registrations and the structure definition remain readable
for existing saves, but no new legacy starts are generated. Old saved empty
candidate records are not repaired or erased. Discovery density in natural pack
terrain still needs field evaluation; 16 attempts does not mean 16 discoveries.

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

`verifyFull` runs the native GameTests, including all four discovery orientations,
blocked/flooded exit rejection, supplied coupled departure, parked-state saving,
and no replacement after removal. A production placed-feature test searches an
authored off-centre cave pocket, checks single-site placement and preserves a solid
centre column. This is not a natural-terrain frequency test. Its fresh-world
preparation moves previous `run-gametest/world` and `logs` into `run-gametest-history/<timestamp>/` rather than
deleting them. These isolated tests do not establish natural-terrain frequency or
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
