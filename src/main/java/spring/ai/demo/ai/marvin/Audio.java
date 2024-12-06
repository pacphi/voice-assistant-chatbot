package spring.ai.demo.ai.marvin;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import javax.sound.sampled.*;
import org.springframework.util.StreamUtils;

public class Audio implements AutoCloseable {
	private final AudioFormat format;
	private final AtomicReference<TargetDataLine> microphone = new AtomicReference<>();
	private final File wavFile;
	private final ReentrantLock recordingLock = new ReentrantLock();
	private volatile boolean isRecording = false;
	private final ExecutorService executorService;
	private Future<?> recordingFuture;

	public Audio() {
		this(new AudioFormat(44100.0f, 16, 1, true, true), "AudioRecordBuffer.wav");
	}

	public Audio(AudioFormat format, String wavFileName) {
		this.format = format;
		this.wavFile = new File(wavFileName);
		this.executorService = Executors.newFixedThreadPool(2, r -> {
			Thread t = new Thread(r);
			t.setDaemon(true);
			return t;
		});
	}

	public void startRecording() {
		if (!recordingLock.tryLock()) {
			throw new IllegalStateException("Recording already in progress");
		}

		try {
			if (isRecording) {
				return;
			}

			stopRecording();

			TargetDataLine mic = AudioSystem.getTargetDataLine(format);
			mic.open(format);
			mic.start();

			if (!microphone.compareAndSet(null, mic)) {
				mic.close();
				return;
			}

			isRecording = true;

			recordingFuture = executorService.submit(() -> {
				try {
					AudioSystem.write(
							new AudioInputStream(microphone.get()),
							AudioFileFormat.Type.WAVE,
							wavFile
					);
					return null;
				}
				catch (IOException e) {
					throw new RuntimeException("Failed to write audio data to file", e);
				}
				catch (IllegalArgumentException e) {
					throw new RuntimeException("Invalid audio format or parameters", e);
				}
				catch (SecurityException e) {
					throw new RuntimeException("Permission denied for audio operations", e);
				}
				finally {
					stopRecording();
				}
			});
		}
		catch (Exception e) {
			throw new RuntimeException("Failed to start recording", e);
		}
		finally {
			recordingLock.unlock();
		}
	}

	public void stopRecording() {
		recordingLock.lock();
		try {
			isRecording = false;
			if (recordingFuture != null) {
				recordingFuture.cancel(true);
				recordingFuture = null;
			}
			TargetDataLine mic = microphone.get();
			if (mic != null) {
				mic.stop();
				mic.close();
				microphone.set(null);
			}
		}
		finally {
			recordingLock.unlock();
		}
	}

	public byte[] getLastRecording() {
		recordingLock.lock();
		try {
			return wavFile.exists()
					? StreamUtils.copyToByteArray(new BufferedInputStream(new FileInputStream(wavFile)))
					: new byte[0];
		}
		catch (IOException e) {
			throw new RuntimeException("Failed to read recording", e);
		}
		finally {
			recordingLock.unlock();
		}
	}

	public void play(byte[] waveData) {
		CompletableFuture.runAsync(() -> {
			try (Clip clip = AudioSystem.getClip();
				 AudioInputStream audio = AudioSystem.getAudioInputStream(
						 new BufferedInputStream(new ByteArrayInputStream(waveData)))) {

				clip.open(audio);
				clip.start();

				while (!clip.isRunning()) {
					TimeUnit.MILLISECONDS.sleep(100);
				}
				while (clip.isRunning()) {
					TimeUnit.MILLISECONDS.sleep(100);
				}

				clip.stop();
			}
			catch (LineUnavailableException | UnsupportedAudioFileException | IOException e) {
				throw new RuntimeException("Audio playback failed: " + e.getMessage(), e);
			}
			catch (IllegalArgumentException | SecurityException e) {
				throw new RuntimeException("Invalid audio configuration: " + e.getMessage(), e);
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new RuntimeException("Playback interrupted", e);
			}
		}, executorService);
	}

	@Override
	public void close() {
		stopRecording();
		executorService.shutdown();
		try {
			if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
				executorService.shutdownNow();
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			executorService.shutdownNow();
		}
	}
}