package com.haru.api.domain.websocket;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class WavUtil {

    public static byte[] convertPcmToWav(byte[] pcmData, int sampleRate, int channels, int bitsPerSample) throws IOException {
        int byteRate = sampleRate * channels * bitsPerSample / 8;
        int blockAlign = channels * bitsPerSample / 8;
        int dataSize = pcmData.length;

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        // RIFF header
        out.write("RIFF".getBytes());
        out.write(intToLittleEndian(36 + dataSize)); // file size - 8
        out.write("WAVE".getBytes());

        // fmt subchunk
        out.write("fmt ".getBytes());
        out.write(intToLittleEndian(16)); // Subchunk1Size (16 for PCM)
        out.write(shortToLittleEndian((short) 1)); // AudioFormat (1 = PCM)
        out.write(shortToLittleEndian((short) channels));
        out.write(intToLittleEndian(sampleRate));
        out.write(intToLittleEndian(byteRate));
        out.write(shortToLittleEndian((short) blockAlign));
        out.write(shortToLittleEndian((short) bitsPerSample));

        // data subchunk
        out.write("data".getBytes());
        out.write(intToLittleEndian(dataSize));
        out.write(pcmData);

        return out.toByteArray();
    }

    public static void saveWavFile(String filePath, byte[] pcmData, int sampleRate, int channels, int bitsPerSample) throws IOException {
        byte[] wavData = convertPcmToWav(pcmData, sampleRate, channels, bitsPerSample);
        try (FileOutputStream fos = new FileOutputStream(filePath)) {
            fos.write(wavData);
        }
    }

    private static byte[] intToLittleEndian(int value) {
        return new byte[] {
                (byte)(value & 0xff),
                (byte)((value >> 8) & 0xff),
                (byte)((value >> 16) & 0xff),
                (byte)((value >> 24) & 0xff)
        };
    }

    private static byte[] shortToLittleEndian(short value) {
        return new byte[] {
                (byte)(value & 0xff),
                (byte)((value >> 8) & 0xff)
        };
    }
}
