public class TaskProxy implements QueuesInterface.TaskInterface {
	private final QueuesInterface.TaskInterface task;
	
	public TaskProxy( QueuesInterface.TaskInterface task ) {
		this.task = task;
	}
	
	public static QueuesInterface.TaskInterface getTask( QueuesInterface.TaskInterface task ) {
		return new TaskProxy( task );
	}
	
	@Override
	public void cancel() {
		task.cancel();
	}
	
	@Override
	public void execute(int cores, long time) {
		task.execute(cores, time);
	}
	
	@Override
	public int getRequiredCores() {
		return task.getRequiredCores();
	}
	
	@Override
	public long getRequiredTime() {
		return task.getRequiredTime();
	}
	
}
