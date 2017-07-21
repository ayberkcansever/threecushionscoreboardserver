package com.cansever.threecushion.threecushionserver.bean;

import lombok.Getter;
import lombok.Setter;

import javax.xml.bind.annotation.XmlRootElement;
import java.sql.Timestamp;

/**
 * Created by AyberkC on 07.07.2017.
 */
@XmlRootElement
public class GameBean implements Comparable<GameBean> {

    @Getter @Setter private String id;
    @Getter @Setter private int innings;
    @Getter @Setter private String player1;
    @Getter @Setter private String player1Id;
    @Getter @Setter private String player1PicUrl;
    @Getter @Setter private String player2;
    @Getter @Setter private String player2Id;
    @Getter @Setter private String player2PicUrl;
    @Getter @Setter private int scorePlayer1;
    @Getter @Setter private int scorePlayer2;
    @Getter @Setter private int turn;
    @Getter @Setter private Timestamp startDate;
    @Getter @Setter private Timestamp endDate;

    @Override
    public int compareTo(GameBean o) {
        return o.getEndDate().compareTo(endDate);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        GameBean gameBean = (GameBean) o;

        if (!id.equals(gameBean.id)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
