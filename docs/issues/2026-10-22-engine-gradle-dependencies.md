---
status: open
verified: yes
area: build
gameplay-change: no
found-by: mega-refactor review (2026-10-22)
related-items: [032, 002]
---

# `engine` pins `gdx-freetype:1.11.0` against `gdxVersion` 1.14.2 and pulls the unused `gdx-tools:1.6.3`

## Where

`engine/build.gradle:8-9`

## What happens

Two LibGDX versions on the classpath, and an old desktop-only tools library that no code imports.

## What should happen

Use `$gdxVersion` for `gdx-freetype`, and remove `gdx-tools`.

## How to reproduce

Read `engine/build.gradle`.

## Short contract

Fixed in branch `fix/engine-small-bugs-2`. The short contract is in `2026-10-22-cullables-system-early-return.md`.
