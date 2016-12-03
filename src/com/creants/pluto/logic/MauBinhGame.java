package com.creants.pluto.logic;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import com.avengers.netty.core.event.SystemNetworkConstant;
import com.avengers.netty.core.om.IRoom;
import com.avengers.netty.core.util.CoreTracer;
import com.avengers.netty.core.util.DefaultMessageFactory;
import com.avengers.netty.gamelib.GameAPI;
import com.avengers.netty.gamelib.key.NetworkConstant;
import com.avengers.netty.gamelib.om.RoomInfo;
import com.avengers.netty.socket.gate.wood.Message;
import com.avengers.netty.socket.gate.wood.User;
import com.creants.pluto.om.Player;
import com.creants.pluto.om.Result;
import com.creants.pluto.om.card.Card;
import com.creants.pluto.om.card.Cards;
import com.creants.pluto.util.GameCommand;
import com.creants.pluto.util.GsonUtils;
import com.creants.pluto.util.MauBinhConfig;
import com.creants.pluto.util.MessageFactory;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * @author LamHa
 *
 */
public class MauBinhGame {
	public STATE gameState;
	private GameAPI gameApi;
	private MoneyManager moneyManager;
	private MauBinhCardSet cardSet;
	private Integer countDownSeconds = 0;

	private long startGameTime;
	private IRoom room;
	private Player[] players;
	private Map<Integer, User> disconnectedUsers;

	private ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
	private ScheduledFuture<?> countdownSchedule;

	public MauBinhGame(IRoom room) {
		this.room = room;
		gameState = STATE.NOT_START;

		cardSet = new MauBinhCardSet();
		players = new Player[] { new Player(), new Player(), new Player(), new Player() };

		disconnectedUsers = new ConcurrentHashMap<Integer, User>();
		Object roomInfo = room.getProperty(NetworkConstant.ROOM_INFO);
		int moneyBet = 0;
		if (roomInfo != null) {
			moneyBet = ((RoomInfo) roomInfo).getBetCoin();
		}

		moneyManager = new MoneyManager(moneyBet);
	}

	public void setGameApi(GameAPI gameApi) {
		this.gameApi = gameApi;
	}

	public JsonObject getGameData() {
		if (isPlaying()) {
			JsonObject gameData = new JsonObject();
			gameData.addProperty("remain_time", getRemainTime());
			gameData.addProperty("game_state", gameState.getValue());
			return gameData;
		}

		return null;
	}

	public boolean reconnect(User user) {
		if (!disconnectedUsers.containsKey(user.getCreantUserId()))
			return false;

		if (!isPlaying())
			return false;

		User disconnectUser = disconnectedUsers.get(user.getCreantUserId());
		user.setUserId(disconnectUser.getUserId());
		user.setLastJoinedRoom(room);
		CoreTracer.debug(MauBinhGame.class, "User reconnect: " + user.getUserName());
		Player playerByUser = getPlayerByUser(disconnectUser);
		playerByUser.setUser(user);
		// báo cho player khác user
		Message message = MessageFactory.createMauBinhMessage(GameCommand.ACTION_RECONNECT);
		message.putInt(SystemNetworkConstant.KEYI_USER_ID, user.getCreantUserId());
		gameApi.sendAllInRoomExceptUser(message, user);
		// trả về data cho joiner
		gameApi.sendToUser(buildRoomInfo(user, isPlaying()), user);
		disconnectedUsers.remove(user.getCreantUserId());
		return true;
	}

	public boolean join(User user, IRoom room) {
		// TODO check theo số tiền thua tối đa
		if (!moneyManager.checkEnoughMoney(user)) {
			return false;
		}

		gameApi.sendResponseForListUser(buildRoomInfo(user, false), room.getPlayersList());

		IRoom lastJoinedRoom = user.getLastJoinedRoom();
		CoreTracer.debug(this.getClass(), String.format("[DEBUG] [user: %s] do join room [roomId:%d, roomName:%s]",
				user.getUserName(), lastJoinedRoom.getId(), lastJoinedRoom.getName()));

		startWaitingPlayer();
		return true;
	}

