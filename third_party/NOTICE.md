# Third-party notices

## libfvad

- Library: libfvad, voice activity detector.
- Licence: BSD-3-Clause. The full licence text is in `third_party/libfvad/LICENSE` and `third_party/libfvad/PATENTS`.
- Based on: the WebRTC voice activity detector.
- Source: https://github.com/dpirch/libfvad

## RNNoise

- Library: RNNoise, recurrent neural network noise suppressor, version v0.2 (commit 904a876) with the model archive rnnoise_data-0b50c45 from https://media.xiph.org/rnnoise/models/.
- Licence: BSD-3-Clause. The full licence text is in `third_party/rnnoise/COPYING`.
- Changes: only `include/`, `src/*.c` and `src/*.h` (without the dump and write tools), the headers of `src/x86/`, the model file `src/rnnoise_data.c` with its header, `COPYING`, `AUTHORS` and `README` are kept.
- `src/vec_neon.h` is the version from upstream commit 372f7b4 ("Fix compilation errors."): the v0.2 file does not compile for ARM.
- Source: https://github.com/xiph/rnnoise
