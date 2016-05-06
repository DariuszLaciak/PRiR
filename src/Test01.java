import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Wystarczajaca liczba zasobow do uruchomienia wszystkich zadan jednoczesnie.
 * Zadanie sie nie blokuja.
 *
 */
public class Test01 implements Runnable {

	private static final int[] TASKS_PER_QUEUE = new int[] { 10, 10, 10, 10, 10, 10, 10, 10, 10, 10 }; // 10x10
																										// zadan
	private static final int[] CORES_LIMIT_PER_QUEUE = new int[] { 11, 12, 13, 14, 15, 16, 17, 18, 19, 20 }; // limity
																												// rdzeni
	private static final int GLOBAL_CORES_LIMIT = 111;

	private static final int[] CONCURRENT_CORES_EXPECTED_USAGE = TASKS_PER_QUEUE;
	private static final int GLOBAL_CONCURRENT_CORES_EXPECTED_USAGE = 100;
	private static final int[] AVAIABLE_CORES_EXPECTED = new int[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 };
	private static final int GLOBAL_AVAIABLE_CORES_EXPECTED = 11;

	static final long TASK_TIME = 5000;
	protected static final float CPU_USAGE_LIMIT = 0.3f;

	protected QueuesInterface qi;

	private AtomicBoolean getAvailableCoresGlobalOK = new AtomicBoolean(true);
	private AtomicBoolean getAvailableCoresQueueOK = new AtomicBoolean(true);

	protected AtomicBoolean commonFinishFlag = new AtomicBoolean(false);
	protected AtomicCounter globalUsage;
	protected AtomicCounter maxGlobalUsage;
	protected AtomicCounter queueUsage[] = new AtomicCounter[TASKS_PER_QUEUE.length];
	protected AtomicCounter maxQueueUsage[] = new AtomicCounter[TASKS_PER_QUEUE.length];

	protected CyclicBarrier synchronizationBarrier;
	protected List<Thread> threads = new ArrayList<>();

	protected List<TestTask> tasks = new ArrayList<>();
	protected List<TaskSubmitter> tasksSub = new ArrayList<>();

	protected CPUusage cpu;

	protected void generateTasks(int[] tasksPerQueue) {

		maxGlobalUsage = CountersFactory.prepareCommonMaxStorageCounter();
		globalUsage = CountersFactory.prepareCounterWithMaxStorageSet();

		for (int i = 0; i < tasksPerQueue.length; i++) {

			maxQueueUsage[i] = CountersFactory.prepareCommonMaxStorageCounter();
			queueUsage[i] = CountersFactory.prepareCounterWithMaxStorageSet();

			for (int j = 0; j < tasksPerQueue[i]; j++) {
				TestTask tt = new TestTask(i, 1, TASK_TIME, true, false);
				tt.setFinishFlag(commonFinishFlag);
				tt.setQueueAccumulator(queueUsage[i]);
				tt.setTotalAccumulator(globalUsage);
				tasks.add(tt);
			}

		}

	}