	public void leave(User player) {
		try {
			if (!isPlaying() || getSeatNumber(player) < 0) {
				return;
			}

			// đá ra khỏi bàn chơi
			kickOut(player);
			long money = moneyManager.updateMoneyForLeave(this, player, countPlayerInTurn(), players);
			if (money != 0) {
				gameApi.updateUserMoney(player, -money, 2, "Phạt rời game");
				debug(String.format("[DEBUG] [user:%s] decrement money for leave [%d]! ", player.getUserName(), money));
			}

			if (canBeFinish()) {
				for (int i = 0; i < players.length; i++) {
					if (players[i].getUser() != null) {
						players[i].setFinishFlag(true);
					}
				}

				if (GameChecker.isFinishAll(players)) {
					processGameFinish();
				}
			}

		} catch (Exception e) {
			debug(String.format("[ERROR] [user:%s] leave room [%s] fail! ", player.getUserName(), room.getName()), e);
		}
	}

	private void kickOut(User user) {
		debug(String.format("[DEBUG] [user:%s] kick out", user.getUserName()));
		int seatNumber = getSeatNumber(user);
		players[seatNumber].setUser(null);
		players[seatNumber].reset();

		changeOwner(user);
	}

	private void changeOwner(User quiter) {
		if (!isOwner(quiter))
			return;

		room.setOwner(room.getListUser().get(0));
	}

	private boolean isOwner(User user) {
		return getOwner().getUserId() == user.getUserId();
	}

	/**
	 * Cho phép reconnect
	 * 
	 * @param user
	 */
	public void disconnect(User user) {
		CoreTracer.debug(this.getClass(), "User disconnect: " + user.getUserName());
		// báo cho các player khác user này bị disconnect
		Message message = MessageFactory.createMauBinhMessage(GameCommand.ACTION_DISCONNECT);
		message.putInt(SystemNetworkConstant.KEYI_USER_ID, user.getCreantUserId());
		gameApi.sendAllInRoomExceptUser(message, user);

		disconnectedUsers.put(user.getCreantUserId(), user);
	}

	private User getOwner() {
		return room.getOwner();
	}

	private int playerSize() {
		return room.getPlayersList().size();
	}

	private void startCountDown(int startAfterSeconds) {
		countDownSeconds = startAfterSeconds;
		if (countdownSchedule == null || countdownSchedule.isCancelled()) {
			countDown();
			return;
		}

		debug(String.format("[ERROR] [room:%s] Do startCountDown with countdown schedule not cancel yet!",
				room.getName()));
	}

	private void debug(Object... msgs) {
		CoreTracer.debug(MauBinhGame.class, msgs);
	}

	private void startGame() {
		try {
			debug(String.format("[DEBUG] Game is begin [roomId: %d, roomName: %s, owner: %s]", room.getId(),
					room.getName(), room.getOwner().getUserName()));
			// TODO kiểm tra trong danh sách disconnect để đá user này ra
			if (playerSize() < 2) {
				debug("[DEBUG] Game is begin with a player");
				return;
			}

			gameState = STATE.PLAYING;
			startGameTime = System.currentTimeMillis();

			// xóc bài
			cardSet.xaoBai();

			// chia bài
			deliveryCard();

			debug(String.format("[DEBUG] Delivery card finish [roomId: %d, roomName: %s]. Start timeout in %d seconds",
					room.getId(), room.getName(), 91));

			startCountDown(MauBinhConfig.limitTime);
		} catch (Exception e) {
			CoreTracer.error(MauBinhGame.class, "[ERROR] startGame fail!", e);
		}
	}

	private void notifyNotEnoughMoney(User user) {
		gameApi.sendToUser(MessageFactory.createErrorMessage(SystemNetworkConstant.KEYR_ACTION_IN_GAME, (short) 1000,
				"Bạn không đủ tiền"), user);
	}

	public void stopGame() {
		debug(String.format("[DEBUG] [room:%s] Do stopGame and reset all player!", room.getName()));
		try {
			for (int i = 0; i < players.length; i++) {
				players[i].reset();
			}

			// gameApi.sendAllInRoom(MessageFactory.makeStopMessage());
		} catch (Exception e) {
			debug(String.format("[ERROR] [room:%s] Do stopGame fail!", room.getName()), e);
			CoreTracer.error(MauBinhGame.class, "[ERROR] stopGame fail!", e);
		}
	}

