import java.util.Collections;

public class Test02 extends Test01 {

	private static final int[] TASKS_PER_QUEUE = new int[] { 10, 10, 10, 10, 10, 10, 10, 10, 10, 10 }; // 10x10
	// zadan
	private static final int[] CORES_LIMIT_PER_QUEUE = new int[] { 5, 6, 5, 6, 5, 6, 5, 6, 5, 6 }; // limity
	// rdzeni
	private static final int GLOBAL_CORES_LIMIT = 111;

	private static final int[] CONCURRENT_CORES_EXPECTED_USAGE = CORES_LIMIT_PER_QUEUE;
	private static final int GLOBAL_CONCURRENT_CORES_EXPECTED_USAGE = 25+30;
	private static final int[] AVAIABLE_CORES_EXPECTED = new int[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
	private static final int GLOBAL_AVAIABLE_CORES_EXPECTED = -11; 
	// skolejkowano (czesc uruchomiono) zadan na 100 rdzeni -> 11
	// inni uwazaja, ze 111-liczba uruchomionych -> 56

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
		TimeHelper.sleep((TASK_TIME * 5) / 2); // czekamy 2.5 * czas trwania
												// zadania, nie wszystkie zadania mozna ruchomic
		commonFinishFlag.set(true); // koniec
		cpu.stop(); // koniec pomiaru uzycia CPU
	}

	public Test02(QueuesInterface qi) {
		super(qi);
	}
}
