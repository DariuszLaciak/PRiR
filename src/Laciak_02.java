import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

class SwitchSimulator implements SwitchSimulatorInterface {
    private final String BROADCAST = "FF:FF:FF:FF:FF:FF";
    private int numerOfPorts;
    private long agingTime;

    private Map<Integer,PortOperation> mapOfPorts = new ConcurrentHashMap<>();

    @Override
    public void setNumberOfPorts(int ports) {
        numerOfPorts = ports;
        init();
    }

    @Override
    public void setAgingTime(long msec) {
        agingTime = msec;
    }

    @Override
    public void connect(int port, Consumer<FrameInterface> frameConsumer) {
        mapOfPorts.get(port).setActive(frameConsumer);
    }

    @Override
    public void insertFrame(int port, FrameInterface frame) {
        if(frame.getDestination().equals(frame.getSource())){
            return; //schizofrenia
        }
        Thread t = new Thread(){
            public void run(){
                //checkBuffers();
                mapOfPorts.get(port).transferFrame(frame);
                int destinationPort = findPortByMac(frame.getDestination());
                if(destinationPort != -1 || frame.getDestination().equals(BROADCAST)) {
                    if(!frame.getDestination().equals(BROADCAST)){
                        mapOfPorts.get(destinationPort).acceptFrame(frame);
                    }
                    else {
                        transwerToAllActivePorts(frame, port);
                    }
                }
                else {
                    transwerToAllActivePorts(frame, port);
                }
            }
        };
        t.start();
        try {
            t.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void disconnect(int port) {
        mapOfPorts.get(port).disconnect();
        removeAssociatedMacs(port);
    }

    private void init(){
        for(int i = 0 ; i < numerOfPorts ; i++){
            mapOfPorts.put(i,new PortOperation(i));
        }
    }

    private void removeAssociatedMacs(int port){
        PortOperation po = mapOfPorts.get(port);
        po.macAdress.clear();
    }

    private void checkBuffers(){
        for(Map.Entry<Integer,PortOperation> entry : mapOfPorts.entrySet()){
            if(!entry.getValue().queue.isEmpty())
                entry.getValue().processQueue();
        }
    }

    private int findPortByMac(String mac){
        for(Map.Entry<Integer,PortOperation> entry : mapOfPorts.entrySet()){
            if(entry.getValue().isMacInside(mac)){
                return entry.getKey();
            }
        }
        return -1;
    }

    private void transwerToAllActivePorts(FrameInterface frame, int source){
        for(Map.Entry<Integer,PortOperation> entry : mapOfPorts.entrySet()){
            if(entry.getKey().equals(source)){
                continue;
            }
            if(entry.getValue().active){
                entry.getValue().acceptFrame(frame);
            }
        }
    }

    private class PortOperation{
        private Consumer<FrameInterface> consumer;
        private boolean active;
        private boolean isTransfering;
        private List<FrameInterface> queue = new ArrayList<>();
        private Map<String,Long> macAdress  = new HashMap<>();
        private int port;
        private int transfered = 0;

        private PortOperation( int port ){
            active = false;
            isTransfering = false;
            this.port = port;
        }
        private void setActive(Consumer<FrameInterface> consumer){
            this.consumer = consumer;
            active = true;
        }
        private void transferFrame(FrameInterface frame){
            if(isMacInside(frame.getSource()) && isMacInside(frame.getDestination())){
                return;
            }
            if( active ) {
                macAdress.put(frame.getSource(), System.currentTimeMillis());
            }

        }
        private void disconnect(){
            active = false;
        }


        private synchronized void acceptFrame(FrameInterface frame){
            if(isMacInside(frame.getSource()) && isMacInside(frame.getDestination())){
                return;
            }
            if(active && consumer != null){
                if(isTimeCorrect(frame)){
                    consumer.accept(frame);
                }
                else {
                    consumer.accept(frame);
                    macAdress.remove(frame.getDestination());
                }
            }
        }
        private boolean isMacInside(String mac){
            return macAdress.containsKey(mac);
        }

        private void processQueue(){
            if(!isTransfering){
                Iterator<FrameInterface> it = queue.iterator();
                int i = 0;
                while(it.hasNext()){
                    transferFrame(queue.get(i));
                    queue.remove(i);
                    i++;
                }

            }
        }
        private boolean isTimeCorrect(FrameInterface frame){
            if(macAdress.get(frame.getDestination())!= null) {
                return System.currentTimeMillis() - macAdress.get(frame.getDestination()) <= agingTime;
            }
            else
                return true;
        }
    }
}
