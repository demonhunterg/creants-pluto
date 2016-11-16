package com.creants.pluto;

import java.util.List;

import com.avengers.netty.core.event.SystemNetworkConstant;
import com.avengers.netty.core.om.IRoom;
import com.avengers.netty.core.util.Tracer;
import com.avengers.netty.gamelib.GameAPI;
import com.avengers.netty.gamelib.GameInterface;
import com.avengers.netty.gamelib.result.IPlayMoveResult;
import com.avengers.netty.gamelib.result.StartGameResult;
import com.avengers.netty.socket.gate.IMessage;
import com.avengers.netty.socket.gate.wood.Message;
import com.avengers.netty.socket.gate.wood.User;
import com.creants.pluto.handler.AutoArrangeRequestHandler;
import com.creants.pluto.handler.FinishRequestHandler;
import com.creants.pluto.handler.ReadyRequestHandler;
import com.creants.pluto.logic.MauBinhGame;
import com.creants.pluto.logic.TestUnit;
import com.creants.pluto.util.GameCommand;
import com.creants.pluto.util.MessageFactory;
import com.google.gson.JsonObject;

/**
 * @author LamHa
 *
 */
public class GameInterfaceImpl extends AbstractGameLogic implements GameInterface {
	private MauBinhGame gameLogic;
	private GameAPI gameAPI;

	public GameInterfaceImpl(IRoom room) {
		super(room);
		Tracer.info(GameInterfaceImpl.class, "- Innit PLUTO");
	}

	@Override
	public MauBinhGame createGameLogic(IRoom room) {
		return gameLogic = new MauBinhGame(room);
	}

	@Override
	public MauBinhGame getGameLogic() {
		return gameLogic;
	}

	@Override
	public void initRequestHandler() {
		addRequestHandler(GameCommand.ACTION_AUTO_ARRANGE, new AutoArrangeRequestHandler());
		addRequestHandler(GameCommand.ACTION_FINISH, new FinishRequestHandler());
		addRequestHandler(GameCommand.ACTION_READY, new ReadyRequestHandler());
	}

	@Override
	public void dispatchEvent(short commandId, User user, Message message) {
		processRequest(commandId, user, message);
	}

	@Override
	public JsonObject getGameData() {
		return gameLogic.getGameData();
	}

	@Override
	public Object getGameDataForViewer() {
		return gameLogic.getGameData();
	}

	@Override
	public void leaveRoom(User user, IRoom room) {
		int totalUsers = room.countPlayer();
		Tracer.debugPlutoGame(GameInterfaceImpl.class,
				String.format("[DEBUG] [IN_GAME] [user:%s] leave room [%s], [countPlayer: %d] ", user.getUserName(),
						room.getName(), totalUsers));

		// trường hợp thoát ra còn một người chơi duy nhất thì không đếm
		if (totalUsers < 2 && gameLogic.isWaitingPlayer()) {
			gameLogic.stopCountDown();
			Tracer.debugPlutoGame(GameInterfaceImpl.class,
					String.format("[DEBUG] [IN_GAME] room [%s] stop countdown", room.getName()));
		}

		// báo người chơi còn lại user đó leave
		Message message = MessageFactory.createMauBinhMessage(GameCommand.ACTION_QUIT_GAME);
		message.putInt(SystemNetworkConstant.KEYI_USER_ID, user.getCreantUserId());
		gameAPI.sendAllInRoomExceptUser(message, user);

		gameLogic.leave(user);
	}

	@Override
	public boolean joinRoom(User user, IRoom room) {
		return gameLogic.join(user);
	}

	@Override
	public void disconnect(User user) {
		gameLogic.disconnect(user);
	}

	@Override
	public IPlayMoveResult onPlayMoveHandle(User sender, Message message) {
		return processRequest(message.getShort(SystemNetworkConstant.KEYR_ACTION_IN_GAME), sender, message);
	}

	@Override
	public StartGameResult onStartGame(User owner, List<User> listPlayer, Message extraData) {
		// TODO Trường hợp chủ động start
		return null;
	}

	@Override
	public void setApi(GameAPI gameApi) {
		this.gameAPI = gameApi;
		// TODO refactor
		initGameApi(gameApi);
		gameLogic.setGameApi(gameApi);

	}

	public GameAPI getGameAPI() {
		return gameAPI;
	}

	@Override
	public void setDealer(long userId) {
	}

	@Override
	public void startCountdownStartGame(long timeout) {

	}

	@Override
	public IPlayMoveResult timeout() {
		return null;
	}

	@Override
	public void test(User user, IMessage message) {
		gameAPI.sendToUser(TestUnit.getInstanse().comparePlayersTest(message), user);
	}

}