	private int getSeatNumber(User user) {
		for (int i = 0; i < players.length; i++) {
			Player player = players[i];
			if (player.getUser() != null && player.getCreantUserId() == user.getCreantUserId())
				return i;
		}

		return -1;
	}

	private int getAvailableSeat() {
		for (int i = 0; i < players.length; i++) {
			if (players[i].getUser() != null) {
				continue;
			}

			return i;
		}

		return -1;
	}

	/**
	 * Player sẵn sàn
	 * 
	 * @param user
	 * @param bln
	 */
	public void playerReady(User user, boolean isReady) {
		try {
			int nSeat = getSeatNumber(user);
			if (nSeat < 0 || nSeat >= 4) {
				return;
			}

			if (moneyManager.checkEnoughMoney(user)) {
				debug("[DEBUG] [user:%s] playerReady!", user.getUserName());
				players[nSeat].setReady(true);
			} else {
				System.out.println("[ERROR] playerReady! Not enough money.");
				notifyNotEnoughMoney(user);
			}
		} catch (Exception ex) {
			debug("[ERROR] playerReady fail!", ex);
		}
	}

	/**
	 * Đếm số player trong bàn chơi
	 * 
	 * @return
	 */
	private int countPlayerInTurn() {
		int count = 0;
		for (int i = 0; i < players.length; i++) {
			if (players[i].getUser() != null) {
				count++;
			}
		}

		return count;
	}

	/**
	 * Kiểm tra game có thể hoàn thành không. Tất cả player đã finish chưa.
	 * 
	 * @return
	 */
	private boolean canBeFinish() {
		if (players.length == 0) {
			return false;
		}

		int playingNo = 0;
		int finishNo = 0;
		for (int i = 0; i < players.length; i++) {
			if (players[i].getUser() != null) {
				if (players[i].isFinish()) {
					finishNo++;
				}
				playingNo++;
			}
		}

		return playingNo == 1 || finishNo == playingNo;
	}

	private User getUser(int index) {
		return room.getListUser().get(index);
	}

	/**
	 * Chia bài
	 */
	private void deliveryCard() {
		for (int i = 0; i < players.length; i++) {
			players[i].reset();
		}

		for (int i = 0; i < cardSet.length(); i++) {
			players[(i % 4)].getCards().receivedCard(cardSet.dealCard());
		}

		for (int i = 0; i < playerSize(); i++) {
			User receiver = getUser(i);
			if (receiver == null)
				continue;

			Player player = players[i];
			player.setUser(receiver);
			if (isOwner(receiver)) {
				player.setOwner(true);
			}

			gameApi.sendToUser(
					MessageFactory.makeStartMessage(room.getId(), MauBinhConfig.limitTime, player.getCardList()),
					receiver);
		}

		// trường hợp đang chia bài mà người chơi bấm tự động bin hết
		if (GameChecker.isFinishAll(players)) {
			processGameFinish();
		}
	}

	private boolean isPlaying() {
		return gameState == STATE.PLAYING;
	}

	public boolean isWaitingPlayer() {
		return gameState == STATE.NOT_START;
	}

