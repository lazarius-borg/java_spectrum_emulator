# ZX Spectrum 128K Emulator in Modern Java

[![Java](https://img.shields.io/badge/Java-26-orange.svg)](https://openjdk.org/projects/jdk/26/)
[![JavaFX](https://img.shields.io/badge/JavaFX-26.0.2-blue.svg)](https://openjfx.io/)
[![Maven](https://img.shields.io/badge/Maven-3.9.16-red.svg)](https://maven.apache.org/)
[![Version](https://img.shields.io/badge/Version-0.1.0-green.svg)](https://github.com/lazarius-borg/java_spectrum_emulator/releases)
[![License](https://img.shields.io/badge/License-MIT-lightgrey.svg)](LICENSE)

A high-performance, cycle-accurate software implementation of the **Sinclair ZX Spectrum 128K** written in **Java 26** using **Maven 3.9.16** and **JavaFX**. Features an authentic retro terminal UI, dual sound engine (Beeper + AY-3-8912), cycle-accurate tape emulation with instant ROM trap, interactive rubber keyboard, virtual arcade joystick, and real-time hardware diagnostics.

---

## 📸 Interface Preview

<!-- Screenshot placeholder: Provide or replace screenshot.png in repo -->
```
+-----------------------------------------------------------------------------------------+
| ZX Spectrum 128K Emulator - [ 0.1.0 ]                                          [ - + x ]|
+-----------------------------------------------------------------------------------------+
| File   Machine   View   Input   Tape   Help                                             |
+------------------------------------+----------------------------------------------------+
|                                    | -[ ⚙ Inspector ]-                        [ 128K ]  |
|                                    | [ CPU ] [ Memory ] [ Sound ] [ All ]               |
|                                    |----------------------------------------------------|
|                                    | PC: 0x8000   AF: 0x0044   BC: 0x0000   DE: 0x0000  |
|                                    | HL: 0x5800   SP: 0xFFFF   IX: 0x0000   IY: 0x5C3A  |
|       [ ZX Spectrum Display ]      | Flags: [----Z---]  IM: 1  IFF1: 1  HALT: 0         |
|         (256 x 192 Border)         | Disassembly:                                       |
|                                    |   0x8000:  LD   A, 0x07                            |
|                                    |   0x8002:  OUT  (0xFE), A                          |
|                                    |   0x8004:  JR   -4                                 |
|                                    |----------------------------------------------------|
|                                    | RAM Banks: [Bank 5: 0x4000] [Bank 2: 0x8000]       |
|                                    | ROM Active: ROM 1 (48K BASIC)                      |
|                                    | AY-3-8912: ChA: 440Hz | ChB: OFF | ChC: OFF        |
+------------------------------------+----------------------------------------------------+
| 📼 Tape Deck: [ ▶ PLAY ] [ ■ STOP ] [ ⟲ REWIND ] [ ⚡ Instant Load: ON ]   Tape: Ah-Harvest |
+-----------------------------------------------------------------------------------------+
| ⌨ Interactive Spectrum Keyboard: [ 1 2 3 4 5 6 7 8 9 0 ] [ Q W E R T Y U I O P ] ...    |
+-----------------------------------------------------------------------------------------+
```

---

## 📦 Download & Install (No Java Required!)

Pre-compiled, standalone native installers and portable bundles are available from the **[Releases](https://github.com/lazarius-borg/java_spectrum_emulator/releases)** page. These bundles include a trimmed, high-performance modular Java 26 runtime created with `jlink`, so **no prior Java installation is required on your system**.

| Platform | Package Format | Size | Installation & Execution |
| :--- | :--- | :--- | :--- |
| **macOS** | **`ZXSpectrum-0.1.0.dmg`** | ~33 MB | Open `.dmg`, drag `ZXSpectrum` to `/Applications`, double-click to play! |
| **Linux** | **`zxspectrum_0.1.0_amd64.deb`** | ~35 MB | Install via `sudo dpkg -i zxspectrum_0.1.0_amd64.deb` or extract `.tar.gz` and run `./bin/zxspectrum`. |
| **Windows** | **`ZXSpectrum-0.1.0.msi`** | ~38 MB | Run MSI setup wizard or extract `.zip` and double-click `bin\zxspectrum.bat`. |

---

## ⚡ Key Architectural Highlights

### 1. Modern Java 26 CPU Core
- **Sealed Records & Pattern Matching**: The Z80 instruction set is modeled as a hierarchy of sealed records (`Instruction.java`), executed via exhaustive, compiler-verified pattern-matching `switch` expressions (`InstructionExecutor.java`).
- **Complete Undocumented Opcodes**: Fully supports undocumented Z80 instructions including `SLL`, `IXH`/`IXL`/`IYH`/`IYL`, `DDCB`/`FDCB` with register write-back, `ED 70`/`ED 71`, undocumented flags `F3`/`F5`, and `MEMPTR`.
- **Cycle-Accurate Timing**: Instructions consume exact T-states, coordinating precisely with the 50.088 Hz ULA frame interrupt (70,908 T-states per frame on 128K, 69,888 on 48K).

### 2. Memory Banking & Paging
- **128K Architecture**: Implements 8 banks of 16KB RAM and 2 banks of 16KB ROM.
- **Port `0x7FFD` Paging**: Handles RAM banking at `0xC000 - 0xFFFF`, ROM 0 (128K Editor) / ROM 1 (48K BASIC) switching at `0x0000 - 0x3FFF`, shadow screen selection (RAM bank 5 vs 7), and bit 5 paging lockout until reset.
- **48K Mode Support**: Can be toggled on-the-fly into classic 48K mode with ROM 1 locked.

### 3. Display Subsystem (ULA)
- **Pixel-Accurate Interleaved Scanlines**: Translates physical Spectrum video memory (`0x4000 - 0x57FF`) into raster lines across 3 interleaved thirds.
- **Attribute Processing**: Full support for standard and bright palettes (8 standard + 8 bright colors), paper/ink masks, and hardware 16-frame Flash blinking.
- **Dynamic Border**: Emulates border changes within the active frame, rendering authentic tape loading stripes and timing effects.

### 4. Dual Sound Synthesis
- **1-Bit Port `0xFE` Beeper**: Recreates typing clicks and cassette audio directly from port writes and EAR pulses.
- **AY-3-8912 PSG Sound Chip**: 3 independent tone channels, 5-bit white noise generator, 16-bit envelope period generator with 8 envelope shapes, on Ports `0xFFFD` and `0xBFFD`.
- **44.1 kHz 16-Bit Stereo Mixing**: Synthesizes and mixes beeper and PSG audio in real-time via Java Sound without audio clipping or latency jitter.

### 5. Dual-Mode Tape Storage Engine
- **Authentic Pulse Playback**: Generates real-time microsecond pulses (pilot tone, sync pulses, data bit pulses) producing authentic tape loading sound and border stripes.
- **Fast ROM Trap Loading**: Automatically detects `0x0556 LD-BYTES` in Sinclair ROM 1, fulfilling the complete register exit contract (`DE=0`, `IX` advanced, `A=0`, `Carry=1`) to load tapes instantly without waiting.
- **Container Support**: Reads and writes standard `.TAP` and `.TZX` container files.
- **Snapshot Support**: Loads and saves `.SNA` (48K/128K) and `.Z80` (V1, V2, V3 with full AY-3-8912 state and banked memory).

---

## 🎮 Retro UI & Peripherals

### Lazygit-Inspired CRT Interface
- **Phosphor Green & Terminal Violet**: Styled with custom CSS (`retro.css`) featuring retro borders, monospace labels, and live status badges.
- **Collapsible Panels**: Real-time tape control bar, visual keyboard, and diagnostics inspector can be toggled on or off with keyboard shortcuts or toolbar buttons.

### Interactive Rubber Keyboard (`KeyboardView.java`)
- **Authentic Rubber Key Layout**: 40-key ZX Spectrum visual keyboard displaying primary keys, red Symbol Shift characters, green Sinclair BASIC keywords (`LOAD`, `PRINT`, `RUN`), and cursor arrows.
- **Real-Time Matrix Illumination**: Keys illuminate in bright retro cyan as you type on your host keyboard or click with your mouse.
- **Click-to-Type & Latch**: Supports mouse clicking with latching for `Caps Shift` and `Symbol Shift`.

### Draggable On-Screen Arcade Joystick (`OnScreenJoystickView.java`)
- **8-Direction Control**: Click-and-drag virtual arcade stick with authentic center deadzone and visual deflection feedback.
- **In-Game Port Auto-Detection & Lock**: When games running from RAM poll Kempston port `0x1F`, the emulator automatically detects it, locks the joystick type, and marks the menu `🔒 [In Use by Game]` to prevent control conflicts. Automatically unlocks upon machine reset.
- **Laptop Touchpad Co-Op Fire Key**: Designed for modern buttonless laptop trackpads. Steer the joystick with your touchpad and fire simultaneously using `Space`, `Control`, `Alt`, or `Z` without physical button cramps or key conflicts.

### Real-Time Diagnostics Inspector (`InspectorView.java`)
- **Segmented Subtabs**: Lazygit-style tab bar (`[ CPU ] [ Memory ] [ Sound ] [ All ]`) with zero truncation at any resolution.
- **Live Disassembly & Registers**: Real-time disassembly showing upcoming instructions, full 8-bit/16-bit register dumps, flags breakdown (`S Z 5 H 3 P/V N C`), and interrupt state.
- **Memory & Audio Monitoring**: Displays active 128K RAM/ROM banks, shadow screen state, and live volume meters for AY-3-8912 channels A/B/C and the ULA beeper.

### Built-in Game & Tape Library (`TapeLibrary.java`)
- **Persistent Library**: Automatically indexes loaded tapes and stores metadata (block count, size, last played) in a lightweight local JSON database.
- **Auto-Discovery**: Scans local `taps/` directory on launch for `.tap` and `.tzx` games.
- **One-Click Play**: Select any game from the side drawer to load or insert it into the virtual tape deck.

---

## ⌨️ Host Keyboard Controls

| Host Key | Spectrum Key | Description |
| :--- | :--- | :--- |
| `A - Z`, `0 - 9` | Alphanumeric | Direct Spectrum alphanumeric keypress |
| `Shift` (Left/Right) | **Caps Shift** | Caps Shift modifier |
| `Ctrl`, `Alt`, `` ` `` | **Symbol Shift** | Symbol Shift modifier |
| `Backspace`, `Delete` | **Delete** | `Caps Shift` + `0` |
| `Enter` / `Return` | **Enter** | Spectrum Enter key |
| `Space` | **Space** | Spectrum Space key (also Touchpad Fire) |
| `Escape` | **Break** | `Caps Shift` + `Space` |
| `Caps Lock` | **Caps Lock** | `Caps Shift` + `2` |
| `Arrow Keys` (`↑, ↓, ←, →`) | **Cursor Keys** | Mapped to `Caps Shift` + `7, 6, 5, 8` and active Joystick |
| `"` (Quote) | `Symbol Shift` + `P` | Double quote character |
| `;` (Semicolon) | `Symbol Shift` + `O` | Semicolon character |
| `:` (Colon) | `Symbol Shift` + `Z` | Colon character |
| `,` (Comma) | `Symbol Shift` + `N` | Comma character |
| `.` (Period) | `Symbol Shift` + `M` | Period character |
| `=` (Equals) | `Symbol Shift` + `L` | Equals sign |
| `+` (Plus) | `Symbol Shift` + `K` | Plus sign |
| `-` (Minus) | `Symbol Shift` + `J` | Minus sign |

---

## 📁 Project Structure

```
java_spectrum_emulator/
├── pom.xml                                # Maven build specification (Java 26, JavaFX 26.0.2)
├── scripts/
│   └── package.sh                         # Multi-platform native installer packaging script
├── .github/workflows/
│   └── release.yml                        # Automated GitHub Actions cross-OS release workflow
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   ├── module-info.java           # Java 26 modular module descriptor
│   │   │   └── nl/invokedynamic/spectrum/
│   │   │       ├── cpu/                   # Z80 CPU execution engine & disassembler
│   │   │       │   ├── Z80Cpu.java        # Core CPU coordinator and interrupt dispatcher
│   │   │       │   ├── CpuState.java      # Register file, flip-flops, and cycle counters
│   │   │       │   ├── Registers.java     # 8-bit, 16-bit, shadow registers, MEMPTR
│   │   │       │   ├── Flags.java         # Flag masks and lookup tables (Parity, SZ53)
│   │   │       │   ├── Instruction.java   # Sealed records for Z80 opcodes
│   │   │       │   ├── InstructionDecoder.java # Prefix decoding (CB, ED, DD, FD, DDCB, FDCB)
│   │   │       │   ├── InstructionExecutor.java # Pattern-matching execution dispatch
│   │   │       │   └── Disassembler.java  # Memory-to-mnemonic disassembler
│   │   │       ├── memory/                # 128K banked memory subsystem
│   │   │       │   ├── MemoryBus.java     # Memory read/write interface
│   │   │       │   └── Spectrum128Memory.java # 8x16K RAM, 2x16K ROM, Port 0x7FFD banking
│   │   │       ├── ula/                   # Video display generation
│   │   │       │   └── UlaDisplay.java    # Interleaved scanline renderer, attributes, flash
│   │   │       ├── sound/                 # Audio synthesis
│   │   │       │   ├── Beeper.java        # 1-bit Port 0xFE audio & EAR pulses
│   │   │       │   ├── Ay38912.java       # 3-channel PSG sound chip (tones, noise, envelopes)
│   │   │       │   └── AudioMixer.java    # 44.1 kHz stereo audio mixer
│   │   │       ├── io/                    # I/O dispatch & input controllers
│   │   │       │   ├── IoBus.java         # Port input/output interface
│   │   │       │   ├── Keyboard.java      # 8x5 keyboard matrix emulation
│   │   │       │   ├── Joystick.java      # Kempston, Sinclair 1/2, Cursor joysticks
│   │   │       │   └── SpectrumIoBus.java # Port router & game input auto-detector
│   │   │       ├── storage/               # Tape and snapshot formats
│   │   │       │   ├── TapFileFormat.java # .TAP container parser and writer
│   │   │       │   ├── TzxFileFormat.java # .TZX container parser and writer
│   │   │       │   ├── TapeFormat.java    # Unified tape loader (.TAP / .TZX)
│   │   │       │   ├── TapePlayer.java    # Real-time pulse generator & ROM trap loader
│   │   │       │   ├── TapeLibrary.java   # Persistent JSON tape catalog
│   │   │       │   ├── SnaSnapshot.java   # .SNA loader and saver (48K / 128K)
│   │   │       │   └── Z80Snapshot.java   # .Z80 loader (V1, V2, V3)
│   │   │       ├── machine/               # System coordinator
│   │   │       │   ├── MachineModel.java  # SPECTRUM_128K and SPECTRUM_48K timing
│   │   │       │   ├── RomLoader.java     # Embedded & filesystem ROM loader with fallback
│   │   │       │   └── SpectrumMachine.java # Master frame runner and coordinator
│   │   │       └── ui/                    # JavaFX user interface
│   │   │           ├── SpectrumApp.java   # JavaFX Application entrypoint
│   │   │           ├── SpectrumController.java # Main FXML UI controller
│   │   │           ├── ScreenView.java    # Integer-scaled CRT canvas
│   │   │           ├── KeyboardView.java  # Visual 40-key rubber keyboard with live matrix
│   │   │           ├── OnScreenJoystickView.java # Draggable arcade joystick widget
│   │   │           ├── InspectorView.java # Real-time CPU, Memory, and Sound inspector
│   │   │           ├── KeyboardMapper.java # Host keyboard translator & profiles
│   │   │           └── JoystickSettingsDialog.java # Joystick configuration dialog
│   │   └── resources/
│   │       ├── roms/                      # Embedded authentic 128K Sinclair ROMs
│   │       │   ├── 128-0.rom              # 128K Editor / Menu ROM
│   │       │   └── 128-1.rom              # 48K Sinclair BASIC ROM
│   │       └── nl/invokedynamic/spectrum/ui/
│   │           ├── main_view.fxml         # Retro layout XML specification
│   │           └── retro.css              # CRT phosphor styling & Lazygit tabs
│   └── test/java/                         # Automated JUnit 5 test suite
```

---

## 🛠️ Building & Developing Locally

### Prerequisites
- **Java 26** (or newer)
- **Maven 3.9.16** (or use the bundled `./mvnw` wrapper)

### Run Unit Tests
```bash
./mvnw clean test
```

### Launch from Source
```bash
./mvnw javafx:run
```

### Build Streamlined Jlink Runtime
Produces a standalone, self-contained modular Java runtime image in `target/zxspectrum-runtime/`:
```bash
./mvnw javafx:jlink
./target/zxspectrum-runtime/bin/zxspectrum
```

### Build Native Installers Locally
Uses `jpackage` to compile platform-native installers (`.dmg`, `.deb`, `.msi`) into `dist/`:
```bash
# Build all supported packages for current OS:
./scripts/package.sh --all

# Or build specific formats:
./scripts/package.sh --dmg   # macOS Disk Image
./scripts/package.sh --deb   # Linux Debian package
./scripts/package.sh --zip   # Portable ZIP bundle
```

---

## 📜 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
Sinclair ZX Spectrum ROMs and hardware architectures are copyright their respective original copyright holders.
