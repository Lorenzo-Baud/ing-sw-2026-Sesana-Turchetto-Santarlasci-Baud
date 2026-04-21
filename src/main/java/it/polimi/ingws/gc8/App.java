package it.polimi.ingws.gc8;


import it.polimi.ingws.gc8.Model.Cards.EventCards.EventCard;
import it.polimi.ingws.gc8.Model.GameModel;
import it.polimi.ingws.gc8.Model.MesosGame;
import it.polimi.ingws.gc8.Network.ViewUpdater;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args )
    {

        System.out.println( "Hello World!" );
        ViewUpdater viewUpdater = new ViewUpdater();
        GameModel game = new MesosGame();
        game.addObserver(viewUpdater);
        game.setupGame(3);

        game.getBoard().getTribeDeck().forEach(card -> {
            System.out.println(card.getEra());
            // If instanceof EventCard, print the event type
             if (card instanceof EventCard) {
                 System.out.println(((EventCard) card).getEventType());
             }
        });

    }

}
