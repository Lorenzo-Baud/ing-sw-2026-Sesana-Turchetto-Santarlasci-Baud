package it.polimi.ingws.gc8.Model.Board;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.polimi.ingws.gc8.Model.Player;
import it.polimi.ingws.gc8.Network.DTO.DtoMapper;
import it.polimi.ingws.gc8.Network.DTO.PlayerDTO;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Manager for player turn order and associated track rewards or penalties
 */
public class TurnOrderTrack {

    /**
     * Number of players in current game
     */
    private int playerCount;

    /**
     * Food bonuses assigned to each position on track
     */
    private List<Integer> foodBonusesBySlot;

    /**
     * Food penalty applied to player in last position
     */
    private int lastSlotFoodPenalty;

    /**
     * Prestige points penalty applied to player in last position
     */
    private int lastSlotPrestigePenalty;

    /**
     * List of players ordered by their position on track
     */
    private List<Player> occupantsBySlot;

    /**
     * Constructs turn order track and initializes empty slots based on player count
     *
     * @param playerCount number of players in game
     */
    public TurnOrderTrack(int playerCount) {
        this.playerCount = playerCount;
        // inizializza tutti gli slot a null (nessun occupante)
        this.occupantsBySlot = new ArrayList<>(Collections.nCopies(playerCount, null));
        loadConfig(playerCount);
    }

    /**
     * Loads turn order configuration from JSON resource file for specific player count
     *
     * @param playerCount number of players used to filter correct configuration
     * @throws RuntimeException if resource file is missing, malformed, or configuration is not found
     */
    private void loadConfig(int playerCount) {
        try {
            InputStream is = getClass()
                    .getClassLoader()
                    .getResourceAsStream("turn_order_track.json");

            ObjectMapper mapper = new ObjectMapper();

            List<TurnOrderConfig> configs = mapper.readValue(is,
                    new TypeReference<List<TurnOrderConfig>>() {});

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

    /**
     * Places player in first available slot and returns its position
     *
     * @param player player to place on track
     * @return index of occupied slot
     * @throws IllegalStateException if no free slots are available
     */
    public int placePlayer(Player player) {
        int index = getFirstFreeSlotIndex();
        if (index == -1) throw new IllegalStateException("Nessuno slot libero disponibile");
        occupantsBySlot.set(index, player);
        return index;
    }

    /**
     * Returns index of first available slot on track
     *
     * @return index of first null slot or -1 if track is full
     */
    public int getFirstFreeSlotIndex() {
        for (int i = 0; i < occupantsBySlot.size(); i++) {
            if (occupantsBySlot.get(i) == null) return i;
        }
        return -1;
    }

    /**
     * Removes player totem from track and returns its previous index
     *
     * @param player player whose totem is removed
     * @return index of slot that was occupied
     * @throws IllegalArgumentException if player is not found on track
     */
    public int returnTotem(Player player) {
        for (int i = 0; i < occupantsBySlot.size(); i++) {
            if (player.equals(occupantsBySlot.get(i))) {
                occupantsBySlot.set(i, null);
                return i;
            }
        }
        throw new IllegalArgumentException("Giocatore non trovato nel tracciato");
    }

    /**
     * Returns food bonus associated with specific slot index
     *
     * @param slotIndex index of track slot
     * @return amount of food bonus
     */
    public int getFoodBonusForSlot(int slotIndex) {
        return foodBonusesBySlot.get(slotIndex);
    }

    /**
     * Returns player currently occupying specific slot
     *
     * @param slotIndex index of track slot
     * @return occupant of slot or null if empty
     */
    public Player getOccupantAtSlot(int slotIndex) {
        return occupantsBySlot.get(slotIndex);
    }

    /**
     * Returns unmodifiable list of players in their current track order
     *
     * @return list of occupants by position
     */
    public List<Player> getCurrentOrder() {
        return Collections.unmodifiableList(occupantsBySlot);
    }

    /**
     * Checks if given slot index is last position on track
     *
     * @param slotIndex index to check
     * @return true if index represents last slot
     */
    public boolean isLastSlot(int slotIndex) {
        return slotIndex == playerCount - 1;
    }

    /**
     * Returns food penalty for last position
     * @return last slot food penalty
     */
    public int getLastSlotFoodPenalty() { return lastSlotFoodPenalty; }

    /**
     * Returns prestige penalty for last position
     * @return last slot prestige penalty
     */
    public int getLastSlotPrestigePenalty() { return lastSlotPrestigePenalty; }

    /**
     * Returns list of all food bonuses by position
     * @return food bonuses list
     */
    public List<Integer> getFoodBonusesBySlot() { return foodBonusesBySlot; }

    /**
     * Returns total player count for track
     * @return number of players
     */
    public int getPlayerCount() { return playerCount; }

    /**
     *
     * @return array list containing
     */
    public List<PlayerDTO> toDto() {
        List<PlayerDTO> dto = new ArrayList<>();
        for(int i = 0; i < 5; i++) {
            if(occupantsBySlot.get(i) == null) {;
                dto.add(null);
            } else {
                dto.add(DtoMapper.toDTO(occupantsBySlot.get(i)));
            }
        }
        return dto;
    }

}