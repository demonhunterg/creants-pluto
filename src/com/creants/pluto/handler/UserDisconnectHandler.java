package com.creants.pluto.handler;

import com.avengers.netty.gamelib.result.IPlayMoveResult;
import com.avengers.netty.socket.gate.wood.Message;
import com.avengers.netty.socket.gate.wood.User;

/**
 * @author LamHa
 *
 */
public class UserDisconnectHandler extends AbstractRequestHandler {

	@Override
	public IPlayMoveResult handleRequest(User sender, Message message) {
		gameLogic.disconnect(sender);
		return null;
	}

}
