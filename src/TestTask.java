import java.util.concurrent.atomic.AtomicBoolean;

public class TestTask implements QueuesInterface.TaskInterface {

	private AtomicCounter executions = CountersFactory.executeOnce();
	private AtomicCounter cancels;
	private AtomicCounter queueAccumulator;
	private AtomicCounter totalAccumulator;
	private int queue;
	private long msecRequest; // wymagany czas "pracy"
	private int cpusRequect; // wymagana liczba rdzeni
	private boolean finish; // zadanie prawidlowo sie zakonczy
	private boolean cancelBlock; // cancel takze blokuje watek
	private AtomicBoolean finishFlag; // flaga zakonczenia testu
	private int cpusReceived; // otrzymana liczba rdzeni
	private long timeReceived; // kolejka uzyta do uruchomienia zadania
	private long startTime; // czas uruchomienia zadania
	private long cancelTime; // czas wywolania cancel
	private boolean interruptedExceptionCatched;

	public TestTask(int queue, int cpus, long msec, boolean finish, boolean cancelBlock) {
		this.queue = queue;
		this.cpusRequect = cpus;
		this.finish = finish;
		this.msecRequest = msec;
		this.cancelBlock = cancelBlock;

		if (finish) {
			cancels = CountersFactory.neverExecute(); // zadanie sie prawidlowo
														// zakonczy, nie ma
														// powodu do uzycia
														// cancel
		} else {
			cancels = CountersFactory.executeOnce(); // zadanie sie nie
														// zakonczy, powinna byc
														// wywolanie cancel
		}
	}

	public int getQueue() {
		return queue;
	}

	public void setQueueAccumulator(AtomicCounter queueAccumulator) {
		this.queueAccumulator = queueAccumulator;
	}

	public void setTotalAccumulator(AtomicCounter totalAccumulator) {
		this.totalAccumulator = totalAccumulator;
	}

	public void setFinishFlag(AtomicBoolean finishFlag) {
		this.finishFlag = finishFlag;
	}

	private void neverWakeUp() {
		while (true) {
			try {
				Thread.sleep((long) (msecRequest * 0.1));
			} catch (InterruptedException e) {
				interruptedExceptionCatched = true;
				e.printStackTrace();
			}
		}
	}

	@Override
	public void cancel() {
		if (finishFlag.get())
			return; // testy uznany za zakonczony
		cancels.inc();

		cancelTime = System.currentTimeMillis();

		if (cancelBlock) {
			neverWakeUp();
		}

	}

	@Override
	public void execute(int cores, long time) {

		if (finishFlag.get()) {
			PMO_SystemOutRedirect.println( "Finish flag detected! Task will not be executed");
			return; // testy uznany za zakonczony
		}

		queueAccumulator.addAndStoreMax(cores);
		totalAccumulator.addAndStoreMax(cores);
		executions.inc();

		cpusReceived = cores;
		timeReceived = time;

		if (finish) {
			try {
				Thread.sleep((long) (msecRequest * 0.9));
			} catch (InterruptedException e) {
				interruptedExceptionCatched = true;
				e.printStackTrace();
			}
		} else {
			startTime = System.currentTimeMillis(); // pomiar czasu rozpoczecia
													// zadania
			neverWakeUp();
		}

		totalAccumulator.sub(cores);
		queueAccumulator.sub(cores);
	}

	@Override
	public int getRequiredCores() {
		return cpusRequect;
	}

	@Override
	public long getRequiredTime() {
		return msecRequest;
	}

	public boolean test() {

		boolean result = true;

		// niezgodnosc liczby rdzeni
		if (cpusReceived != cpusRequect) {
			PMO_SystemOutRedirect
					.println("BLAD: Zadanie potrzebowalo " + cpusRequect + " rdzeni, a otrzymalo " + cpusReceived);
			result = false;
		}

		// zniezgodnosci czasu pracy
		if (timeReceived != msecRequest) {
			PMO_SystemOutRedirect
					.println("BLAD: Zadanie potrzebowalo " + msecRequest + " msec czasu, a otrzymalo " + timeReceived);
			result = false;
		}

		// blad w wykonaniu execute (np. brak, lub wielokrotne)
		if (executions.isFail().get()) {
			PMO_SystemOutRedirect.println("Blad w wykonaniu execute. Naliczono " + executions.get() + " wywolan");
			result = false;
		}

		// blad w wykonaniu cancel (np. brak, lub wielokrotne)
		if (cancels.isFail().get()) {
			PMO_SystemOutRedirect.println("Blad w wykonaniu cancel. Naliczono " + cancels.get() + " wywolan");
			if (finish) {
				PMO_SystemOutRedirect.println("Cancel nie powinno zostac wykonane, bo zadanie konczy sie poprawnie");
			} else {
				PMO_SystemOutRedirect
						.println("Cancel powinno zostac wykonane, bo zadanie nie konczy sie w odpowienim czasie");
			}
			result = false;
		}

		// wywolano cancel. ale czy poprawnie?
		if (cancels.isOK().get() && ( !finish )) { // powinno byc wykonane cancel
			if (Math.abs(cancelTime - startTime - msecRequest) > 0.1 * msecRequest) {
				PMO_SystemOutRedirect.println("Czas wykonania cancel jest bledny. Rozbieznosc: "
						+ Math.abs(cancelTime - startTime - msecRequest) + " limit " + 0.1 * msecRequest);
				result = false;
			}
		}

		// przerwano sleep
		if (interruptedExceptionCatched) {
			PMO_SystemOutRedirect.println("W trakcie pracy zlapano wyjatek InterruptedException - nie pownno go byc");
			result = false;
		}

		return result;
	}

}
