package com.haru.api.infra.mp3encoder;

import org.springframework.stereotype.Service;
import ws.schild.jave.Encoder;
import ws.schild.jave.MultimediaObject;
import ws.schild.jave.encode.AudioAttributes;
import ws.schild.jave.encode.EncodingAttributes;
import ws.schild.jave.process.ProcessWrapper;
import ws.schild.jave.process.ffmpeg.DefaultFFMPEGLocator;
import ws.schild.jave.process.ffmpeg.FFMPEGProcess;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class Mp3EncoderService {

    /**
     * Raw PCM 오디오 데이터(byte[])를 MP3 형식으로 인코딩합니다.
     *
     * @param pcmData       인코딩할 원본 PCM 데이터
     * @param channels      오디오 채널 (1: 모노, 2: 스테레오)
     * @param samplingRate  샘플링 레이트 (e.g., 44100, 16000)
     * @param bitRate       비트레이트 (e.g., 128000 for 128kbps)
     * @return 인코딩된 MP3 데이터 byte[]
     */
    public byte[] encodePcmToMp3(byte[] pcmData, int channels, int samplingRate, int bitRate) {
        File sourcePcm = null;
        File targetMp3 = null;

        try {
            sourcePcm = File.createTempFile("source_pcm_", ".raw");
            try (FileOutputStream fos = new FileOutputStream(sourcePcm)) {
                fos.write(pcmData);
            }

            targetMp3 = File.createTempFile("target_mp3_", ".mp3");

            AudioAttributes audio = new AudioAttributes();
            audio.setCodec("libmp3lame");
            audio.setBitRate(bitRate);
            audio.setChannels(channels);
            audio.setSamplingRate(samplingRate);

            EncodingAttributes attrs = new EncodingAttributes();
            attrs.setOutputFormat("mp3");
            attrs.setAudioAttributes(audio);

            // Encoder를 생성할 때 커스텀 Locator를 전달합니다.
            Encoder encoder = new Encoder(new RawAudioLocator(channels, samplingRate));

            // MultimediaObject는 소스 파일만 감쌉니다.
            encoder.encode(new MultimediaObject(sourcePcm), targetMp3, attrs);

            return Files.readAllBytes(targetMp3.toPath());

        } catch (Exception e) {
            throw new RuntimeException("MP3 인코딩 실패", e);
        } finally {
            if (sourcePcm != null && sourcePcm.exists()) {
                sourcePcm.delete();
            }
            if (targetMp3 != null && targetMp3.exists()) {
                targetMp3.delete();
            }
        }
    }

    /**
     * Raw PCM 파일을 디코딩하기 위한 정보를 FFmpeg에 제공하는 private static inner 클래스.
     * 외부에서는 알 필요 없는 구현 세부사항이므로 내부에 캡슐화합니다.
     */
    private static class RawAudioFFMPEGProcess  extends FFMPEGProcess  {

        private final int channels;
        private final int samplingRate;

        public RawAudioFFMPEGProcess(String executablePath, int channels, int samplingRate) {
            super(executablePath);
            this.channels = channels;
            this.samplingRate = samplingRate;
        }

        @Override
        protected Stream<String> enhanceArguments(Stream<String> execArgs) {
            // Stream을 List로 변환하여 '-i' 인자의 위치를 찾기
            List<String> args = execArgs.collect(Collectors.toList());
            int inputIndex = args.indexOf("-i");

            if (inputIndex != -1) {
                // '-i' 바로 앞에 raw pcm 포맷 정보를 추가
                List<String> pcmArgs = new ArrayList<>();
                pcmArgs.add("-f");
                pcmArgs.add("s16le");
                pcmArgs.add("-ac");
                pcmArgs.add(String.valueOf(this.channels));
                pcmArgs.add("-ar");
                pcmArgs.add(String.valueOf(this.samplingRate));

                // 원래 리스트의 '-i' 위치에 pcm 관련 인자들을 삽입
                args.addAll(inputIndex, pcmArgs);
            }

            // 수정된 List를 다시 Stream으로 변환하여 반환
            return args.stream();
        }
    }

    private static class RawAudioLocator extends DefaultFFMPEGLocator {
        private final int channels;
        private final int samplingRate;

        public RawAudioLocator(int channels, int samplingRate) {
            super();
            this.channels = channels;
            this.samplingRate = samplingRate;
        }

        @Override
        public ProcessWrapper createExecutor() {
            // 기본 FFMPEGProcess 대신 직접 구현한 RawAudioFFMPEGProcess를 반환
            return new RawAudioFFMPEGProcess(this.getExecutablePath(), this.channels, this.samplingRate);
        }
    }
}
