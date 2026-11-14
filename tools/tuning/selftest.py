import numpy as np

from gate_sim import gate_gain

FRAME = 480

speech = np.zeros(100, dtype=bool)
speech[10:20] = True
gain = gate_gain(speech, hold_frames=20, release_ms=150, attack_ms=5.0)

assert np.all(gain[: 8 * FRAME] == 0), "(a) gain must be 0 for frames 0 to 7"
assert abs(gain[8 * FRAME] - 1 / 240) < 1e-5, "(b) first sample of frame 8 must be 1/240"
assert np.all(gain[8 * FRAME + FRAME - 1 : 40 * FRAME] >= 0.9999), "(c) gain must be open through frame 39"
assert abs(gain[40 * FRAME] - (1 - 1 / 7200)) < 1e-4, "(d) first sample of frame 40 must be 1 - 1/7200"
assert np.all(gain[56 * FRAME :] < 0.001), "(e) gain must be below 0.001 from frame 56"
assert np.all((gain >= 0) & (gain <= 1.5)), "(f) every gain must be between 0 and 1.5"

print("PASS")
