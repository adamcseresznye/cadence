import csv
import glob
import json
import sys
from pathlib import Path

import numpy as np
from scipy.ndimage import binary_dilation

from common import FRAME_SAMPLES, frame_levels_db, load_clip, to_frames
from gate_sim import SENSITIVITY_TABLE, analyze_frames, gate_gain

HERE = Path(__file__).resolve().parent
DATA = HERE / "data"
OUT = HERE / "out"
PARAMS = HERE / "params.json"
SNRS_DB = (10, 15, 20)
HOLD_CHOICES = (10, 20, 30)
RELEASE_CHOICES = (100, 150, 250)
FAR_RADIUS_FRAMES = 50
MIN_RECALL = 0.95
RESULT_FIELDS = ["sensitivity", "holdFrames", "releaseMs", "recall", "falseOpen", "score"]
PARAM_FIELDS = ["sensitivity", "holdFrames", "releaseMs", "recall", "falseOpen"]


def load_trimmed(path):
    return to_frames(load_clip(path)).reshape(-1)


def label_speech(x):
    levels = frame_levels_db(to_frames(x))
    labels = levels > np.percentile(levels, 10) + 12
    kept = np.flatnonzero(labels)
    for a, b in zip(kept[:-1], kept[1:]):
        if 2 <= b - a <= 10:  # gap of 1 to 9 False frames between two speech frames
            labels[a + 1 : b] = True
    return labels


def mix(voice, labels, noise, snr_db):
    noise = np.resize(noise, len(voice))
    speech_power = np.mean(voice[np.repeat(labels, FRAME_SAMPLES)] ** 2)
    noise_power = np.mean(noise ** 2)
    scale = np.sqrt(speech_power / (10 ** (snr_db / 10) * noise_power))
    return np.clip(voice + scale * noise, -1, 1), labels


def build_items(voices, noises):
    labelled = [(voice, label_speech(voice)) for voice in voices]
    items = [mix(voice, labels, noise, snr) for voice, labels in labelled for noise in noises for snr in SNRS_DB]
    items += [(noise, np.zeros(len(noise) // FRAME_SAMPLES, dtype=bool)) for noise in noises]
    return items


def far_from_speech(labels):
    return ~binary_dilation(labels, structure=np.ones(2 * FAR_RADIUS_FRAMES + 1, dtype=bool))


def score_candidate(frame_gains, prepared):
    hit = labelled = far_total = open_far = 0
    for gains, (labels, far) in zip(frame_gains, prepared):
        hit += np.count_nonzero(labels & (gains >= 0.9))
        labelled += np.count_nonzero(labels)
        far_total += np.count_nonzero(far)
        open_far += np.count_nonzero(far & (gains > 0.1))
    recall = hit / labelled
    false_open = open_far / far_total
    return recall, false_open, recall - 3 * false_open


def sweep(items):
    prepared = [(labels, far_from_speech(labels)) for _, labels in items]
    rows = []
    for sensitivity, (vad_mode, margin_db) in enumerate(SENSITIVITY_TABLE):
        speech = [analyze_frames(x, vad_mode, margin_db)[2] for x, _ in items]
        for hold in HOLD_CHOICES:
            for release in RELEASE_CHOICES:
                gains = [gate_gain(sp, hold, release).reshape(-1, FRAME_SAMPLES).mean(axis=1) for sp in speech]
                recall, false_open, score = score_candidate(gains, prepared)
                rows.append({"sensitivity": sensitivity, "holdFrames": hold, "releaseMs": release,
                             "recall": recall, "falseOpen": false_open, "score": score})
    return rows


def choose(rows):
    eligible = [r for r in rows if r["recall"] >= MIN_RECALL]
    if eligible:
        return eligible[0]
    print(f"WARNING: no candidate reaches recall {MIN_RECALL}; using the highest score.")
    return rows[0]


def write_results(rows):
    OUT.mkdir(exist_ok=True)
    with open(OUT / "results.csv", "w", newline="") as f:
        writer = csv.DictWriter(f, fieldnames=RESULT_FIELDS)
        writer.writeheader()
        writer.writerows(rows)


def main():
    voice_paths = sorted(glob.glob(str(DATA / "*_voice.wav")))
    noise_paths = sorted(glob.glob(str(DATA / "*_noise.wav")))
    missing = [kind for kind, paths in (("voice", voice_paths), ("noise", noise_paths)) if not paths]
    if missing:
        print(f"Missing {' and '.join(missing)} clips in {DATA} (need *_voice.wav and *_noise.wav)")
        sys.exit(1)
    items = build_items([load_trimmed(p) for p in voice_paths], [load_trimmed(p) for p in noise_paths])
    rows = sorted(sweep(items), key=lambda r: r["score"], reverse=True)
    best = choose(rows)
    write_results(rows)
    print("Top 5 candidates:")
    for r in rows[:5]:
        print(f"sensitivity={r['sensitivity']} holdFrames={r['holdFrames']} releaseMs={r['releaseMs']} "
              f"recall={r['recall']:.3f} falseOpen={r['falseOpen']:.3f} score={r['score']:.3f}")
    PARAMS.write_text(json.dumps({k: best[k] for k in PARAM_FIELDS}, indent=2))
    print(f"Chosen settings written to {PARAMS}")


if __name__ == "__main__":
    main()
