package com.espi.streamer;

import android.content.Context;

import java.io.File;

public class CompressionUtils {
    private CompressionUtils() {}

    public static File compressMedia(Context context, File source, String mode) {
        // In AIDE/offline scenarios, keep CPU low by optionally returning source file.
        // You can replace this with MediaCodec transcode implementation if needed.
        if (source == null || !source.exists()) {
            return source;
        }

        // Placeholder for lightweight compression policy:
        // - video_audio: already recorded at low bitrate in RecordingService.
        // - audio_only: AAC 64kbps already configured.
        return source;
    }
}
