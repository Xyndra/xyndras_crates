# Xyndra's Crates

This is a MC mod based on [Pebble's Lootcrate](https://github.com/navneetset/pebbles-crate).

Note that it requires Kotlin for Forge to run.

## Permissions
For all admin commands, if you have LuckPerm installed, the following is required:<br>
`xyndra.admin.crate`

## Available Commands
`/crate_admin crate` Displays all available crate in the config and lets you grab a crate transformer/crate key <br>
`/crate_admin getcrate <name>` Get a crate transformer <br>
`/crate_admin givekey <player> <amount> <cratename>` Gives cratekey to a specific player <br>

## How to use?
After installing the mod, you may navigate to your `/config/xyndras-crate/crates`then create a file, for example `example.json`
<br>
You may use the follow example as reference for the structure: <br>

```
{
  "crateName": "Vanilla Items Crate",
  "crateKey": {
    "material": "minecraft:tripwire_hook",
    "name": "&#FFBF00Vanilla Key",
    "lore": [
      "&#FFDC73• Opens a Vanilla Items Crate"
    ]
  },
  "prize": [
    {
      "name": "&#63BC5DDiamond",
      "material": "minecraft:diamond",
      "amount": 1,
	  "nbt": "{species:\"cobblemon:bulbasaur\",aspects:[\"shiny\"]}",
      "commands": [
        "give {player_name} minecraft:diamond 1"
      ],
      "broadcast": "&6{player_name} &fhas received {prize_name} &ffrom {crate_name}",
      "messageToOpener": "&6[Xyndra's Crates] &f&lYou got {prize_name} &f&lfrom Vanilla Items Crate",
      "lore": [
        "Chance of getting the drop: {chance}%"
      ],
      "chance": 40
    },
    {
      "name": "&rElytra",
      "material": "minecraft:elytra",
      "amount": 1,
      "commands": [
        "give {player_name} minecraft:elytra 1"
      ],
      "broadcast": "&6{player_name} &fhas received {prize_name} &ffrom {crate_name}",
      "messageToOpener": "&6[Xyndra's Crates] &f&lYou got {prize_name} &f&lfrom Vanilla Items Crate",
      "lore": [
        "Chance of getting the drop: {chance}%"
      ],
      "chance": 5
    },
    {
      "name": "&rGolden Apple",
      "material": "minecraft:golden_apple",
      "amount": 1,
      "commands": [
        "give {player_name} minecraft:golden_apple 1"
      ],
      "broadcast": "&6{player_name} &fhas received {prize_name} &ffrom {crate_name}",
      "messageToOpener": "&6[Xyndra's Crates] &f&lYou got {prize_name} &f&lfrom Vanilla Items Crate",
      "lore": [
        "Chance of getting the drop: {chance}%"
      ],
      "chance": 55
    }
  ]
}
```
Note that the mod supports legacy formatting (e.g. &#63BC5D, &4, &f, &r) as demonstrated in the example.