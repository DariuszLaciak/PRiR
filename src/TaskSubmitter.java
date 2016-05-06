import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicBoolean;

public class TaskSubmitter implements Runnable {

	private QueuesInterface.TaskInterface taskToSubmit;
	private AtomicBoolean globalFinishFlag;
	private CyclicBarrier barrier;
	private volatile boolean taskSubmitted;
	private volatile long msec;
	private QueuesInterface interfaceToSystem;
	private int queue;

	public TaskSubmitter(TestTask tt, CyclicBarrier barrier, AtomicBoolean finishFlag,
			QueuesInterface interfaceToSystem) {
		this.barrier = barrier;
		taskToSubmit = TaskProxy.getTask(tt);
		queue = tt.getQueue();
		this.interfaceToSystem = interfaceToSystem;
		globalFinishFlag = finishFlag;
	}

	@Override
	public void run() {
		try {
			barrier.await();
			msec = TimeHelper.executionTime(() -> {
				interfaceToSystem.submitTask(queue, taskToSubmit);
			});
			if (globalFinishFlag.get())
				return; // zadanie nawet jesli dotarlo do kolejki to juz po
						// czasie
			taskSubmitted = true;
		} catch (InterruptedException | BrokenBarrierException e) {
			e.printStackTrace();
		}
	}

	public boolean hasBeenSumitted() {
		return taskSubmitted;
	}

	public long summisionTime() {
		return msec;
	}

}
