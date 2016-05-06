import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.PriorityQueue;


class Queues implements QueuesInterface {

    private int[] limits;
    private int[] formerLimits;
    private int globalLimit;
    private int formerGlobalLimit;
    private Map<Integer, PriorityQueue<TaskRun>> queues = new HashMap<>();
    private Map<Integer, PriorityQueue<TaskRun>> buffer = new HashMap<>();
    private boolean addedTask = false;

    public Queues() {
        checkThreadStates();
    }

    @Override
    public void configure(int[] limits, int globalLimit) {
        this.limits = limits;
        this.globalLimit = globalLimit;
        formerGlobalLimit = globalLimit;
        // INIT
        int index = 0;
        formerLimits = new int[limits.length];
        for (int ignored : limits) {
            queues.put(index, new PriorityQueue<>());
            buffer.put(index, new PriorityQueue<>());
            formerLimits[index] = ignored;
            index++;
        }
    }

    private void checkThreadStates() {
//        while (true) {
            checkTasks();
            runNextTask();
//        }
    }

    @Override
    public synchronized void submitTask(int queue, TaskInterface task) {
        if (queue < limits.length) {
            if (getAvailableCores() >= task.getRequiredCores() && getAvailableCores(queue) >= task.getRequiredCores()) {
                addedTask = true;
                limits[queue] -= task.getRequiredCores();
                globalLimit -= task.getRequiredCores();
                queues.get(queue).add(new TaskRun(task));
            } else if (formerLimits[queue] >= task.getRequiredCores() && formerGlobalLimit >= task.getRequiredCores()) {
                buffer.get(queue).add(new TaskRun(task));
            }
        }
    }

    private void checkBuffer(int queue) {
        if (buffer.get(queue) != null)
            for (TaskRun task : buffer.get(queue)) {
                if (!task.wasRunning()) {
                    if (getAvailableCores() >= task.getUsedCores() && getAvailableCores(queue) >= task.getUsedCores()) {
                        limits[queue] -= task.getUsedCores();
                        globalLimit -= task.getUsedCores();
                        queues.get(queue).add(task);
                        buffer.get(queue).remove(task);
                        return;
                    }
                } else {
                    buffer.get(queue).remove(task);
                    return;
                }

            }
    }

    @Override
    public int getAvailableCores() {
        return globalLimit;
    }

    @Override
    public int getAvailableCores(int queue) {
        return limits[queue];
    }

    public void checkTasks() {
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                Iterator<Integer> it = queues.keySet().iterator();
                int i = 0;
                while (it.hasNext() && addedTask) {
                    checkBuffer(i);
                    if (queues.get(i) != null) {
                        Iterator<TaskRun> ittask = queues.get(i).iterator();
                        while (ittask.hasNext()) {
                            TaskRun actualTask = queues.get(i).peek();
                            if (actualTask != null && actualTask.wasRunning() && System.currentTimeMillis() - actualTask.getStartTime() >= actualTask.getExecutionTime()) {
                                actualTask.cancelTask();
                                limits[i] += actualTask.getUsedCores();
                                globalLimit += actualTask.getUsedCores();
                                queues.get(i).remove(actualTask);
                            }
                        }
                        i++;
                    }
                }
            }
        });
        t.start();
    }

    private void runNextTask() {
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    Iterator<Integer> it = queues.keySet().iterator();
                    int i = 0;
                    while (it.hasNext()) {
                        if (queues.get(i) != null && !queues.get(i).isEmpty()) {
                            System.out.println("Ruruchamianie");
                            TaskRun t = queues.get(i).peek();
                            if (t != null && !t.isRunning()) {
                                t.runTask();
                            }
                        }
                        i++;
                    }
                }
            }
        });
        t.start();
    }
}

class TaskRun implements Comparable{
    private QueuesInterface.TaskInterface task;
    private Thread associatedThread;
    private long startTime;
    private long executionTime;
    private int usedCores;
    private boolean isRunning;
    private boolean wasRunning = false;

    public TaskRun(QueuesInterface.TaskInterface task) {
        this.task = task;
        this.executionTime = task.getRequiredTime();
        this.usedCores = task.getRequiredCores();

        associatedThread = new Thread(() -> {
            task.execute(task.getRequiredCores(),task.getRequiredTime());
            System.out.println("Uruchomiono zadanie");
        });
    }

    public void runTask(){
        startTime = System.currentTimeMillis();
        if(!associatedThread.isAlive()) {
            wasRunning = true;
            associatedThread.start();
            this.isRunning = true;
            try {
                associatedThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void cancelTask(){
        task.cancel();
        if(associatedThread.isAlive())
            associatedThread.interrupt();
            this.isRunning = false;
    }

    public long getStartTime() {
        return startTime;
    }

    public int getUsedCores() {
        return usedCores;
    }

    public long getExecutionTime() {
        return executionTime;
    }

    public boolean isRunning() {
        return isRunning;
    }

    public boolean wasRunning() {
        return wasRunning;
    }

    @Override
    public int compareTo(Object o) {
        return this.hashCode() - o.hashCode();
    }
}
