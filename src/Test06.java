import java.util.concurrent.atomic.AtomicBoolean;

public class Test06 implements Runnable {
	private QueuesInterface qi;

	public Test06(QueuesInterface qi) {
		this.qi = qi;
	}

	@Override
	public void run() {

		AtomicBoolean finish = new AtomicBoolean(false);

		TestTask t1 = new TestTask(0, 4, 2000, true, false);
		TestTask t2 = new TestTask(0, 4, 2000, true, false);
		TestTask t3 = new TestTask(0, 4, 2000, true, false);
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

		qi.configure(new int[] { 5, 5 }, 5); // dwie kolejki 2x5

		// zadanie do kolejki 1
		long it1 = TimeHelper.executionTime(() -> qi.submitTask(1, TaskProxy.getTask(t1)));

		// czekamy na uruchomienie zadania i blokade systemu z braku wolnych rdzeni
		while (maxT1c.get() != 4 ) {
			TimeHelper.sleep(1);
		}

		PMO_SystemOutRedirect.println("Task 01 has been started");

		// zadanie do kolejki 1
		long it2 = TimeHelper.executionTime(() -> qi.submitTask(1, TaskProxy.getTask(t2)));
		TimeHelper.sleep(100);
		// zadanie do kolejki 0, to sie powinno uruchomic
		long it3 = TimeHelper.executionTime(() -> qi.submitTask(0, TaskProxy.getTask(t3)));

		TimeHelper.sleep(3000); // powinno sie juz zakonczyc zadanie 1 i uruchomic, ale nie zakonczyc zadanie 3 (ma blokowac 2)

		finish.set(true);

		boolean result = true;

		if (maxGlobalUsage.get() != 4) {
			PMO_SystemOutRedirect.println("Blad: powinno byc 4 rdzeni uzytych rownoczesnie, bo tyle pobieraja zadania");
			PMO_SystemOutRedirect.println("Wynik: " + maxGlobalUsage.get() + " uzytych lacznie rdzeni");
			result = false;
		}

		if (maxT3c.get() != 4) {
			PMO_SystemOutRedirect.println("Blad: Zadanie 3 powinno byc wykonane, bo jego kolejka ma wyzszy priorytet");
			PMO_SystemOutRedirect.println("Wynik: " + maxT3c.get() + " uzytych rdzeni, spodziewano sie 4");
			result = false;
		}

		if (maxT2c.get() != 0) {
			PMO_SystemOutRedirect.println("Blad: Zadanie 2 nie powinno byc wykonane, bo jego kolejka ma nizszy priorytet");
			PMO_SystemOutRedirect.println("Wynik: " + maxT2c.get() + " uzytych rdzeni, spodziewno sie 0");
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
