import java.util.Random;

public class InsertFrameParallelTester implements Runnable {
	private static final int PORTS = 10;
	private static final int FRAMES_USED_IN_TEST = 1000;
	private static final int DELAY_BETWEEN_FRAMES = 1; // msec
	private SwitchSimulatorInterface ssi = new SwitchSimulator();

	private Frame[] frames = new Frame[PORTS];
	private float[] results = new float[PORTS];

	private void configureSwitch() {
		ssi.setNumberOfPorts(PORTS);
		ssi.setAgingTime(1000); // wartosc bez znaczenia
	}

	private void prepareConsumers() {
		for (int port = 0; port < PORTS; port++) {
			ssi.connect(port, (f) -> {
			}); // nic nie robie z ramka!
		}
	}

	private void generateFrames() {
		for (int i = 0; i < PORTS; i++) {
			frames[i] = new Frame(Consts.MAC_ADDRESSES[i], Consts.MAC_ADDRESSES[i + 1]);
		}
	}

	private FrameInserter prepareTestInserter() {
		FrameInserter fi = new FrameInserter(ssi, 0);
		Random rnd = new Random();
		for (int i = 0; i < FRAMES_USED_IN_TEST; i++) {
			fi.addFrame(
					new Frame(Consts.MAC_ADDRESSES[0], Consts.MAC_ADDRESSES[rnd.nextInt(Consts.MAC_ADDRESSES.length)]));
		}
		fi.setSleepTime(DELAY_BETWEEN_FRAMES);
		return fi;
	}

	private FrameInserter prepareEndlessInserter(int port) {
		FrameInserter fi = new FrameInserter(ssi, port) {
			public void run() {
				do {
					ssi.insertFrame(port, frames[port]);
				} while (true);
			};
		};
		return fi;
	}

	private Thread startThread(Runnable run) {
		Thread th = new Thread(run);
		th.setDaemon(true);
		th.start();
		return th;
	}

	private float runTest() {
		FrameInserter fi = prepareTestInserter();
		Thread th = startThread(fi);
		long start = System.currentTimeMillis();
		try {
			th.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		PMO_SystemOutRedirect.println("Join trwalo          : " + (System.currentTimeMillis() - start) + " msec");
		PMO_SystemOutRedirect.println("Praca netto          : "
				+ (System.currentTimeMillis() - start - DELAY_BETWEEN_FRAMES * FRAMES_USED_IN_TEST) + " msec");
		PMO_SystemOutRedirect.println("Praca netto per frame: "
				+ ((System.currentTimeMillis() - start - DELAY_BETWEEN_FRAMES * FRAMES_USED_IN_TEST)
						/ (float) FRAMES_USED_IN_TEST)
				+ " msec");
		return fi.getAvgInsertTime();
	}

	@Override
	public void run() {
		PMO_SystemOutRedirect.println("Test opoznien przy rownoczesnym nadawaniu ramek");
		configureSwitch();
		prepareConsumers();
		generateFrames();

		results[0] = runTest();
		// dodawane sa kolejne generatory ramek - pomiar tylko dla portu 0
		for (int i = 1; i < PORTS; i++) {
			startThread(prepareEndlessInserter(i));
			results[i] = runTest();
		}

		for (int i = 0; i < PORTS; i++)
			PMO_SystemOutRedirect.println("Wynik pomiaru: " + i + " -> " + results[i] + " msec");
	}

}
