# ZX Spectrum 128K Emulator in Modern Java

A full software implementation of the **Sinclair ZX Spectrum 128K** written in **Java 26** using **Maven 3.9.16** and **JavaFX**.

---

## 📦 Download & Install (No Java Required!)

Pre-compiled, standalone native installers and portable bundles are available from the **[Releases](https://github.com/lazarius-borg/java_spectrum_emulator/releases)** page. These include a self-contained runtime so **no Java installation is required**.

| Platform | Format | How to Run |
| :--- | :--- | :--- |
| **macOS** | **`ZXSpectrum.dmg`** (~33 MB) | Open `.dmg`, drag `ZXSpectrum` to `/Applications`, double-click to play! |
| **Linux** | **`zxspectrum.deb`** (~35 MB) | Install via `sudo dpkg -i zxspectrum.deb` or extract `.tar.gz` and run `./bin/zxspectrum`. |
| **Windows** | **`ZXSpectrum.msi`** (~38 MB) | Run installer setup wizard or extract `.zip` and double-click `bin\zxspectrum.bat`. |

---

## Features

- **Z80 CPU Core**:
  - Full opcode support, including all **undocumented instructions** (`SLL`, `IXH`/`IXL`/`IYH`/`IYL`, `DDCB`/`FDCB` with register write-back, `ED 70`/`ED 71`, undocumented flags `F3`/`F5`, and `MEMPTR`).
  - Implemented using modern Java **sealed interfaces and records** (`Instruction.java`), with execution dispatched via exhaustive pattern-matching **`switch` expressions** (`InstructionExecutor.java`).
  - Cycle-accurate timing per instruction.
- **ZX Spectrum 128K Architecture**:
  - 128KB RAM (8 banks of 16KB) and 32KB ROM (ROM 0 = 128K Editor/Menu, ROM 1 = 48K BASIC).
  - Memory banking, shadow screen paging, and paging lock via Port `0x7FFD`.
  - Also supports ZX Spectrum 48K mode.
- **Video Display (ULA)**:
  - 256×192 pixel resolution with customizable border (320×240 total display buffer).
  - Exact screen layout interleaved scanlines and attribute cells (Ink, Paper, Bright, Flash).
  - 50.088 Hz frame rate (70,908 T-states per frame on 128K).
- **Dual Sound Engine**:
  - 1-bit ULA beeper on Port `0xFE`.
  - 3-channel **AY-3-8912 PSG** (Tone generators A, B, C, noise generator, envelope generator) on Ports `0xFFFD` and `0xBFFD`.
  - 44.1 kHz 16-bit stereo mixing via Java Sound.
- **Input & Peripherals**:
  - Full 40-key ZX Spectrum keyboard matrix mapped to host keyboard.
  - Joysticks supported: **Kempston** (Port `0x1F`), **Sinclair Interface II** (Ports 1 & 2), and **Cursor/Protek**.
- **Tape Player & Recorder**:
  - Full `.TAP` file container support.
  - Cycle-accurate pulse generator (pilot tone, sync, bit pulses, authentic loading stripes and sound).
  - Fast ROM trap loading (intercepts `0x0556 LD-BYTES`) for instant loading.
- **Snapshots**:
  - `.SNA` (both 48K and 128K formats).
  - `.Z80` (Version 1, Version 2, and Version 3 with 128K banked RAM and AY state).

---

## Project Structure

```
java_spectrum_emulator/
├── pom.xml
├── roms/                              # Directory for 128K/48K ROM files
├── src/
│   ├── main/java/nl/invokedynamic/spectrum/
│   │   ├── cpu/
│   │   │   ├── Z80Cpu.java            # CPU core coordinator
│   │   │   ├── CpuState.java          # Registers and interrupt state
│   │   │   ├── Registers.java         # 8-bit, 16-bit, shadow, index registers, MEMPTR
│   │   │   ├── Flags.java             # Flag definitions and lookup tables
│   │   │   ├── Instruction.java       # Sealed interface & opcode records
│   │   │   ├── InstructionDecoder.java# Byte fetch and prefix decoding
│   │   │   └── InstructionExecutor.java# Pattern-matching switch execution
│   │   ├── memory/
│   │   │   ├── MemoryBus.java         # Memory read/write interface
│   │   │   └── Spectrum128Memory.java # 8x16K RAM, 2x16K ROM, Port 0x7FFD banking
│   │   ├── ula/
│   │   │   └── UlaDisplay.java        # 256x192 + border rendering, flash, colors
│   │   ├── sound/
│   │   │   ├── Beeper.java            # 1-bit Port 0xFE audio
│   │   │   ├── Ay38912.java           # 3-channel PSG sound chip
│   │   │   └── AudioMixer.java        # 44.1 kHz stereo mixer
│   │   ├── io/
│   │   │   ├── IoBus.java             # I/O interface
│   │   │   ├── Keyboard.java          # 8x5 keyboard matrix
│   │   │   ├── Joystick.java          # Kempston, Sinclair, Cursor joysticks
│   │   │   └── SpectrumIoBus.java     # Port dispatcher
│   │   ├── storage/
│   │   │   ├── TapFileFormat.java     # .TAP loader and saver
│   │   │   ├── TapePlayer.java        # Pulse generator and ROM trap loader
│   │   │   ├── SnaSnapshot.java       # .SNA loader and saver (48K/128K)
│   │   │   └── Z80Snapshot.java       # .Z80 loader (V1, V2, V3)
│   │   ├── machine/
│   │   │   ├── MachineModel.java      # SPECTRUM_128K and SPECTRUM_48K timing
│   │   │   ├── RomLoader.java         # ROM finder with built-in test pattern fallback
│   │   │   └── SpectrumMachine.java   # Frame runner and master coordinator
│   │   └── ui/
│   │       ├── SpectrumApp.java       # JavaFX Application GUI
│   │       ├── ScreenView.java        # Canvas with pixel-perfect scaling
│   │       └── KeyboardMapper.java    # Host key event translator
│   └── test/java/nl/invokedynamic/spectrum/        # Automated JUnit 5 test suite
```

---

## ROM Files

Place your ZX Spectrum 128K ROM files in the `roms/` directory:
- `roms/128-0.rom` (16KB: 128K Editor / Menu)
- `roms/128-1.rom` (16KB: 48K BASIC)
- *Alternatively*: `roms/128k.rom` (32KB combined ROM)

> **Note**: If no ROM files are placed in `roms/`, the emulator automatically boots with an internal test pattern bootloader so you can test display, border, and CPU execution immediately!

---

## Building and Running

### Requirements
- **Java 26** (or newer)
- **Maven 3.9.16** (or newer)

### Run Unit Tests
```bash
mvn test
```

### Build Jar
```bash
mvn package
```

### Launch the Emulator GUI
```bash
mvn javafx:run
```

### Build Streamlined Jlink Runtime
Produces a trimmed, self-contained modular Java runtime in `target/zxspectrum-runtime/`:
```bash
mvn javafx:jlink
./target/zxspectrum-runtime/bin/zxspectrum
```

### Build Native Installers & Bundles
Produces native `.dmg` (macOS), `.deb` (Linux), and portable `.zip` bundles in `dist/`:
```bash
./scripts/package.sh --all
```


---

## Host Keyboard Controls

| Host Key | Spectrum Key |
| :--- | :--- |
| `A-Z`, `0-9` | Corresponding Spectrum alphanumeric key |
| `Shift` (Left/Right) | **Caps Shift** |
| `Alt` or `Ctrl` | **Symbol Shift** |
| `Backspace` | **Delete** (`Caps Shift` + `0`) |
| `Enter` | **Enter** |
| `Space` | **Space** |
| `Arrow Keys` | Mapped to cursor keys (`Caps Shift` + `5,6,7,8`) and Active Joystick |
| `Ctrl` | **Fire Button** on Active Joystick |
