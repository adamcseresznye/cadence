package com.cadence.daf

import android.media.AudioManager

/** A call or another app playing stops practice; a short duck (a notification sound) does not. */
fun stopsOnFocusChange(change: Int): Boolean =
    change == AudioManager.AUDIOFOCUS_LOSS || change == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT
