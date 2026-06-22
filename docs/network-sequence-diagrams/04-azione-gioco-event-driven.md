# 04 - Azione di gioco event-driven

Durante la partita il client non riceve una risposta sincrona alla propria
azione. La view invia una `PlayerAction`; il server aggiorna il model e poi
manda push event-driven a tutti i listener della partita.

```mermaid
sequenceDiagram
    autonumber
    participant V as View
    participant CC as ClientController
    participant SC as ServerConnection
    participant RS as RMIServer
    participant RMIQ as RMIClientConnection
    participant TCP as SocketClientConnection
    participant CH as ClientHandler
    participant MC as MainController
    participant GC as GameController
    participant GS as GameState
    participant MG as MesosGame
    participant VU as ViewUpdater
    participant L as ClientHandler listeners

    V->>CC: updateFromView(PlayerAction)
    CC->>SC: sendAction(action)

    alt RMI
        SC->>RS: submitAction(action, matchId)
        RS->>RMIQ: enqueueAction(action)
        RMIQ-->>CH: receiveMessage()
    else TCP
        SC->>TCP: writeObject(PlayerAction)
        TCP-->>CH: receiveMessage()
    end

    CH->>CH: validate action.playerId == handler.playerId
    CH->>MC: dispatchAction(action)

    alt partita sospesa
        MC->>VU: notifyError(playerId, SUSPENDED, ...)
        VU->>CH: ErrorMessage
    else partita attiva
        MC->>GC: handleAction(action)
        GC->>GS: states[model.phase].handleAction(context, action)
        GS->>MG: mutate domain state
        MG->>VU: notifyUpdate(model)
        VU->>L: ModelUpdateMessage(GameDTO)
        opt cambio turno
            VU->>L: TurnUpdateMessage(activePlayer)
        end
        opt errore di validazione
            GS->>VU: notifyError(playerId, code, detail)
            VU->>CH: ErrorMessage
        end
        MC->>MC: persistAsync()
    end

    L-->>SC: NetworkMessage push
    SC-->>CC: receive()
    CC-->>V: addMessage(NetworkMessage)
```

## Confine MVC

- La view invia solo azioni semantiche (`PlayerAction`).
- Il model non conosce la view.
- Il server non ritorna il risultato dell'azione come response sincrona.
- Lo stato visibile alla UI arriva come DTO dentro `ModelUpdateMessage`.
