# GunsAndGadgets - Revision and craft provenance

## Status

| Batch | Status |
|-------|--------|
| GG-0 + GG-1 (foundation) | **Done** - provenance IO, RevisionTracker, disabled parts, broken guns |
| GG-1a Stamp on craft (Batch 2) | **Done** - `gg_craft_parts` stamped on real craft only |
| GG-1c GunStatRefresher (Batch 3) | **Done** - lazy refresh, `/gg refresh`, runtime preservation |
| GG-1d Recycler provider | **Done** - `recycler` `GunsAndGadgetsProvider` sums stamped part costs |

## Implemented (Batch 3 - GunStatRefresher)

When a stamped part revision is behind the live `parts.yml` revision:

- `GunStatRefresher.refresh()` rebuilds the gun from stamped part ids via `InventoryManager.rebuildFromParts()`.
- Preserves runtime identity: `gun_id`, `accuracy_salt`, loaded ammo (`bullets_loaded`, `ammo_loaded`), mid-reload state (`reload_ammo`, `reload_amount`), and `last_fire`.
- Syncs provenance revisions and re-applies `gg_craft_parts` / `gg_parts_revision`.
- Does **not** refresh broken guns (`GunBrokenMarker.isBroken`) or guns with missing stamped ids (failed refresh; `GunManager` still marks broken on use).

### Lazy refresh

`GunRefreshListener` (MONITOR, next tick) refreshes outdated managed guns on:

- Hotbar slot change (`PlayerItemHeldEvent`)
- Inventory click (clicked slot + cursor)
- Item drop (`PlayerDropItemEvent`)

Managed = has `gun_id` PDC and readable `gg_craft_parts`.

### `/gg refresh`

- Player only; force-refreshes main hand (`GunStatRefresher.refresh(hand, true)`).
- Permission: `gunsandgadgets.reload` (same as `/gg reload`).
- Lore-only part yaml edits do **not** bump revision, so lazy refresh will not update those guns.

### Visual skin note

If the gun had loaded ammo when refreshed, runtime ammo PDC is preserved. The displayed skin may stay on the CARRY variant until the next fire/reload cycle updates the skin state.

### Debug

`config.yml`:

```yaml
stat_refresh_debug: false
```

When `true`, logs outdated part ids and `gun_id` to the server console on refresh.

## Implemented (Batch 2 - stamp on craft)

When `InventoryManager.createOutputItem(..., gui=false)` completes a real craft:

- `GunCraftProvenance.from(parts).applyTo(item)` writes `gg_craft_parts` (JSON list of part id + revision) and `gg_parts_revision` (max revision).
- `GunBrokenMarker.clearBroken(item)` clears any prior broken flag.
- Assembly **preview** (`gui=true`) does **not** stamp provenance. Preview **and** craft stamp majority tier lore (`Tier II`) plus `gg_majority_tier` / `gg_tier_lore_start`.

Revisions come from `GunPart.getRevision()` assigned by `RevisionTracker` on load.

## Implemented (Batch 0 + 1)

### RevisionTracker

- File: `data/revisions.json` with `parts` section
- On `PartLoader.load()`: `GunPart.buildRevisionContent()` hashed -> `RevisionTracker.resolvePart(id, hash)`
- Loaded on enable, flushed on disable and `/gg reload`
- Revisions auto-bump when gameplay yaml content changes (stats, cost, calibers, sounds, skin impacts, `tier`, etc.)
- **Not** bumped by: part `name`, `lore`, `permissions`, or `disabled`. `tier` **does** bump revision; lore-only flavor edits still do not.

### GunCraftProvenance

- `guns/data/GGCraftPart.java` - stamped part id + revision (`id`, `r` in JSON)
- `guns/data/GunCraftProvenance.java` - read/write PDC, `resolveStampedParts()`, `isOutdated()`, `syncRevisions()`
- PDC keys via `utils/GGCraftKeys.java`: `gg_craft_parts`, `gg_parts_revision`, `gg_broken`, `gg_majority_tier`, `gg_tier_lore_start`
- Stamped on craft in `InventoryManager.createOutputItem` when `!gui`

Crafted guns use these runtime keys plus provenance when crafted:

