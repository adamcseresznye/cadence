import json
import sys

import matplotlib.pyplot as plt
import numpy as np
import soundfile as sf

from common import FRAME_SAMPLES, SAMPLE_RATE
from gate_sim import simulate
from tune import DATA, OUT, PARAMS, load_trimmed

SHOW_SECONDS = 20
FRAMES_PER_SECOND = SAMPLE_RATE // FRAME_SAMPLES


def plot(path, result):
    frames = min(len(result["level_db"]), SHOW_SECONDS * FRAMES_PER_SECOND)
    samples = frames * FRAME_SAMPLES
    t = np.arange(frames) / FRAMES_PER_SECOND
    t_samples = np.arange(samples) / SAMPLE_RATE
    fig, (ax_level, ax_speech, ax_gain) = plt.subplots(3, 1, figsize=(12, 9), sharex=True)
    ax_level.plot(t, result["level_db"][:frames], label="level dB")
    ax_level.plot(t, result["floor_db"][:frames], label="noise floor dB")
    ax_level.set_ylabel("dB")
    ax_level.legend(loc="upper right")
    ax_speech.step(t, result["speech"][:frames].astype(float), where="post")
    ax_speech.set_ylabel("speech")
    ax_gain.plot(t_samples, result["gain"][:samples])
    ax_gain.set_ylabel("gain")
    ax_gain.set_xlabel("time (s)")
    fig.savefig(path, dpi=100)
    plt.close(fig)


def render_clip(wav, params):
    x = load_trimmed(wav)
    result = simulate(x, params["sensitivity"], params["holdFrames"], params["releaseMs"])
    name = wav.stem
    sf.write(OUT / f"{name}_gated.wav", x * result["gain"], SAMPLE_RATE, subtype="PCM_16")
    plot(OUT / f"{name}.png", result)


def main():
    if not PARAMS.exists():
        print("Run tune.py first")
        sys.exit(1)
    params = json.loads(PARAMS.read_text())
    OUT.mkdir(exist_ok=True)
    for wav in sorted(DATA.glob("*.wav")):
        render_clip(wav, params)
        print(f"Wrote {wav.stem}_gated.wav and {wav.stem}.png")


if __name__ == "__main__":
    main()
