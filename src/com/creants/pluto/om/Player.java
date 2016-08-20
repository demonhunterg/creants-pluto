package com.creants.pluto.om;

import com.avengers.netty.socket.gate.wood.User;
import com.creants.pluto.om.card.Cards;

/**
 * @author LamHa
 *
 */
public class Player {
	private User user;
	private Cards cards;
	private int bonusMoney;
	private int bonusChi;
	private boolean isFinish;
	private boolean isUsedAutoArrangement;
	private boolean timeOut;
	private boolean isReady;

	public Player() {
		reset();
	}

	public void reset() {
		user = null;
		cards = new Cards();
		bonusMoney = 0;
		bonusChi = 0;
		isFinish = false;
		isUsedAutoArrangement = false;
		timeOut = false;
		isReady = false;
	}

	public int getUserId() {
		return user.getUserId();
	}

	public Cards getCards() {
		return cards;
	}

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	public boolean isTimeOut() {
		return timeOut;
	}

	public void setIsTimeOut(boolean timeOut) {
		this.timeOut = timeOut;
	}

	public int getBonusMoney() {
		return bonusMoney;
	}

	public void addBonusMoney(int value) {
		bonusMoney += value;
	}

	public int getBonusChi() {
		return bonusChi;
	}

	public void addBonusChi(int chi) {
		bonusChi += chi;
	}

	public boolean isFinish() {
		return isFinish;
	}

	public void setFinishFlag(boolean value) {
		isFinish = value;
	}

	public boolean isUsedAutoArrangement() {
		return isUsedAutoArrangement;
	}

	public void setAutoArrangementFlag(boolean value) {
		isUsedAutoArrangement = value;
	}

	public boolean isReady() {
		return isReady;
	}

	public void setReady(boolean isReady) {
		this.isReady = isReady;
	}

}
