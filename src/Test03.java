
public class Test03 extends Test01 {

	private static final int[] TASKS_PER_QUEUE = new int[] { 11, 11, 11, 11, 11, 11, 11, 3, 3, 3 }; // liczby zadan
	private static final int[] CORES_LIMIT_PER_QUEUE = new int[] { 5, 5, 5, 5, 5, 5, 5, 3, 3, 3 }; // limity
	// rdzeni
	private static final int GLOBAL_CORES_LIMIT = 7 * 5 + 3*3;

	private static final int[] CONCURRENT_CORES_EXPECTED_USAGE = CORES_LIMIT_PER_QUEUE;
	private static final int GLOBAL_CONCURRENT_CORES_EXPECTED_USAGE = GLOBAL_CORES_LIMIT;
	private static final int[] AVAIABLE_CORES_EXPECTED = new int[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
	private static final int GLOBAL_AVAIABLE_CORES_EXPECTED = 0; // liczba zadan > limit

	protected void generateTasks(int[] tasksPerQueue) {

		maxGlobalUsage = CountersFactory.prepareCommonMaxStorageCounter();
		globalUsage = CountersFactory.prepareCounterWithMaxStorageSet();

		for (int i = 0; i < tasksPerQueue.length; i++) {

			maxQueueUsage[i] = CountersFactory.prepareCommonMaxStorageCounter();
			queueUsage[i] = CountersFactory.prepareCounterWithMaxStorageSet();

			// 3 zadania blokujace w kazdej z kolejek
			for (int j = 0; j < 3; j++) {
				TestTask tt = new TestTask(i, 1, TASK_TIME, false, true); // zadanie sie nie konczy, a wywolnie calcel blokuje watek
				tt.setFinishFlag(commonFinishFlag);
				tt.setQueueAccumulator(queueUsage[i]);
				tt.setTotalAccumulator(globalUsage);
				tasks.add(tt);
			}

			for (int j = 0; j < tasksPerQueue[i] - 3; j++) {
				TestTask tt = new TestTask(i, 1, TASK_TIME, true, false);
				tt.setFinishFlag(commonFinishFlag);
				tt.setQueueAccumulator(queueUsage[i]);
				tt.setTotalAccumulator(globalUsage);
				tasks.add(tt);
			}

		}
	}

	protected void testProcedure() {
		// 10 kolejek o takich samych limitach
		generateTasks(TASKS_PER_QUEUE);
		configureQueueSystem(CORES_LIMIT_PER_QUEUE, GLOBAL_CORES_LIMIT);
		prepareMaxCounters(CONCURRENT_CORES_EXPECTED_USAGE, GLOBAL_CONCURRENT_CORES_EXPECTED_USAGE,
				AVAIABLE_CORES_EXPECTED, GLOBAL_AVAIABLE_CORES_EXPECTED);
		generateSubmitters();
		startThreads();
		TimeHelper.sleep( 1000 );
		cpu.start(); // start pomiaru uzycia CPU

		// najgorszy scenariusz: rusza 5 zadan z czego 3 blokujace. 5 zadan -
		// TASK_TIME,
		// pozostale 6 zadan w 3 turach po 2. 3x TASK_TIME

		TimeHelper.sleep((TASK_TIME * 9) / 2); // czekamy 4.5 * czas trwania
												// zadania, nie wszystkie
												// zadania mozna ruchomic
		commonFinishFlag.set(true); // koniec
		cpu.stop(); // koniec pomiaru uzycia CPU
	}

	public Test03(QueuesInterface qi) {
		super(qi);
	}
}
