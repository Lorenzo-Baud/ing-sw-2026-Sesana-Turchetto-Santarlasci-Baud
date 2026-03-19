package it.polimi.ingws.gc8.Model.Board;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.polimi.ingws.gc8.Model.Player;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TurnOrderTrack {

    // configurazione statica (dalla tessera)
    private int playerCount;
    private List<Integer> foodBonusesBySlot;
    private int lastSlotFoodPenalty;
    private int lastSlotPrestigePenalty;

    // configurazione dinamica
    private List<Player> occupantsBySlot;

    // Constructor — prende solo playerCount e carica tutto dal JSON
    public TurnOrderTrack(int playerCount) {
        this.playerCount = playerCount;
        // inizializza tutti gli slot a null (nessun occupante)
        this.occupantsBySlot = new ArrayList<>(Collections.nCopies(playerCount, null));
        loadConfig(playerCount);
    }

    private void loadConfig(int playerCount) {
        try {
            InputStream is = getClass()
                    .getClassLoader()
                    .getResourceAsStream("turn_order_track.json");

            ObjectMapper mapper = new ObjectMapper();
            // legge la lista di configurazioni dal JSON
            List<TurnOrderConfig> configs = mapper.readValue(is,
                    new TypeReference<List<TurnOrderConfig>>() {});

            // trova la configurazione per il numero di giocatori corretto
            TurnOrderConfig config = configs.stream()
                    .filter(c -> c.getPlayerCount() == playerCount)
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException(
                            "Config non trovata per " + playerCount + " giocatori"));

            this.foodBonusesBySlot = config.getFoodBonusesBySlot();
            this.lastSlotFoodPenalty = config.getLastSlotFoodPenalty();
            this.lastSlotPrestigePenalty = config.getLastSlotPrestigePenalty();

        } catch (Exception e) {
            throw new RuntimeException("Errore caricamento turn_order_track.json", e);
        }
    }

    // Piazza il giocatore nel primo slot libero e restituisce l'indice
    public int placePlayer(Player player) {
        int index = getFirstFreeSlotIndex();
        if (index == -1) throw new IllegalStateException("Nessuno slot libero disponibile");
        occupantsBySlot.set(index, player);
        return index;
    }

    // Restituisce l'indice del primo slot libero, -1 se non ce ne sono
    public int getFirstFreeSlotIndex() {
        for (int i = 0; i < occupantsBySlot.size(); i++) {
            if (occupantsBySlot.get(i) == null) return i;
        }
        return -1;
    }

    // Rimuove il totem del giocatore e restituisce l'indice in cui era
    public int returnTotem(Player player) {
        for (int i = 0; i < occupantsBySlot.size(); i++) {
            if (player.equals(occupantsBySlot.get(i))) {
                occupantsBySlot.set(i, null);
                return i;
            }
        }
        throw new IllegalArgumentException("Giocatore non trovato nel tracciato");
    }

    // Restituisce il cibo bonus per lo slot indicato
    public int getFoodBonusForSlot(int slotIndex) {
        return foodBonusesBySlot.get(slotIndex);
    }

    // Restituisce l'occupante dello slot indicato
    public Player getOccupantAtSlot(int slotIndex) {
        return occupantsBySlot.get(slotIndex);
    }

    // Restituisce la lista degli occupanti in ordine (dall'alto verso il basso)
    public List<Player> getCurrentOrder() {
        return Collections.unmodifiableList(occupantsBySlot);
    }

    // Restituisce true se lo slot è l'ultimo
    public boolean isLastSlot(int slotIndex) {
        return slotIndex == playerCount - 1;
    }

    // Getters
    public int getLastSlotFoodPenalty() { return lastSlotFoodPenalty; }
    public int getLastSlotPrestigePenalty() { return lastSlotPrestigePenalty; }
    public List<Integer> getFoodBonusesBySlot() { return foodBonusesBySlot; }
    public int getPlayerCount() { return playerCount; }
}