package com.cansever.threecushion.threecushionserver.db;

import com.cansever.threecushion.threecushionserver.bean.GameBean;
import com.cansever.threecushion.threecushionserver.bean.GameEvent;
import com.cansever.threecushion.threecushionserver.bean.ProfileBean;
import com.cansever.threecushion.threecushionserver.cache.CacheHolder;
import com.datastax.driver.core.*;
import com.google.common.base.Strings;
import com.google.common.hash.Hashing;
import org.apache.ignite.cache.CacheEntryProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import javax.annotation.PostConstruct;
import java.nio.charset.Charset;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Created by AyberkC on 07.07.2017.
 */
@Repository
public class DBHelper {

    @Value( "${db.points}" )
    private String dbPoints;

    @Value( "${db.port}" )
    private int dbPort;

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
    private PreparedStatement loadFinishedGamePs;
    private PreparedStatement deletePlayerFinishedGamePs;
    private PreparedStatement loadPlayersFinishedGamesPs;
    private PreparedStatement loadGamePs;

    @Autowired
    private CacheHolder cacheHolder;

    @PostConstruct
    public void init(){
        if(cluster == null) {
            Cluster.Builder clusterBuilder = Cluster.builder().withPort(dbPort);
            for(String contactPoint : dbPoints.split(",")) {
                clusterBuilder.addContactPoint(contactPoint);
            }
            cluster = clusterBuilder.build();
        }
        if(session == null) {
            session = cluster.connect(dbKeyspace);
        }

        newGamePs = session.prepare("insert into game (id, innings, " +
                " player1, player1_id, player1_pic_url," +
                " player2, player2_id, player2_pic_url, " +
                " score_player1, score_player2, " +
                " start_date, turn) " +
                " values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) using ttl 18000");
        finishedGamePs = session.prepare("insert into finished_game (id, innings, " +
                " player1, player1_id, player1_pic_url," +
                " player2, player2_id, player2_pic_url, " +
                " score_player1, score_player2, " +
                " start_date, end_date, turn) " +
                " values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
        deleteGamePs = session.prepare("delete from game where id = ?");
        insertPlayerGamePs = session.prepare("insert into player_game (player_id, game_id) values(?, ?) using ttl 18000");
        insertPlayerFinishedGamePs = session.prepare("insert into player_finished_game (player_id, game_id) values(?, ?)");
        deletePlayerGamePs = session.prepare("delete from player_game where player_id = ?");
        loadGamePs = session.prepare("select * from game where id = ?");
        loadPlayersGamePs = session.prepare("select * from player_game where player_id = ?");
        deletePlayerFinishedGamePs = session.prepare("delete from player_finished_game where player_id = ? and game_id = ?");
        loadFinishedGamePs = session.prepare("select * from finished_game where id = ?");
        loadPlayersFinishedGamesPs = session.prepare("select * from player_finished_game where player_id = ?");
    }

    public String insertNewGame(GameBean gameBean){
        String gameId = Hashing.murmur3_32(12).hashString(gameBean.getPlayer1() + gameBean.getPlayer2() + String.valueOf(System.currentTimeMillis()), Charset.defaultCharset()).toString();
        gameBean.setStartDate(new java.sql.Timestamp(new Date().getTime()));
        session.execute(newGamePs.bind(
                gameId, 0,
                gameBean.getPlayer1(), gameBean.getPlayer1Id(), gameBean.getPlayer1PicUrl(),
                gameBean.getPlayer2(), gameBean.getPlayer2Id(), gameBean.getPlayer2PicUrl(),
                gameBean.getScorePlayer1(), gameBean.getScorePlayer2(),
                gameBean.getStartDate(), 1)
        );
        session.execute(insertPlayerGamePs.bind(gameBean.getPlayer1Id(), gameId));
        cacheHolder.getPlayerGameCache().put(gameBean.getPlayer1Id(), gameId);
        session.execute(insertPlayerGamePs.bind(gameBean.getPlayer2Id(), gameId));
        cacheHolder.getPlayerGameCache().put(gameBean.getPlayer2Id(), gameId);
        gameBean.setId(gameId);
        cacheHolder.getGamesCache().put(gameId, gameBean);
        return gameId;
    }

    public void updateGame(GameBean gameBean) {
        session.execute(newGamePs.bind(
                        gameBean.getId(), gameBean.getInnings(),
                        gameBean.getPlayer1(), gameBean.getPlayer1Id(), gameBean.getPlayer1PicUrl(),
                        gameBean.getPlayer2(), gameBean.getPlayer2Id(), gameBean.getPlayer2PicUrl(),
                        gameBean.getScorePlayer1(), gameBean.getScorePlayer2(),
                        gameBean.getStartDate(), gameBean.getTurn())
        );
        cacheHolder.getGamesCache().put(gameBean.getId(), gameBean);
    }

    public void deleteGame(GameBean gameBean){
        if(gameBean == null) {
            return;
        }
        session.execute(deleteGamePs.bind(gameBean.getId()));
        cacheHolder.getGamesCache().remove(gameBean.getId());
        session.execute(deletePlayerGamePs.bind(gameBean.getPlayer1Id()));
        cacheHolder.getPlayerGameCache().remove(gameBean.getPlayer1Id());
        session.execute(deletePlayerGamePs.bind(gameBean.getPlayer2Id()));
        cacheHolder.getPlayerGameCache().remove(gameBean.getPlayer2Id());
    }

    public void deleteGame(String gameId){
        deleteGame(loadGame(gameId));
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
                gameBean.setStartDate(new java.sql.Timestamp(row.getTimestamp("start_date").getTime()));
                gameBean.setTurn(row.getInt("turn"));
            }
            cacheHolder.getGamesCache().put(id, gameBean);
        }
        return gameBean;
    }

    public GameBean loadFinishedGame(String gameId){
        GameBean gameBean = cacheHolder.getFinishedGamesCache().get(gameId);
        if(gameBean == null) {
            gameBean = new GameBean();
            ResultSet rs = session.execute(loadFinishedGamePs.bind(gameId));
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
                gameBean.setStartDate(new java.sql.Timestamp(row.getTimestamp("start_date").getTime()));
                gameBean.setEndDate(new java.sql.Timestamp(row.getTimestamp("end_date").getTime()));
                gameBean.setTurn(row.getInt("turn"));
                gameBean.setInnings(row.getInt("innings"));
            }
            cacheHolder.getFinishedGamesCache().put(gameId, gameBean);
        }
        return gameBean;
    }

    public ProfileBean deletePlayerFinishedGame(String playerId, String gameId) {
        session.execute(deletePlayerFinishedGamePs.bind(playerId, gameId));
        cacheHolder.getPlayerFinishedGamesCache().invoke(playerId, (CacheEntryProcessor<String, List<String>, Object>) (mutableEntry, objects) -> {
            List<String> oldList = mutableEntry.getValue();
            oldList.remove(gameId);
            mutableEntry.setValue(oldList);
            return null;
        });
        return getPlayersProfile(playerId);
    }

    public void finishGame(String gameId){
        GameBean gameBean = loadGame(gameId);
        gameBean.setEndDate(new java.sql.Timestamp(new Date().getTime()));
        session.execute(finishedGamePs.bind(
                gameId, gameBean.getInnings(),
                gameBean.getPlayer1(), gameBean.getPlayer1Id(), gameBean.getPlayer1PicUrl(),
                gameBean.getPlayer2(), gameBean.getPlayer2Id(), gameBean.getPlayer2PicUrl(),
                gameBean.getScorePlayer1(), gameBean.getScorePlayer2(),
                gameBean.getStartDate(), gameBean.getEndDate(), gameBean.getTurn()
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
        deleteGame(gameBean);
    }

    public ProfileBean getPlayersProfile(String playerId, String... searchStr) {
        ProfileBean profileBean = new ProfileBean();
        List<GameBean> finishedGames = new ArrayList<>();
        List<String> gameIdList = cacheHolder.getPlayerFinishedGamesCache().get(playerId);
        if(gameIdList == null) {
            gameIdList = new ArrayList<>();
            ResultSet rs = session.execute(loadPlayersFinishedGamesPs.bind(playerId));
            for (Row row : rs) {
                String gameId = row.getString("game_id");
                gameIdList.add(gameId);
            }
            cacheHolder.getPlayerFinishedGamesCache().put(playerId, gameIdList);
        }
        int wonGamesCount = 0, totalGamesCount = 0;
        int totalPoinstCount = 0, totalInnings = 0;
        for(String gameId : gameIdList) {
            GameBean gameBean = loadFinishedGame(gameId);
            if(gameBean != null) {
                boolean firstPlayer = gameBean.getPlayer1Id().equals(playerId);
                boolean secondPlayer = gameBean.getPlayer2Id().equals(playerId);
                if(searchStr.length == 0 || (searchStr.length > 0 && searchStr[0] != null
                        && (firstPlayer && gameBean.getPlayer2().toUpperCase().contains(searchStr[0].toUpperCase()))
                        || (secondPlayer && gameBean.getPlayer1().toUpperCase().contains(searchStr[0].toUpperCase())))) {
                    finishedGames.add(gameBean);
                    int innings = gameBean.getInnings();
                    if(firstPlayer) {
                        totalPoinstCount += gameBean.getScorePlayer1();
                        if(gameBean.getScorePlayer1() > gameBean.getScorePlayer2()) {
                            wonGamesCount++;
                        }
                    } else if(secondPlayer) {
                        totalPoinstCount += gameBean.getScorePlayer2();
                        if(gameBean.getScorePlayer2() > gameBean.getScorePlayer1()) {
                            wonGamesCount++;
                        }
                        if(gameBean.getTurn() == 2) {
                            innings--;
                        }
                    }
                    totalGamesCount++;
                    totalInnings += innings;
                }
            }
        }
        NumberFormat formatter = new DecimalFormat("#0.000");
        if(totalGamesCount > 0) {
            profileBean.setWinPercentage(wonGamesCount * 100 / totalGamesCount);
        }
        if(totalInnings > 0) {
            profileBean.setGeneralAverage(formatter.format((double) totalPoinstCount / (double) totalInnings));
        }
        Collections.sort(finishedGames);
        profileBean.setLastGames(finishedGames.size() <= 10 ? finishedGames : finishedGames.subList(0, 10));
        return profileBean;
    }

    public void processGameEvent(GameEvent gameEvent){
        GameBean gameBean = loadGame(gameEvent.getGameId());
        if(gameBean != null) {
            if("enter".equals(gameEvent.getEventType())) {
                int newTurn = 1;
                int run = Integer.parseInt(gameEvent.getEventParams()[0]);
                if(gameBean.getPlayer1Id().equals(gameEvent.getUserId())) {
                    gameBean.setScorePlayer1(gameBean.getScorePlayer1() + run);
                    if(run >= 0) {
                        gameBean.setInnings(gameBean.getInnings() + 1);
                        newTurn = 2;
                    }
                } else if(gameBean.getPlayer2Id().equals(gameEvent.getUserId())) {
                    gameBean.setScorePlayer2(gameBean.getScorePlayer2() + Integer.parseInt(gameEvent.getEventParams()[0]));
                }
                if(run >= 0) {
                    gameBean.setTurn(newTurn);
                }
                updateGame(gameBean);
            } else if("innings".equals(gameEvent.getEventType())) {
                String direction = gameEvent.getEventParams()[0];
                if("+".equals(direction)) {
                    gameBean.setInnings(gameBean.getInnings() + 1);
                } else if("-".equals(direction)) {
                    gameBean.setInnings(gameBean.getInnings() - 1);
                }
                updateGame(gameBean);
            }
        }
    }

}
