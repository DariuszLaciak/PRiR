import java.io.IOException;
import java.rmi.registry.LocateRegistry;

/**
 * Created by ldar on 2016-05-06.
 */
public class RmiRegistry {
    public static void main(String[] args) throws IOException {
        LocateRegistry.createRegistry(0);
//
    }
}
