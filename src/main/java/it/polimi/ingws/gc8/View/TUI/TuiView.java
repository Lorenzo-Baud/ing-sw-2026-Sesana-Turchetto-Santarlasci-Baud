package it.polimi.ingws.gc8.View.TUI;

import it.polimi.ingws.gc8.Network.DTO.GameDTO;
import it.polimi.ingws.gc8.View.View;

public class TuiView implements View {

    @Override
    public void showMessage(String message) {
    }

    @Override
    public void showError(String message) {}

    @Override
    public void update(GameDTO game){}
}
