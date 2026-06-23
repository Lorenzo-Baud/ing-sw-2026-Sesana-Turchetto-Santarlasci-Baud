package it.polimi.ingws.gc8.Model.Cards.BuildingCard;

import it.polimi.ingws.gc8.Model.Cards.CharacterCards.CharacterCard;
import it.polimi.ingws.gc8.Model.Cards.CharacterCards.Inventor;
import it.polimi.ingws.gc8.Model.Enum.CharacterType;
import it.polimi.ingws.gc8.Model.Player;

/**
 * Building effect that awards food each time the player forms a new pair of identical Inventors.
 */
public class InventorPairEffect implements BuildingEffect {

    private static final long serialVersionUID = 1L;

    /** Food granted for each new pair of Inventors formed */
    private final int foodReward;

    /**
     * @param foodReward food granted each time a new pair of Inventors is formed
     */
    public InventorPairEffect(int foodReward) {
        this.foodReward = foodReward;
    }

    /**
     * Checks whether the newly added card completes a new Inventor pair.
     * Only fires if the added card is an Inventor and has the same icon as others inventors in the tribe
     *
     * @param player the player who owns this building
     * @param card   the character card just added to the tribe
     */
    @Override
    public void onCardAdded(Player player, CharacterCard card) {
        if (card.getCharacterType() != CharacterType.INVENTOR) return;

        int count = 0;
        for (CharacterCard c : player.getTribe().getCharacters()) {
            if (c.getCharacterType() == CharacterType.INVENTOR) {

                if (((Inventor) c).getIcon() == (((Inventor) card )).getIcon()) {
                    count++;
                }
            }
        }

        if(count % 2 == 0){
            player.addFood(foodReward);
        }
    }
}
