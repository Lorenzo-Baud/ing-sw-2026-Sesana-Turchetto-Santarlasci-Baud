package it.polimi.ingws.gc8.Model.Cards.BuildingCard;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.polimi.ingws.gc8.Model.Cards.Card;
import it.polimi.ingws.gc8.Model.Enum.Era;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class BuildingCard extends Card {

    private String id;
    private int foodCost;
    private int prestigeBonus;
    private BuildingEffect effect;

    public BuildingCard(String id, int foodCost, int prestigeBonus,
                        Era era, String front, String back,
                        BuildingEffect effect) {
        super(front, back, era);
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
            JsonNode root = mapper.readTree(new File(path));

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
            e.printStackTrace();
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

    // Getters/setters
    public String getId() {
        return id;
    }

    public int getFoodCost() {
        return foodCost;
    }

    public int getPrestigeBonus() {
        return prestigeBonus;
    }

    public BuildingEffect getEffect() {
        return effect;
    }

}
