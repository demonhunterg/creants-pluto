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

import com.avengers.netty.core.event.SystemNetworkConstant;
import com.avengers.netty.core.om.IRoom;
import com.avengers.netty.core.util.Tracer;
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
import com.creants.pluto.util.MauBinhConfig;
import com.creants.pluto.util.MessageFactory;
import com.google.gson.JsonObject;

/**
 * @author LamHa
 *
 */
public class MauBinhGame {
	public STATE gameState;
	private GameAPI gameApi;
	private static final MauBinhConfig gameConfig = MauBinhConfig.getInstance();
	private User winner = null;
	private MoneyManager moneyManager = null;
	private transient MauBinhCardSet cardSet;
	private Player[] players;
	private int limitTime = 90;
	// thời gian khi start game
	private long startTime;
	private IRoom room;
	private Map<String, User> disconnectedUsers;

	// game start sau bao 10s
	private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
	private ScheduledFuture<?> countdownSchedule;
	private int startAfterSeconds = 10;
	private Integer countDownSeconds = 0;

	public MauBinhGame(IRoom room) {
		this.room = room;
		gameState = STATE.NOT_START;
		cardSet = new MauBinhCardSet();
		players = new Player[4];
		for (int i = 0; i < players.length; i++) {
			players[i] = new Player();
		}

		disconnectedUsers = new ConcurrentHashMap<String, User>();
	}

	public void setGameApi(GameAPI gameApi) {
		this.gameApi = gameApi;
	}

	public long getGuaranteeMoney(User user) {
		return moneyManager.getGuaranteeMoney(user);
	}

	protected User getWinner() {
		return winner;
	}

	public JsonObject getGameData() {
		if (isPlaying()) {
			JsonObject gameData = new JsonObject();
			// lấy thời gian đang đếm còn lại
			int remainTime = limitTime - (int) ((System.currentTimeMillis() - startTime) / 1000);
			gameData.addProperty("remain_time", remainTime);
			gameData.addProperty("game_state", gameState.getValue());
			return gameData;
		}

		return null;
	}

	public void disconnect(User user) {
		// cho phép reconnect
		disconnectedUsers.put(user.getUserName(), user);
	}

	/**
	 * User reconnect
	 * 
	 * @param user
	 */
	public void onReturnGame(User user) {
		if (user == null) {
			return;
		}

		// lấy lại thông tin user cũ
		user = disconnectedUsers.get(user.getUserName());
		Player player = getPlayerByUser(user);
		if (player == null) {
			return;
		}

		disconnectedUsers.remove(user.getUserName());
		int restTime = 0;
		if (isPlaying()) {
			restTime = (int) ((limitTime - (System.currentTimeMillis() - startTime)) / 1000L);
		}

		if (!isPlaying() || player.getUser() == null) {
			gameApi.sendToUser(MessageFactory.makeInGameInforMessageForViewer(limitTime / 1000, restTime), user);
			return;
		}

		List<Card> cardList = player.isFinish() ? player.getCards().getArrangeCards() : player.getCards().list();
		gameApi.sendToUser(MessageFactory.makeInGameInforMessage(player.isFinish(), cardList, limitTime / 1000,
				restTime, (byte) player.getCards().getMauBinhType()), user);
	}

	private User getOwner() {
		return room.getOwner();
	}

	public long getMoney() {
		// TODO optimize
		RoomInfo roomInfo = (RoomInfo) room.getProperty(NetworkConstant.ROOM_INFO);
		return roomInfo.getBetCoin();
	}

	private int playerSize() {
		return room.getPlayersList().size();
	}

	public void startCountDown(int startAfterSeconds) {
		countDownSeconds = startAfterSeconds;

		if (countdownSchedule == null || countdownSchedule.isCancelled()) {
			countDown();
			return;
		}

		debug(String.format("[ERROR] [room:%s] Do startCountDown with countdown schedule not cancel yet!",
				room.getName()));
	}

	private void debug(Object... msgs) {
		Tracer.debugPlutoGame(MauBinhGame.class, msgs);
	}

