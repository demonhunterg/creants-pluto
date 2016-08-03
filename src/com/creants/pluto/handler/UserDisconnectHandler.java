package com.creants.pluto.handler;

import com.avengers.netty.socket.gate.wood.Message;
import com.avengers.netty.socket.gate.wood.User;

/**
 * @author LamHa
 *
 */
public class UserDisconnectHandler extends AbstractRequestHandler {

	@Override
	public void handleRequest(User sender, Message message) {
		gameLogic.disconnect(sender);
	}

}
