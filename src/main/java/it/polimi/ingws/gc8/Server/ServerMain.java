package it.polimi.ingws.gc8.Server;

import it.polimi.ingws.gc8.Controller.MainController;
import it.polimi.ingws.gc8.Network.Connection.RMI.RMIServer;
import it.polimi.ingws.gc8.Network.Connection.RMI.RMIServerConnection;
import it.polimi.ingws.gc8.Network.Connection.Socket.SocketServer;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Enumeration;

/**
 * Server-side entry point.
 *
 * <p>Usage: {@code java ServerMain [host] [port]}.</p>
 *
 * <p>If no host is passed, the server auto-detects the first non-loopback
 * IPv4 address of the machine and binds RMI to it, so that clients on the
 * same LAN can reach it without extra configuration. If no such interface
 * is found, falls back to {@code 127.0.0.1} with an explicit warning that
 * only local connections will work.</p>
 */
public class ServerMain {

    private static final int DEFAULT_RMI_PORT = 1099;
    private static final int DEFAULT_SOCKET_PORT = 1100;
    private static final String LOOPBACK = "127.0.0.1";

    public static void main(String[] args) {
        String hostname = (args.length > 0) ? args[0] : detectLocalIp();
        int port = (args.length > 1) ? parsePort(args[1]) : DEFAULT_RMI_PORT;

        // Required when server and client are on different machines or
        // when there is no DNS able to resolve the local hostname.
        System.setProperty("java.rmi.server.hostname", hostname);

        try {
            MainController mainController = new MainController();
            RMIServer rmiServer = new RMIServer(mainController);

            Registry registry = LocateRegistry.createRegistry(port);
            registry.rebind(RMIServerConnection.SERVICE_NAME, rmiServer);

            System.out.println("[Server] MESOS RMI ready on " + hostname + ":" + port
                    + " (service=" + RMIServerConnection.SERVICE_NAME + ")");

            int socketPort = DEFAULT_SOCKET_PORT;
            SocketServer socketServer = new SocketServer(socketPort, mainController);
            // Accept loop is NOT a daemon: it is the heartbeat of the TCP transport
            // and keeps the JVM alive. Per-client handlers spawned inside are daemon.
            Thread socketThread = new Thread(socketServer, "tcp-accept-loop");
            socketThread.start();
            System.out.println("[Server] MESOS TCP ready on " + hostname + ":" + socketPort);

            System.out.println("[Server] Clients must connect using host '" + hostname + "'.");
            if (LOOPBACK.equals(hostname)) {
                System.out.println("[Server] WARNING: bound to 127.0.0.1 — "
                        + "only clients on this same machine can connect.");
            }
        } catch (Exception e) {
            System.err.println("[Server] Startup failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Picks the first non-loopback, non-link-local IPv4 address on any
     * up interface. Falls back to {@code 127.0.0.1} if none is found.
     */
    private static String detectLocalIp() {
        try {
            Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();
            while (ifaces.hasMoreElements()) {
                NetworkInterface iface = ifaces.nextElement();
                if (!iface.isUp() || iface.isLoopback() || iface.isVirtual()) continue;
                Enumeration<InetAddress> addrs = iface.getInetAddresses();
                while (addrs.hasMoreElements()) {
                    InetAddress addr = addrs.nextElement();
                    if (addr instanceof Inet4Address
                            && !addr.isLoopbackAddress()
                            && !addr.isLinkLocalAddress()) {
                        return addr.getHostAddress();
                    }
                }
            }
        } catch (Exception ignored) {
            // fall through to loopback
        }
        return LOOPBACK;
    }

    private static int parsePort(String raw) {
        try {
            int p = Integer.parseInt(raw);
            if (p > 0 && p < 65536) return p;
        } catch (NumberFormatException ignored) {
            // fall through
        }
        System.err.println("[Server] Invalid port '" + raw + "', using default " + DEFAULT_RMI_PORT);
        return DEFAULT_RMI_PORT;
    }
}
