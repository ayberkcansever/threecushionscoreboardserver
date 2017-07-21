package com.cansever.threecushion.threecushionserver.bean;

import lombok.Getter;
import lombok.Setter;

import java.util.Arrays;

/**
 * Created by ayberkcansever on 08/07/2017.
 */
public class GameEvent {

    @Getter @Setter private String gameId;
    @Getter @Setter private String userId;
    @Getter @Setter private String eventType;
    @Getter @Setter private String[] eventParams;

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
