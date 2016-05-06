import java.util.function.Consumer;

/**
 * Created by ldar on 2016-03-31.
 */
public class Runner {
    public static void main(String[] args){
        zad1();

    }

    static private void zad2(){
        SwitchSimulator sm = new SwitchSimulator();
        sm.setNumberOfPorts(5);
        sm.setAgingTime(10000);
        SwitchSimulatorInterface.FrameInterface frame1 = new SwitchSimulatorInterface.FrameInterface() {
            @Override
            public String getDestination() {
                return "B";
            }

            @Override
            public String getSource() {
                return "A";
            }
        };

        SwitchSimulatorInterface.FrameInterface frame2 = new SwitchSimulatorInterface.FrameInterface() {
            @Override
            public String getDestination() {
                return "C";
            }

            @Override
            public String getSource() {
                return "B";
            }
        };

        SwitchSimulatorInterface.FrameInterface frame3 = new SwitchSimulatorInterface.FrameInterface() {
            @Override
            public String getDestination() {
                return "C";
            }

            @Override
            public String getSource() {
                return "B";
            }
        };

        sm.connect(0, new Consumer<SwitchSimulatorInterface.FrameInterface>() {
            @Override
            public void accept(SwitchSimulatorInterface.FrameInterface frameInterface) {
                System.out.println("Port 1 - Ramka z "+frameInterface.getSource() + " do "+frameInterface.getDestination());
            }
        });
        sm.connect(1, new Consumer<SwitchSimulatorInterface.FrameInterface>() {
            @Override
            public void accept(SwitchSimulatorInterface.FrameInterface frameInterface) {
                System.out.println("Port 2 - Ramka z "+frameInterface.getSource() + " do "+frameInterface.getDestination());
            }
        });
        sm.connect(2, new Consumer<SwitchSimulatorInterface.FrameInterface>() {
            @Override
            public void accept(SwitchSimulatorInterface.FrameInterface frameInterface) {
                System.out.println("Port 3 - Ramka z "+frameInterface.getSource() + " do "+frameInterface.getDestination());
            }
        });
        sm.insertFrame(1,frame1);
        sm.insertFrame(2,frame2);
        sm.insertFrame(2,frame3);
    }

    static private void zad1(){
        Queues q = new Queues();
        int[] limits = {3,4,3,4};
        q.configure(limits,10);
        QueuesInterface.TaskInterface task = new QueuesInterface.TaskInterface() {
            private Thread t;
            @Override
            public int getRequiredCores() {
                return 2;
            }

            @Override
            public long getRequiredTime() {
                return 1000*7;
            }

            @Override
            public void execute(int cores, long time) {
                System.out.println("Thread 1: ");
            }

            @Override
            public void cancel() {
                System.out.println("Przerawano task 1");
            }
        };

        QueuesInterface.TaskInterface task2 = new QueuesInterface.TaskInterface() {
            private Thread t;
            @Override
            public int getRequiredCores() {
                return 2;
            }

            @Override
            public long getRequiredTime() {
                return 1000*5;
            }

            @Override
            public void execute(int cores, long time) {
                System.out.println("Thread 2: ");
            }

            @Override
            public void cancel() {
                System.out.println("Przerwano task2");
            }
        };

        QueuesInterface.TaskInterface task3 = new QueuesInterface.TaskInterface() {
            private Thread t;
            @Override
            public int getRequiredCores() {
                return 3;
            }

            @Override
            public long getRequiredTime() {
                return 1000*2;
            }

            @Override
            public void execute(int cores, long time) {
                System.out.println("Thread 3: ");
            }

            @Override
            public void cancel() {
                System.out.println("Przerwano task3");
            }
        };
        System.out.println(q.getAvailableCores());
        q.submitTask(0,task);
        q.submitTask(1,task2);
        q.submitTask(2,task3);
        System.out.println(q.getAvailableCores());
        try {
            Thread.sleep(11000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println(q.getAvailableCores());

    }

    static private void test(){
        Thread th = new Thread(new Runnable() {
            @Override
            public void run() {
                for(int i = 0 ; i < 10 ; i++){
                    System.out.println("Watek 1: "+i);
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        Thread th1 = new Thread(new Runnable() {
            @Override
            public void run() {
                for(int i = 0 ; i < 10 ; i++){
                    System.out.println("Watek 2: "+i);
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        th.start();
        th1.start();
        for(int i = 0 ; i < 10 ; i++){
            System.out.println("Watek glowny: "+i);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }
    }
}
