
public class FramesCountingConsumer extends FrameConsumer {
	@Override
	public void accept(SwitchSimulatorInterface.FrameInterface f) {
		if ( canCount.get() )
			framesReceivedTotalCounter.inc();
	}
}
