# 05 - Heartbeat e disconnessione

Il server rileva drop silenziosi con ping/pong applicativi. Tutti i canali di
disconnessione convergono su `ClientHandler.finally -> unregisterClient`.

```mermaid
sequenceDiagram
    autonumber
    participant SCH as Match scheduler
    participant MC as MainController
    participant CH as ClientHandler
    participant CONN as ClientConnection
    participant CC as ClientController
    participant SC as ServerConnection
    participant VU as ViewUpdater
    participant GC as GameController
    participant GS as GameState

    loop ogni PING_INTERVAL_MS
        SCH->>MC: heartbeatTick()
        MC->>MC: check lastPongMs

        alt pong ancora valido
            MC->>CH: onGameUpdate(PingMessage)
            CH->>CONN: sendMessage(PingMessage)
            CONN-->>CC: receive(PingMessage)
            CC->>SC: pong()
            alt RMI
                SC->>MC: onPong(playerId)
            else TCP
                SC->>CH: PongMessage
                CH->>MC: onPong(playerId)
            end
        else timeout superato
            MC->>CH: close()
            CH->>CONN: close()
        end
    end

    alt EOF, write fallita o close da heartbeat
        CONN-->>CH: receiveMessage throws
        CH->>CONN: close()
        CH->>MC: unregisterClient(handler)
        MC->>MC: connState[playerId]=DISCONNECTED
        MC->>VU: notifyPlayerDisconnected(nickname)
        VU-->>CH: ConnectionUpdateMessage(disconnected)

        alt countConnected() == 1
            MC->>MC: suspended=true
            MC->>VU: notifyMatchSuspended(soleSurvivor)
            VU-->>CH: ErrorMessage(SUSPENDED)
            MC->>SCH: schedule sole-survivor timeout
        else almeno 2 connessi
            MC->>GC: onPlayerDisconnected()
            GC->>GS: onCurrentPlayerDisconnected(context)
            GS->>GS: skip disconnected player if current
        end
    end

    opt sole-survivor timeout scaduto
        SCH->>MC: declareSoleSurvivorWinner()
        MC->>GC: forceGameOver(message)
        GC->>VU: notifyGameOver(model, message)
        VU-->>CH: GameOverMessage
        MC->>MC: shutdown()
    end
```

## Canali di rilevazione

| Canale | Effetto |
|---|---|
| EOF in read | il peer ha chiuso ordinatamente |
| `IOException` in write | il server non riesce piu' a scrivere al peer |
| timeout heartbeat | nessun `PongMessage` entro `PING_TIMEOUT_MS` |
