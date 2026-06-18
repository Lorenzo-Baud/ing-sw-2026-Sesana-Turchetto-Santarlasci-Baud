package it.polimi.ingws.gc8.Model.Cards.BuildingCard;

import it.polimi.ingws.gc8.Model.Cards.CharacterCards.CharacterCard;
import it.polimi.ingws.gc8.Model.Player;

/**
 * Building effect that awards food each time the player completes a new set of
 * all 6 different Character types.
 *
 * This effect tracks how many sets have been completed so far (completedSets)
 * and fires foodReward food for each newly completed set when a character card is added.
 */
public class SetCompletionEffect implements BuildingEffect {

    private static final long serialVersionUID = 1L;

    private final int foodReward;
    private int completedSets;

    /**
     * @param foodReward food granted each time a new complete set of 6 types is formed
     */
    public SetCompletionEffect(int foodReward) {
        this.foodReward = foodReward;
        this.completedSets = 0;
    }

    /**
     * Checks whether a new complete set of all 6 character types has been formed
     * after adding the given card. Awards foodReward for each newly completed set.
     *
     * @param player the player who owns this building
     * @param card   the character card just added to the tribe
     */
    @Override
    public void onCardAdded(Player player, CharacterCard card) {
        int newSets = player.getTribe().getMinCountAcrossAllTypes();
        if (newSets > completedSets) {
            player.addFood(foodReward * (newSets - completedSets));
            completedSets = newSets;
        }
    }
}
