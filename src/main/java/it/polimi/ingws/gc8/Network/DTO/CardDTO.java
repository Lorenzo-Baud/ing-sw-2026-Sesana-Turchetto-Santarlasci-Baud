package it.polimi.ingws.gc8.Network.DTO;

import java.io.Serializable;

/**
 * Data Transfer Object representing a Card's state for network communication
 * This record is sent to clients to render the card correctly in the GUI
 *
 * @param front     resource path or identifier for the front image
 * @param back      resource path or identifier for the back image
 * @param isFlipped true if the card is currently showing its back, false otherwise
 */
public record CardDTO(
        String front,
        String back,
        boolean isFlipped
) implements Serializable {}