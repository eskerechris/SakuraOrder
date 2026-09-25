# 🌸 SakuraOrder

A player-driven item order marketplace plugin for Paper & Folia.

Players post buy orders for items, other players fulfill them through a GUI-driven
marketplace flow

## Features

- GUI-driven marketplace: create, browse, and fulfill orders entirely through menus
- Search, filter, and sort in the order browser (`/order <search>` opens it pre-filtered)
- Vault economy integration
- Player order history
- SQLite or MySQL/MariaDB storage
- Folia-aware scheduling (region-based task handling, no cross-region violations)
- Configurable per-player active order limits via permissions
- Temporary maintenance lock for the order system

## Requirements

- Java 21
- Paper or Folia 1.21+
- Vault

## Installation

1. Download the jar from [Spigot](https://www.spigotmc.org/resources/sakuraorder-%E2%9A%9C%EF%B8%8F-player-driven-orders-folia-support.139095/) or the [Releases](https://github.com/eskerechris/SakuraOrder/releases) page
2. Drop it into your server's `plugins/` folder
3. Restart the server
4. Configure `config.yml` and the language files under `lang/` to taste

## Commands

| Command | Description |
|---|---|
| `/order` | Opens the marketplace menu |
| `/order <search>` | Opens the marketplace menu pre-filtered by search |
| `/order create` | Starts the order creation flow |
| `/order history` | Shows players order history |
| `/order admin` | Open the admin GUI |
| `/order reload` | Reloads the plugin configuration |
| `/order lock` | Temporarily locks the order system for maintenance |

## Permissions

| Permission | Description |
|---|---|
| `sakuraorder.limit.<limit>` | Maximum number of active orders a player can have at once (e.g. `sakuraorder.limit.5`) |
| `sakuraorder.command.admin` | Access to the admin GUI |
| `sakuraorder.command.remove` | Cancel other players' orders from the menu |
| `sakuraorder.command.reload` | Reload the plugin configuration |
| `sakuraorder.command.lock` | Lock/unlock the order system for maintenance |

## Configuration

- `config.yml` -> general settings (grace period days, storage)
- `lang/it_IT.yml` -> chat messages
- `lang/menu/it_IT.yml` -> GUI text

## Building from source

Gradle multi-module project (`:api` + `:core`), Java 21 toolchain.

```
./gradlew shadowJar
```

## License

This project is licensed under the [MIT License](https://github.com/eskerechris/SakuraOrder/blob/main/LICENSE)
