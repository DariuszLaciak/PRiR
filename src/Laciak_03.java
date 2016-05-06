import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by ldar on 2016-04-26.
 */
class TaskExecutor implements TaskExecutorInterface {
    Map<String,RMI> mapper = new HashMap<>();

    @Override
    public void execute(SerializableRunnableInterface codeToRun, Runnable callbackCode, boolean keepCallbackRunning) {
        String taskName = "";
        RMI rmi = new RMI();
        rmi.callbackRMI = new CallbackRMI(callbackCode);
        rmi.taskRMI = new TaskRMI(codeToRun, taskName);

    }
}
class CallbackRMI implements CallbackRMIExecutorInterface{

    private Runnable codeToRun;

    public CallbackRMI(Runnable code){
        codeToRun = code;
    }

    @Override
    public void callback() throws RemoteException {
        codeToRun.run();
    }
}

class TaskRMI implements TaskRMIExecutorInterface{

    SerializableRunnableInterface codeToRun;
    String name;

    public TaskRMI(SerializableRunnableInterface code, String taskName){
        codeToRun = code;
        name = taskName;
    }

    @Override
    public void execute(SerializableRunnableInterface codeToRun, String callbackServiceName) throws RemoteException {
        this.codeToRun.run();
    }
}
class RMI {
    CallbackRMI callbackRMI;
    TaskRMI taskRMI;
}