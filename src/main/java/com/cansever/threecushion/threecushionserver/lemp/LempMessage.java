package com.cansever.threecushion.threecushionserver.lemp;

import com.lemp.packet.Datum;
import lombok.Getter;
import lombok.Setter;

/**
 * Created by ayberkcansever on 13/07/2017.
 */
public class LempMessage {

    @Getter @Setter private Datum datum;
    @Getter @Setter private long sentTime;
    @Getter @Setter private int retry;

    public LempMessage(Datum datum) {
        this.datum = datum;
        this.sentTime = System.currentTimeMillis();
    }

    public void increaseRetryCount() {
        this.retry = this.retry + 1;
    }
}
