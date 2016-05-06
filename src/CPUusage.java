import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import java.lang.management.ManagementFactory;

/*
 * Go to Window-->Preferences-->Java-->Compiler-->Error/Warnings. Select
 * Deprecated and Restricted API. Change it to warning. Change forbidden
 * and Discouraged Reference and change it to warning. (or as your
 * need.)
 */

public class CPUusage {
	private AtomicInteger msecMax = new AtomicInteger(0);
	private long period;
	private long lastReading;

	private ScheduledExecutorService ses = Executors.newSingleThreadScheduledExecutor();
	private com.sun.management.OperatingSystemMXBean bean;

	private class Measurement implements Runnable {
		@Override
		public void run() {
			long newReading = bean.getProcessCpuTime() / 1000000L;
			int result = (int) (newReading - lastReading);

			if (result > msecMax.get()) {
				msecMax.set(result);
			}

			lastReading = newReading;
		}
	}

	public CPUusage(long period) {
		this.period = period;
		bean = (com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
	}

	public void start() {
		lastReading = bean.getProcessCpuTime() / 1000000L; // odczyt w nsec
		ses.scheduleAtFixedRate(new Measurement(), 0, period, TimeUnit.MILLISECONDS);
	}

	public void stop() {
		ses.shutdown();
	}

	public float maxUsedCores() {
		return (float) msecMax.get() / (float) period;
	}

	public static void main(String[] args) throws InterruptedException {

		CPUusage cpu = new CPUusage(1000);

		Thread th = new Thread(() -> {
			double sum = 0;
			for (long i = 0; i < 3000000000L; i++) {
				sum += i;
			}

			System.out.println(sum);
		});
		th.setDaemon(true);
		th.start();

		th = new Thread(() -> {
			double sum = 0;
			for (long i = 0; i < 5000000000L; i++) {
				sum += i;
			}

			System.out.println(sum);
		});
		th.setDaemon(true);
		th.start();

		th = new Thread(() -> {
			double sum = 0;
			for (long i = 0; i < 10000000000L; i++) {
				sum += i;
			}

			System.out.println(sum);
		});
		th.setDaemon(true);
		th.start();

		cpu.start();

		th.join();
		cpu.stop();

		System.out.println("Wynik " + cpu.maxUsedCores());
	}

}
