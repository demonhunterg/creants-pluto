package com.creants.pluto.handler;

import com.avengers.netty.gamelib.GameAPI;
import com.avengers.netty.gamelib.result.IPlayMoveResult;
import com.avengers.netty.socket.gate.wood.Message;
import com.avengers.netty.socket.gate.wood.User;
import com.creants.pluto.logic.MauBinhGame;

/**
 * @author LamHa
 *
 */
public abstract class AbstractRequestHandler {
	protected MauBinhGame gameLogic;
	protected GameAPI gameApi;

	// TODO TIMEOUT SCHEDULER test
	public static final long TIMEOUT_MILLIS = 60 * 1000;

	public void setGameLogic(MauBinhGame gameLogic) {
		this.gameLogic = gameLogic;
	}

	public abstract IPlayMoveResult handleRequest(User sender, Message message);

	public void initGameApi(GameAPI gameApi) {
		this.gameApi = gameApi;
	}

	public void writeMessage(User receiver, Message message) {
		// TODO implement
	}
}
