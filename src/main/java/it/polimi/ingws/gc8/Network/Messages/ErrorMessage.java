package it.polimi.ingws.gc8.Network.Messages;

/**
 * Sent only to a specific player when they perform an invalid action.
 */
public class ErrorMessage implements NetworkMessage {

    private final String errorDetail;
    private final String error;

    public ErrorMessage(String error, String errorDetail) {
        this.errorDetail = errorDetail;
        this.error = error;
    }

    public String getErrorDetail() {
        return errorDetail;
    }

    public String getError() {
        return error;
    }
}