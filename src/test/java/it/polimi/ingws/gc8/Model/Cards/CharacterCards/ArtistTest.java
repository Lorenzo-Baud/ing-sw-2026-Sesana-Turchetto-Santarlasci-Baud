package it.polimi.ingws.gc8.Model.Cards.CharacterCards;

import it.polimi.ingws.gc8.Model.Enum.CardType;
import it.polimi.ingws.gc8.Model.Enum.CharacterType;
import it.polimi.ingws.gc8.Model.Enum.Era;
import it.polimi.ingws.gc8.Network.DTO.CardDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ArtistTest {

    @Test
    @DisplayName("Should correctly initialize Artist and verify all inherited fields")
    void testArtistInitialization() {

        String front = "artist_front.png";
        String back = "artist_back.png";
        Era era = Era.ERA_1;
        int players = 4;

        // Initializes class
        Artist artist = new Artist(front, back, era, players);

        // Assert
        assertAll("Verify Artist properties",
                () -> assertEquals(front, artist.getFront(), "Front image mismatch"),
                () -> assertEquals(back, artist.getBack(), "Back image mismatch"),
                () -> assertEquals(Era.ERA_1, artist.getEra(), "Era mismatch"),
                () -> assertEquals(players, artist.getNumberOfPlayers(), "Player count mismatch"),
                () -> assertEquals(CharacterType.ARTIST, artist.getCharacterType(), "CharacterType must be ARTIST")
        );
    }

    @Test
    @DisplayName("Should maintain consistency across different eras")
    void testArtistEras() {
        // Test Artist in different Eras to ensure the enum is handled correctly
        Artist artistEra2 = new Artist("f", "b", Era.ERA_2, 2);
        Artist artistEra3 = new Artist("f", "b", Era.ERA_3, 3);

        assertEquals(Era.ERA_2, artistEra2.getEra());
        assertEquals(Era.ERA_3, artistEra3.getEra());
    }

    @Test
    void testToDTO() {
        String front = "artist_front.png";
        String back = "artist_back.png";

        // Using Artist as a concrete implementation of CharacterCard
        CharacterCard card = new Artist(front, back, Era.ERA_1, 2);

        CardDTO dto = card.toDTO();

        assertNotNull(dto);
        assertEquals(front, dto.front());
        assertEquals(back, dto.back());
        assertEquals(CardType.CHARACTER, dto.type());
        assertFalse(dto.isFlipped());
    }

}