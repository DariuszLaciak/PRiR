import java.util.Iterator;
import java.util.Map;
import java.util.PriorityQueue;
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
    private Map<Integer, PriorityBlockingQueue<TaskRun>> finishedTasks = new ConcurrentHashMap<>();
    private PriorityBlockingQueue<TaskRun> destroy = new PriorityBlockingQueue<>();

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
            finishedTasks.put(index, new PriorityBlockingQueue<>());
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
                    releaseResources();
                    destroyUnneededTasks();
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

    private void checkTasks() {

        Iterator<PriorityBlockingQueue<TaskRun>> it = queues.values().iterator();
        int i = 0;
        while (it.hasNext()) {
            checkBuffer(i);
            PriorityBlockingQueue<TaskRun> n = it.next();
            if (n != null && !n.isEmpty()) {
                Iterator<TaskRun> ittask = n.iterator();
//                System.out.println(n.size());
                while (ittask.hasNext()) {
                    TaskRun actualTask = ittask.next();
                    actualTask.checkIsOver();
                }

            }
            i++;
        }

    }

    private void releaseResources() {
        Iterator<PriorityBlockingQueue<TaskRun>> it = finishedTasks.values().iterator();
        int i = 0;
        while (it.hasNext()) {
            PriorityBlockingQueue<TaskRun> n = it.next();
            if (n != null && !n.isEmpty()) {
                Iterator<TaskRun> ittask = n.iterator();
                while (ittask.hasNext()) {
                    TaskRun actualTask = ittask.next();
//                    System.out.println("asdasd");
                    if(actualTask.checkIsOver()) {
                        actualTask.setFinished();
                    }
                    if (actualTask != null && !actualTask.hasFinished()) {
//                        System.out.println("usuwanie");
                        limits[i].set(limits[i].get() + actualTask.getUsedCores());
                        globalLimit.set(globalLimit.get() + actualTask.getUsedCores());
//                        actualTask.checkIsOverAndOver();

                        n.remove(actualTask);
                    }

                }
            }
            i++;
        }
    }

    private void destroyUnneededTasks() {
        Iterator<TaskRun> it = destroy.iterator();
        while (it.hasNext()) {
                    TaskRun actualTask = it.next();
                    if ( actualTask.getMark()) {
//                        System.out.println("usuwamy");
                        actualTask.cancelTask();
                        it.remove();
                    }
        }
    }

    private boolean runNextTask() {
        boolean didTheTaskRun = false;
                Iterator<PriorityBlockingQueue<TaskRun>> it = queues.values().iterator();
                int i = 0;
                while (it.hasNext()) {
                    PriorityBlockingQueue<TaskRun> n = it.next();
                    if (n != null && !n.isEmpty()) {
                        TaskRun t = n.peek();
                        if (t != null && !t.wasRunning() && !t.isRunning()) {
                            t.runTask();
                            finishedTasks.get(i).add(t);
                            destroy.add(t);
                            n.remove(t);
                            didTheTaskRun = true;
                        }
                    }
                    i++;
                }
        return didTheTaskRun;

    }
//    private boolean isBufferUsed(){
//        boolean flag = false;
//        Iterator<PriorityBlockingQueue<TaskRun>> it = buffer.values().iterator();
//        while(it.hasNext()){
//            if(it.next().size() != 0){
//                return true;
//            }
//        }
//        return flag;
//    }
}

class TaskRun implements Comparable {
    private QueuesInterface.TaskInterface task;
    private Thread associatedThread;
    private AtomicLong startTime = new AtomicLong();
    private AtomicLong executionTime = new AtomicLong();
    private AtomicInteger usedCores = new AtomicInteger();
    private AtomicBoolean isRunning = new AtomicBoolean();
    private AtomicBoolean wasRunning = new AtomicBoolean(false);
    private AtomicBoolean markToCancel = new AtomicBoolean(false);

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

                if (!associatedThread.isAlive()) {
                    startTime.set(System.currentTimeMillis());
                        wasRunning.set(true);
                        isRunning.set(true);
                        associatedThread.start();
                }
    }

    public void cancelTask() {
                if (associatedThread.isAlive()) {
                    task.cancel();

                    associatedThread.interrupt();
                }
                isRunning.set(false);
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

    public boolean hasFinished(){
        return System.currentTimeMillis() - startTime.get() <= executionTime.get();
    }

    public boolean checkIsOver(){
        if(isRunning() && System.currentTimeMillis() - startTime.get() > executionTime.get()){
            return true;
        }
        return false;
    }

    public void setFinished(){
        isRunning.set(false);
        markToCancel.set(true);
    }

    public boolean getMark(){
        return markToCancel.get();
    }

    @Override
    public int compareTo(Object o) {
        return this.hashCode() - o.hashCode();
    }
}
