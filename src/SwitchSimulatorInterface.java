import java.util.function.Consumer;

public interface SwitchSimulatorInterface {

	public interface FrameInterface {
		public String getDestination();

		public String getSource();
	}

	/**
	 * Metoda ustawia liczbe portow urzadzenia
	 * 
	 * @param ports
	 *            liczba portow
	 */
	void setNumberOfPorts(int ports);

	/**
	 * Czas po jakim wpis wiazący adres MAC i numer portu przełacznika jest
	 * usuwany z pamieci urzadzenia.
	 * 
	 * @param msec
	 *            maksymalny czas utrzymania nieodswiezanego wpisu
	 */
	void setAgingTime(long msec);

	/**
	 * Metoda wskazuje na podlaczenie do portu wirtualnego kabla
	 * 
	 * @param port
	 *            numer portu, do ktorego kabel jest podlaczany
	 * @param frameConsumer
	 *            wirtualny kabel przyjmujacy ramki
	 */
	void connect(int port, Consumer<FrameInterface> frameConsumer);

	/**
	 * Metoda wprowadza do portu nowa ramke. Metoda dla danego portu dziala
	 * sekwencyjnie - przed zakonczeniem poprzedniego wykonania nie zostanie
	 * rozpoczete nowe. Metoda nie powinna jednak blokowac zbyt dlugo
	 * uzytkownika - switch ma dzialac efektywnie!
	 * 
	 * @param port
	 *            numer portu, na ktory ramka przychodzi
	 * @param frame
	 *            ramka, ktora dociera do portu urzadzenia
	 */
	void insertFrame(int port, FrameInterface frame);

	/**
	 * Port o podanym numerze jest rozlaczany. Zadne ramki juz sie na nim nie
	 * pojawia (o ile nie zostanie ponownie podlaczony). Zadne ramki nie moga
	 * byc na ten port przez switch wyslane.
	 * 
	 * @param port
	 *            numer portu
	 */
	void disconnect(int port);
}
