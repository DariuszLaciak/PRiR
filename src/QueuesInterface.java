
public interface QueuesInterface {
	
	interface TaskInterface {
		/**
		 * Zwraca zadana liczbe rdzeni
		 * @return liczba rdzeni potrzebna do wykonania zadania
		 */
		int getRequiredCores();
		
		/**
		 * Zwraca w msec czas potrzebny do wykonania zadania
		 * @return czas w msec potrzebny od realizacji zadania
		 */
		long getRequiredTime();
		
		/**
		 * Metoda uruchamia obliczenia. Metoda blokuje wywolujacy ja watek
		 * do czasu zakonczenie obliczen.
		 *  
		 * @param cores liczba przyznanych rdzeni
		 * @param time czas w msec na wykonanie obliczen
		 */
		void execute( int cores, long time );
		
		/**
		 * Metoda informuje zadanie, ze przekracza przyznany czas obliczen i powinno
		 * sie juz zakonczyc. Metoda moze byc blokujaca. Metoda nie gwarantuje
		 * powodzenia.
		 */
		void cancel();
	}
	
	/**
	 * Metoda konfiguruje kolejki systemu i globalny limit jednoczesnie uzywanych
	 * rdzeni. Im nizszy numer kolejki tym jej priorytet wyzszy.
	 * Metoda wykonywana przed wprowadzeniem do systemu pierwszego zadania. 
	 * 
	 * @param limits maksymalna liczba rdzeni uzywana do realizacji zadan przez dana kolejke
	 * @param globalLimit globalny limit jednoczesnie uzywanych rdzeni
	 */
	void configure( int[] limits, int globalLimit );
	
	/**
	 * Nieblokujaca metoda wprowadzania zadan do systemu. Zadanie reprezentowane jest przez
	 * obiekt zgodny z TaskInterface.
	 * @param queue numer kolejki, do ktorej zadanie ma trafic. Numeracja kolejek od 0 do 
	 * liczba kolejek-1
	 * @param task zadanie do wykonania
	 */
	void submitTask( int queue, TaskInterface task );
	
	/**
	 * Zwraca aktualna liczbe dostepnych na poziomie globalnym rdzeni. Zadanie, ktore
	 * zostanie wprowadzone do systemu i zadajace 
	 * zwroconej przez ta metode liczby rdzeni,
	 * bedzie uruchomione bezzwlocznie (o ile limit kolejki na to pozwala).
	 * Wynik uwzglednia zarowno aktualnie realizowane, jak i <b>skolejkowane</b> zadania.
	 * @return liczba aktualnie dostepnych w systemie rdzeni
	 */
	int getAvailableCores();

	/**
	 * Zwraca aktualna liczbe dostepnych w danej kolejce rdzeni. Zadanie, ktore
	 * zostanie wprowadzone do systemu do tej kolejki i zadajace 
	 * zwroconej przez ta metode liczby rdzeni,
	 * bedzie uruchomione bezzwlocznie.
	 * Wynik uwzglednia zarowno limit globalny, piorytet kolejki, 
	 * jak i wszystkie aktualnie realizowane i <b>skolejkowane</b> zadania.
	 * @param queue numer badanej kolejki
	 * @return liczba aktualnie dostepnych w systemie rdzeni
	 */
	int getAvailableCores( int queue );
}
