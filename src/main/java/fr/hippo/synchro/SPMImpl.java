package fr.hippo.synchro;

import com.jcraft.jsch.SftpProgressMonitor;

public class SPMImpl implements SftpProgressMonitor {

	long totalDL = 0;
	long size;

	// every 10 seconds
	int threshold = 1 * 1000;

	long startTime;
	long endTime;

	long thresholdTime;
	long thresholdDL = 0;

	@Override
	public boolean count(long currentDL) {
		totalDL += currentDL;
		long now = System.currentTimeMillis();

		if (now >= thresholdTime + threshold) {
			int percentage = (int) (totalDL * 100.0 / size + 0.5);
			int speed = getSpeed(totalDL - thresholdDL, thresholdTime, now);

			String remainingTime = getRemainingTime(size, totalDL, speed);

			System.out.print(getProgressBar(percentage) + "\t" + percentage + "%" + getSpaces(speed) + speed
					+ "ko/s (" + remainingTime + ")\r");
			thresholdTime = now;
			thresholdDL = totalDL;
		}
		return true;
	}

    private String getSpaces(int speed) {
        int nbSpaces = 6 - (speed+"").length();
        if (nbSpaces < 0) {
            nbSpaces = 0;
        }
        return new String(new char[nbSpaces]).replace("\0", " ");
    }

    private String getProgressBar(int percent) {

		int nbEqual = percent / 2;
		int nbSpace = 50 - nbEqual;

		return "[" + new String(new char[nbEqual]).replace("\0", "=")
				+ new String(new char[nbSpace]).replace("\0", " ") + "]";
	}

	public int getSpeed(long size, long start, long end) {
		if (end - start == 0) {
			return -1;
		}
		return (int) (size / (end - start));
	}

	public String getRemainingTime(long size, long dl, int speed) {

		speed = speed * 1000;

		long remaining = size - dl;

		if (speed == 0) {
			return "XX:XX:XX";
		}

		int remainingSec = (int) remaining / speed;

		int hours = (int) remainingSec / 3600;
		int minutes = (int) (remainingSec % 3600) / 60;
		int seconds = (int) remainingSec % 60;

		return String.format("%02d:%02d:%02d", hours, minutes, seconds);

	}

	@Override
	public void end() {
		System.out.print("\n");
		endTime = System.currentTimeMillis();
	}

	@Override
	public void init(int arg0, String arg1, String arg2, long arg3) {
		size = arg3;
		startTime = System.currentTimeMillis();
		thresholdTime = startTime;
	}

}