	/**
	 * User gửi lệnh bin xong
	 * 
	 * @param user
	 * @param listCards
	 *            danh sách card user gửi lên
	 */
	public void processBinhFinish(User user, List<Card> listCards) {
		debug(String.format("[DEBUG] [user:%s] Do processBinhFinish", user.getUserName()));
		Player player = getPlayerByUser(user);
		if (player == null) {
			gameState = STATE.NOT_START;
			debug(String.format("[ERROR] [user:%s] Do processBinhFinish fail! Game is not start yet.",
					user.getUserName()));
			return;
		}

		try {
			Cards cards = player.getCards();
			cards.clearArrangement();

			for (int i = 0; i < 3; i++) {
				cards.receivedCardTo1stSet(listCards.get(i));
			}

			int beginset2 = 3;
			for (int i = beginset2; i < 5 + beginset2; i++) {
				cards.receivedCardTo2ndSet(listCards.get(i));
			}

			int beginset3 = 8;
			for (int i = beginset3; i < 5 + beginset3; i++) {
				cards.receivedCardTo3rdSet(listCards.get(i));
			}

			// trường hợp bin thủng
			if (!cards.isFinishArrangement()) {
				debug(String.format("[DEBUG] [user:%s] Do processBinhFinish! BIN THUNG", user.getUserName()));
				gameApi.sendToUser(MessageFactory.makeInterfaceErrorMessage(GameCommand.ACTION_FINISH,
						GameCommand.ERROR_IN_GAME_BIN_THUNG, "Binh Thủng"), user);

				if (getRemainTime() <= 0) {
					debug(String.format("[DEBUG] [user:%s] Do processBinhFinish! Over time BIN THUNG",
							user.getUserName()));
					player.setFinishFlag(true);
					if (GameChecker.isFinishAll(players)) {
						processGameFinish();
					}
				}
			} else {
				cards.setMauBinhTypeAfterArrangement();
				// Báo cho các player khác người chơi này đã bin xong
				if (!player.isTimeOut()) {
					gameApi.sendAllInRoom(MessageFactory.makeFinishMessage(user.getCreantUserId()));
				}

				player.setFinishFlag(true);
				if (GameChecker.isFinishAll(players)) {
					processGameFinish();
				}
			}
		} catch (Exception e) {
			debug(String.format("[ERROR] [user:%s] Do processBinhFinish fail!", user.getUserName()), e);
			CoreTracer.error(MauBinhGame.class, "[ERROR] Do processBinhFinish fail!", e);
			gameApi.sendToUser(MessageFactory.makeInterfaceErrorMessage(GameCommand.ACTION_FINISH,
					GameCommand.ERROR_IN_GAME_MISS_CARD, "Lỗi kết thúc game"), user);
		}

	}

	/**
	 * Tự động xếp bài
	 * 
	 * @param player
	 * @return
	 */
	public List<Card> processAutoArrangeCommand(User user) {
		List<Card> result = new ArrayList<Card>();
		try {
			Player player = getPlayerByUser(user);
			result = AutoArrangement.getSolution(player.getCardList());
			debug(String.format("[DEBUG] Auto Arrange [username:%s] cards:\n %s", user.getUserName(), logCard(result)));

			if (!player.isTimeOut()) {
				Message m = MessageFactory.makeAutoArrangeResultMessage(result);
				gameApi.sendToUser(m, player.getUser());
			}

			player.setAutoArrangementFlag(true);
		} catch (Exception e) {
			CoreTracer.error(MauBinhGame.class, "[ERROR] processAutoArrangeCommand fail!", e);
		}

		return result;
	}

	private void processGameFinish() {
		gameState = STATE.CALCULATE;
		stopCountDown();
		debug("[DEBUG] processGameFinish........................!");
		Result[][] result = GameChecker.comparePlayers(players);
		int[] winChi = GameChecker.getWinChi(players, result);
		long[] winMoney = moneyManager.calculateMoney(winChi);
		winMoney = moneyManager.addBonusChi(players, winMoney, winChi);
		// winMoney = moneyManager.updateMoney(this, players, winMoney, winChi,
		// result);
		// winMoney = moneyManager.updateMoneyForAutoArrangementUsing(this,
		// players, winMoney);
		debug("[DEBUG] //////////////////////////////////////// PLAYER IN GAME //////////////////////////");
		List<User> userInGame = new ArrayList<User>();
		for (int i = 0; i < players.length; i++) {
			User user = players[i].getUser();
			if (user == null)
				continue;
			userInGame.add(user);
			debug(user.getUserName());
		}

		debug("[DEBUG] ======= Users:");
		List<User> playersList = room.getPlayersList();
		for (User user : playersList) {
			debug("-" + user.getUserName() + "/" + userInGame.contains(user));
		}

		debug("[DEBUG] //////////////////////////////////////// PLAYER IN GAME //////////////////////////");

		Message message = null;
		for (int i = 0; i < players.length; i++) {
			User user = players[i].getUser();
			if (user == null)
				continue;

			if (winMoney[i] != 0) {
				gameApi.updateUserMoney(user, winMoney[i], 1, "Cập nhật tiền kết thúc game");
			}

			// gửi thông tin kết quả cho player
			message = MessageFactory.makeTestResultMessage(i, players, winMoney, winChi, result);
			if (message != null) {
				gameApi.sendToUser(message, user);
			}
		}

		if (message != null) {
			for (User user : playersList) {
				if (userInGame.contains(user))
					continue;
				gameApi.sendToUser(message, user);
			}
		}

		// show bài trong 10s
		startCountDown(MauBinhConfig.showCardSeconds);
	}

