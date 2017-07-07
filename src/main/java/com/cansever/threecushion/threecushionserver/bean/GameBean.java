package com.cansever.threecushion.threecushionserver.bean;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.Date;

/**
 * Created by AyberkC on 07.07.2017.
 */
@XmlRootElement
public class GameBean {

    private String id;
    private int innings;
    private String player1;
    private String player1Id;
    private String player1PicUrl;
    private String player2;
    private String player2Id;
    private String player2PicUrl;
    private int scorePlayer1;
    private int scorePlayer2;
    private int turn;
    private Date startDate;
    private Date endDate;

    public String getId() {
        return id;
    }

    public int getInnings() {
        return innings;
    }

    public void setInnings(int innings) {
        this.innings = innings;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPlayer1() {
        return player1;
    }

    public void setPlayer1(String player1) {
        this.player1 = player1;
    }

    public String getPlayer1Id() {
        return player1Id;
    }

    public void setPlayer1Id(String player1Id) {
        this.player1Id = player1Id;
    }

    public String getPlayer2Id() {
        return player2Id;
    }

    public void setPlayer2Id(String player2Id) {
        this.player2Id = player2Id;
    }

    public String getPlayer1PicUrl() {
        return player1PicUrl;
    }

    public void setPlayer1PicUrl(String player1PicUrl) {
        this.player1PicUrl = player1PicUrl;
    }

    public String getPlayer2() {
        return player2;
    }

    public void setPlayer2(String player2) {
        this.player2 = player2;
    }

    public String getPlayer2PicUrl() {
        return player2PicUrl;
    }

    public void setPlayer2PicUrl(String player2PicUrl) {
        this.player2PicUrl = player2PicUrl;
    }

    public int getScorePlayer1() {
        return scorePlayer1;
    }

    public void setScorePlayer1(int scorePlayer1) {
        this.scorePlayer1 = scorePlayer1;
    }

    public int getScorePlayer2() {
        return scorePlayer2;
    }

    public void setScorePlayer2(int scorePlayer2) {
        this.scorePlayer2 = scorePlayer2;
    }

    public int getTurn() {
        return turn;
    }

    public void setTurn(int turn) {
        this.turn = turn;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

}
