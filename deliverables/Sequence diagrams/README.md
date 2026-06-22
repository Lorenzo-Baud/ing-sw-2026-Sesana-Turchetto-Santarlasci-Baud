# Diagrammi di sequenza rete

Questa cartella raccoglie i diagrammi di sequenza della parte di rete.
Il formato e' Markdown con blocchi Mermaid `sequenceDiagram`, renderizzabili da
GitHub, IntelliJ e molti viewer Markdown.

## Indice

| File | Flusso |
|---|---|
| `01-avvio-server.md` | Avvio server, restore snapshot, pubblicazione RMI e TCP |
| `02-selezione-partita.md` | Connessione client, lista partite, create/join su RMI e TCP |
| `03-lobby-start-partita.md` | Lobby, `expectedPlayers`, broadcast lobby e start partita |
| `04-azione-gioco-event-driven.md` | Invio `PlayerAction`, dispatch server, update model e broadcast |
| `05-heartbeat-disconnessione.md` | Ping/pong, timeout, disconnessione, sospensione e vittoria per abbandono |
| `06-riconnessione.md` | Rientro con `matchId` + nickname e invio snapshot |
| `07-persistenza-snapshot.md` | Persistenza asincrona dopo azione e cleanup a game over |
| `08-restore-post-crash.md` | Restore dopo crash server e riconnessione dei giocatori |

## Nota sui nomi TCP

Nel codice i nomi storici seguono la convenzione gia' usata per RMI:

| Classe | Lato reale |
|---|---|
| `SocketServer` | server, accept loop TCP |
| `SocketClientConnection` | server, adapter per un client TCP accettato |
| `SocketServerConnection` | client, connessione verso il server TCP |

Nei diagrammi i partecipanti mantengono questi nomi, ma quando serve viene
aggiunta una label per chiarire il lato.
