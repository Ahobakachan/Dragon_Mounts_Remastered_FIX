# Dimension-bound dragons

Target: Minecraft 1.21.1, NeoForge 21.1.176, Java 21.
Build version: `1.9.2-dimensionfix.1`.

## Behaviour

- An egg records its hatching dimension on the dragon entity before spawning it.
- Entity NBT preserves `homeDimension` through saves, portals and whistle reconstruction.
- First binding uses the entity's home, even after a portal trip before binding.
- Legacy bound dragons use the previously stored whistle `dimension` when no home exists. The historical hatching dimension cannot be recovered from old saves that never recorded it.
- Migration matches the logical dragon UUID, including rebinding to another whistle. Replacing a whistle's dragon does not inherit the previous dragon's home.
- A summon is rejected before inventory or world-data transfer when the player's dimension differs from the selected dragon's home. The action bar displays: `This dragon belongs to another dimension and cannot be summoned here.`
- The selected whistle retains priority; a rejected call does not fall back to another dragon.
- Portals and manual egg transport remain allowed. An egg's destination at hatching determines home, regardless of its breed or loot origin.
- Calling within the home dimension retains existing follow/teleport behaviour, including recalling a dragon that was manually moved away from home.

## Eggs

Existing Nether loot was already configured in this branch: Nether eggs have a 35% chance in bastion treasure and 10% in fortress chests.

Aether eggs retain the existing Overworld dungeon entry. Optional The Aether support now adds one egg roll to each dungeon reward chest: bronze 20%, silver 35%, gold 50%. These use the `aether:chests/dungeon/<tier>/<tier>_dungeon_reward` tables verified against The Aether's `1.21.1-develop` sources. Reward tables avoid adding multiple egg rolls via nested treasure tables.

The existing server egg-chance multiplier applies. No Aether classes or hard dependency are introduced; absent tables are skipped by the existing loot injector.

## Verification

Run `./gradlew build runGameTestServer --no-daemon` using Java 21.

GameTests cover:
- real egg hatching and home persistence;
- portal transfer before first binding;
- legacy NBT migration, reconstruction and reload;
- replacing a whistle's dragon and rebinding the original;
- wrong-dimension rejection and selected-whistle priority;
- Nether rejection without inventory transfer or source-entity removal;
- existing same-home calls and inventory recall from another dimension;
- Nether loot pools and simulated optional Aether table-load events without Aether installed.

The optional Aether loot test simulates table loading; it is not a full playthrough with Aether installed. Manual multiplayer and existing-world playtesting remain useful checks before merging.

`Build DMR` now builds this feature branch and PRs targeting `dmr-fixes`, runs GameTests, and only uploads the JAR after success. Reports are uploaded even on failures. The separate PR test workflow is named `PR GameTests`. GameTest failures now print their reason to the job log. Builds no longer run a source-mutating formatter concurrently with compilation; `spotlessApply` remains an explicit developer command.
