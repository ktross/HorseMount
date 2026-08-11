# HorseMount

HorseMount is a Minecraft Java server plugin that lets players choose, summon, ride, and dismiss personal horse-family mounts. It is a modern continuation of the original Bukkit plugin, now using the current entity API and limiting cleanup and inventory rules to mounts created by HorseMount.

## Compatibility

| Component | Supported version |
| --- | --- |
| Minecraft | 26.2 |
| Servers | Paper, Spigot, and CraftBukkit 26.2 |
| Recommended server | Paper 26.2, stable build 111 |
| Java | 25 |
| HorseMount | 2.x |

HorseMount ships one Bukkit-format JAR for all three supported servers. Paper is recommended and powers the included local development server, but it is not required. CI compile-checks the shared source against both the Spigot and Paper APIs, then manually dispatched and release-gated smoke tests boot the same built JAR on Paper, Spigot, and CraftBukkit.

Support is scoped to the current Minecraft release. The JAR declares `api-version: '26.2'`, so older servers will refuse to load it; newer Minecraft versions are not supported until the compatibility checks move forward. Paper-compatible forks such as Purpur may work but are not tested. HorseMount does not declare Folia support.

## Install

1. Install [Paper 26.2](https://docs.papermc.io/paper/getting-started/) (recommended), or use the latest [Spigot BuildTools](https://www.spigotmc.org/wiki/buildtools/) with `--rev 26.2` to build Spigot; add `--compile craftbukkit` to build CraftBukkit instead. Run the server with Java 25.
2. Download the HorseMount JAR from [GitHub Releases](https://github.com/ktross/horsemount/releases).
3. Put the JAR in the server's `plugins` directory and restart the server.
4. Grant the desired `horsemount.*` permissions with your permissions plugin.

HorseMount creates `plugins/HorseMount/config.yml` on first start.

## Commands

| Command | Purpose | Permission |
| --- | --- | --- |
| `/horsemount` or `/hm` | Show help | `horsemount.help` |
| `/hm reload` | Reload configuration | `horsemount.reload` |
| `/mount` or `/mnt` | Summon the selected mount, or dismiss the current HorseMount mount | `horsemount.mount` |
| `/dismount` | Dismiss the current HorseMount mount | `horsemount.dismount` |
| `/setmount <variant> [style] [color]` | Select the player's default mount | `horsemount.setmount` |
| `/setarmor <none\|iron\|gold\|diamond>` | Select armor for a regular horse | `horsemount.setarmor` |
| `/showmount` | Show the current selection | `horsemount.showmount` |
| `/spawnmount <variant> [style] [color]` | Create a protected display mount | `horsemount.spawnmount` |

`/hm reload` and help may be run from the console. Commands that act on a player or entity must be run by a player.

### Mount values

- Variants: `horse`, `donkey`, `mule`, `skeleton`, `zombie` (`undead` is accepted as a legacy alias for `zombie`)
- Horse styles: `default`, `white`, `whitefield`, `whitedots`, `blackdots`
- Horse colors: `white`, `creamy`, `chestnut`, `brown`, `black`, `gray`, `darkbrown`
- Horse armor: `none`, `iron`, `gold`, `diamond`

Styles, colors, and armor apply only to the regular `horse` variant. Each selectable value has a matching permission such as `horsemount.variant.donkey`, `horsemount.style.whitefield`, `horsemount.color.chestnut`, or `horsemount.armor.diamond`. `horsemount.*` grants every HorseMount permission and is disabled by default.

## Signs

A player with `horsemount.signs.create` can create selection signs. A player with `horsemount.signs.use` can use them.

Regular horse sign:

```text
[HorseMount]
horse
whitefield
chestnut
```

Other variant sign:

```text
[HorseMount]
donkey


```

Armor sign:

```text
[HorseMount]
diamond


```

`[HM]` is also accepted when creating a sign. HorseMount normalizes a valid sign's heading and values.

## Configuration

```yaml
disable-spawning: true
disable-item-drops: true
players:
  default:
    variant: horse
    style: default
    color: white
    armor: none
```

- `disable-spawning` prevents natural horse-family spawns. HorseMount's custom spawns are still allowed.
- `disable-item-drops` suppresses drops and experience from HorseMount-created mounts. It does not alter ordinary horses.
- `players.default` is the fallback profile. Per-player selections are stored below `players.<uuid>`.

Incomplete player profiles inherit valid default values. Invalid profiles are rejected with a clear message, and an invalid server default falls back to `horse/default/white/none`. `/hm reload` refreshes the runtime settings.

### Upgrading from the original plugin

Back up the old `plugins/HorseMount` directory before upgrading. HorseMount 2.x reads the existing setting names and UUID-based player sections, but it deliberately does not preserve live entities created by the old plugin. Very old releases stored player names rather than UUIDs; those sections must be moved manually to `players.<uuid>` if they still matter.

The 2.x safety model is intentionally narrower: only entities tagged as HorseMount-managed are removed on dismount, protected from inventory changes, or stripped of drops. Ordinary and other plugins' horses are left alone.

## Build

The repository includes a Gradle Wrapper and automatically provisions the Java 25 compilation toolchain when necessary:

```bash
./gradlew clean build
```

The versioned plugin JAR is written to `build/libs/`. A byte-identical copy with the stable name `HorseMount.jar` is staged in `build/docker/plugins/` for the local Docker server. Server APIs are compile-only dependencies and are not bundled into the plugin.

## Local Paper server

The included Docker Compose stack runs the recommended local target: Paper 26.2 build 111 on Java 25. Server data is retained in the ignored `.server/` directory, and port 25565 is bound only to localhost. Spigot and CraftBukkit coverage lives in CI; the local stack intentionally remains Paper-only.

First, create the local environment file. Review the linked Minecraft EULA, set `MINECRAFT_EULA=TRUE` only if you accept it, and replace the operator name:

```bash
cp .env.example .env
```

Build HorseMount, then start Paper with Compose Watch:

```bash
./gradlew clean build
docker compose up --watch
```

Connect a Minecraft Java Edition 26.2 client to `127.0.0.1`. Operators listed in `MINECRAFT_OPS` receive all HorseMount permissions on this local development server.

While Compose Watch is running, rebuild without `clean` after code changes:

```bash
./gradlew build
```

Gradle replaces `build/docker/plugins/HorseMount.jar`; Compose detects the verified artifact, restarts Paper gracefully, and the container synchronizes the new JAR into its writable plugin directory. For a fully automatic edit-build-restart loop, run `./gradlew build --continuous` in a second terminal.

Use `docker compose stop` to stop the server without deleting its world, or `docker compose down` to remove the container and network while retaining `.server/data`. Do not use Minecraft's `/reload` command to load a new plugin JAR.

## CI and releases

For pull requests and pushes, GitHub Actions runs the unit tests, compiles the shared source against the Paper and Spigot 26.2 APIs, and packages one platform-neutral JAR. These fast checks do not start a Minecraft server.

Manually dispatched and release-gated compatibility jobs then boot that exact JAR on Paper, Spigot, and CraftBukkit 26.2 and verify that HorseMount enables cleanly. These are startup compatibility checks rather than exhaustive gameplay tests. This project does not redistribute Spigot or CraftBukkit server JARs, so those lanes build them with the official BuildTools and do not cache or publish the resulting server artifacts; keeping the slower server builds out of routine pull-request checks makes that constraint manageable.

The runtime workflow requires a repository Actions variable named `MINECRAFT_EULA` with the value `TRUE`. Set it only after reviewing and accepting the [Minecraft EULA](https://www.minecraft.net/eula); without that explicit confirmation, the server jobs stop before launch.

Pushing a semantic-version tag creates a GitHub Release:

```bash
git tag v2.0.0
git push origin v2.0.0
```

The release workflow validates the tag, rebuilds and tests the tagged commit, requires the three-server compatibility smoke test, creates the versioned JAR and SHA-256 checksum, and publishes both with build provenance. Dependency updates for Gradle and GitHub Actions are proposed by Dependabot.

## Historical page

The original BukkitDev listing is archived at [dev.bukkit.org/projects/horsemount](https://dev.bukkit.org/projects/horsemount).

## License

HorseMount is licensed under the terms in [LICENSE](LICENSE).
