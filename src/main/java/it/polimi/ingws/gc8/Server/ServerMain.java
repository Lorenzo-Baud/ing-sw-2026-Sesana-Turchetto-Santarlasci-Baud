package it.polimi.ingws.gc8.Server;

import it.polimi.ingws.gc8.Controller.MainController;
import it.polimi.ingws.gc8.Controller.MatchRegistry;
import it.polimi.ingws.gc8.Network.Connection.RMI.RMIServer;
import it.polimi.ingws.gc8.Network.Connection.RMI.RMIServerConnection;
import it.polimi.ingws.gc8.Network.Connection.Socket.SocketServer;
import it.polimi.ingws.gc8.Persistence.MatchSnapshot;
import it.polimi.ingws.gc8.Persistence.PersistenceManager;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
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
            // load persisted matches before accepting any connection, so a
            // reconnecting client can immediately rejoin a match that survived
            // a server crash. Restored matches start suspended (IN_GAME) and
            // resume once enough players reconnect by nickname.
            PersistenceManager persistenceManager = PersistenceManager.fromEnvironment();
            MatchRegistry matchRegistry = new MatchRegistry(persistenceManager);
            int restored = 0;
            for (MatchSnapshot snap : persistenceManager.loadAll()) {
                try {
                    MainController mc = MainController.restore(snap, matchRegistry, persistenceManager);
                    matchRegistry.reattach(snap.matchId(), mc);
                    restored++;
                } catch (Exception e) {
                    System.err.println("[Server] Could not restore match "
                            + snap.matchId() + ": " + e.getMessage());
                }
            }
            if (restored > 0) {
                System.out.println("[Server] Restored " + restored
                        + " match(es) from disk (awaiting reconnections).");
            }

            RMIServer rmiServer = new RMIServer(matchRegistry);

            Registry registry = LocateRegistry.createRegistry(port);
            registry.rebind(RMIServerConnection.SERVICE_NAME, rmiServer);

            System.out.println("[Server] MESOS RMI ready on " + hostname + ":" + port
                    + " (service=" + RMIServerConnection.SERVICE_NAME + ")");

            int socketPort = DEFAULT_SOCKET_PORT;
            SocketServer socketServer = new SocketServer(socketPort, matchRegistry);
            // Accept loop is NOT a daemon: it is the heartbeat of the TCP transport
            // and keeps the JVM alive. Per-client handlers spawned inside are daemon.
            Thread socketThread = new Thread(socketServer, "tcp-accept-loop");
            socketThread.start();
            System.out.println("[Server] MESOS TCP ready on " + hostname + ":" + socketPort);

            // Graceful shutdown (Ctrl-C / SIGTERM): unexport the RMI objects so
            // port 1099 is released, and close the TCP accept loop. Without this
            // the non-daemon RMI threads keep the JVM (and the port) alive,
            // causing "Address already in use" on the next start.
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("[Server] Shutting down…");
                // FA2: flush pending snapshot writes before the JVM dies.
                matchRegistry.shutdownAll();
                socketServer.stop();
                try {
                    UnicastRemoteObject.unexportObject(rmiServer, true);
                } catch (Exception ignored) {}
                try {
                    UnicastRemoteObject.unexportObject(registry, true);
                } catch (Exception ignored) {}
            }, "server-shutdown"));

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

    /** Interface name fragments that indicate a virtual / VM / tunnel adapter. */
    private static final String[] VIRTUAL_IFACE_HINTS = {
            "vnic", "vmnet", "vboxnet", "bridge", "utun", "awdl", "llw",
            "docker", "veth", "tap", "tun", "ham", "zt"
    };

    /** Address prefixes of known VM host-only / container subnets to deprioritize. */
    private static final String[] VIRTUAL_SUBNET_PREFIXES = {
            "10.37.129.",   // Parallels (Shared)
            "10.211.55.",   // Parallels (Host-Only)
            "192.168.56.",  // VirtualBox Host-Only
            "172.17.", "172.18." // Docker default bridges
    };

    /**
     * Picks the best non-loopback, non-link-local IPv4 address for clients to
     * reach. Instead of returning the first match (which on a machine with
     * Parallels/VirtualBox/Docker often is a virtual host-only address such as
     * {@code 10.37.129.x}, unreachable from the real LAN), every candidate is
     * scored: physical NIC names (en0/eth0/wlan0) and common LAN ranges are
     * preferred, while virtual/tunnel interface names and known VM subnets are
     * penalized. Falls back to {@code 127.0.0.1} if none is found.
     *
     * <p>Note: {@code NetworkInterface.isVirtual()} only flags logical
     * sub-interfaces (e.g. {@code en0:1}), NOT VM adapters — those look like
     * ordinary interfaces — hence the name/subnet heuristics below.
     */
    private static String detectLocalIp() {
        String best = null;
        int bestScore = Integer.MIN_VALUE;
        try {
            Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();
            while (ifaces.hasMoreElements()) {
                NetworkInterface iface = ifaces.nextElement();
                if (!iface.isUp() || iface.isLoopback() || iface.isVirtual()) continue;
                Enumeration<InetAddress> addrs = iface.getInetAddresses();
                while (addrs.hasMoreElements()) {
                    InetAddress addr = addrs.nextElement();
                    if (!(addr instanceof Inet4Address)
                            || addr.isLoopbackAddress()
                            || addr.isLinkLocalAddress()) {
                        continue;
                    }
                    int score = scoreCandidate(iface, addr.getHostAddress());
                    if (score > bestScore) {
                        bestScore = score;
                        best = addr.getHostAddress();
                    }
                }
            }
        } catch (Exception ignored) {
            // fall through to loopback
        }
        return best != null ? best : LOOPBACK;
    }

    /** Higher score = more likely to be the real LAN address clients can reach. */
    private static int scoreCandidate(NetworkInterface iface, String ip) {
        int score = 0;
        String name = iface.getName() == null ? "" : iface.getName().toLowerCase();
        String display = iface.getDisplayName() == null ? "" : iface.getDisplayName().toLowerCase();

        for (String hint : VIRTUAL_IFACE_HINTS) {
            if (name.contains(hint) || display.contains(hint)) {
                score -= 100;
                break;
            }
        }
        for (String prefix : VIRTUAL_SUBNET_PREFIXES) {
            if (ip.startsWith(prefix)) {
                score -= 50;
                break;
            }
        }

        // Prefer the conventional physical NIC names, then any en*/eth*/wl*.
        if (name.equals("en0") || name.equals("eth0") || name.equals("wlan0")) {
            score += 20;
        } else if (name.startsWith("en") || name.startsWith("eth") || name.startsWith("wl")) {
            score += 10;
        }

        // Prefer typical home/office LAN ranges.
        if (ip.startsWith("192.168.") || ip.startsWith("10.")) {
            score += 5;
        }
        return score;
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
