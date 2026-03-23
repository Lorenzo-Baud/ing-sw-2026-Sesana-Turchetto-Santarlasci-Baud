package it.polimi.ingws.gc8.Model.Cards.BuildingCard;

import com.fasterxml.jackson.databind.JsonNode;
import it.polimi.ingws.gc8.Model.Cards.CharacterCard;
import it.polimi.ingws.gc8.Model.Cards.EventCard;
import it.polimi.ingws.gc8.Model.Enum.CharacterType;
import it.polimi.ingws.gc8.Model.Enum.EventType;
import it.polimi.ingws.gc8.Model.Player;

/**
 * A parameterizable {@link BuildingEffect} that covers most building cards in the game.
 * Each instance is configured from JSON and uses only a subset of its fields;
 */
public class ConfigurableEffect implements BuildingEffect {

    // Sustenance reduction
    private final CharacterType sustenanceType;
    private final int sustenanceReductionPerUnit;

    // Shamanic ritual parameters
    private final boolean shield;
    private final boolean doubleShamanBonus;
    private final boolean gainOnTie;
    private final int shamanExtraIcons;

    // Turn order
    private final int extraFoodOnTurnOrder;

    // Event trigger
    private final EventType triggerEvent;
    private final CharacterType triggerTargetType;
    private final int foodPerUnit;
    private final int ppPerUnit;

    // End game
    private final String endGameStrategy;
    private final CharacterType endGameTargetType;
    private final int endGamePPPerUnit;

    // Extra card pick
    private final boolean extraCardPick;

    private ConfigurableEffect(
            CharacterType sustenanceType, int sustenanceReductionPerUnit,
            boolean shield, boolean doubleShamanBonus, boolean gainOnTie, int shamanExtraIcons,
            int extraFoodOnTurnOrder,
            EventType triggerEvent, CharacterType triggerTargetType, int foodPerUnit, int ppPerUnit,
            String endGameStrategy, CharacterType endGameTargetType, int endGamePPPerUnit,
            boolean extraCardPick) {
        this.sustenanceType = sustenanceType;
        this.sustenanceReductionPerUnit = sustenanceReductionPerUnit;
        this.shield = shield;
        this.doubleShamanBonus = doubleShamanBonus;
        this.gainOnTie = gainOnTie;
        this.shamanExtraIcons = shamanExtraIcons;
        this.extraFoodOnTurnOrder = extraFoodOnTurnOrder;
        this.triggerEvent = triggerEvent;
        this.triggerTargetType = triggerTargetType;
        this.foodPerUnit = foodPerUnit;
        this.ppPerUnit = ppPerUnit;
        this.endGameStrategy = endGameStrategy;
        this.endGameTargetType = endGameTargetType;
        this.endGamePPPerUnit = endGamePPPerUnit;
        this.extraCardPick = extraCardPick;
    }

    /**
     * Creates a ConfigurableEffect from a JSON params object.
     * Only reads fields that are present; missing fields stay at default.
     * @return a configured ConfigurableEffect instance
     */
    public static ConfigurableEffect fromJson(JsonNode params) {
        // Sustenance reduction (E1_02, E1_06, E2_05)
        CharacterType sustenanceType = null;
        int sustenanceReductionPerUnit = 0;
        if (params.has("sustenanceType")) {
            sustenanceType = CharacterType.valueOf(params.get("sustenanceType").asText());
            sustenanceReductionPerUnit = params.get("sustenanceReductionPerUnit").asInt();
        }

        // Shaman modifiers (E1_04, E2_01, E2_02)
        boolean shield = params.has("shield") && params.get("shield").asBoolean();
        boolean doubleShamanBonus = params.has("doubleShamanBonus") && params.get("doubleShamanBonus").asBoolean();
        boolean gainOnTie = params.has("gainOnTie") && params.get("gainOnTie").asBoolean();
        int shamanExtraIcons = params.has("shamanExtraIcons") ? params.get("shamanExtraIcons").asInt() : 0;

        // Turn order (E1_05)
        int extraFoodOnTurnOrder = params.has("extraFoodOnTurnOrder") ? params.get("extraFoodOnTurnOrder").asInt() : 0;

        // Event trigger (E2_04, E2_06)
        EventType triggerEvent = null;
        CharacterType triggerTargetType = null;
        int foodPerUnit = 0;
        int ppPerUnit = 0;
        if (params.has("triggerEvent")) {
            switch (params.get("triggerEvent").asText()) {
                case "HUNT":          triggerEvent = EventType.hunt;         break;
                case "CAVE_PAINTING": triggerEvent = EventType.cavePaintings; break;
            }
            triggerTargetType = CharacterType.valueOf(params.get("targetType").asText());
            foodPerUnit = params.has("foodPerUnit") ? params.get("foodPerUnit").asInt() : 0;
            ppPerUnit   = params.has("ppPerUnit")   ? params.get("ppPerUnit").asInt()   : 0;
        }

        // End game strategy (E2_03, E2_07, E3_03–E3_08)
        String endGameStrategy = params.has("endGameStrategy") ? params.get("endGameStrategy").asText() : null;
        CharacterType endGameTargetType = null;
        int endGamePPPerUnit = 0;
        if (endGameStrategy != null) {
            endGamePPPerUnit = params.has("endGamePPPerUnit") ? params.get("endGamePPPerUnit").asInt() : 0;
            if (params.has("targetType")) {
                endGameTargetType = CharacterType.valueOf(params.get("targetType").asText());
            }
        }

        // Extra card pick (E3_01)
        boolean extraCardPick = params.has("extraCardPick") && params.get("extraCardPick").asBoolean();

        return new ConfigurableEffect(
                sustenanceType, sustenanceReductionPerUnit,
                shield, doubleShamanBonus, gainOnTie, shamanExtraIcons,
                extraFoodOnTurnOrder,
                triggerEvent, triggerTargetType, foodPerUnit, ppPerUnit,
                endGameStrategy, endGameTargetType, endGamePPPerUnit,
                extraCardPick);
    }

