# BuddyDash

**Fast, polished mobile companion for Bambuddy.**

BuddyDash is an unofficial Android companion app for Bambuddy, built to make managing your Bambu printers feel faster, cleaner, and more mobile-friendly.

Think of BuddyDash as:

> a better mobile dashboard for Bambuddy

not a replacement for it.

Whether you run a single printer or a small farm, BuddyDash focuses on fast status checks, cleaner printer management, NFC quick actions, smart outlet control, and a premium mobile experience.

---

## Screenshots

> Coming soon

*Add screenshots of Home, printer details, NFC workflows, archives, fold/tablet layouts, etc.*

---

## What is BuddyDash?

BuddyDash connects to your existing Bambuddy server and gives you a fast, mobile-first experience for everyday printer management.

It is intentionally focused on:

* 🚀 Fast at-a-glance status
* 🖨️ Multi-printer usability
* 📱 Fold & tablet-friendly layouts
* 🏷️ NFC-powered quick actions
* ⚡ Smart outlet integration
* ✨ Premium, low-friction UX

BuddyDash intentionally does **not** expose every Bambuddy feature.

Instead, it focuses on the workflows that make sense in a clean mobile experience.

Less menu diving.
Less friction.
More glanceability.

---

## Important: Bambuddy Required

BuddyDash is **not standalone**.

You must already have:

* A running Bambuddy server
* Bambu printers configured in Bambuddy

BuddyDash uses Bambuddy as its backend and connects directly to your existing instance.

If you are not already using Bambuddy, start there first.

https://github.com/maziggy/bambuddy

---

## Features

### 🏠 Multi-printer dashboard

A fast, glanceable Home screen designed for quickly checking printer state.

* Printer status
* Print thumbnails
* HMS indicators
* Maintenance status
* Smart outlet state
* Plate clear state
* Configurable Home card views
* Multi-printer friendly layouts

Designed to feel fast, glanceable, and clean.

---

### 🖨️ Printer details

Quick access to useful printer information without digging through menus.

* Machine information
* AMS overview
* Maintenance tracking
* HMS handling
* Smart outlet controls
* Printer status & metadata

---

### 🏷️ NFC quick actions

Stick NFC tags directly on your printers and trigger actions instantly.

Examples:

* Clear plate
* Toggle printer power (safe idle checks included)
* Finish workflow (clear plate + safe power off)

Tap → toast → haptic → done.

No app navigation required.

No secrets stored on NFC tags.

BuddyDash securely uses your saved Bambuddy connection.

---

### ⚡ Smart outlet integration

Control compatible printer outlets directly from BuddyDash.

* Power state visibility
* Safe idle-state validation
* Quick power actions
* NFC integration

BuddyDash prevents unsafe power-off actions while printers are active.

---

### 📂 Archives & print history

Browse print history in a mobile-friendly way.

* Print thumbnails
* Search
* Filtering
* Cleaner browsing experience

---

### 📱 Fold & tablet optimized

Designed to scale cleanly across:

* Phones
* Foldables
* Tablets
* Multi-printer setups

---

## Home Card Views

Choose the Home experience that fits your setup.

### Minimal

Compact cards optimized for many printers.

### Standard

Balanced BuddyDash experience.

### Detailed

More printer information at a glance.

---

## Privacy & Security

BuddyDash is built for self-hosted workflows.

### No cloud required

BuddyDash connects directly to **your Bambuddy server**.

### No analytics

No analytics SDKs.

### No telemetry

No background usage tracking.

### No crash reporting

No silent uploads or hidden reporting.

### Secure credential storage

BuddyDash stores connection credentials securely on-device.

### NFC safety

NFC tags never store secrets.

Only BuddyDash action links are written to tags.

---

## Installation

### Beta APK

Download the latest APK from Releases.

1. Download APK
2. Install on Android
3. Open BuddyDash
4. Add your Bambuddy server URL + API key
5. Printers populate automatically

---

## Setup

Open **Settings** and configure:

* Bambuddy server URL
* API key
* Camera / cover token (optional)

Example:

`http://192.168.x.x:8000`

BuddyDash will connect to your Bambuddy instance and automatically load printers.

---

## Why BuddyDash?

Most printer dashboards either feel:

* overly technical
* cluttered
* desktop-first
* slow to navigate on mobile

BuddyDash was built around a simple idea:

> printer management should feel fast and enjoyable on mobile.

The goal is simple:

* Less friction
* Less menu diving
* More glanceability
* Faster actions

BuddyDash focuses on the things you actually do from your phone every day.

---

## Project Philosophy

BuddyDash is intentionally opinionated.

We prioritize:

✅ Fast interactions
✅ Clean visuals
✅ Mobile-first UX
✅ Glanceable information
✅ Fold/tablet usability
✅ Premium feel

We intentionally avoid:

❌ Replacing Bambuddy
❌ Feature overload
❌ Enterprise dashboards
❌ Giant settings menus
❌ Workflow-builder complexity
❌ UI clutter

---

## Beta Status

BuddyDash is currently in **public beta**.

Expect:

* rapid iteration
* UI improvements
* occasional bugs
* breaking polish changes

Feedback matters.

If something feels clunky, confusing, or ugly:

Open an issue.

---

## Roadmap

Planned / ongoing improvements:

* Home dashboard polish
* More NFC workflows
* Fold/tablet improvements
* Multi-printer QoL
* Smart outlet improvements
* UI polish

---

## Contributing

Bug reports, ideas, screenshots, and feedback are welcome.

When reporting issues, include:

* Device model
* Android version
* BuddyDash version
* Bambuddy version
* Steps to reproduce

Use **Export diagnostics** in Settings → About when possible.

---

## Disclaimer

BuddyDash is an unofficial community project.

It is **not affiliated with**:

* Bambu Lab
* Bambuddy

Bambu Lab, Bambu printers, and related trademarks belong to their respective owners.