	/**
	 * Ustawienia licznikow zapamiatujacych maksymalne uzycia zasobow
	 * kolejek/systemu
	 * 
	 * @param concurrentCoresExpectedUsage
	 *            oczekiwana liczba uzytych rdzenie per kolejka
	 * @param globalconcurrentCoresExpectedUsage
	 *            oczekiwana liczba globalne uzytych rdzeni
	 * @param avaiableCoresExpected
	 *            poprawna liczba wolnych rdzeni na kolejke
	 * @param globalAvaiableCoreExpected
	 *            poprawna liczba globalnie wolnych rdzeni
	 */
	protected void prepareMaxCounters(int[] concurrentCoresExpectedUsage, int globalconcurrentCoresExpectedUsage,
			int[] avaiableCoresExpected, int globalAvaiableCoreExpected) {

		for (int i = 0; i < concurrentCoresExpectedUsage.length; i++) {
			Integer ii = new Integer(i);
			PMO_SystemOutRedirect.println("Oczekiwana liczba jednoczesnie uzywanych w kolejce " + ii + " rdzeni to "
					+ concurrentCoresExpectedUsage[ii]);
			maxQueueUsage[i].setOKPredicate(k -> k == concurrentCoresExpectedUsage[ii]);
			maxQueueUsage[i].setFailPredicate(k -> k != concurrentCoresExpectedUsage[ii]);
			maxQueueUsage[i].setAutoRun(concurrentCoresExpectedUsage[ii], () -> {
				int coresA = qi.getAvailableCores(ii);
				if (coresA != Math.abs(avaiableCoresExpected[ii])) {
					if (avaiableCoresExpected[ii] < 0) {
						PMO_SystemOutRedirect.println("To tylko warning...");
					}
					PMO_SystemOutRedirect
							.println("System zeznaje inna niz spodziewana liczbe wolnych zasobow kolejki.");
					PMO_SystemOutRedirect.println("Oczekiwano " + Math.abs(avaiableCoresExpected[ii]));
					PMO_SystemOutRedirect.println("A jest     " + coresA);
					if (avaiableCoresExpected[ii] >= 0) {
						getAvailableCoresQueueOK.set(false);
					}
				}
			});

		}

		PMO_SystemOutRedirect
				.println("Spodziewane globalne uzycie rdzeni to:      " + globalconcurrentCoresExpectedUsage);
		PMO_SystemOutRedirect
				.println("Spodziewana wtedy liczba wolnych rdzeni to: " + Math.abs(globalAvaiableCoreExpected));

		maxGlobalUsage.setOKPredicate(k -> k == globalconcurrentCoresExpectedUsage);
		maxGlobalUsage.setFailPredicate(k -> k != globalconcurrentCoresExpectedUsage);

		maxGlobalUsage.setAutoRun(globalconcurrentCoresExpectedUsage, () -> {
			int coresA = qi.getAvailableCores();
			if (coresA != Math.abs(globalAvaiableCoreExpected)) {
				if (globalAvaiableCoreExpected < 0) {
					PMO_SystemOutRedirect.println("Warning: ");
				}
				PMO_SystemOutRedirect.println("System zeznaje inna niz spodziewana globalna liczbe wolnych zasobow.");
				PMO_SystemOutRedirect.println("Oczekiwano " + Math.abs(globalAvaiableCoreExpected));
				PMO_SystemOutRedirect.println("A jest     " + coresA);
				if (globalAvaiableCoreExpected >= 0)
					getAvailableCoresGlobalOK.set(false); // blad nie jest brany
															// pod uwage!
			}
		});
	}

	protected void configureQueueSystem(int[] tasksCoreLimit, int totalTasksCoreLimit) {
		qi.configure(tasksCoreLimit.clone(), totalTasksCoreLimit);
	}

	protected void generateSubmitters() {
		synchronizationBarrier = new CyclicBarrier(tasks.size(), () -> {
			PMO_SystemOutRedirect.println("-- Synchronization --");
		});

		for (int i = 0; i < tasks.size(); i++) {
			tasksSub.add(new TaskSubmitter(tasks.get(i), synchronizationBarrier, commonFinishFlag, qi));
		}
	}

	protected void startThreads() {
		PMO_SystemOutRedirect.showCurrentMethodName();
		for (int i = 0; i < tasksSub.size(); i++) {
			Thread th = new Thread(tasksSub.get(i));
			th.setDaemon(true);
			threads.add(th);
			th.start();
		}
		PMO_SystemOutRedirect.println("Watki wystartowaly");
	}

	protected boolean testTasks() {
		boolean result = true;
		PMO_SystemOutRedirect.showCurrentMethodName();
		for (TestTask tt : tasks) {
			result &= tt.test();
		}
		return result;
	}

	protected boolean testSubmitters() {
		boolean result = true;
		PMO_SystemOutRedirect.showCurrentMethodName();

		for (TaskSubmitter ts : tasksSub) {
			if (!ts.hasBeenSumitted()) {
				PMO_SystemOutRedirect.println("Nie wykonano (doczekano sie na wykonanie) submitTask");
				result = false;
			} else {
				if (ts.summisionTime() > 75) { // nie dluzej niz 75 msec
					PMO_SystemOutRedirect
							.println("Wykryto blokade wprowadzania zadania " + ts.summisionTime() + "msec");
					result = false;
				}
			}
		}

		return result;
	}

	protected boolean testMaxGlobalCounter() {
		boolean result = true;

		PMO_SystemOutRedirect.showCurrentMethodName();
		if (maxGlobalUsage.isFail().get()) {
			PMO_SystemOutRedirect.println("Globalne uzycie rdzeni przez zadania nie jest zgodne ze spodziewanym");
			PMO_SystemOutRedirect.println("Zmierzono jednoczesnie : " + maxGlobalUsage.get());
			result = false;
		}

		return result;

	}

	protected boolean testMaxQueueCounter(int queue) {
		boolean result = true;

		PMO_SystemOutRedirect.showCurrentMethodName();
		if (maxQueueUsage[queue].isFail().get()) {
			PMO_SystemOutRedirect.println("Bladne uzycie rdzeni przez zadania na poziomie kolejki " + queue);
			PMO_SystemOutRedirect.println("Zmierzono jednoczesnie : " + maxQueueUsage[queue].get());
			result = false;
		}

		return result;

	}

