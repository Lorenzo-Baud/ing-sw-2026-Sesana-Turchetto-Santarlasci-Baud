package it.polimi.ingws.gc8.Model.Cards.BuildingCard;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.polimi.ingws.gc8.Model.Cards.Card;
import it.polimi.ingws.gc8.Model.Enum.CardType;
import it.polimi.ingws.gc8.Model.Enum.Era;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class BuildingCard extends Card {

    private static final long serialVersionUID = 1L;

    /** Logger object */
    private static final Logger LOGGER = Logger.getLogger(BuildingCard.class.getName());

    /** Building card id */
    private String id;
    /** Food cost */
    private int foodCost;
    /** Prestige bonus given at end of the match */
    private int prestigeBonus;
    /** Effect of the card */
    private BuildingEffect effect;

    /**
     * Building card constructor
     * @param id id of the building
     * @param foodCost cost of the building
     * @param prestigeBonus prestige bonus at end of the match
     * @param era era
     * @param front front image
     * @param back back image
     * @param effect building effect
     */
    public BuildingCard(String id, int foodCost, int prestigeBonus,
                        Era era, String front, String back,
                        BuildingEffect effect) {
        super(front, back, era, CardType.BUILDING);
        this.id = id;
        this.foodCost = foodCost;
        this.prestigeBonus = prestigeBonus;
        this.effect = effect;
    }

    /**
     * Loads all Building cards from a JSON file.
     * @param path path to the buildings_cards.json file
     * @return list of all BuildingCard objects
     */
    public static List<BuildingCard> loadFromJson(String path) {
        List<BuildingCard> cards = new ArrayList<>();
        ObjectMapper mapper = new ObjectMapper();

        try {

            InputStream is = BuildingCard.class.getResourceAsStream(path);

            if (is == null) {
                throw new FileNotFoundException("Impossibile trovare buildings_cards.json nelle risorse del JAR");
            }

            JsonNode root = mapper.readTree(is);


            for (JsonNode node : root) {
                // Skip comment entries
                if (!node.has("id")) continue;

                // Read card data
                String id = node.get("id").asText();
                int foodCost = node.get("foodCost").asInt();
                int prestigeBonus = node.has("prestigeBonus") && !node.get("prestigeBonus").isNull()
                        ? node.get("prestigeBonus").asInt()
                        : 0;
                int eraNum = Integer.parseInt(node.get("era").asText().replace("ERA_", ""));
                Era era = Era.values()[eraNum - 1];
                String front = node.get("front").asText();
                String back = node.get("back").asText();

                // Read effect
                String effectType = node.get("effectType").asText();
                JsonNode effectParams = node.get("effectParams");
                BuildingEffect effect = BuildingEffect.createFrom(effectType, effectParams);

                cards.add(new BuildingCard(id, foodCost, prestigeBonus, era, front, back, effect));
            }

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to parse buildings from " + path + ": building deck may be incomplete", e);
        }

        return cards;
    }

    /**
     * Returns the effective food cost after applying the Builder discount.
     * The cost can never go below zero.
     * @param builderDiscount total discount from all Builders in the player's tribe
     * @return effective food cost
     */
    public int getEffectiveCost(int builderDiscount) {
        return Math.max(0, foodCost - builderDiscount);
    }

    /**
     * Returns the unique identifier of the building.
     * @return the building id
     */
    public String getId() {
        return id;
    }

    /**
     * Returns the amount of food resources required to build this card.
     * @return the food cost
     */
    public int getFoodCost() {
        return foodCost;
    }

    /**
     * Returns the amount of prestige points awarded by this building.
     * @return the prestige bonus
     */
    public int getPrestigeBonus() {
        return prestigeBonus;
    }

    /**
     * Returns the special effect or ability associated with this building.
     * @return the building effect
     */
    public BuildingEffect getEffect() {
        return effect;
    }

}
