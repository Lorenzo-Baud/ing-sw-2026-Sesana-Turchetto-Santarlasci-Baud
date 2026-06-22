# 02 - Selezione partita

Il client si connette, ottiene la lista delle lobby aperte e poi crea o entra in
una partita. Il ramo RMI usa metodi remoti; il ramo TCP usa comandi
`Serializable` letti dal `ClientHandler` in fase pre-match.

```mermaid
sequenceDiagram
    autonumber
    participant CM as ClientMain
    participant SC as ServerConnection
    participant RS as RMIServer
    participant SS as SocketServer
    participant SCC as SocketClientConnection
    participant CH as ClientHandler
    participant MR as MatchRegistry
    participant MC as MainController

    CM->>SC: connect(host, port)
    alt RMI
        SC->>RS: lookup RemoteServer via RMI Registry
        SC->>SC: create RMIClientView callback
    else TCP
        SC->>SS: open socket
        SS->>SCC: new SocketClientConnection(socket)
        SS->>CH: new ClientHandler(connection, registry, null)
        SS->>CH: start client-handler-tcp
    end

    loop refresh schermata selezione
        CM->>SC: listMatches()
        alt RMI
            SC->>RS: listOpenMatches()
            RS->>MR: listOpenMatches()
            MR-->>RS: List of MatchInfo
            RS-->>SC: List of MatchInfo
        else TCP
            SC->>CH: ListMatchesRequest
            CH->>MR: listOpenMatches()
            MR-->>CH: List of MatchInfo
            CH-->>SC: ListMatchesResponse
        end
        SC-->>CM: List of MatchInfo
    end

    alt Nuova partita
        CM->>SC: createMatch(matchName, nickname)
        alt RMI
            SC->>RS: createMatch(matchName, nickname, callback)
            RS->>MR: createMatch(matchName)
            MR-->>RS: MainController
            RS->>CH: new ClientHandler(RMIClientConnection, registry, nickname)
            RS->>MC: joinOrReconnect(nickname, handler)
            MC-->>RS: JoinResult(reconnected=false)
            RS->>CH: start client-handler-rmi
            RS-->>SC: JoinResult
        else TCP
            SC->>CH: CreateMatchRequest(matchName, nickname)
            CH->>MR: createMatch(matchName)
            MR-->>CH: MainController
            CH->>MC: joinOrReconnect(nickname, handler)
            MC-->>CH: JoinResult(reconnected=false)
            CH-->>SC: JoinResult
        end
    else Join partita esistente
        CM->>SC: joinMatch(matchId, nickname)
        alt RMI
            SC->>RS: joinMatch(matchId, nickname, callback)
            RS->>MR: getMatch(matchId)
            MR-->>RS: MainController
            RS->>CH: new ClientHandler(RMIClientConnection, registry, nickname)
            RS->>MC: joinOrReconnect(nickname, handler)
            MC-->>RS: JoinResult
            RS->>CH: start client-handler-rmi
            RS-->>SC: JoinResult
        else TCP
            SC->>CH: JoinMatchRequest(matchId, nickname)
            CH->>MR: getMatch(matchId)
            MR-->>CH: MainController
            CH->>MC: joinOrReconnect(nickname, handler)
            MC-->>CH: JoinResult
            CH-->>SC: JoinResult
        end
    end

    SC-->>CM: matchId, playerId, isHost, reconnected
```

## Punti da ricordare

- `MatchRegistry` conosce quali partite esistono.
- `MainController` gestisce una sola partita.
- Dopo `JoinResult`, il `ClientHandler` e' agganciato al `MainController` della
  partita scelta.
- `listOpenMatches()` ritorna solo match in stato `LOBBY`.
