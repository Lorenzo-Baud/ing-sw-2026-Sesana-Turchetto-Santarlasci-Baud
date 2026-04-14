package it.polimi.ingws.gc8.Network.Messages;

public class RoundEndedMessage {
    private final int currentRound;
    private final boolean eraChanged;
    private final String currentEra;

    public  RoundEndedMessage(int currentRound, boolean eraChanged, String currentEra) {
        this.currentRound = currentRound;
        this.eraChanged = eraChanged;
        this.currentEra = currentEra;
    }

    public int getCurrentRound() {
        return currentRound;
    }
    public boolean getEraChanged() {
        return eraChanged;
    }
    public String getCurrentEra() {
        return currentEra;
    }
}
