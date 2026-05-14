# MarisShop

MarisShop is a configurable GUI shop plugin with category-based item files.

## What It Handles

- GUI shop browsing
- Category-driven item layout
- Reload support for shop configuration
- Sound configuration for shop actions

## Requirements

- Paper / Folia 1.21+
- Java 21

## Installation

1. Place the plugin jar in `plugins`.
2. Start the server once.
3. Review `config.yml`, category files, and `sounds.yml`.
4. Restart the server.

## Commands

- `/shop` - Open the shop.
- `/shopreload` - Reload shop files.
- `/marisshop reload` - Reload shop files.

## Files

- `config.yml` - Main settings.
- `sounds.yml` - Sound configuration.
- Category YAML files - Shop pages and item layout.

## Notes

- Keep category files organized by purpose to simplify maintenance.
- Test item pricing and permissions after every major content update.