import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicBoolean;

public class FrameInserter implements Runnable {
	private List<Frame> framesToSend = new ArrayList<Frame>(1024);
	private int portNumber;
	private SwitchSimulatorInterface ssi;
	private long sleep;
	private AtomicBoolean endFlag = new AtomicBoolean(false);
	private long insertTimeCummulativeSum;
	private int framesInserted;
	private CyclicBarrier barrier;

	public FrameInserter(SwitchSimulatorInterface ssi, int port) {
		this( ssi, port, null );
	}
	
	public FrameInserter(SwitchSimulatorInterface ssi, int port, CyclicBarrier barrier ) {
		portNumber = port;
		this.ssi = ssi;
		this.barrier = barrier;
	}
	
	public void setSleepTime( long s ) {
		sleep = s;
	}

	public void addFrame(Frame f) {
		framesToSend.add(f);
	}

	public int getNumberOfFrames() {
		return framesToSend.size();
	}
	
	private void fastInserter() {
		for (Frame f : framesToSend) {
			ssi.insertFrame(portNumber, f);
		}
	}

	private void slowInserter() {
		for (Frame f : framesToSend) {
			insertTimeCummulativeSum += TimeHelper.executionTime( () -> ssi.insertFrame(portNumber, f) );
			framesInserted++;
			TimeHelper.sleep(sleep);
		}

	}

	public float getAvgInsertTime() {
		PMO_SystemOutRedirect.println( "getAvgInsertTime usrednione po " + framesInserted + " wynikach");
		return (float)insertTimeCummulativeSum / (float)framesInserted;
	}
	
	@Override
	public void run() {
		if ( barrier != null ) {
			try {
				barrier.await();
			} catch (InterruptedException | BrokenBarrierException e) {
				e.printStackTrace();
			}
		}
		if (sleep == 0) {
			fastInserter();
		} else {
			slowInserter();
		}
		endFlag.set(true);
	}

	public boolean hasFinished() {
		return endFlag.get();
	}

}
