
// zadania chca po 2 rdzenie
// limity sa nieparzyste - nie wszystkie zadania moga sie realizowac

public class Test04 extends Test01 {
	private static final int[] TASKS_PER_QUEUE = new int[] { 6, 6, 6, 6, 6, 6, 6, 3, 3, 3 }; // liczby zadan
	private static final int[] CORES_LIMIT_PER_QUEUE = new int[] { 5, 5, 5, 5, 5, 5, 5, 3, 3, 3 }; // limity
	// rdzeni
	private static final int GLOBAL_CORES_LIMIT = 7 * 4 + 3*2 + 1;

	private static final int[] CONCURRENT_CORES_EXPECTED_USAGE = new int[] { 4, 4, 4, 4, 4, 4, 4, 2, 2, 2 };
	private static final int GLOBAL_CONCURRENT_CORES_EXPECTED_USAGE =  GLOBAL_CORES_LIMIT - 1;
	private static final int[] AVAIABLE_CORES_EXPECTED = new int[] { -1, -1, -1, -1, -1, -1, -1, -1, -1, -1 };
	private static final int GLOBAL_AVAIABLE_CORES_EXPECTED = -1; // limit nieparzysty, a zadania pobieraja po 2 rdzenie!
	
	// ujemne liczby oznaczaja warning zamiast bledu, choc w tym tescie 
	// sytuacja wydawala sie oczywista
	
	protected void generateTasks(int[] tasksPerQueue) {

		maxGlobalUsage = CountersFactory.prepareCommonMaxStorageCounter();
		globalUsage = CountersFactory.prepareCounterWithMaxStorageSet();

		for (int i = 0; i < tasksPerQueue.length; i++) {

			maxQueueUsage[i] = CountersFactory.prepareCommonMaxStorageCounter();
			queueUsage[i] = CountersFactory.prepareCounterWithMaxStorageSet();

			for (int j = 0; j < tasksPerQueue[i]; j++) {
				TestTask tt = new TestTask(i, 2, TASK_TIME, true, false); // zadania pobieraja po 2 rdzenie!
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

		TimeHelper.sleep((TASK_TIME * 7) / 2); // czekamy 3.5 * czas trwania
												// zadania, nie wszystkie
												// zadania mozna ruchomic
		commonFinishFlag.set(true); // koniec
		cpu.stop(); // koniec pomiaru uzycia CPU
	}
	
	public Test04( QueuesInterface qi ) {
		super( qi );
	}
	
	
}
