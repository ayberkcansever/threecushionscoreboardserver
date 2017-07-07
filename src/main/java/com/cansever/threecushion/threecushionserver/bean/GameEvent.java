package com.cansever.threecushion.threecushionserver.bean;

import java.util.Arrays;

/**
 * Created by ayberkcansever on 08/07/2017.
 */
public class GameEvent {

    private String gameId;
    private String userId;
    private String eventType;
    private String[] eventParams;

    public String getGameId() {
        return gameId;
    }

    public void setGameId(String gameId) {
        this.gameId = gameId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String[] getEventParams() {
        return eventParams;
    }

    public void setEventParams(String[] eventParams) {
        this.eventParams = eventParams;
    }

    @Override
    public String toString() {
        return "GameEvent{" +
                "gameId='" + gameId + '\'' +
                ", userId='" + userId + '\'' +
                ", eventType='" + eventType + '\'' +
                ", eventParams=" + Arrays.toString(eventParams) +
                '}';
    }
}