	protected boolean testAvailableCoresTestExecuted(int queue) {
		boolean result = true;

		PMO_SystemOutRedirect.showCurrentMethodName();
		if (!maxQueueUsage[queue].autorunExecuted()) {
			PMO_SystemOutRedirect
					.println("Test getAvailableCores po poziomie kolejek nie zostal wykonany dla kolejki " + queue);
			result = false;
		}

		return result;
	}

	protected boolean testAvailableCoresOnQueueLevel() {
		PMO_SystemOutRedirect.showCurrentMethodName();

		if (!getAvailableCoresQueueOK.get()) {
			PMO_SystemOutRedirect.println(
					"System zwrocil nieoczekiwana liczbe wolnych zasobow na poziomie jednej z kolejek kolejki");
			return false;
		}

		return true;
	}

	protected boolean testAvailableCoresOnGlobalLevel() {

		PMO_SystemOutRedirect.showCurrentMethodName();

		if (!maxGlobalUsage.autorunExecuted()) {
			PMO_SystemOutRedirect.println("Globalny test getAvailableCores nie zostal przeprowadzony");
			return false;
		} else {
			if (!getAvailableCoresGlobalOK.get()) {
				PMO_SystemOutRedirect.println("System zwrocil nieoczekiwana liczbe wolnych zasobow");
				return false;
			}
		}

		return true;
	}

	protected boolean cpuUsageTest(float limit) {
		float cores = cpu.maxUsedCores();
		boolean result = true;
		if (cores > limit) {
			PMO_SystemOutRedirect.println("Program uzyl do " + cores + " rdzeni. Zdecydowanie za duzo!");
			result = false;
		} else {
			PMO_SystemOutRedirect.println("Maksymalne zmierzone uzycie CPU to " + cores + " rdzeni");
		}
		return result;
	}

	protected boolean test() {
		boolean result = true;
		result &= testTasks();
		result &= testSubmitters();
		result &= testMaxGlobalCounter();
		for (int i = 0; i < TASKS_PER_QUEUE.length; i++) {
			result &= testMaxQueueCounter(i);
			result &= testAvailableCoresTestExecuted(i);
		}

		result &= testAvailableCoresOnGlobalLevel();
		result &= testAvailableCoresOnQueueLevel();

		result &= cpuUsageTest(CPU_USAGE_LIMIT);

		return result;
	}

	public static void verdict(boolean result) {
		if (result) {
			PMO_SystemOutRedirect.println("--------------------------------------------------------");
			PMO_SystemOutRedirect.println("--- NIE WYKRYTO BLEDU (co nie oznacza, ze go nie ma) ---");
			PMO_SystemOutRedirect.println("--------------------------------------------------------");
		} else {
			PMO_SystemOutRedirect.println("  ____  _        _    ____  ");
			PMO_SystemOutRedirect.println(" | __ )| |      / \\  |  _ \\ ");
			PMO_SystemOutRedirect.println(" |  _ \\| |     / _ \\ | | | |");
			PMO_SystemOutRedirect.println(" | |_) | |___ / ___ \\| |_| |");
			PMO_SystemOutRedirect.println(" |____/|_____/_/   \\_\\____/ ");
		}
	}

	protected void testProcedure() {
		// 10 kolejek o takich samych limitach
		generateTasks(TASKS_PER_QUEUE);
		configureQueueSystem(CORES_LIMIT_PER_QUEUE, GLOBAL_CORES_LIMIT);
		prepareMaxCounters(CONCURRENT_CORES_EXPECTED_USAGE, GLOBAL_CONCURRENT_CORES_EXPECTED_USAGE,
				AVAIABLE_CORES_EXPECTED, GLOBAL_AVAIABLE_CORES_EXPECTED);
		Collections.shuffle(tasks);
		generateSubmitters();
		startThreads();
		TimeHelper.sleep( 1000 );
		cpu.start(); // start pomiaru uzycia CPU
		TimeHelper.sleep((TASK_TIME * 3) / 2); // czekamy 1.5 * czas trwania
												// zadania
		commonFinishFlag.set(true); // koniec
		cpu.stop(); // koniec pomiaru uzycia CPU
	}

	@Override
	public void run() {
		cpu = new CPUusage(1000);

		testProcedure();

		verdict(test());
	}

	public Test01(QueuesInterface qi) {
		this.qi = qi;
	}

}
