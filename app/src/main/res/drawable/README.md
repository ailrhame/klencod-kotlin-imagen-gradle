# Pollimagen — App Icon Assets

Icon concept: a bloom, straight from "Pollinations" itself rather than the
usual AI-app sparkle/magic-wand cliché. Six petals sweep through a
magenta-to-violet gradient (the generative, ever-different output), gathered
around a glowing pollen-gold center — doubling as the "generate" button
this whole app revolves around. Deep plum background keeps the bloom
glowing without competing with it.

## What's in here

```
app/src/main/res/
  mipmap-mdpi/        ic_launcher.png (48px)   ic_launcher_round.png
  mipmap-hdpi/          "        "    (72px)     "         "
  mipmap-xhdpi/         "        "    (96px)     "         "
  mipmap-xxhdpi/        "        "    (144px)    "         "
  mipmap-xxxhdpi/       "        "    (192px)    "         "
                       ic_launcher_foreground.png (432px, adaptive icon layer)
  mipmap-anydpi-v26/  ic_launcher.xml, ic_launcher_round.xml   (adaptive icon wiring, API 26+)
  values/             ic_launcher_background.xml               (background color: #1B1029)

play_store_icon_512.png — flat 512×512, no transparency, for the Play Console listing
icon_preview.png        — 1024×1024 preview, not meant to ship in the app
pollimagen-icon.svg     — full vector source (background + bloom), editable
```

## How to install it (CodeAssist)

Same flow as your other apps: CodeAssist can't import image files from
inside its own UI, but it exposes the project through Android's Storage
Access Framework, so your phone's Files app can reach it directly.

1. Extract the zip on your phone.
2. Open Files (not CodeAssist) — look for "CodeAssist" listed as a
   location in the sidebar, then browse to Pollimagen's
   `app/src/main/res/`.
3. For each of `mipmap-mdpi/hdpi/xhdpi/xxhdpi/xxxhdpi`, copy in
   `ic_launcher.png` and `ic_launcher_round.png`, overwriting the existing
   files of the same name.
4. In `mipmap-xxxhdpi` only, also add `ic_launcher_foreground.png` (a new
   file, not a replacement).
5. In `mipmap-anydpi-v26`, replace `ic_launcher.xml` and
   `ic_launcher_round.xml` with these two.
6. Drop `ic_launcher_background.xml` into `values/`, or just add its one
   `<color>` line into an existing colors file by hand.
7. Leave everything else in `res/` untouched.

The `.svg` is the editable vector source if you want to shift the palette
later — petals sweep from `#F0429A` (magenta) to `#5B3FD6` (violet), pollen
center is `#FFC94D`.
