package it.polimi.ingws.gc8.Network.Messages;

import it.polimi.ingws.gc8.Model.Enum.EventType;

public class EventResolvedMessage {
    private final String typeName;
    private final String front;
    public EventResolvedMessage(String type, String front) {
        this.typeName = type;
        this.front = front;
    }

    public String getTypeName() {
        return typeName;
    }
    public String getFront() {
        return front;
    }
}
