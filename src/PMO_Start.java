import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

import org.omg.CORBA.IntHolder;

public class PMO_Start {

	private final static int SEMAPHORES = 100;
	private final static int REPETITIONS = 5;

	private Sygnalizatory si;

	public PMO_Start(String[] args) {
		si = PMO_Narrow.get(args);

		if (si == null) {
			PMO_SystemOutRedirect.println("BLAD: nie udalo sie polaczyc z serwisem");
			System.exit(0);
		}
	}

	private List<PMO_Sygnalizator> semaphores = new ArrayList<>();
	private List<Runnable> tasks = new ArrayList<>();
	private List<Thread> threads = new ArrayList<>();

	private class Registration implements Runnable {
		CyclicBarrier cb;
		PMO_Sygnalizator semaphoreToRegister;

		public Registration(CyclicBarrier cb, PMO_Sygnalizator str) {
			this.cb = cb;
			semaphoreToRegister = str;
		}

		@Override
		public void run() {
			IntHolder ih = new IntHolder(0);
			try {
				cb.await();
			} catch (InterruptedException | BrokenBarrierException e) {
				e.printStackTrace();
			}

			si.rejestracja(semaphoreToRegister.getTypSygnalizatora(), semaphoreToRegister.getCzasy(), ih);

			semaphoreToRegister.setId(ih.value);
		}

	}

	private void prepareSemaphores() {
		for (int i = 0; i < SEMAPHORES; i++)
			semaphores.add(new PMO_Sygnalizator());
	}

	private void prepareRegistrationTasks() {
		CyclicBarrier cb = new CyclicBarrier(SEMAPHORES);
		for (int i = 0; i < SEMAPHORES; i++) {
			tasks.add(new Registration(cb, semaphores.get(i)));
		}
	}

	private void prepareAndStartThreads() {
		PMO_SystemOutRedirect.println("Uruchamiamy " + tasks.size() + " watkow");
		for (int i = 0; i < tasks.size(); i++) {
			Thread th = new Thread(tasks.get(i));
			th.setDaemon(true);
			th.start();
			threads.add(th);
		}
	}

	private void joinThreads(long maxWait) {
		long start = 0;
		for (Thread th : threads) {
			start = System.currentTimeMillis();
			do {
				try {
					th.join(500);
				} catch (InterruptedException e) {
					PMO_SystemOutRedirect.println("W trakcie join() wystapil wyjatek " + e.getLocalizedMessage());
				}
			} while (th.isAlive() && ((System.currentTimeMillis() - start) < maxWait));
		}
		PMO_SystemOutRedirect.println("Koniec join");
		if ((System.currentTimeMillis() - start) > maxWait) {
			PMO_SystemOutRedirect
					.println("Przekroczono limit czasu ( " + maxWait + " msec) przeznaczonego na wykonanie testu");
		}

	}

	private boolean testUniqID() {
		Set<Integer> idSet = new TreeSet<>();

		for (PMO_Sygnalizator s : semaphores) {
			if (!idSet.add(s.getId())) {
				PMO_SystemOutRedirect.println("BLAD KRYTYCZNY: identyfikatory sygnalizatorow nie sa unikalne");
				PMO_SystemOutRedirect.println("BLAD KRYTYCZNY: nie bedzie mozna ich od siebie odroznic");
				return false;
			}
		}
		return true;
	}

	public static void main(String[] args) {
		PMO_SystemOutRedirect.startRedirectionToNull();
		boolean result = false;
		int i;
		for (i = 0; i < REPETITIONS; i++) {
			PMO_Start start = new PMO_Start(args);

			start.prepareSemaphores();
			start.prepareRegistrationTasks();
			start.prepareAndStartThreads();
			start.joinThreads(2000); // czas na odebranie zadan

			if (i == (REPETITIONS - 1)) { // ten test tylko 1x
				PMO_SystemOutRedirect.println("Test poprawnosci dzialania blokady wzbudzenia - 1x");
				result &= PMO_TimeTest.waitingForGreenBlocadeTest(start.si, start.semaphores);
			} else {
				PMO_SystemOutRedirect.println("Test unikalnosci ID");
				result = start.testUniqID();
				if (!result) {
					Verdict.show(false);
					break;
				}

				PMO_SystemOutRedirect.println("Test poprawnosci sekwencji kolorow");
				result = PMO_TimeTest.test(start.si, start.semaphores);
				if (!result) {
					Verdict.show(false);
					break;
				}

				PMO_SystemOutRedirect.println("Test poprawnosci wzbudzenia w obrebie jednego cyklu");
				result = PMO_TimeTest.waitingForGreenTest(start.si, start.semaphores);
				if (!result) {
					Verdict.show(false);
					break;
				}
			}

		}

		if (i == REPETITIONS) {
			PMO_SystemOutRedirect.println("KONCOWY WERDYKT:");
			Verdict.show(result);
		}

		PMO_SystemOutRedirect.println("");
		PMO_SystemOutRedirect.println("------  THE END  ------");
		PMO_SystemOutRedirect.println("");

		System.exit(0);
		java.lang.Runtime.getRuntime().halt(0);

	}

}
