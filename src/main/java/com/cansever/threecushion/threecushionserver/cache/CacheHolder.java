package com.cansever.threecushion.threecushionserver.cache;

import com.cansever.threecushion.threecushionserver.bean.GameBean;
import lombok.Getter;
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
    @Getter private IgniteCache<String, GameBean> gamesCache;
    @Getter private IgniteCache<String, String> playerGameCache;
    @Getter private IgniteCache<String, GameBean> finishedGamesCache;
    @Getter private IgniteCache<String, List<String>> playerFinishedGamesCache;

    @PostConstruct
    public void init(){
        ignite = Ignition.start("ignite-config.xml");
        gamesCache = ignite.getOrCreateCache("gamesCache");
        playerGameCache = ignite.getOrCreateCache("playerGameCache");
        finishedGamesCache = ignite.getOrCreateCache("finishedGamesCache");
        playerFinishedGamesCache = ignite.getOrCreateCache("playerFinishedGamesCache");
    }

}
