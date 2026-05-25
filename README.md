# BuddyDash

**A fast, polished mobile companion for Bambuddy and your Bambu printers.**

BuddyDash is an unofficial Android app built on top of Bambuddy, designed to make managing your printers feel faster, cleaner, and more mobile-friendly.

Think of BuddyDash as:

> a better mobile dashboard for Bambuddy

not a replacement for it.

BuddyDash focuses on fast status checks, printer management, NFC quick actions, smart outlet control, and a premium mobile experience for people running one printer or an entire wall of them.

---

## What is BuddyDash?

BuddyDash connects to your existing Bambuddy server and gives you a cleaner, faster mobile interface for everyday printer management.

It is intentionally focused on:

* Fast at-a-glance status
* Cleaner printer management
* Multi-printer usability
* Fold/tablet-friendly layouts
* NFC-powered quick actions
* Low-friction mobile workflows

BuddyDash does **not** aim to replace Bambuddy or expose every feature.

Instead, it focuses on the features that make sense in a polished mobile experience.

---

## Important: Bambuddy Required

BuddyDash is **not standalone**.

You must already have:

* A running Bambuddy server
* Bambu printers configured in Bambuddy

BuddyDash connects to your Bambuddy instance and uses its API.

If you are not already using Bambuddy, start there first.

*https://github.com/maziggy/bambuddy*

---

## Features

### Multi-printer Home dashboard

Quickly scan printer status at a glance.

* Printer status
* Plate clear state
* HMS indicators
* Smart outlet status
* Current print thumbnails
* Maintenance visibility
* Configurable Home card views

Designed to feel fast and glanceable, especially for multiple printers.

---

### Printer details

Quick access to useful printer information without digging through menus.

* Printer status
* Machine information
* AMS overview
* HMS handling
* Maintenance tracking
* Smart outlet controls

---

### NFC quick actions

Stick NFC tags directly on your printers and trigger actions instantly.

Examples:

* Clear plate
* Toggle printer power (safe checks included)
* Finish workflow (clear plate + safe power off)

Tap a printer, get a haptic + toast, and move on.

No app navigation required.

No secrets stored on NFC tags.

BuddyDash securely uses your saved Bambuddy connection.

---

### Smart outlet integration

Control compatible printer outlets directly from BuddyDash.

* Power state
* Safe idle checks
* Quick actions
* NFC integration

BuddyDash prevents unsafe power-off actions while printers are active.

---

### Fold & tablet friendly

Designed to scale cleanly across:

* Phones
* Foldables
* Tablets
* Multi-printer setups

---

### Archives & print history

Browse print history with a cleaner mobile experience.

* Print thumbnails
* Search
* Filtering
* Compact browsing

---

## Why BuddyDash?

Most printer dashboards either feel overly technical, cluttered, or desktop-first.

BuddyDash was built around a simple idea:

> managing printers on mobile should feel fast and enjoyable.

The goal is simple:

Less friction.
Less menu diving.
More glanceability.

BuddyDash focuses on the things you actually do from your phone every day.

---

## Installation

### Beta APK

Download the latest APK from Releases.

1. Install APK
2. Open BuddyDash
3. Add your Bambuddy server URL + API key
4. Printers populate automatically

---

## Setup

In Settings configure:

* Bambuddy server URL
* API key
* Camera / cover token (optional)

Example:

`http://192.168.x.x:8000`

BuddyDash will connect to your Bambuddy instance and automatically load printers.

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
❌ Enterprise-style dashboards
❌ Giant settings menus
❌ UI clutter

---

## Roadmap

Planned / ongoing improvements:

* Home dashboard polish
* More NFC workflows
* Better fold/tablet experiences
* QoL improvements
* Smart outlet enhancements
* UI polish

---

## Contributing

Bug reports, ideas, and feedback are welcome.

BuddyDash is still evolving quickly and polish matters.

If something feels clunky, confusing, or ugly:

Open an issue.

---

## Disclaimer

BuddyDash is an unofficial community project and is not affiliated with Bambu Lab or Bambuddy.

Bambu Lab, Bambu printers, and related trademarks belong to their respective owners.
