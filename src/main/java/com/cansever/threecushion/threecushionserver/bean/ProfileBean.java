package com.cansever.threecushion.threecushionserver.bean;

import java.math.BigDecimal;
import java.util.List;

/**
 * Created by ayberkcansever on 11/07/2017.
 */
public class ProfileBean {

    private int winPercentage;
    private String generalAverage;
    private List<GameBean> lastGames;

    public int getWinPercentage() {
        return winPercentage;
    }

    public void setWinPercentage(int winPercentage) {
        this.winPercentage = winPercentage;
    }

    public String getGeneralAverage() {
        return generalAverage;
    }

    public void setGeneralAverage(String generalAverage) {
        this.generalAverage = generalAverage;
    }

    public List<GameBean> getLastGames() {
        return lastGames;
    }

    public void setLastGames(List<GameBean> lastGames) {
        this.lastGames = lastGames;
    }
}
