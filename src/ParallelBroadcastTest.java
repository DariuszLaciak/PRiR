import java.util.Random;
import java.util.concurrent.CyclicBarrier;

public class ParallelBroadcastTest implements Runnable {

	private static final int PORTS = 6;
	private static final int MIN_FRAMES = 500;
	private static final int RANGE_FRAMES = 100;

	private SwitchSimulatorInterface ssi = new SwitchSimulator();
	private FrameInserter[] frameInserters = new FrameInserter[PORTS];
	private Thread[] threads = new Thread[PORTS];
	private FrameConsumer[] consumers = new FrameConsumer[PORTS];

	private void configureSwitch() {
		ssi.setNumberOfPorts(PORTS);
		ssi.setAgingTime(1000); // wartosc bez znaczenia
	}

	private void prepareInserters() {
		CyclicBarrier cb = new CyclicBarrier( PORTS );
		Random rnd = new Random();
		for (int port = 0; port < PORTS; port++) {
			frameInserters[port] = new FrameInserter(ssi, port, cb );
			int framesToGenerate = MIN_FRAMES + rnd.nextInt(RANGE_FRAMES);

			for (int j = 0; j < framesToGenerate; j++) {
				frameInserters[port].addFrame(new Frame(Consts.MAC_ADDRESSES[port], Consts.BROADCAST_MAC));
			}
		}
	}

	private void prepareThreads() {
		for (int port = 0; port < PORTS; port++) {
			threads[port] = new Thread(frameInserters[port]);
			threads[port].setDaemon(true);
		}
	}

	private void startThreads() {
		PMO_SystemOutRedirect.showCurrentMethodName();
		for (int port = 0; port < PORTS; port++)
			threads[port].start();
	}

	private void joinThreads() {
		PMO_SystemOutRedirect.showCurrentMethodName();
		for (int port = 0; port < PORTS; port++)
			try {
				threads[port].join();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	}

	private void prepareConsumers() {
		for (int port = 0; port < PORTS; port++) {
			consumers[port] = new FramesCountingConsumer();
			ssi.connect(port, consumers[port]);
		}
	}

	private boolean testIfAllFramesInserted() {
		for (int port = 0; port < PORTS; port++)
			if (!frameInserters[port].hasFinished())
				return false;
		return true;
	}

	private boolean testConsumers() {

		PMO_SystemOutRedirect.showCurrentMethodName();

		for (int port = 0; port < PORTS; port++) {
			int expected = 0;
			for (int portSrc = 0; portSrc < PORTS; portSrc++) {
				if (portSrc != port)
					expected += frameInserters[portSrc].getNumberOfFrames();
			}

			if (expected != consumers[port].getTotalNumberOfReceivedFrames()) {
				PMO_SystemOutRedirect.println("BLAD: odebrano na porcie " + port + " inna liczbe ramek niz oczekiwano");
				PMO_SystemOutRedirect.println("Odebrano  : " + consumers[port].getTotalNumberOfReceivedFrames());
				PMO_SystemOutRedirect.println("Oczekiwano: " + expected);
				return false;
			} else {
				PMO_SystemOutRedirect.println("Na porcie " + port + " odebrano "
						+ consumers[port].getTotalNumberOfReceivedFrames() + " ramek");
			}
		}

		return true;
	}

	private void cpuUsageTest() {

		PMO_SystemOutRedirect.println("Test obciazenia CPU gdy switch nic nie robi");

		CPUusage cpu = new CPUusage(1000);
		cpu.start();
		TimeHelper.sleep(5000);
		cpu.stop();
		float cpus = cpu.maxUsedCores();

		if (cpus > 0.2) {
			PMO_SystemOutRedirect
					.println("BLAD: Switch, ktory nie przekazuje zadnych ramek zajmuja soba " + cpus + " rdzenia CPU");
			Verdict.show(false);
		} else {
			Verdict.show(true);
		}
	}

	@Override
	public void run() {

		PMO_SystemOutRedirect.println("Test rownoleglego przekazywania ramek rozgloszeniowych");
		configureSwitch();

		prepareInserters();
		prepareConsumers();
		prepareThreads();
		startThreads();
		joinThreads();

		do {
			TimeHelper.sleep(1000);
		} while (!testIfAllFramesInserted()); // oczekiwanie na przeslanie
												// wszystkich ramek

		TimeHelper.sleep(2500); // ekstra czas na odebranie ramek

		Verdict.show(testConsumers());

		cpuUsageTest();

	}
}
