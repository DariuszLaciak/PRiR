import java.util.concurrent.atomic.AtomicBoolean;

public class Test05 implements Runnable {
	private QueuesInterface qi;

	public Test05(QueuesInterface qi) {
		this.qi = qi;
	}

	@Override
	public void run() {

		AtomicBoolean finish = new AtomicBoolean(false);

		TestTask t1 = new TestTask(0, 4, 2000, true, false);
		TestTask t2 = new TestTask(0, 2, 2000, true, false);
		TestTask t3 = new TestTask(0, 1, 2000, true, false);
		t1.setFinishFlag(finish);
		t2.setFinishFlag(finish);
		t3.setFinishFlag(finish);

		AtomicCounter maxGlobalUsage = CountersFactory.prepareCommonMaxStorageCounter();
		AtomicCounter globalUsage = CountersFactory.prepareCounterWithMaxStorageSet();

		AtomicCounter maxT1c = CountersFactory.prepareCommonMaxStorageCounter();
		AtomicCounter t1c = CountersFactory.prepareCounterWithMaxStorageSet();

		AtomicCounter maxT2c = CountersFactory.prepareCommonMaxStorageCounter();
		AtomicCounter t2c = CountersFactory.prepareCounterWithMaxStorageSet();

		AtomicCounter maxT3c = CountersFactory.prepareCommonMaxStorageCounter();
		AtomicCounter t3c = CountersFactory.prepareCounterWithMaxStorageSet();

		t1.setQueueAccumulator(t1c);
		t2.setQueueAccumulator(t2c);
		t3.setQueueAccumulator(t3c);

		t1.setTotalAccumulator(globalUsage);
		t2.setTotalAccumulator(globalUsage);
		t3.setTotalAccumulator(globalUsage);

		qi.configure(new int[] { 5 }, 5);

		long it1 = TimeHelper.executionTime(() -> qi.submitTask(0, TaskProxy.getTask(t1)));

		// czekamy na uruchomienie zadania
		while (t1c.get() != 4 ) {
			TimeHelper.sleep(1);
		}

		PMO_SystemOutRedirect.println("Task 01 has been started");

		long it2 = TimeHelper.executionTime(() -> qi.submitTask(0, TaskProxy.getTask(t2)));
		TimeHelper.sleep(100);
		long it3 = TimeHelper.executionTime(() -> qi.submitTask(0, TaskProxy.getTask(t3)));

		TimeHelper.sleep(1000);

		finish.set(true);

		TimeHelper.sleep(2000); // tak na wszelki wypadek...

		boolean result = true;

		if (maxGlobalUsage.get() != 5) {
			PMO_SystemOutRedirect.println("Blad: powinno byc 5 rdzeni uzytych rownoczesnie, bo tyle jest wolnych zasobow");
			PMO_SystemOutRedirect.println("Wynik: " + maxGlobalUsage.get() + " uzytych lacznie rdzeni");
			result = false;
		}

		if (maxT3c.get() != 1) {
			PMO_SystemOutRedirect.println("Blad: Zadanie 3 powinno byc wykonane");
			PMO_SystemOutRedirect.println("Wynik: " + maxT3c.get() + " uzytych rdzeni");
			result = false;
		}

		if (maxT2c.get() != 0) {
			PMO_SystemOutRedirect.println("Blad: Zadanie 2 nie powinno byc wykonane");
			PMO_SystemOutRedirect.println("Wynik: " + maxT2c.get() + " uzytych rdzeni");
			result = false;
		}

		long sumT = it1 + it2 + it3;

		PMO_SystemOutRedirect.println("Sumaryczny czas wprowadzenia zadan 1, 2 i 3 do systemu to " + sumT + " msec");
		if (sumT > 50) {
			PMO_SystemOutRedirect.println("ten czas jest za dlugi..."); 
			result = false;
		}

		Test01.verdict(result);

	}

}
