package com.cansever.threecushion.threecushionserver.db;

import com.cansever.threecushion.threecushionserver.bean.GameBean;
import com.cansever.threecushion.threecushionserver.cache.CacheHolder;
import com.datastax.driver.core.*;
import com.google.common.base.Strings;
import com.google.common.hash.Hashing;
import org.apache.ignite.cache.CacheEntryProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import javax.annotation.PostConstruct;
import javax.cache.processor.EntryProcessorException;
import javax.cache.processor.MutableEntry;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by AyberkC on 07.07.2017.
 */
@Repository
public class DBHelper {

    @Value( "${db.points}" )
    private String dbPoints;

    @Value( "${db.keyspace}" )
    private String dbKeyspace;

    private Cluster cluster;
    private Session session;

    private PreparedStatement newGamePs;
    private PreparedStatement finishedGamePs;
    private PreparedStatement deleteGamePs;
    private PreparedStatement insertPlayerGamePs;
    private PreparedStatement insertPlayerFinishedGamePs;
    private PreparedStatement deletePlayerGamePs;
    private PreparedStatement loadPlayersGamePs;
    private PreparedStatement loadPlayersFinishedGamesPs;
    private PreparedStatement loadGamePs;

    @Autowired
    private CacheHolder cacheHolder;

    @PostConstruct
    public void init(){
        if(cluster == null) {
            Cluster.Builder clusterBuilder = Cluster.builder();
            for(String contactPoint : dbPoints.split(",")) {
                clusterBuilder.addContactPoint(contactPoint);
            }
            cluster = clusterBuilder.build();
        }
        if(session == null) {
            session = cluster.connect(dbKeyspace);
        }

        newGamePs = session.prepare("insert into game (id, " +
                " player1, player1_id, player1_pic_url," +
                " player2, player2_id, player2_pic_url, " +
                " score_player1, score_player2, " +
                " start_date, turn) " +
                " values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) using ttl 18000");
        finishedGamePs = session.prepare("insert into finished_game (id, " +
                " player1, player1_id, player1_pic_url," +
                " player2, player2_id, player2_pic_url, " +
                " score_player1, score_player2, " +
                " start_date, end_date) " +
                " values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
        deleteGamePs = session.prepare("delete from game where id = ?");
        insertPlayerGamePs = session.prepare("insert into player_game (player_id, game_id) values(?, ?) using ttl 18000");
        insertPlayerGamePs = session.prepare("insert into player_finished_game (player_id, game_id) values(?, ?)");
        deletePlayerGamePs = session.prepare("delete from player_game where player_id = ?");
        loadGamePs = session.prepare("select * from game where id = ?");
        loadPlayersGamePs = session.prepare("select * from player_game where player_id = ?");
        loadPlayersFinishedGamesPs = session.prepare("select * from player_finished_game where player_id = ?");
    }

    public String insertNewGame(GameBean gameBean){
        String gameId = Hashing.murmur3_32(12).hashString(gameBean.getPlayer1() + gameBean.getPlayer2() + String.valueOf(System.currentTimeMillis()), Charset.defaultCharset()).toString();
        session.execute(newGamePs.bind(
                gameId,
                gameBean.getPlayer1(), gameBean.getPlayer1Id(), gameBean.getPlayer1PicUrl(),
                gameBean.getPlayer2(), gameBean.getPlayer2Id(), gameBean.getPlayer2PicUrl(),
                gameBean.getScorePlayer1(), gameBean.getScorePlayer2(),
                new Date(), 1)
        );
        session.execute(insertPlayerGamePs.bind(gameBean.getPlayer1(), gameId));
        cacheHolder.getPlayerGameCache().put(gameBean.getPlayer1(), gameId);
        session.execute(insertPlayerGamePs.bind(gameBean.getPlayer2(), gameId));
        cacheHolder.getPlayerGameCache().put(gameBean.getPlayer2(), gameId);
        gameBean.setId(gameId);
        cacheHolder.getGamesCache().put(gameId, gameBean);
        return gameId;
    }

    public void deleteGame(String gameId){
        GameBean gameBean = loadGame(gameId);
        session.execute(deleteGamePs.bind(gameId));
        cacheHolder.getGamesCache().remove(gameId);
        session.execute(deletePlayerGamePs.bind(gameBean.getPlayer1Id()));
        cacheHolder.getPlayerGameCache().remove(gameBean.getPlayer1());
        session.execute(deletePlayerGamePs.bind(gameBean.getPlayer2Id()));
        cacheHolder.getPlayerGameCache().remove(gameBean.getPlayer2());
    }

    public GameBean loadPlayersGame(String playerId) {
        String gameId = cacheHolder.getPlayerGameCache().get(playerId);
        GameBean gameBean = null;
        if(gameId != null) {
            gameBean = cacheHolder.getGamesCache().get(gameId);
        }
        if(gameBean == null) {
            ResultSet rs = session.execute(loadPlayersGamePs.bind(playerId));
            for (Row row : rs) {
                gameId = row.getString("game_id");
            }
            if(!Strings.isNullOrEmpty(gameId)) {
                gameBean = loadGame(gameId);
                cacheHolder.getPlayerGameCache().put(playerId, gameId);
            }
        }
        return gameBean;
    }

    public GameBean loadGame(String id) {
        GameBean gameBean = cacheHolder.getGamesCache().get(id);
        if(gameBean == null) {
            gameBean = new GameBean();
            ResultSet rs = session.execute(loadGamePs.bind(id));
            for (Row row : rs) {
                gameBean.setId(row.getString("id"));
                gameBean.setPlayer1(row.getString("player1"));
                gameBean.setPlayer1Id(row.getString("player1_id"));
                gameBean.setPlayer1PicUrl(row.getString("player1_pic_url"));
                gameBean.setPlayer2(row.getString("player2"));
                gameBean.setPlayer2Id(row.getString("player2_id"));
                gameBean.setPlayer2PicUrl(row.getString("player2_pic_url"));
                gameBean.setScorePlayer1(row.getInt("score_player1"));
                gameBean.setScorePlayer2(row.getInt("score_player2"));
                gameBean.setStartDate(row.getTimestamp("start_date"));
                gameBean.setTurn(row.getInt("turn"));
            }
            cacheHolder.getGamesCache().put(id, gameBean);
        }
        return gameBean;
    }

    public void finishGame(String gameId){
        GameBean gameBean = loadGame(gameId);
        session.execute(finishedGamePs.bind(
                gameId,
                gameBean.getPlayer1(), gameBean.getPlayer1Id(), gameBean.getPlayer1PicUrl(),
                gameBean.getPlayer2(), gameBean.getPlayer2Id(), gameBean.getPlayer2PicUrl(),
                gameBean.getScorePlayer1(), gameBean.getScorePlayer2(),
                gameBean.getStartDate(), new Date()
        ));
        cacheHolder.getFinishedGamesCache().put(gameId, gameBean);
        session.execute(insertPlayerFinishedGamePs.bind(gameBean.getPlayer1Id(), gameId));
        cacheHolder.getPlayerFinishedGamesCache().invokeAsync(gameBean.getPlayer1Id(), (CacheEntryProcessor<String, List<String>, Object>) (mutableEntry, objects) -> {
            List<String> oldList = mutableEntry.getValue();
            oldList.add(gameId);
            mutableEntry.setValue(oldList);
            return null;
        });
        session.execute(insertPlayerFinishedGamePs.bind(gameBean.getPlayer2Id(), gameId));
        cacheHolder.getPlayerFinishedGamesCache().invokeAsync(gameBean.getPlayer2Id(), (CacheEntryProcessor<String, List<String>, Object>) (mutableEntry, objects) -> {
            List<String> oldList = mutableEntry.getValue();
            oldList.add(gameId);
            mutableEntry.setValue(oldList);
            return null;
        });
        deleteGame(gameId);
    }

    public List<GameBean> getPlayersFinishedGames(String playerId) {
        List<GameBean> finishedGames = new ArrayList<>();
        List<String> gameIdList = cacheHolder.getPlayerFinishedGamesCache().get(playerId);
        if(gameIdList == null) {
            gameIdList = new ArrayList<>();
            ResultSet rs = session.execute(loadPlayersFinishedGamesPs.bind(playerId));
            for (Row row : rs) {
                String gameId = row.getString("game_id");
                gameIdList.add(gameId);
                finishedGames.add(loadGame(gameId));
            }
            cacheHolder.getPlayerFinishedGamesCache().put(playerId, gameIdList);
        }
        return finishedGames;
    }

}