	public Player getPlayerByUser(User user) {
		int sNum = getSeatNumber(user);
		if (sNum == -1 || sNum >= players.length) {
			return null;
		}

		return players[sNum];
	}

	public static String getMessage(String className, Locale locale, String property) {
		try {
			return ResourceBundle.getBundle(className, locale).getString(property);
		} catch (Exception e) {
			CoreTracer.error(MauBinhGame.class, "[ERROR] getMessage fail!",
					"className:" + className + ", property:" + property + ", locale:" + locale + "}", e);
		}
		return null;
	}

	public void countDown() {
		debug("[DEBUG] Start countdown in " + countDownSeconds + " seconds");
		countdownSchedule = scheduler.scheduleAtFixedRate(new Runnable() {
			public void run() {
				try {
					synchronized (countDownSeconds) {
						if (countDownSeconds > 0) {
							countDownSeconds--;
							// debug("[FATAL] " + countDownSeconds);
							return;
						}

						// Game is begin
						if (gameState == STATE.NOT_START) {
							stopCountDown();
							debug(String.format("[DEBUG] countdown finished! Start game"));
							startGame();
							return;
						}

						// trường hợp hết giờ xếp bài, thực hiện kết thúc
						if (isPlaying()) {
							debug(String.format("[DEBUG] countdown finished! Timeout"));
							for (int i = 0; i < players.length; i++) {
								if (players[i].getUser() != null) {
									players[i].setFinishFlag(true);
								}
							}

							if (GameChecker.isFinishAll(players)) {
								processGameFinish();
							}

							return;
						}

						if (gameState == STATE.CALCULATE) {
							gameState = STATE.FINISH;
							stopCountDown();
							processUserDisconnect();

							// đá player hết tiền
							for (int i = 0; i < players.length; i++) {
								User user = players[i].getUser();
								if (user != null && !moneyManager.checkEnoughMoney(user)) {
									// báo user đó đã hết tiền
									debug("[DEBUG] " + user.getUserName() + " Đá khi hết tiền! Not enough money. "
											+ user.getMoney() + "/bet:" + moneyManager.getGameMoney());

									notifyNotEnoughMoney(user);
									gameApi.leaveRoom(user.getUserId());
								}

								// đá player chưa sẵn sàn cho ván kế
								if (user != null && !players[i].isReady()) {
									debug("[DEBUG] kickout player not ready!" + user.getUserName());
									gameApi.leaveRoom(user.getUserId());
								}
							}

							stopGame();
							gameState = STATE.NOT_START;
							startWaitingPlayer();
							return;
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}

			}
		}, 0, 1, TimeUnit.SECONDS);

	}

	public void startWaitingPlayer() {
		if (playerSize() < 2 || gameState != STATE.NOT_START)
			return;

		// đếm cho ván tiếp theo
		startCountDown(MauBinhConfig.startAfterSeconds);

		CoreTracer.debug(this.getClass(), String.format("[IN_GAME] [DEBUG] waiting user join game in %d seconds",
				MauBinhConfig.startAfterSeconds));

		Message message = MessageFactory.createMauBinhMessage(GameCommand.ACTION_START_AFTER_COUNTDOWN);
		message.putInt(NetworkConstant.KEYI_TIMEOUT, MauBinhConfig.startAfterSeconds);
		message.putLong(GameCommand.KEYL_UTC_TIME, DateTime.now().toDateTime(DateTimeZone.UTC).getMillis());
		gameApi.sendAllInRoom(message);
	}

	private Message buildRoomInfo(User joiner, boolean reconnect) {
		JsonObject jsonData = new JsonObject();

		// put room info
		JsonObject obj = new JsonObject();
		obj.addProperty("room_id", room.getId());
		obj.addProperty("room_name", room.getName());
		Object roomInfoObj = room.getProperty(NetworkConstant.ROOM_INFO);
		if (roomInfoObj != null) {
			RoomInfo roomInfo = (RoomInfo) roomInfoObj;
			obj.addProperty("bet_coin", roomInfo.getBetCoin());
			obj.addProperty("time_out", roomInfo.getTimeOut());

		}
		jsonData.add("room_info", obj);

		// put joiner
		jsonData.add("joiner", buildPlayerInfo(obj, joiner));

		List<User> players = room.getPlayersList();
		// put player list
		if (players.size() > 1) {
			JsonArray playerList = new JsonArray();
			for (User user : players) {
				if (user.getUserId() != joiner.getUserId()) {
					playerList.add(buildPlayerInfo(obj, user));
				}
			}

			jsonData.add("player_list", playerList);
		}

		if (isPlaying()) {
			JsonObject gameData = new JsonObject();
			gameData.addProperty("remain_time", getRemainTime());
			gameData.addProperty("game_state", gameState.getValue());
			jsonData.add("game_data", gameData);
			if (reconnect) {
				Player player = getPlayerByUser(joiner);
				jsonData.addProperty("card_list", GsonUtils.toGsonString(player.getCards().getCardIdArray()));
			}
		}

		// put game data, danh sách bài, thời gian còn lại của bàn chơi
		Message message = DefaultMessageFactory.createMessage(SystemNetworkConstant.COMMAND_USER_JOIN_ROOM);
		message.putString(SystemNetworkConstant.KEYS_JSON_DATA, jsonData.toString());
		return message;
	}

	/**
	 * lấy thời gian đang đếm còn lại
	 * 
	 * @return
	 */
	private int getRemainTime() {
		return MauBinhConfig.limitTime - (int) ((System.currentTimeMillis() - startGameTime) / 1000);
	}

	private JsonObject buildPlayerInfo(JsonObject obj, User user) {
		int seatNumber = getSeatNumber(user);
		seatNumber = seatNumber >= 0 ? seatNumber : getAvailableSeat();
		obj = new JsonObject();
		obj.addProperty("position", seatNumber);
		obj.addProperty("user_id", user.getCreantUserId());
		obj.addProperty("user_name", user.getUserName());
		obj.addProperty("avatar", user.getAvatar());
		obj.addProperty("money", user.getMoney());
		obj.addProperty("is_owner", user.getUserId() == getOwner().getUserId());
		return obj;

	}

	private void processUserDisconnect() {
		debug("[DEBUG] processUserDisconnect...................");
		// có thằng nào disconnect trước đó ko, có thì đá ra
		if (disconnectedUsers.size() > 0) {
			Message message = MessageFactory.createMauBinhMessage(GameCommand.ACTION_QUIT_GAME);
			User user = null;
			for (Integer creantUserId : disconnectedUsers.keySet()) {
				user = disconnectedUsers.get(creantUserId);
				debug(String.format("[DEBUG] process disconnect for [user: %s]", user.toString()));
				message.putInt(SystemNetworkConstant.KEYI_USER_ID, user.getCreantUserId());
				gameApi.sendAllInRoomExceptUser(message, user);
				disconnectedUsers.remove(creantUserId);
			}
		}

		debug("[DEBUG] processUserDisconnect finished...................");
	}

	public void stopCountDown() {
		debug("[DEBUG] stop count down");
		// countDownSeconds = startAfterSeconds;

		countdownSchedule.cancel(false);
	}

	private String logCard(List<Card> cards) {
		StringBuilder sb = new StringBuilder();
		int count = 0;
		String chi1 = "";
		String chi2 = "";
		String chi3 = "";
		for (Card card : cards) {
			sb.append(card.getId() + ",");
			if (count < 3) {
				chi1 += card.getName() + "   ";
			} else if (count < 8) {
				chi2 += card.getName() + "   ";
			} else {
				chi3 += card.getName() + "   ";
			}
			count++;
		}

		debug(String.format("[DEBUG] Auto Arrange cards: %s", sb.toString()));

		return chi1 + "\n" + chi2 + "\n" + chi3;
	}

	private static enum STATE {
		NOT_START(0), PLAYING(1), FINISH(2), CALCULATE(3);
		private int value;

		STATE(int value) {
			this.value = value;
		}

		public int getValue() {
			return this.value;
		}
	}
}
