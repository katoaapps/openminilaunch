# Embedded Sable Games

OpenMink embeds the reusable modules from [Sable Games](https://github.com/aimindseye/sablegames):

- `sable-design`
- `sable-games-runtime`
- `sable-games-console`

The source is Apache-2.0. See `LICENSE`, `NOTICE`, and `THIRD_PARTY_NOTICES.md` in this directory.

## OpenMink adaptations

OpenMink changed the Gradle module names and build files to match its dependency catalog and Android configuration. The reusable source remains aligned with the canonical `design`, `games-runtime`, and `games-console` source trees. OpenMink adds its own launcher adapter, selection keys, discovery UI, and Fun page outside those vendored modules.

OpenMink configures the console as a closed bundled library. The standalone Sable Games Console can
load Lua games from a user-selected folder; that capability is disabled in the embedded launcher.

The exact imported source state is recorded in `/sablegames-source.properties`. Until the upstream module refactor is committed, that file deliberately identifies a working-tree refactor rather than claiming an immutable commit that does not yet exist.