	public int getStartAfterSeconds() {
		return startAfterSeconds;
	}

	public void startGame() {
		try {
			debug(String.format("[DEBUG] Game is begin [roomId: %d, roomName: %s]", room.getId(), room.getName()));
			// TODO kiểm tra trong danh sách disconnect để đá user này ra
			if (playerSize() < 2) {
				debug("[DEBUG] Game is begin with a player");
				return;
			}

			moneyManager = new MoneyManager(getMoney());
			// kiểm tra có chủ phòng và chủ phòng còn tiền không để start game
			// không
			if (getOwner() != null && !moneyManager.enoughMoneyToStart(getOwner())) {
				gameApi.sendToUser(MessageFactory.makeErrorMessage("Khong du tien"), getOwner());
				for (int i = 0; i < playerSize(); i++) {
					User user = players[i].getUser();
					if (user != null && !user.equals(getOwner())) {
						playerReady(user, false);
					}
				}

				debug(String.format("[DEBUG] startGame fail! [user: %s] not enough money", getOwner().getUserName()));
				return;
			}

			gameState = STATE.PLAYING;
			startTime = System.currentTimeMillis();

			// xóc bài
			cardSet.xaoBai();

			// chia bài
			deliveryCard();

			debug(String.format("[DEBUG] Delivery card finish [roomId: %d, roomName: %s]. Start timeout in %d seconds",
					room.getId(), room.getName(), 91));

			startCountDown(91);
		} catch (Exception e) {
			Tracer.error(MauBinhGame.class, "[ERROR] startGame fail!", e);
		}
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
			Tracer.error(MauBinhGame.class, "[ERROR] stopGame fail!", e);
		}
	}

	private int getSeatNumber(User user) {
		for (int i = 0; i < players.length; i++) {
			Player player = players[i];
			if (player.getUser() != null && player.getUserId() == user.getUserId())
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

			if (moneyManager == null || moneyManager.getGameMoney() != getMoney()) {
				moneyManager = new MoneyManager(getMoney());
			}

			if (moneyManager.enoughMoneyToStart(user)) {
				debug("[DEBUG] [user:%s] playerReady!", user.getUserName());
				Player player = players[nSeat];
				player.setReady(true);
			} else {
				gameApi.sendToUser(MessageFactory.makeErrorMessage("Bạn không đủ tiền"), user);
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
			debug(String.format("[ERROR] [user:%s] leave room [%s] fail! ", player.getUserName(), getRoom().getName()),
					e);
		}
	}

	private void kickOut(User user) {
		debug(String.format("[DEBUG] [user:%s] kick out", user.getUserName()));
		int seatNumber = getSeatNumber(user);
		players[seatNumber].setUser(null);
		players[seatNumber].reset();
	}

	public void setMoney(int value) {
		if (getOwner() == null) {
			return;
		}

		if (getOwner().getMoney() >= gameConfig.getStartMoneyRate() * value) {
			moneyManager = new MoneyManager(value);
		} else {
			gameApi.sendToUser(MessageFactory.makeErrorMessage("Khong du tien"), getOwner());
		}
	}

	protected void join(User user, String pwd) {
		System.out.println("[FATAL] join room");
		if (user != null
				&& (user.getMoney() >= gameConfig.getStartMoneyRate() * Math.max(getMinRoomMoney(), getMoney()))) {
			// super.join(user, pwd);
			if (moneyManager == null || moneyManager.getGameMoney() != getMoney()) {
				moneyManager = new MoneyManager(getMoney());
			}

			if (!moneyManager.enoughMoneyToStart(user)) {
				setMoneyToMin();
			}

			if (getOwner().equals(user)) {
				int limitTimeType;
				if (limitTime == gameConfig.getLimitTimeSlow()) {
					limitTimeType = 0;
				} else {

					if (limitTime == gameConfig.getLimitTimeFast()) {
						limitTimeType = 2;
					} else {
						limitTimeType = 1;
					}
				}

				gameApi.sendToUser(
						MessageFactory.makeSetLimitTimeMessage((byte) (limitTime / 1000), (byte) limitTimeType), user);
			}

			byte restTime = 0;
			if (isPlaying()) {
				restTime = (byte) ((limitTime - (System.currentTimeMillis() - startTime)) / 1000L);
			}

			gameApi.sendToUser(MessageFactory.makeTableInfoMessage((byte) (limitTime / 1000), restTime), user);
		} else {
			gameApi.sendToUser(MessageFactory.makeErrorMessage("NotEnoughMoneyToSet"), user);
		}
	}

	/**
	 * Kiểm tra game có thể hoàn thành không. Tất cả player đã finish chưa.
	 * 
	 * @return
	 */
	private boolean canBeFinish() {
		if (players == null || players.length == 0) {
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
		// TODO optimize
		return room.getListUser().get(index);
	}

	private IRoom getRoom() {
		return room;
	}

	/**
	 * Chia bài
	 */
	private void deliveryCard() {
		debug("[ERROR] Do deliver card");
		for (int i = 0; i < players.length; i++) {
			players[i].reset();
		}

		for (int i = 0; i < cardSet.length(); i++) {
			players[(i % 4)].getCards().receivedCard(cardSet.dealCard());
		}

		for (int i = 0; i < playerSize(); i++) {
			User receiver = getUser(i);
			players[i].setUser(receiver);
			if (receiver != null) {
				Message message = MessageFactory.makeStartMessage(getRoom().getId(), limitTime / 1000,
						players[i].getCards().list(), (byte) players[i].getCards().getMauBinhType());
				gameApi.sendToUser(message, receiver);
			}
		}

		// trường hợp đang chia bài mà người chơi bấm tự động bin hết
		if (GameChecker.isFinishAll(players)) {
			processGameFinish();
		}
	}

	private boolean isPlaying() {
		return gameState == STATE.PLAYING;
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

				int remainTime = limitTime - (int) ((System.currentTimeMillis() - startTime) / 1000);
				if (remainTime <= 0) {
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
					gameApi.sendAllInRoom(MessageFactory.makeFinishMessage(user.getUserId()));
				}

				player.setFinishFlag(true);
				if (GameChecker.isFinishAll(players)) {
					processGameFinish();
				}
			}
		} catch (Exception e) {
			debug(String.format("[ERROR] [user:%s] Do processBinhFinish fail!", user.getUserName()), e);
			Tracer.error(MauBinhGame.class, "[ERROR] Do processBinhFinish fail!", e);
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
	public List<Card> processAutoArrangeCommand(Player player) {
		List<Card> result = new ArrayList<Card>();
		try {
			result = AutoArrangement.getSolution(player.getCards().list());
			debug(String.format("[DEBUG] Auto Arrange [username:%s] cards:\n %s", player.getUser().getUserName(),
					logCard(result)));

			if (!player.isTimeOut()) {
				Message m = MessageFactory.makeAutoArrangeResultMessage(result);
				gameApi.sendToUser(m, player.getUser());
			}

			player.setAutoArrangementFlag(true);
		} catch (Exception e) {
			Tracer.error(MauBinhGame.class, "[ERROR] processAutoArrangeCommand fail!", e);
		}

		return result;
	}

	private void processGameFinish() {
		gameState = STATE.CALCULATE;
		stopCountDown();
		debug(String.format("[DEBUG] calculating........................ in! 5 seconds"));
		Result[][] result = GameChecker.comparePlayers(players);
		int[] winChi = GameChecker.getWinChi(players, result);
		long[] winMoney = moneyManager.calculateMoney(winChi);
		winMoney = moneyManager.addBonusChi(players, winMoney, winChi);
		// winMoney = moneyManager.updateMoney(this, players, winMoney, winChi,
		// result);
		// winMoney = moneyManager.updateMoneyForAutoArrangementUsing(this,
		// players, winMoney);

		for (int i = 0; i < players.length; i++) {
			User user = players[i].getUser();
			if (user != null) {
				if (winMoney[i] != 0) {
					gameApi.updateUserMoney(user, winMoney[i], 1, "Cập nhật tiền kết thúc game");
					debug(String.format("[DEBUG] Do processGameFinish! update money [user: %s, money: %d]",
							user.getUserName(), winMoney[i]));
				}

				// gửi thông tin kết quả cho player
				Message message = MessageFactory.makeResultMessage(i, players, winMoney, winChi, result);
				if (message != null) {
					debug("Game result: " + message.toString());
					gameApi.sendToUser(message, user);
				}
			}
		}

		winner = getWinner(winChi);

		// kiểm tra chủ phòng có đủ tiền để start không
		if (!moneyManager.enoughMoneyToStart(getOwner())) {
			setMoneyToMin();
		}

		// show bài trong 5s
		startCountDown(5);
	}

	private User getWinner(int[] winChi) {
		if (winChi == null || winChi.length == 0 || players == null || winChi.length != players.length) {
			return null;
		}

		int seatWinner = -1;
		User ret = null;
		int maxWinChi = Integer.MIN_VALUE;
		for (int i = 0; i < players.length; i++) {
			if (players[i].getUser() != null) {

				if (winChi[i] == maxWinChi) {
					seatWinner = -1;

				} else if (maxWinChi < winChi[i]) {
					maxWinChi = winChi[i];
					seatWinner = i;
				}
			}
		}

		if (seatWinner != -1) {
			ret = players[seatWinner].getUser();
		}

		return ret;
	}

	private void setMoneyToMin() {
		setMoney(getMinRoomMoney());
	}

	private int getMinRoomMoney() {
		return 0;
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
			Tracer.error(MauBinhGame.class, "[ERROR] getMessage fail!",
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
						if (gameState == STATE.PLAYING) {
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
							stopCountDown();
							// đá player bị mất kết nối
							processUserDisconnect();
							debug("[FATAL] Check player ready.");
							// đá player chưa sẵn sàn cho ván kế
							for (int i = 0; i < players.length; i++) {
								Player player = players[i];
								if (player.getUser() != null && !player.isReady()) {
									// TODO đá ra
								}
							}

							stopGame();
							gameState = STATE.FINISH;
							debug("[FATAL] Update state to FINSH");

							// đếm cho ván tiếp theo
							startCountDown(6);
							return;
						}

						if (gameState == STATE.FINISH) {
							gameState = STATE.NOT_START;
							startGame();
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}

			}
		}, 0, 1, TimeUnit.SECONDS);

	}

	private void processUserDisconnect() {
		debug("[DEBUG] processUserDisconnect...................");
		// có thằng nào disconnect trước đó ko, có thì đá ra
		if (disconnectedUsers.size() > 0) {
			Message message = MessageFactory.createMauBinhMessage(GameCommand.ACTION_QUIT_GAME);
			User user = null;
			for (String username : disconnectedUsers.keySet()) {
				user = disconnectedUsers.get(username);
				debug(String.format("[DEBUG] process disconnect for [user: %s, user_id: %d]", username,
						user.getUserId()));
				message.putInt(SystemNetworkConstant.KEYI_USER_ID, user.getUserId());
				gameApi.sendAllInRoomExceptUser(message, user);
				disconnectedUsers.remove(username);
			}
		}

		debug("[FATAL] processUserDisconnect finished...................");
	}

	public void stopCountDown() {
		debug("[DEBUG] stop count down");
		// countDownSeconds = startAfterSeconds;

		countdownSchedule.cancel(false);
	}

	private String logCard(List<Card> cards) {
		int count = 0;
		String chi1 = "";
		String chi2 = "";
		String chi3 = "";
		for (Card card : cards) {
			if (count < 3) {
				chi1 += card.getName() + "   ";
			} else if (count < 8) {
				chi2 += card.getName() + "   ";
			} else {
				chi3 += card.getName() + "   ";
			}
			count++;
		}

		return chi1 + "\n" + chi2 + "\n" + chi3;
	}

	public static enum STATE {
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
