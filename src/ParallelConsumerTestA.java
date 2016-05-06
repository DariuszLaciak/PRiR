import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CyclicBarrier;

public class ParallelConsumerTestA implements Runnable {

	protected static final int PORTS = 8;
	protected static final int FRAMES_USED_IN_TEST = 250;
	private static final int DELAY_BETWEEN_FRAMES = 1; // msec
	private static final int CONSUMER_DELAY = 25;

	private SwitchSimulatorInterface ssi = new SwitchSimulator();
	protected List<List<Frame>> frames = new ArrayList<>();
	private List<FrameConsumer> consumers = new ArrayList<>();
	private List<FrameInserter> inserters = new ArrayList<>();
	private Thread[] threads = new Thread[PORTS];

	private AtomicCounter maxConcurrentSwitchUsage = CountersFactory.prepareCommonMaxStorageCounter();
	private AtomicCounter concurrentSwitchUsage = CountersFactory.prepareCounterWithMaxStorageSet();

	private void configureSwitch() {
		ssi.setNumberOfPorts(PORTS);
		ssi.setAgingTime(1000); // wartosc bez znaczenia
	}

	protected void prepareFrames() {
		for (int port = 0; port < PORTS; port++) {
			List<Frame> frames4port = new ArrayList<>();

			// A->B, B->C, ... X->A
			for (int i = 0; i < FRAMES_USED_IN_TEST; i++) {
				frames4port.add(new Frame(Consts.MAC_ADDRESSES[port], Consts.MAC_ADDRESSES[(port + 1) % PORTS]));
			}

			frames.add(frames4port);
		}
	}

	private void prepareInserters() {
		CyclicBarrier cb = new CyclicBarrier(PORTS, new Runnable() {

			@Override
			public void run() {
				PMO_SystemOutRedirect.println("Sending frames...");
			}
		});
		for (int port = 0; port < PORTS; port++) {
			FrameInserter fi = new FrameInserter(ssi, port, cb);
			fi.setSleepTime(DELAY_BETWEEN_FRAMES);
			for (Frame f : frames.get(port)) {
				fi.addFrame(f);
			}
			inserters.add(port, fi);
		}
	}

	private void prepareThreads() {
		for (int port = 0; port < PORTS; port++) {
			threads[port] = new Thread(inserters.get(port));
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
			FrameConsumer fc = new FrameConsumer();
			fc.setPrintFrame(false);
			fc.setFrameProcessingTime(CONSUMER_DELAY);
			fc.setGlobalConcurrentUsageCounter(concurrentSwitchUsage);
			consumers.add(fc);
			ssi.connect(port, fc);
		}
	}

	private void clearConsumers() {
		consumers.forEach(x -> x.clearFramesHistory());
	}

	private void sendBroadcastFrames() {
		for (int port = 0; port < PORTS; port++) {
			ssi.insertFrame(port, new Frame(Consts.MAC_ADDRESSES[port], Consts.BROADCAST_MAC));
		}
		TimeHelper.sleep(250);
		clearConsumers();
	}

	protected boolean findFramesTest(int srcPort, int dstPort) {
		for (Frame f : frames.get(srcPort)) {
			if (!consumers.get(dstPort).wasFrameReceived(f)) {
				PMO_SystemOutRedirect.println("BLAD: Konsument dla portu " + dstPort + " nie otrzymal ramki " + f);
				return false;
			}
		}
		return true;
	}

	protected boolean findFramesTest() {
		boolean result = true;

		for (int port = 0; port < PORTS; port++) {
			if (port == 0) {
				result &= findFramesTest(PORTS - 1, 0);
			} else {
				result &= findFramesTest(port - 1, port);
			}
		}

		return result;

	}

	protected boolean test() {
		boolean result = true;
		if (maxConcurrentSwitchUsage.get() < PORTS / 2) {
			PMO_SystemOutRedirect
					.println("BLAD: Nie zaobserwowano rownoczesnego wysylania ramek na porty przelacznika");
			PMO_SystemOutRedirect.println("Limit " + PORTS / 2);
			PMO_SystemOutRedirect.println("Wynik " + maxConcurrentSwitchUsage.get());
			result = false;
		} else {
			PMO_SystemOutRedirect
					.println("Zaobserwowano " + maxConcurrentSwitchUsage.get() + " jednoczesnych wywolan konsumerow");
		}

		for (int port = 0; port < PORTS; port++) {
			if (!consumers.get(port).wasExecuted()) {
				PMO_SystemOutRedirect.println("BLAD: na port " + port + " nie docieraly ramki");
				result = false;
			}

			if (consumers.get(port).wasExecutedInParallel()) {
				PMO_SystemOutRedirect.println("BLAD: konsumer ramek dla portu " + port + " zostal uzyty wspolbieznie");
				result = false;
			}

		}

		result &= findFramesTest();

		return result;

	}

	@Override
	public void run() {
		configureSwitch();
		prepareConsumers();
		sendBroadcastFrames();
		prepareFrames();
		prepareInserters();
		prepareThreads();
		startThreads();
		joinThreads();
		TimeHelper.sleep(FRAMES_USED_IN_TEST * CONSUMER_DELAY * 2); // troche
																	// czasu na
																	// odebranie
																	// ramek
		Verdict.show(test());
	}

}
