# Dimension-bound dragons

Implementation plan for the `dmr-fixes` branch.

## Goal

Keep dragons useful inside the dimension where they were raised without allowing one Overworld dragon to trivialize progression in the Nether or The Aether.

## Behaviour

- [ ] Persist a home dimension for every whistle-bound dragon.
- [ ] New dragons use the dimension where they hatch / are first bound as their home dimension.
- [ ] Existing saves migrate safely: if home-dimension data is absent, initialize it once from the dragon instance's currently stored dimension.
- [ ] Migration must preserve UUID, owner, name, breed, age, inventory and existing whistle binding.
- [ ] Never silently rewrite a dragon's home dimension when the owner changes dimensions.
- [ ] A whistle only attempts to summon the dragon bound to that whistle/index. Do not fall back to another whistle or another summonable dragon.
- [ ] When player and bound dragon home dimensions differ, cancel remote summon before inventory/world-data transfer.
- [ ] Show this action-bar message on a blocked summon: `This dragon belongs to another dimension and cannot be summoned here.`
- [ ] Keep current walk/follow/teleport behaviour unchanged when player is in the dragon's home dimension.
- [ ] Do not block deliberately moving a living dragon through a portal; the restriction is on whistle summon/recall across dimensions.

## Egg availability

- [ ] Audit current dragon egg loot generation.
- [ ] Add dragon eggs to appropriate Nether treasure loot.
- [ ] Add dragon eggs to appropriate The Aether treasure loot when The Aether is installed.
- [ ] Prefer meaningful rare treasure sources over tiny grind-oriented chances in common chests.
- [ ] Keep manual egg transport between dimensions valid.

## Compatibility / tests

- [ ] Load an existing world with a previously bound dragon and verify migration.
- [ ] Verify multiple whistles each target only their own bound dragon.
- [ ] Verify wrong-dimension summon is blocked with the English action-bar message.
- [ ] Verify same-dimension summon works as before.
- [ ] Verify death/respawn and whistle re-binding cannot leave stale home-dimension data.
- [ ] Verify Nether and Aether loot integration does not hard-depend on The Aether when absent.
- [ ] Run the repository GitHub Actions build before merge.
