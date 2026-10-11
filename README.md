# EoSD Mobile

[![Android](https://github.com/Brenninho123/th06-mobile/actions/workflows/android.yml/badge.svg)](https://github.com/Brenninho123/th06-mobile/actions/workflows/android.yml)

A mobile port of 東方紅魔郷 ～ the Embodiment of Scarlet Devil (1.02h), built on top of the
[portable branch](https://github.com/GensokyoClub/th06/tree/portable) of the GensokyoClub decompilation.

No game data is included. You need your own copy of Touhou 6 version 1.02h.

## Android

### Installing

1. Download `th06-mobile.apk` from the latest [Actions run](https://github.com/Brenninho123/th06-mobile/actions/workflows/android.yml) or [release](https://github.com/Brenninho123/th06-mobile/releases) and install it.
2. Copy your game folder to the phone (the folder with `紅魔郷CM.DAT`, `紅魔郷ED.DAT`, `紅魔郷IN.DAT`, `紅魔郷MD.DAT`, `紅魔郷ST.DAT`, `紅魔郷TL.DAT` and the `bgm` folder).
3. Open the app, tap **Select game folder** and pick that folder. The files are copied into the app once.

DAT files whose Japanese names were garbled by a zip tool are detected by their suffix (`CM.DAT`, `ST.DAT`, ...)
and renamed automatically. `msgothic.ttc` is used if present, otherwise a bundled Noto Sans JP is used.
Music requires the `bgm/*.wav` files, since Android has no MIDI output.

To import a different copy later, long-press the app icon and choose **Import game files**.

### Controls

| Touch | Action |
| --- | --- |
| Left half of the screen | Floating joystick (8 directions) |
| `Z` | Shoot / confirm |
| `X` | Bomb / cancel |
| `⇧` | Focus (slide between `Z` and `⇧` to shoot while focused) |
| `II` / Android back | Pause / menu |
| `»` | Skip dialogue |

Gamepads and keyboards work too; the touch overlay hides itself when one is used and comes back on the next touch.

### Building

```
git clone --recursive https://github.com/Brenninho123/th06-mobile.git
cd th06-mobile/android
./gradlew assembleRelease
```

Requirements: JDK 17, Android SDK 35, NDK 27.2.12479018 and CMake 3.22.1 (all installable through the SDK manager).
The APK is written to `android/app/build/outputs/apk/release/`.

To sign release builds, set `TH06_KEYSTORE_PATH`, `TH06_KEYSTORE_PASSWORD`, `TH06_KEY_ALIAS` and `TH06_KEY_PASSWORD`.
In GitHub Actions the same values come from the `TH06_KEYSTORE_BASE64`, `TH06_KEYSTORE_PASSWORD`, `TH06_KEY_ALIAS`
and `TH06_KEY_PASSWORD` secrets; without them the APK is signed with the debug key.

### How the port works

- `CMakeLists.txt` builds the game as `libmain.so` together with SDL2, SDL2_image and SDL2_ttf from the submodules in `android/deps`.
- The renderer reads the real drawable size at runtime and letterboxes the 640x480 game to 4:3 at native resolution.
- The game runs from the app's internal storage, where saves, replays and `score.dat` are written.
- `android/app` contains the launcher/importer activity, the SDL activity and the touch overlay, which sends its button state to the game through JNI.

## Desktop (portable build)

This is the readme for the portable fork of EoSD. For the readme of the decomp project, see [here](https://github.com/GensokyoClub/th06/blob/master/README.md).

EoSD-portable is a port of Touhou 6 using SDL2 and OpenGL (with a more general renderer abstraction layer hopefully on the way).
This enables theoretical portability to any system supported by SDL2, with Linux, Windows, and macOS in particular being known to work.
Builds for the BSDs and other Unices are also almost certainly possible, but may require some slight modifications to the build system.

### Platform Requirements

- SDL2, SDL2-image, and SDL2-ttf support
- C++20 standard library support
- A little endian architecture (though big endian support is currently being worked on)
- OpenGL ES 1.1, OpenGL 1.3, or GL 2.1 / GL ES 2.0 / WebGL support

### Dependencies

EoSD-portable has the following dependencies:

- `SDL2`
- `SDL2_image`
- `SDL2_ttf`
- `libasound` (Optional and Linux-only, enables MIDI support. This will almost always be present as part of a desktop distro.)

On Windows and macOS, MIDI support uses the system APIs and needs no extra dependencies.

In addition, building uses [`premake5`](https://premake.github.io/download) and a compiler that supports C++20.

#### Building

In the repository root directory, run `premake5` with the desired build system as an argument (a list can be seen by running `premake5 --help`).
This will output the build files to the `build` directory, and then compilation may be done with the desired build system.

##### Build Options (Use with Premake Invocation)
`--no-asoundlib`: On Linux, doesn't build MIDI support. Removes libasound as a dev and runtime dependency
`--use-c23-embed`: Uses `#embed` for resource inclusion instead of a lua script in the Premake file.

##### Build Example (Debian-based Linux)

Obtain dependencies:

`sudo apt install build-essential libsdl2-dev libsdl2-image-dev libsdl2-ttf-dev libasound2-dev`

Generate makefile:

`premake5 gmake`

Compile:

`cd build && make -j16`

##### Build Example (macOS)

Install the Xcode Command Line Tools (provides the compiler), if not already present:

`xcode-select --install`

Obtain dependencies (using [Homebrew](https://brew.sh)):

`brew install premake sdl2 sdl2_image sdl2_ttf`

Generate makefile:

`premake5 gmake`

Compile:

`cd build && make -j16`

### Use

EoSD-portable is designed to be a drop-in replacement for the vanilla EoSD binary.
You will also need to add a font to your game directory with the filename `msgothic.ttc`.
This may be the actual MS Gothic, taken from a Windows machine, or a compatible font such as Kochi Gothic.
EoSD-portable uses the Japanese filenames (e.g. 紅魔郷CM.DAT, 東方紅魔郷.cfg). English and other patches, static or thcrap, do not currently work.
A Japanese locale is not required.

# Decomp Credits

We would like to extend our thanks to the following individuals for their
invaluable contributions:

- @EstexNT for porting the [`var_order` pragma](scripts/pragma_var_order.cpp) to
  MSVC7.
