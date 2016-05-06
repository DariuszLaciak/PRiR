import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class FrameConsumer implements Consumer<SwitchSimulatorInterface.FrameInterface> {

	protected final AtomicCounter framesReceivedTotalCounter = new AtomicCounter();
	private final List<Frame> framesReceived = new ArrayList<>();
	// max liczba jednoczesnych uzyc Consumer-a
	private final AtomicCounter maxConcurrentCalls = CountersFactory.prepareCommonMaxStorageCounter();
	// licznik jednoczesnych uzyc Consumer-a
	private final AtomicCounter councurrentCallsCounter = CountersFactory.prepareCounterWithMaxStorageSet();

	private AtomicCounter globalConcurrentUsageCounter;
	
	protected AtomicBoolean canCount = new AtomicBoolean( true );
	
	volatile private long frameProcessingTime = 1; 
	private boolean printFrame = true;
	
	@Override
	public void accept(SwitchSimulatorInterface.FrameInterface f) {
		councurrentCallsCounter.incAndStoreMax();
		globalConcurrentUsageCounter.incAndStoreMax();
		
		framesReceivedTotalCounter.inc();
		synchronized (this) {
			if ( f != null ) {
				if ( printFrame )
					PMO_SystemOutRedirect.println( ">> >  Odebrano ramke : " + f.toString() );
				framesReceived.add(Frame.create(f));				
			} else {
				PMO_SystemOutRedirect.println( "Frame consumer otrzymal null zamiast ramki" );
			}
		}

		TimeHelper.sleep(frameProcessingTime);

		globalConcurrentUsageCounter.dec();
		councurrentCallsCounter.dec();
	}
	
	public void setCanCount(AtomicBoolean canCount) {
		this.canCount = canCount;
	}
	
	public void setPrintFrame(boolean printFrame) {
		this.printFrame = printFrame;
	}

	// czas obslugi ramki
	public void setFrameProcessingTime(long frameProcessingTime) {
		this.frameProcessingTime = frameProcessingTime;
	}
	
	public void setGlobalConcurrentUsageCounter(AtomicCounter globalConcurrnetUsageCounter) {
		this.globalConcurrentUsageCounter = globalConcurrnetUsageCounter;
	}
	
	// czy pojawila sie ta ramka
	public boolean wasFrameReceived( Frame f ) {
		synchronized ( this ) {
			return framesReceived.contains( f );
		}
	}
	
	public int getTotalNumberOfReceivedFrames() {
		return framesReceivedTotalCounter.get();
	}
	
	// uzyto w sposob rownolegly
	public boolean wasExecutedInParallel() {
		return maxConcurrentCalls.get() > 1;
	}

	// uzyto Consumer-a
	public boolean wasExecuted() {
		return maxConcurrentCalls.get() > 0;
	}
	
	// kasowanie historii odebranych ramek
	public void clearFramesHistory() {
		synchronized ( this ) {
			framesReceived.clear();
		}
	}

}
