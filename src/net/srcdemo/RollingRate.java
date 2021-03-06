package net.srcdemo;

import java.text.DecimalFormat;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public final class RollingRate {
	private static final DecimalFormat framesProcessedPerSecondFormat = new DecimalFormat(
		Strings.lblFramesProcessedPerSecondFormat);
	private static final int numberOfSamples = 20;
	private static final double rateClampMax = 9001;
	private static final double rateClampMin = 0;
	private boolean full = false;
	private int index = 0;
	private final Lock lock = new ReentrantLock();
	private final long[] times = new long[numberOfSamples];

	public RollingRate() {
		// Nothing
	}

	public String getFormattedRate() {
		final Double rate = getRatePerSecond();
		return rate == null ? Strings.lblFramesProcessedPerSecondDefault : framesProcessedPerSecondFormat.format(rate);
	}

	public Double getRatePerSecond() {
		lock.lock();
		if (!full) {
			lock.unlock();
			return null;
		}
		final long timeElapsed = times[index - 1 < 0 ? numberOfSamples - 1 : index - 1] - times[index];
		lock.unlock();
		if (timeElapsed == 0) {
			return null;
		}
		return Math.max(rateClampMin, Math.min(rateClampMax, 1000.0 * numberOfSamples / timeElapsed));
	}

	public void mark() {
		lock.lock();
		times[index] = System.currentTimeMillis();
		index++;
		if (index >= numberOfSamples) {
			index = 0;
			full = true;
		}
		lock.unlock();
	}
}
