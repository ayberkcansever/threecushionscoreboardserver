package com.cansever.threecushion.threecushionserver.rest;

import com.cansever.threecushion.threecushionserver.bean.GameBean;
import com.cansever.threecushion.threecushionserver.bean.GameEvent;
import com.cansever.threecushion.threecushionserver.bean.ProfileBean;
import com.cansever.threecushion.threecushionserver.db.DBHelper;
import com.cansever.threecushion.threecushionserver.lemp.LempClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestBody;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;

/**
 * Created by AyberkC on 07.07.2017.
 */
@Component
@Path("/rest")
public class Endpoint {

    private static Logger logger = LogManager.getLogger(Endpoint.class);

    @Autowired
    private DBHelper dbHelper;

    @Autowired
    private LempClient lempClient;

    @POST
    @Path("/createUser/{playerId}")
    public Response createUser(@PathParam("playerId") String playerId) {
        logger.info("createUser|" + playerId);
        try {
            lempClient.createUser(playerId);
        } catch (IOException e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
        return Response.ok().build();
    }

    @GET
    @Path("/loadgame/{player_id}")
    @Produces(MediaType.APPLICATION_JSON)
    public GameBean load(@PathParam("player_id") String playerId) {
        logger.info("loadgame|" + playerId);
        return dbHelper.loadPlayersGame(playerId);
    }

    @GET
    @Path("/profile/{player_id}")
    @Produces(MediaType.APPLICATION_JSON)
    public ProfileBean getProfile(@PathParam("player_id") String playerId,
                                  @QueryParam("searchStr") String searchStr) {
        logger.info("profile|" + playerId + "|" + searchStr);
        return dbHelper.getPlayersProfile(playerId, searchStr);
    }

    @POST
    @Path("/start")
    public String startGame(@RequestBody GameBean gameBean) {
        logger.info("start|" + gameBean.getPlayer1Id() + "|" + gameBean.getPlayer2Id());
        return dbHelper.insertNewGame(gameBean);
    }

    @POST
    @Path("/cancel/{gameId}")
    public void cancelGame(@PathParam("gameId") String gameId) {
        logger.info("cancel|" + gameId);
        dbHelper.deleteGame(gameId);
    }

    @POST
    @Path("/finish/{gameId}")
    public void finishGame(@PathParam("gameId") String gameId) {
        logger.info("finish|" + gameId);
        dbHelper.finishGame(gameId);
    }

    @POST
    @Path("/delete/{playerId}/{gameId}")
    @Produces(MediaType.APPLICATION_JSON)
    public ProfileBean deleteGame(@PathParam("playerId") String playerId,
                                  @PathParam("gameId") String gameId) {
        logger.info("delete|" + playerId + "|" + gameId);
        return dbHelper.deletePlayerFinishedGame(playerId, gameId);
    }

    @POST
    @Path("/event")
    public void processEvent(@RequestBody GameEvent gameEvent) {
        logger.info("event|" + gameEvent.toString());
        dbHelper.processGameEvent(gameEvent);
    }

}