import numpy as np
import webrtcvad

from common import FRAME_SAMPLES, SAMPLE_RATE, frame_levels_db, to_frames

SAMPLES_PER_MS = 48  # kSamplesPerMs in DESIGN.md section 2

# (voice detector mode, marginDb) per sensitivity level 0..10, DESIGN.md section 9
SENSITIVITY_TABLE = [
    (3, 20.0), (3, 18.5), (3, 17.0),
    (2, 15.5), (2, 14.0), (2, 12.5),
    (1, 11.0), (1, 9.5), (1, 8.0),
    (0, 6.5), (0, 5.0),
]


def analyze_frames(x, vad_mode, margin_db):
    frames = to_frames(x)
    level_db = frame_levels_db(frames)
    pcm = np.clip(np.round(frames * 32768), -32768, 32767).astype("<i2")
    vad = webrtcvad.Vad(vad_mode)
    floor = -60.0
    floor_db = []
    speech = []
    for level, samples in zip(level_db.tolist(), pcm):
        if level < floor:
            floor = floor + 0.3 * (level - floor)
        else:
            floor = min(floor + 0.03, level)
        loud = level > floor + margin_db and level > -55.0
        voiced = vad.is_speech(samples.tobytes(), SAMPLE_RATE)
        floor_db.append(floor)
        speech.append(voiced and loud)
    return level_db, np.array(floor_db), np.array(speech, dtype=bool)


def gate_gain(speech, hold_frames, release_ms, attack_ms=5.0):
    # Entry i of the result is the gain for input sample i (gate position = sample index).
    speech = np.asarray(speech, dtype=bool)
    count = len(speech)
    padded = np.concatenate([speech, [False, False]])
    any_speech = speech | padded[1:count + 1] | padded[2:count + 2]
    attack = 1.0 / (attack_ms * SAMPLES_PER_MS)
    release = 1.0 / (release_ms * SAMPLES_PER_MS)
    is_open = np.empty(count, dtype=bool)
    frame_start = np.empty(count)
    gain = 0.0
    hold = 0
    for f, speaking in enumerate(any_speech.tolist()):
        opened = speaking or hold > 0
        if speaking:
            hold = hold_frames
        elif hold > 0:
            hold -= 1
        is_open[f] = opened
        frame_start[f] = gain
        step = FRAME_SAMPLES * (attack if opened else -release)
        gain = min(1.0, max(0.0, gain + step))
    sample_index = np.tile(np.arange(1, FRAME_SAMPLES + 1), count)
    rate = np.repeat(np.where(is_open, attack, -release), FRAME_SAMPLES)
    start = np.repeat(frame_start, FRAME_SAMPLES)
    return np.clip(start + sample_index * rate, 0.0, 1.0).astype(np.float32)


def simulate(x, sensitivity, hold_frames, release_ms):
    vad_mode, margin_db = SENSITIVITY_TABLE[sensitivity]
    level_db, floor_db, speech = analyze_frames(x, vad_mode, margin_db)
    gain = gate_gain(speech, hold_frames, release_ms)
    return {"level_db": level_db, "floor_db": floor_db, "speech": speech, "gain": gain}
