package com.cansever.threecushion.threecushionserver.bean;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Created by ayberkcansever on 11/07/2017.
 */
public class ProfileBean {

    @Getter @Setter private int winPercentage;
    @Getter @Setter private String generalAverage;
    @Getter @Setter  private List<GameBean> lastGames;

}
