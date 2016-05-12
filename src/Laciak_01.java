import java.util.Iterator;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;


class Queues implements QueuesInterface {

    private int[] limits;
    private int[] formerLimits;
    private int globalLimit;
    private int formerGlobalLimit;
    private Map<Integer, PriorityBlockingQueue<TaskRun>> queues = new ConcurrentHashMap<>();
    private Map<Integer, PriorityBlockingQueue<TaskRun>> buffer = new ConcurrentHashMap<>();

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
            queues.put(index, new PriorityBlockingQueue<>());
            buffer.put(index, new PriorityBlockingQueue<>());
            formerLimits[index] = ignored;
            index++;
        }
    }

    private void checkThreadStates() {
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    checkTasks();
                    runNextTask();
                }
            }
        });
//        try {
//            Thread.sleep(1000);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
        t.start();
    }

    @Override
    public void submitTask(int queue, TaskInterface task) {
        if (queue < limits.length && queues.get(queue) != null) {
            if (getAvailableCores() >= task.getRequiredCores() && getAvailableCores(queue) >= task.getRequiredCores()) {
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

        Iterator<PriorityBlockingQueue<TaskRun>> it = queues.values().iterator();
        int i = 0;
        while (it.hasNext()) {
            checkBuffer(i);
            PriorityBlockingQueue<TaskRun> n = it.next();
            if (n != null && !n.isEmpty()) {
                Iterator<TaskRun> ittask = n.iterator();
                while (ittask.hasNext()) {
                    TaskRun actualTask = ittask.next();
                    if (actualTask != null && actualTask.wasRunning() && System.currentTimeMillis() - actualTask.getStartTime() >= actualTask.getExecutionTime()) {
                        actualTask.cancelTask();
                        limits[i] += actualTask.getUsedCores();
                        globalLimit += actualTask.getUsedCores();
                        ittask.remove();
                    }
                }
                i++;
            }
        }

    }

    private void runNextTask() {
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                Iterator<PriorityBlockingQueue<TaskRun>> it = queues.values().iterator();
                while (it.hasNext()) {
                    PriorityBlockingQueue<TaskRun> n = it.next();
                    if (n != null && !n.isEmpty()) {
                        TaskRun t = n.peek();
                        if (t != null && !t.isRunning()) {
                            t.runTask();
                            n.remove();
                        }
                    }
                }
            }
        });
        t.start();
        try {
            t.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }
}

class TaskRun implements Comparable {
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
            task.execute(task.getRequiredCores(), task.getRequiredTime());
//            System.out.println("Uruchomiono zadanie");
        });
    }

    public void runTask() {
        startTime = System.currentTimeMillis();
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                if (!associatedThread.isAlive()) {
                    wasRunning = true;
                    isRunning = true;
                    associatedThread.start();
                }
            }
        });
        t.start();
        try {
            t.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void cancelTask() {
        task.cancel();
        if (associatedThread.isAlive())
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
