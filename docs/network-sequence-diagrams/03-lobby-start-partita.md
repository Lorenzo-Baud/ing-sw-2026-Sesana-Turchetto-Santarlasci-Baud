# 03 - Lobby e start partita

Il flusso di lobby e' request/response solo per i comandi host. Gli aggiornamenti
di lobby e di partita sono push event-driven verso tutti i client.

```mermaid
sequenceDiagram
    autonumber
    participant H as Host client
    participant SC as ServerConnection
    participant CH as ClientHandler
    participant MC as MainController
    participant VU as ViewUpdater
    participant GC as GameController
    participant MG as MesosGame
    participant C as ClientHandler listeners
    participant PM as PersistenceManager

    H->>SC: setExpectedPlayers(n)
    alt RMI
        SC->>MC: setExpectedPlayers(playerId, matchId, n)
    else TCP
        SC->>CH: SetExpectedPlayersCommand(n)
        CH->>MC: setExpectedPlayers(playerId, n)
    end

    MC->>MC: validate host, MIN_PLAYERS, MAX_PLAYERS
    MC->>C: LobbyUpdateMessage(current, expected, nicknames)

    alt lobby.size == expectedPlayers or host requestStartGame()
        MC->>MC: tryStartGame()
        MC->>VU: new ViewUpdater()
        loop per ogni client in lobby
            MC->>VU: addListener(clientHandler)
        end

        MC->>GC: new GameController(viewUpdater)
        MC->>GC: setDisconnectedCheck(this::isDisconnected)
        MC->>GC: setOnGameOver(this::handleGameOver)
        MC->>GC: startGame(players)
        GC->>MG: setupGame(numPlayers, players)
        MG->>VU: notifyUpdate(model)
        VU->>C: ModelUpdateMessage(GameDTO)

        GC->>GC: transitionTo(POSITIONING)
        GC->>MG: setPhase(POSITIONING)
        MG->>VU: notifyUpdate(model)
        VU->>C: ModelUpdateMessage(GameDTO)
        VU->>C: TurnUpdateMessage(firstPlayer)

        MC->>MC: schedule heartbeatTick()
        MC->>MC: startPersistence()
        MC->>PM: persistAsync()
    end
```

## Messaggi prodotti

| Evento | Messaggio |
|---|---|
| Cambio lobby | `LobbyUpdateMessage` |
| Setup model / cambio fase | `ModelUpdateMessage` |
| Primo turno | `TurnUpdateMessage` |

`PhaseUpdateMessage` esiste nel package messaggi, ma il flusso implementato usa
soprattutto snapshot completi via `ModelUpdateMessage`.
