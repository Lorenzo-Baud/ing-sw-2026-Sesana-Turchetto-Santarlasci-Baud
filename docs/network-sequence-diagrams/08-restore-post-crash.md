# 08 - Restore post-crash

Dopo un crash del server, le connessioni sono perse ma gli snapshot restano su
disco. Al riavvio il server ricostruisce le partite come `IN_GAME` e sospese; i
client rientrano con `matchId` e nickname.

```mermaid
sequenceDiagram
    autonumber
    participant OLD as Server process crashed
    participant SM as ServerMain restarted
    participant PM as PersistenceManager
    participant MC as MainController
    participant GC as GameController
    participant MR as MatchRegistry
    participant C1 as Client 1
    participant C2 as Client 2
    participant SC as ServerConnection
    participant CH as ClientHandler
    participant VU as ViewUpdater

    OLD-->>SM: process terminated, RAM and connections lost
    SM->>PM: loadAll()
    PM-->>SM: MatchSnapshot[]

    loop per ogni snapshot
        SM->>MC: restore(snapshot, registry, pm)
        MC->>VU: new ViewUpdater()
        MC->>GC: new GameController(viewUpdater, restoredModel)
        GC->>GC: setDisconnectedCheck(mc::isDisconnected)
        GC->>GC: setOnGameOver(mc::handleGameOver)
        MC->>MC: status=IN_GAME, suspended=true
        MC->>MC: all players DISCONNECTED
        MC->>MC: start heartbeat and persistence
        SM->>MR: reattach(matchId, controller)
    end

    C1->>SC: connect()
    C1->>SC: joinMatch(matchId, nickname1)
    SC->>CH: join request
    CH->>MR: getMatch(matchId)
    MR-->>CH: restored MainController
    CH->>MC: joinOrReconnect(nickname1, handler)
    MC->>MC: connState[player1]=CONNECTED
    MC-->>CH: JoinResult(reconnected=true)
    CH-->>SC: JoinResult
    CH->>MC: completeReconnection(handler)
    MC->>VU: sendFullSnapshotTo(handler, model)
    VU-->>C1: ModelUpdateMessage(GameDTO)

    alt solo un client rientrato
        MC->>MC: suspended remains true
        MC-->>C1: actions rejected with ErrorMessage(SUSPENDED)
    else secondo client rientra
        C2->>SC: joinMatch(matchId, nickname2)
        SC->>CH: join request
        CH->>MC: joinOrReconnect(nickname2, handler)
        MC->>MC: connState[player2]=CONNECTED
        MC->>MC: maybeResumeAfterReconnect()
        MC->>VU: notifyMatchResumed()
        VU-->>C1: ErrorMessage(RESUMED)
        VU-->>C2: ErrorMessage(RESUMED)
        CH->>MC: completeReconnection(handler)
        MC->>VU: sendFullSnapshotTo(handler, model)
        VU-->>C2: ModelUpdateMessage(GameDTO)
    end
```

## Nota operativa

`listOpenMatches()` mostra solo le lobby. Per rientrare in una partita gia'
iniziata dopo restore, il client deve conoscere il `matchId` della partita e il
proprio nickname.
