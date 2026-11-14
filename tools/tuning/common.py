import numpy as np
import soundfile as sf
from scipy.signal import resample_poly

SAMPLE_RATE = 48000
FRAME_SAMPLES = 480


def load_clip(path):
    data, rate = sf.read(path, dtype="float32", always_2d=True)
    x = data[:, 0]
    if rate != SAMPLE_RATE:
        x = resample_poly(x, SAMPLE_RATE, rate).astype(np.float32)
    return np.ascontiguousarray(x, dtype=np.float32)


def to_frames(x):
    count = len(x) // FRAME_SAMPLES
    return x[: count * FRAME_SAMPLES].reshape(count, FRAME_SAMPLES)


def frame_levels_db(frames):
    rms = np.sqrt(np.mean(np.square(frames, dtype=np.float64), axis=1))
    return np.maximum(20 * np.log10(np.maximum(rms, 1e-9)), -90.0)
