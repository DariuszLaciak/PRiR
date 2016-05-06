import java.io.IOException;
import java.rmi.registry.LocateRegistry;

/**
 * Created by ldar on 2016-05-06.
 */
public class RmiRegistry {
    public static void main(String[] args) throws IOException {
//        Runtime.getRuntime().exec("rmiregistry 2020");
        LocateRegistry.createRegistry(0);
    }
}
