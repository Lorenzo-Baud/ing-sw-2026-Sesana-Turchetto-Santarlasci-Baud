package it.polimi.ingws.gc8;


import it.polimi.ingws.gc8.Model.GameModel;
import it.polimi.ingws.gc8.Model.MesosGame;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args )
    {

        System.out.println( "Hello World!" );

        GameModel game = new MesosGame();
        game.setupGame(3);

        game.getBoard().getTribeDeck().forEach(card -> {
            System.out.println(card.getEra());
            // If instanceof EventCard, print the event type
             if (card instanceof it.polimi.ingws.gc8.Model.Cards.EventCard) {
                 System.out.println(((it.polimi.ingws.gc8.Model.Cards.EventCard) card).getEventType());
             }
        });

    }

}
