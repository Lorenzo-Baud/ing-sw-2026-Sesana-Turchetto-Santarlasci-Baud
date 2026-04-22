package it.polimi.ingws.gc8.Client;

/**
 * Ascoltatore di eventi prodotti dalla View.
 *
 * La View non conosce la rete: quando l'utente compie un'azione (piazzare
 * totem, prendere carta, ecc.) la View notifica tutti i suoi listener
 * tramite {@link #updateFromView(Object)}. Tipicamente l'unico listener
 * registrato è {@link ClientController}, che inoltra il messaggio al server.
 *
 * L'oggetto passato è un {@code PlayerAction} (o una delle sue sottoclassi),
 * ma il tipo è {@code Object} per mantenere la View disaccoppiata dalle
 * classi del dominio lato server.
 */
public interface ViewListener {

    /**
     * Chiamato dalla View quando l'utente ha prodotto un input che deve
     * essere inoltrato al server.
     *
     * @param message tipicamente un {@code PlayerAction}
     */
    void updateFromView(Object message);
}
