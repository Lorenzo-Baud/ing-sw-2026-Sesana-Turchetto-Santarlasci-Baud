# 06 - Riconnessione

La riconnessione usa la stessa API del join: `joinMatch(matchId, nickname)`.
Se il nickname e' gia' noto nella partita e non e' attualmente connesso, il
client rientra con lo stesso `playerId`.

```mermaid
sequenceDiagram
    autonumber
    participant C as Client
    participant SC as ServerConnection
    participant TR as Transport endpoint
    participant CH as ClientHandler
    participant MR as MatchRegistry
    participant MC as MainController
    participant VU as ViewUpdater
    participant O as Other clients

    C->>SC: joinMatch(matchId, nickname)

    alt RMI
        SC->>TR: RemoteServer.joinMatch(matchId, nickname, callback)
        TR->>MR: getMatch(matchId)
        TR->>CH: new ClientHandler(RMIClientConnection, registry, nickname)
    else TCP
        SC->>CH: JoinMatchRequest(matchId, nickname)
        CH->>MR: getMatch(matchId)
    end

    MR-->>CH: MainController
    CH->>MC: joinOrReconnect(nickname, handler)

    alt nickname noto e DISCONNECTED
        MC->>CH: attachToMatch(this, originalPlayerId)
        MC->>MC: clientMap[playerId]=handler
        MC->>MC: connState[playerId]=CONNECTED
        MC->>MC: lastPongMs[playerId]=now
        MC->>VU: notifyPlayerReconnected(nickname)
        VU-->>O: ConnectionUpdateMessage(connected=true)
        MC->>MC: maybeResumeAfterReconnect()
        MC-->>CH: JoinResult(reconnected=true)
        CH-->>SC: JoinResult

        Note over CH,MC: Snapshot inviato solo dopo JoinResult, per non desincronizzare TCP
        CH->>MC: completeReconnection(handler)
        MC->>VU: addListener(handler)
        MC->>VU: sendFullSnapshotTo(handler, model)
        VU-->>CH: ModelUpdateMessage(GameDTO)

        opt countConnected() >= MIN_TO_CONTINUE
            MC->>VU: notifyMatchResumed()
            VU-->>O: ErrorMessage(RESUMED)
        end
    else nickname gia' CONNECTED
        MC-->>CH: error "Nickname gia' attivo"
        CH-->>SC: ErrorMessage
    else nickname sconosciuto con match IN_GAME
        MC-->>CH: error "Match in corso, nickname non riconosciuto"
        CH-->>SC: ErrorMessage
    end
```

## Regole

- Il client deve conoscere `matchId` e nickname.
- Il `playerId` originale viene ripristinato dal server.
- Una doppia connessione viva con lo stesso nickname viene rifiutata.
- Lo snapshot completo al client rientrante e' un `ModelUpdateMessage`.
