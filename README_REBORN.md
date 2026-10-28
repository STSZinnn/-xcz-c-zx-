# FrostPVP Reborn — v4.0.0-beta1 (test source)

Target: Paper **1.21.11**, Java **21**. This is an initial major gameplay rework of the uploaded v3.3.1 project, not a clean-room rewrite or a verified production release.

## Changes

- All retained **weapons and utility tools** have a single F-key active (Minecraft Swap Hands key, canceled while a tagged Frost item is held) and a passive. Reborn combat is isolated in `FrostRebornCombat.java`.
- Frozen Sword: **Absolute Zero** (3-stack Permafrost passive; telegraphed ice crescent ultimate, stacking bonus). The former sword dash is disabled.
- 13 other held weapon/tool slots: Frost Spires, Glacier Breaker, Arrowfall, Sonic Frost, Frozen Tether, Thunderfall, Sky Hunters, Crystal Guard, Winter Blessing, Ice Anchor, Call of Winter, and the F-key Duality pickaxe mode switch.
- The **snowball, TNT, wind charge and flint-and-steel** are no longer issued from `/frost`, event rewards or configured from the menu. Old combat activations and old tagged snowball/windcharge launch effects are disabled. Preexisting items are not automatically deleted from players' inventories.
- Chestplate increases MAX_HEALTH attribute by **10 health** (5 hearts) while equipped; bonus is removed on unequip and on plugin disable, without giving free healing on repeated equipping. No longer reflects damage.
- Leggings give Fire Resistance instead of Resistance.
- `FrostTooltips` writes animated-ready `frost:<weapon>` tooltip style IDs automatically, and one active + passive in gradient text.
- Existing trusted player lists, events, PDC marker keys and weapon IDs stay supported. Main older core class still contains inert deprecated code paths; the architectural cleanup is **not complete**.

## Install

1. BACK UP worlds, `plugins/FrostPVP/config.yml` and `trusted.yml`.
2. Upload the project files, including `.github/workflows/build.yml`, to GitHub.
3. Run GitHub Actions `Build Frost Plugin` to compile.
4. Place only the NEW compiled JAR under `/plugins` (remove the older Frost JAR).
5. Put `FrostPVP_Reborn_Animated_Tooltips.zip` into `.minecraft/resourcepacks` and enable it on each client.
6. Restart server and use `/frost` (OP only) to obtain a new weapon or `/frost restyle` to upgrade tooltips on authentic old weapons.
7. Hold a tagged weapon in your main hand and press F to use the active ability.

## Testing priorities

1. Does the GitHub Actions build compile? The Paper API dependency was not available in the authoring environment, so **compilation has not been verified**.
2. Can old and fresh tagged weapons activate their F ability and avoid their old right/left-click ability?
3. Does `/frost resetcooldowns` clear Reborn cooldowns? Does F work after using an inventory item?
4. Does the Chestplate increase max health by 10, stop reflecting, and remove those hearts on unequip?
5. Do the Leggings grant Fire Resistance but no Resistance?
6. Do all six events still work, and can the user select only surviving weapons as event prizes?
7. Does the animated resource pack render and animate nine-slice tooltip styles in Minecraft 1.21.11?

## Important limits

- Old YAML keys remain for backward compatibility, while all Reborn ability cooldowns use `reborn.<id>.cooldown-seconds`. Some old `/frost config` sliders still configure legacy inactive mechanics and can be ignored; a later clean-up would replace them.
- The helmet now has a passive only; the old sneak reveal was disabled to meet the F-only ability direction without conflicting with held weapon F. Armor is wearable equipment rather than a held weapon.
- Some passives are basic first-pass prototypes; ability timings and particles have not been battle-tested.
- The combined model is still based on v3.3.1 source, not a complete from-scratch rewrite.