    //TRIGGERS

    /**
     * Awards food (and optionally PP) per character of the target type when a matching event fires.
     * Only activates if this effect is configured with a triggerEvent matching the current event.
     *
     * @param player the player who owns this building
     * @param event  the event card being resolved
     */
    @Override
    public void onEvent(Player player, EventCard event) {
        if (triggerEvent == null || event.getEventType() != triggerEvent) return;
        int count = player.getTribe().countByType(triggerTargetType);
        player.addFood(count * foodPerUnit);
        if (ppPerUnit > 0) player.addPrestigePoints(count * ppPerUnit);
    }

    /**
     * Returns bonus PP at end of game according to the configured strategy:
     * @param player the player who owns this building
     * @return bonus PP granted by this effect (0 if no end-game strategy is configured)
     */
    @Override
    public int onGameEnd(Player player) {
        if (endGameStrategy == null) return 0;
        switch (endGameStrategy) {
            case "TYPE":
                return player.getTribe().countByType(endGameTargetType) * endGamePPPerUnit;
            case "SET":
                return player.getTribe().getMinCountAcrossAllTypes() * endGamePPPerUnit;
            case "DOUBLE_BUILDER":
                return player.getTribe().getBuilderPrestigeSum();
            default:
                return 0;
        }
    }

    //QUERIES

    /**
     * @return extra shaman icons granted during Shamanic Ritual (0 if not configured)
     */
    @Override
    public int getShamanIconBonus() { return shamanExtraIcons; }

    /**
     * @return true if this building prevents PP loss when the player loses the Shamanic Ritual
     */
    @Override
    public boolean preventsShamanPenalty() { return shield; }

    /**
     * @return true if this building doubles the PP gained when the player wins the Shamanic Ritual
     */
    @Override
    public boolean doublesShamanBonus() { return doubleShamanBonus; }

    /**
     * @return true if this building grants PP even when tied during the Shamanic Ritual
     */
    @Override
    public boolean gainsOnShamanTie() { return gainOnTie; }

    /**
     * Returns the food cost reduction during the Sustenance event.
     * Calculated as: count of sustenanceType characters in tribe × sustenanceReductionPerUnit.
     *
     * @param player the player who owns this building
     * @return food reduction (0 if no sustenance reduction is configured)
     */
    @Override
    public int getSustenanceFoodReduction(Player player) {
        if (sustenanceType == null) return 0;
        return player.getTribe().countByType(sustenanceType) * sustenanceReductionPerUnit;
    }

    /**
     * @return true if this building allows the player to pick one extra card after all Totems return
     */
    @Override
    public boolean canPickExtraCard() { return extraCardPick; }

    /**
     * @return extra food gained when the player's Totem lands on a Turn Order slot with a food bonus
     */
    @Override
    public int getExtraFoodOnTurnOrder() { return extraFoodOnTurnOrder; }

}
