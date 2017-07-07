package com.cansever.threecushion.threecushionserver.cache;

import com.cansever.threecushion.threecushionserver.bean.GameBean;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.Ignition;
import org.springframework.stereotype.Repository;

import javax.annotation.PostConstruct;
import java.util.List;

/**
 * Created by AyberkC on 07.07.2017.
 */
@Repository
public class CacheHolder {

    private Ignite ignite;
    private IgniteCache<String, GameBean> gamesCache;
    private IgniteCache<String, String> playerGameCache;
    private IgniteCache<String, GameBean> finishedGamesCache;
    private IgniteCache<String, List<String>> playerFinishedGamesCache;

    @PostConstruct
    public void init(){
        ignite = Ignition.start("ignite-config.xml");
        gamesCache = ignite.getOrCreateCache("gamesCache");
        playerGameCache = ignite.getOrCreateCache("playerGameCache");
        finishedGamesCache = ignite.getOrCreateCache("finishedGamesCache");
        playerFinishedGamesCache = ignite.getOrCreateCache("playerFinishedGamesCache");
    }

    public IgniteCache<String, String> getPlayerGameCache() {
        return playerGameCache;
    }

    public IgniteCache<String, GameBean> getGamesCache() {
        return gamesCache;
    }

    public IgniteCache<String, GameBean> getFinishedGamesCache() {
        return finishedGamesCache;
    }

    public IgniteCache<String, List<String>> getPlayerFinishedGamesCache() {
        return playerFinishedGamesCache;
    }
}
