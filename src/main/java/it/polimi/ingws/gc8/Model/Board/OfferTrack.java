package it.polimi.ingws.gc8.Model.Board;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import it.polimi.ingws.gc8.Model.Player;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class OfferTrack {
    private List<OfferTile> tiles;

    public OfferTrack(int playerCount) {
        this.tiles = new ArrayList<>();
        loadTiles(playerCount);
    }

    private void loadTiles(int playerCount) {
        try {
            // getClass() → prende la classe corrente (OfferTrack)
            // getClassLoader() → prende il ClassLoader, che sa dove sono i file del progetto
            // getResourceAsStream("offer_tiles.json") → apre il file JSON come stream di bytes
            //   cercandolo nella cartella resources/ (che Maven mette nel classpath)
            InputStream is = getClass()
                    .getClassLoader()
                    .getResourceAsStream("offer_tiles.json");

            // ObjectMapper è la classe principale di Jackson
            // è quella che sa leggere JSON e convertirlo in oggetti Java
            ObjectMapper mapper = new ObjectMapper();

            // mapper.readValue() legge il JSON dallo stream e lo converte
            // TypeReference<List<OfferTile>>(){} dice a Jackson il tipo esatto da creare:
            //   "voglio una List di OfferTile"
            // serve perché Java a runtime non sa il tipo generico (type erasure)
            // allTiles contiene TUTTE le 7 tessere lette dal JSON
            List<OfferTile> allTiles = mapper.readValue(is,
                    new TypeReference<List<OfferTile>>() {});

            // .stream() → trasforma la lista in uno stream per elaborarla
            // .filter() → tiene solo le tessere disponibili per il numero di giocatori
            //   es. con 4 giocatori scarta la A (minPlayers=5)
            // .sorted() → ordina le tessere per nome dell'enum (A, B, C, D, E, F, G)
            //   .name() restituisce il nome dell'enum come stringa, es. "A", "B"...
            // .collect(Collectors.toList()) → riconverte lo stream in una List
            this.tiles = allTiles.stream()
                    .filter(t -> t.isAvailableFor(playerCount))
                    .sorted(Comparator.comparing(t -> t.getId().name()))
                    .collect(Collectors.toList());

        } catch (Exception e) {
            // se qualcosa va storto (file non trovato, JSON malformato, ecc.)
            // lancia una RuntimeException con messaggio chiaro e l'eccezione originale
            throw new RuntimeException("Errore caricamento offer_tiles.json", e);
        }
    }

    public List<OfferTile> getTiles() { return tiles; }

    public OfferTile getTileById(OffertileId id) {
        for (OfferTile tile : tiles) {
            if (tile.getId() == id) return tile;
        }
        return null;
    }

    public boolean isTileFree(OffertileId id) {
        OfferTile tile = getTileById(id);
        return tile != null && tile.getOccupant() == null;
    }

    public void placeTotem(OffertileId id, Player player) {
        OfferTile tile = getTileById(id);
        if (tile != null && tile.getOccupant() == null) {
            tile.setOccupant(player);
        } else {
            throw new IllegalStateException("Tile not found or already occupied.");
        }
    }

    public List<OfferTile> getAvailableTiles() {
        return tiles.stream()
                .filter(t -> t.getOccupant() == null)
                .collect(Collectors.toList());
    }

    public List<OfferTile> getResolutionOrder() {
        return tiles.stream()
                .filter(t -> t.getOccupant() != null)
                .collect(Collectors.toList());
    }

    public void clear() {
        tiles.forEach(t -> t.setOccupant(null));
    }

    //controlla se il giocatore ha già piazzato il suo totem su una tessera del tracciato
    public boolean containsPlayer(Player player) {
        for (OfferTile t : tiles) {
            if (player.equals(t.getOccupant())) {
                return true;  // trovato, esce subito
            }
        }
        return false;  // non trovato in nessuna tessera
    }

    //controlla se tutti i giocatori hanno già piazzato il loro totem su una tessera del tracciato
    public boolean isFull(int playerCount) {
        int occupied = 0;
        for (OfferTile t : tiles) {
            if (t.getOccupant() != null) {
                occupied++;
            }
        }
        return occupied == playerCount;
    }
}