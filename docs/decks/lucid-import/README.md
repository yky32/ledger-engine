# Import into Lucidchart

Lucid does **not** use a public offline `.lucid` file format we can generate without your Lucid account API.  
This folder is the engineering-ready package for Lucid.

## Files

| File | Use |
|------|-----|
| `../UAF-Rewards-Target-Architecture.drawio` | **Best** — Lucid can import draw.io |
| `01-target-architecture.svg` | Page 1 — paste / import as image, then ungroup if needed |
| `02-hard-boundary-rs-cbp.svg` | Page 2 — boundary (Redeem/Confirm · CBP) |
| `UAF-Rewards-Target-Architecture-Lucid-Import.zip` | Bundle of the above |

## Method A — Import draw.io (recommended)

1. Open [lucid.app](https://lucid.app) → **Lucidchart** → **+ New**
2. **File → Import data** / **Import** → choose **draw.io** / **diagrams.net** if listed  
   *or* drag `UAF-Rewards-Target-Architecture.drawio` into Lucid
3. You get **editable shapes** (two pages)

If Import draw.io is not on your plan:

## Method B — SVG → Lucid (always works)

1. New Lucidchart blank document  
2. **Insert → Image** → upload `01-target-architecture.svg`  
3. New page → upload `02-hard-boundary-rs-cbp.svg`  
4. Optional: **right-click image → Trace** / redraw key boxes if you need native edit

## Method C — diagrams.net first, then Lucid

1. Open the `.drawio` in [app.diagrams.net](https://app.diagrams.net)  
2. **File → Export as → PNG** (300 dpi) or **SVG**  
3. Insert into Lucid

## Content (no product vendor name)

- **Page 1:** A–E target architecture (Channels · Integration · **Reward System** · Add-ons · Fulfillment)  
- **Page 2:** Hard boundary RS | CBP · **Redeem / Confirm** only · CoD · INV · SOT · Analytics  

## Note

Native Lucid cloud docs require login + optional REST API (`LUCID_API_KEY`).  
If you add a Lucid API token later, we can push a live Lucid document via API.
