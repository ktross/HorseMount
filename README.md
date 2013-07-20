# HorseMount
HorseMount is a flexible player mount system. It allows players to summon and dismiss horse mounts with a simple command. Players can also choose to set their default mount via command or sign from 140 different mount types.

## Features
* Easily summon mounts, tame mounts, attach a saddle, attach armor, and mount the horse with a single command
* Destroy horses on dismount
* Only let players mount horses if they have permission
* Choose from 140 variant, color and armor combinations including Horses, Mules, Donkeys, Skeleton Horses, and Undead Horses
* Permissions for every Horse combination
* Option to prevent horses from spawning naturally
* Option to prevent mounts from dropping items
* Prevent players from using/modifying horse inventories

## Planned Features (in no specific order)
* Transfer configurable percentage of damage from mount to rider
* Configurable damage reduction for each horse armor type
* Configurable chance to dismount when attacked
* Configurable miss chance while mounted
* Option to dismount when player attacks another entity
* Option to prevent item pickups while mounted
* Configurable cooldown/warmup time
* Command blacklist while mounted
* Persistent Horse Chests
* Configurable speed for each horse variant
* Configurable speed reduction for armored horses
* Signs to set default horses
* Economy support

## Commands
* /horsemount, /hm - Plugin help. Currently just directs you to the bukkit page
* /horsemount reload, /hm reload - Reload plugin configuration
* /mount, /mnt - Summon your default mount. This command will also dismount you if you are currently mounted
* /dismount - Dismiss your mount
* /setmount <variant> [style] [color] - Set your default mount. "style" and "color" arguments only apply to the "horse" variant
* /setarmor <iron|gold|diamond> - Only applies to the "horse" variant

## Permissions
**Commands**
* horsemount.help - Use the /horsemount command
* horsemount.reload - Use the /horsemount reload command
* horsemount.dismount - Use the /dismount command
* horsemount.mount - Use the /mount command
* horsemount.setmount - Use the /setmount command
* horsemount.setarmor - Use the /setarmor command

**Variants**
* horsemount.variant.horse
* horsemount.variant.mule
* horsemount.variant.donkey
* horsemount.variant.skeleton
* horsemount.variant.zombie

**Styles**
* horsemount.style.default
* horsemount.style.white
* horsemount.style.whitefield
* horsemount.style.whitedots
* horsemount.style.blackdots

**Colors**
* horsemount.color.white
* horsemount.color.creamy
* horsemount.color.chestnut
* horsemount.color.brown
* horsemount.color.black
* horsemount.color.gray
* horsemount.color.darkbrown

**Armor**
* horsemount.armor.iron
* horsemount.armor.gold
* horsemount.armor.diamond

## Horse Variants
* horse
* skeleton - Does not allow colors
* undead - Does not allow colors
* donkey - Does not allow colors
* mule - Does not allow colors

## Horse Styles
* default
* white
* whitefield
* whitedots
* blackdots

## Horse Colors
* white
* creamy
* chestnut
* brown
* black
* gray
* darkbrown

## Configuration
* disable-spawning: [true|false] - Disable natural horse spawning
* disable-item-drops: [true|false] - Prevent horses from dropping items. Useful if you want to prevent saddle/armor drops from being exploited