| PDC key | Purpose |
|---------|---------|
| `skin_id` | Resolved skin |
| `gun_id` | Instance UUID |
| `gun_type` | Rifle / pistol / etc. |
| `accuracy_salt` | Accuracy RNG |
| `calibers` | Ammo keys |
| `shoot_sounds` / `reload_sounds` | Serialized sounds |
| `stat_value_*` / `stat_index_*` | Aggregated stat totals + lore indices |
| `gg_craft_parts` | Stamped part list (craft only) |
| `gg_parts_revision` | Max stamped part revision |
| `gg_majority_tier` | Majority part tier (preview and craft) |
| `gg_tier_lore_start` | Lore index of the `Tier II` line |
| `bullets_loaded` / `ammo_loaded` | Runtime ammo (preserved on refresh) |
| `reload_ammo` / `reload_amount` | Mid-reload cancel state |
| `last_fire` | Last fire timestamp |

### Disabled parts (new crafts only)

In `parts.yml`:

```yaml
old_barrel:
  disabled: true   # hidden from assembly picker; cannot craft new guns with this part
  name: ...
```

- Disabled parts are excluded from part selection and assembly preview.
- Craft is blocked with: `§cOne or more parts are no longer available for crafting.`
- **Existing guns** with stamped disabled parts still resolve them via `PartLoader` (when part remains in yaml).

**Policy:** never delete part ids from yaml. Use `disabled: true` instead.

### Deleted parts (broken guns)

If a gun has `gg_craft_parts` (Batch 2+) and a stamped id no longer exists in `parts.yml`:

- PDC `gg_broken = true`
- Display name: `§c§lBROKEN`
- Lore line: `§7Missing parts: id1, id2`
- Console warning with `gun_id` and holder name
- Chat warning once per `gun_id` per server session
- Gun cannot fire or reload (`GunManager.blockIfBroken`)

Wired on gun use and hotbar switch (next tick).

## PDC on craft (Batch 2)

| Key | Type | Content |
|-----|------|---------|
| `gg_craft_parts` | STRING (GSON) | List of `{ "id": "barrel_steel", "r": 3 }` |
| `gg_parts_revision` | INTEGER | Max revision across stamped parts |

```json
[
  { "id": "action_bolt", "r": 2 },
  { "id": "barrel_long", "r": 5 }
]
```

### Why not only `skin_id`?

`SkinResolver` picks a skin from weighted votes across parts. Multiple part combinations can share a skin. Recycling must use **actual part costs** from `GunPart.getCost()`, not skin heuristics.

## Remaining batches

### GG-1d - Recycler integration

- [x] Enable `recycler` `GunsAndGadgetsProvider` (sums live `GunPart.getCost()` from stamped ids)

## Source files

| File | Role |
|------|------|
| `utils/GGCraftKeys.java` | PDC key helpers |
| `utils/RevisionTracker.java` | Hash-based revision persistence |
| `utils/GunBrokenMarker.java` | BROKEN state + notifications |
| `utils/GunStatRefresher.java` | Stat refresh + `RefreshResult` |
| `guns/data/GGCraftPart.java` | Stamped part record |
| `guns/data/GunCraftProvenance.java` | Provenance read/write/resolve |
| `guns/parts/GunPart.java` | `revision`, `disabled`, `buildRevisionContent()` |
| `loader/PartLoader.java` | Revision resolve on load |
| `loader/ConfigLoader.java` | `stat_refresh_debug` |
| `GunsAndGadgets.java` | Tracker load/flush, listener registration |
| `manager/inventory/InventoryManager.java` | Craft stamp, `rebuildFromParts`, runtime PDC copy |
| `manager/CraftingManager.java` | Disabled craft block |
| `manager/GunManager.java` | Broken gun block on use/hold |
| `manager/GunRefreshListener.java` | Lazy outdated refresh |
| `manager/GgCommand.java` | `/gg reload`, `/gg refresh` |

## Testing checklist

1. Change part **stat** in yaml, `/gg reload` - revision increments in `data/revisions.json`.
2. Change part **lore** only - revision should **not** change; gun should **not** lazy-refresh.
3. Set `disabled: true` on a part - hidden from picker, craft blocked.
4. Craft gun - verify `gg_craft_parts` PDC lists part ids and revisions.
5. Preview slot before craft - no `gg_craft_parts` on preview item.
6. Remove part from yaml on stamped gun - BROKEN name, chat warning, cannot fire.
7. After stat yaml change + reload, hold gun or `/gg refresh` - stat lore updates; `gun_id` unchanged.
8. Load ammo, change yaml, refresh - `bullets_loaded` / `ammo_loaded` preserved.

## Related docs

- `recycler/docs/IMPLEMENTATION_BATCHES.md` - Batch 5 blocked on GG provenance
- `recycler/docs/SYSTEM.md` - GG provider section
