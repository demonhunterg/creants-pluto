package com.creants.pluto.handler;

import com.avengers.netty.core.om.IRoom;
import com.avengers.netty.core.util.Tracer;
import com.avengers.netty.socket.gate.wood.Message;
import com.avengers.netty.socket.gate.wood.User;

/**
 * @author LamHa
 *
 */
public class JoinRoomRequestHandler extends AbstractRequestHandler {

	@Override
	public void handleRequest(User user, Message message) {
		IRoom lastJoinedRoom = user.getLastJoinedRoom();
		Tracer.debugRoom(JoinRoomRequestHandler.class,
				String.format("[DEBUG] [user: %s] do join room [roomId:%d, roomName:%s]", user.getUserName(),
						lastJoinedRoom.getId(), lastJoinedRoom.getName()));

		gameLogic.startWaitingPlayer();
	}

}
