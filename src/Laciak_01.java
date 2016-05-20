import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;


class Queues implements QueuesInterface {

    private AtomicInteger[] limits;
    private AtomicInteger[] formerLimits;
    private AtomicInteger globalLimit = new AtomicInteger();
    private AtomicInteger formerGlobalLimit = new AtomicInteger();
    private Map<Integer, PriorityBlockingQueue<TaskRun>> queues = new ConcurrentHashMap<>();
    private Map<Integer, PriorityBlockingQueue<TaskRun>> buffer = new ConcurrentHashMap<>();

    public Queues() {
        checkThreadStates();
    }

    @Override
    public void configure(int[] limits, int globalLimit) {
        this.limits = new AtomicInteger[limits.length];
        this.globalLimit.set(globalLimit);
        formerGlobalLimit.set(globalLimit);
        // INIT
        int index = 0;
        formerLimits = new AtomicInteger[limits.length];
        for (int ignored : limits) {
            queues.put(index, new PriorityBlockingQueue<>());
            buffer.put(index, new PriorityBlockingQueue<>());
            formerLimits[index] = new AtomicInteger(ignored);
            this.limits[index] = new AtomicInteger(ignored);
            index++;
        }
    }

    private void checkThreadStates() {
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    checkTasks();
                    if(!runNextTask()){
                        try {
                            Thread.sleep(1);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        });
        t.start();
    }

    @Override
    public void submitTask(int queue, TaskInterface task) {
        if (queue < limits.length && queues.get(queue) != null) {
            if (getAvailableCores() >= task.getRequiredCores() && getAvailableCores(queue) >= task.getRequiredCores()) {
                limits[queue].set(limits[queue].get() - task.getRequiredCores());
                globalLimit.set(globalLimit.get() - task.getRequiredCores());
                queues.get(queue).add(new TaskRun(task));
            } else if (formerLimits[queue].get() >= task.getRequiredCores() && formerGlobalLimit.get() >= task.getRequiredCores()) {
                buffer.get(queue).add(new TaskRun(task));
            }
        }
    }

    private void checkBuffer(int queue) {
        if (buffer.get(queue) != null)
            for (TaskRun task : buffer.get(queue)) {
                if (!task.wasRunning()) {
                    if (getAvailableCores() >= task.getUsedCores() && getAvailableCores(queue) >= task.getUsedCores()) {
                        limits[queue].set( limits[queue] . get() - task.getUsedCores());
                        globalLimit.set(globalLimit.get() - task.getUsedCores());
                        queues.get(queue).add(task);
                        buffer.get(queue).remove(task);
                        return;
                    }
                }
                else {
                    buffer.get(queue).remove(task);
                    return;
                }

            }
    }

    @Override
    public int getAvailableCores() {
        return globalLimit.get();
    }

    @Override
    public int getAvailableCores(int queue) {
        return limits[queue].get();
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
                    if (actualTask != null && actualTask.wasRunning() && actualTask.isRunning() && System.currentTimeMillis() - actualTask.getStartTime() >= actualTask.getExecutionTime()) {
                        actualTask.cancelTask();
                        limits[i].set(limits[i].get() + actualTask.getUsedCores());
                        globalLimit.set(globalLimit.get() +actualTask.getUsedCores() );
                        n.remove(actualTask);
                    }
                    else if(actualTask.wasRunning() && !actualTask.isRunning()){
                        limits[i].set(limits[i].get() + actualTask.getUsedCores());
                        globalLimit.set(globalLimit.get() +actualTask.getUsedCores() );
                        n.remove(actualTask);
                    }
                }

            }
            i++;
        }

    }

    private boolean runNextTask() {
        boolean didTheTaskRun = false;
                Iterator<PriorityBlockingQueue<TaskRun>> it = queues.values().iterator();
                while (it.hasNext()) {
                    PriorityBlockingQueue<TaskRun> n = it.next();
                    if (n != null && !n.isEmpty()) {
                        TaskRun t = n.peek();
                        if (t != null ) {
                            t.runTask();
                            n.remove(t);
                            didTheTaskRun = true;
                        }
                    }
                }
        return didTheTaskRun;

    }
}

class TaskRun implements Comparable {
    private QueuesInterface.TaskInterface task;
    private Thread associatedThread;
    private AtomicLong startTime = new AtomicLong();
    private AtomicLong executionTime = new AtomicLong();
    private AtomicInteger usedCores = new AtomicInteger();
    private AtomicBoolean isRunning = new AtomicBoolean();
    private AtomicBoolean wasRunning = new AtomicBoolean(false);

    public TaskRun(QueuesInterface.TaskInterface task) {
        this.task = task;
        this.executionTime.set(task.getRequiredTime());
        this.usedCores.set(task.getRequiredCores());

        associatedThread = new Thread(() -> {
            task.execute(task.getRequiredCores(), task.getRequiredTime());
//            System.out.println("Uruchomiono zadanie");
        });
    }

    public void runTask() {
        startTime.set(System.currentTimeMillis());
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                if (!associatedThread.isAlive()) {
                        wasRunning.set(true);
                        isRunning.set(true);
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

        if (associatedThread.isAlive()) {
            task.cancel();
            System.out.println("Zakonczono zadanie");
            associatedThread.interrupt();
        }
        this.isRunning.set(false);
    }

    public long getStartTime() {
        return startTime.get();
    }

    public int getUsedCores() {
        return usedCores.get();
    }

    public long getExecutionTime() {
        return executionTime.get();
    }

    public synchronized boolean isRunning() {
        return isRunning.get();
    }

    public synchronized boolean wasRunning() {
        return wasRunning.get();
    }

    @Override
    public int compareTo(Object o) {
        return this.hashCode() - o.hashCode();
    }
}
