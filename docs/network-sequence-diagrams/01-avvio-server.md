# 01 - Avvio server

Questo diagramma mostra cosa succede all'avvio: caricamento degli snapshot,
riattacco delle partite ripristinate e pubblicazione dei due trasporti.

```mermaid
sequenceDiagram
    autonumber
    participant SM as ServerMain
    participant PM as PersistenceManager
    participant MR as MatchRegistry
    participant MC as MainController
    participant VU as ViewUpdater
    participant GC as GameController
    participant RMI as RMIServer
    participant REG as RMI Registry
    participant SS as SocketServer
    participant SH as ShutdownHook

    SM->>PM: fromEnvironment()
    SM->>MR: new MatchRegistry(pm)
    SM->>PM: loadAll()
    PM-->>SM: List of MatchSnapshot

    loop per ogni snapshot valido
        SM->>MC: restore(snapshot, registry, pm)
        MC->>VU: new ViewUpdater()
        MC->>GC: new GameController(viewUpdater, snapshot.model)
        MC->>MC: status=IN_GAME, suspended=true
        MC->>MC: connState[playerId]=DISCONNECTED
        MC-->>SM: restored MainController
        SM->>MR: reattach(matchId, controller)
    end

    SM->>RMI: new RMIServer(matchRegistry)
    SM->>REG: LocateRegistry.createRegistry(port)
    SM->>REG: rebind(SERVICE_NAME, rmiServer)
    REG-->>SM: RMI service ready

    SM->>SS: new SocketServer(1100, matchRegistry)
    SM->>SS: start tcp-accept-loop thread
    SS-->>SM: TCP accept loop running

    SM->>SH: addShutdownHook()
    SH-->>MR: shutdownAll()
    SH-->>SS: stop()
    SH-->>RMI: unexportObject(...)
    SH-->>REG: unexportObject(...)
```

## Classi principali

| Responsabilita | Classe |
|---|---|
| Entry point server | `ServerMain` |
| Caricamento/salvataggio snapshot | `PersistenceManager` |
| Registro globale partite | `MatchRegistry` |
| Controller di una singola partita | `MainController` |
| Trasporto RMI | `RMIServer`, `RMI Registry` |
| Trasporto TCP | `SocketServer` |
