package it.polimi.ingws.gc8.Network.Messages;

import it.polimi.ingws.gc8.Model.GameModel;
import it.polimi.ingws.gc8.Network.DTO.GameDTO;

public class ModelUpdateMessage implements NetworkMessage{
    private GameDTO gameDTO;

    public ModelUpdateMessage(GameDTO gameDTO) {
        this.gameDTO = gameDTO;
    }

    public GameDTO getGameModelDTO() {
        return gameDTO;
    }
}
