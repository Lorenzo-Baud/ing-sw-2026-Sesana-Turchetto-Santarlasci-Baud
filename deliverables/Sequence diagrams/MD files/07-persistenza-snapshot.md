# 07 - Persistenza snapshot

La persistenza salva uno snapshot dopo ogni azione applicata e anche
periodicamente. La serializzazione avviene sotto game-lock; la scrittura su disco
avviene fuori lock, su executor dedicato.

```mermaid
sequenceDiagram
    autonumber
    participant CH as ClientHandler
    participant MC as MainController
    participant GC as GameController
    participant GS as GameState
    participant MG as GameModel
    participant PE as persistExec
    participant PM as PersistenceManager
    participant FS as File system

    CH->>MC: dispatchAction(PlayerAction)
    MC->>GC: handleAction(action)
    GC->>GS: handleAction(context, action)
    GS->>MG: mutate model
    MG-->>GC: model updated
    GC-->>MC: action handling completed

    MC->>MC: persistAsync()
    MC->>MC: copy metadata (nickToPlayerId, host, counters)
    MC->>GC: withGameLock(...)
    GC->>MC: build MatchSnapshot(matchId, metadata, model)
    MC->>PM: toBytes(snapshot)
    PM-->>MC: byte[]
    GC-->>MC: release game-lock

    MC->>PE: submit(save task)
    PE->>PM: saveBytes(matchId, data)
    PM->>FS: write *.snapshot.tmp
    PM->>FS: copy current *.snapshot to *.snapshot.bak
    PM->>FS: move tmp to *.snapshot
    PM-->>PE: save completed

    opt fallback periodico
        MC->>MC: scheduler tick every PERSIST_INTERVAL_MS
        MC->>MC: persistAsync()
    end

    opt game over
        GC->>MC: onGameOver callback
        MC->>MC: shutdown()
        MC->>PM: delete(matchId)
        PM->>FS: delete snapshot, bak, tmp
        MC->>MC: registry.removeMatch(matchId)
    end
```

## Garanzie

- Lo snapshot e' una root unica: `MatchSnapshot`.
- Nessuna connessione, socket, thread o `ClientHandler` viene serializzato.
- Il file finale non viene scritto direttamente: temp file, backup e rename.
