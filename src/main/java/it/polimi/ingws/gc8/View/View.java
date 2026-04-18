package it.polimi.ingws.gc8.View;
import it.polimi.ingws.gc8.Network.DTO.GameDTO;

public interface View {
    void update(GameDTO game);
    void showError(String message);
    void showMessage(String message);
}
