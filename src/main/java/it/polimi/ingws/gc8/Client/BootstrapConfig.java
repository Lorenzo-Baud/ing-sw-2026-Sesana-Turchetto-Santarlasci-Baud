package it.polimi.ingws.gc8.Client;

/**
 * Pre-game configuration collected by {@link Bootstrap}.
 *
 * @param uiFlavor chosen user-interface
 * @param host     server host or IP
 * @param port     server port
 * @param nickname player nickname
 * @param transport connection type
 */
public record BootstrapConfig(UiFlavor uiFlavor, String host, int port, String nickname, String transport) {}
