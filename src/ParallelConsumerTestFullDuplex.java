import java.util.ArrayList;
import java.util.List;

public class ParallelConsumerTestFullDuplex extends ParallelConsumerTestA {

	@Override
	protected void prepareFrames() {
		for (int portP = 0; portP < PORTS / 2; portP++) {
			List<Frame> frames4portA = new ArrayList<>();
			List<Frame> frames4portB = new ArrayList<>();

			int port = portP * 2;

			// A->B, B->A,C->D, D->C itd.
			for (int i = 0; i < FRAMES_USED_IN_TEST; i++) {
				frames4portA.add(new Frame(Consts.MAC_ADDRESSES[port], Consts.MAC_ADDRESSES[port + 1]));
				frames4portB.add(new Frame(Consts.MAC_ADDRESSES[port + 1], Consts.MAC_ADDRESSES[port]));
			}

			frames.add(frames4portA);
			frames.add(frames4portB);
		}

	}

	@Override
	protected boolean findFramesTest() {
		boolean result = true;

		for (int port = 0; port < PORTS/2; port++) {
			result &= findFramesTest(2*port, 2*port+1);
			result &= findFramesTest(2*port+1, 2*port);
		}

		return result;

	}

}
