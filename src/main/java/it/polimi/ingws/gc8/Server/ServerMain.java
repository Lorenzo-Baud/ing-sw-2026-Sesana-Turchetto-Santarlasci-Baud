package it.polimi.ingws.gc8.Server;

import it.polimi.ingws.gc8.Controller.MainController;
import it.polimi.ingws.gc8.Network.Connection.RMI.RMIServer;
import it.polimi.ingws.gc8.Network.Connection.RMI.RMIServerConnection;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

/**
 * Server-side entry point
 */
public class ServerMain {

    private static final int DEFAULT_RMI_PORT = 1099;

    public static void main(String[] args) {
        String hostname = (args.length > 0) ? args[0] : "127.0.0.1";
        int port = (args.length > 1) ? Integer.parseInt(args[1]) : DEFAULT_RMI_PORT;

        // Required when server and client are on different machines or
        // when there is no DNS able to resolve the local hostname.
        System.setProperty("java.rmi.server.hostname", hostname);

        try {
            MainController mainController = new MainController();
            RMIServer rmiServer = new RMIServer(mainController);

            Registry registry = LocateRegistry.createRegistry(port);
            registry.rebind(RMIServerConnection.SERVICE_NAME, rmiServer);

            // TODO: socket implementation

            System.out.println("[Server] RMI ready on " + hostname + ":" + port
                    + " (service=" + RMIServerConnection.SERVICE_NAME + ")");
        } catch (Exception e) {
            System.err.println("[Server] Startup failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
