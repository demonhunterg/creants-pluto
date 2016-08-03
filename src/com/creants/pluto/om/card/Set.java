package com.creants.pluto.om.card;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.creants.pluto.logic.GameChecker;
import com.creants.pluto.logic.MauBinhCardSet;
import com.creants.pluto.util.MauBinhConfig;

/**
 * Đối tượng Chi của bài
 * 
 * @author LamHa
 *
 */
public class Set {
	public static final int NOT_ENOUGH_CARD = -1;
	public static final int HIGH_CARD = 0;
	public static final int ONE_PAIR = 1;
	public static final int TWO_PAIR = 2;
	public static final int THREE_OF_KIND = 3;
	public static final int STRAIGHT = 4;
	public static final int FLUSH = 5;
	public static final int FULL_HOUSE = 6;
	public static final int FOUR_OF_KIND = 7;
	public static final int STRAIGHT_FLUSH = 8;

	private List<Card> cards;
	private int cardNumber;
	// Kiểu của chi như là thùng, sảnh được sắp xếp tăng dần
	private int type;

	public Set(int size) {
		cards = new ArrayList<Card>();
		cardNumber = size;
		type = SetType.NOT_ENOUGH_CARD;
	}

	public List<Card> getCards() {
		return cards;
	}

	public void clear() {
		cards.clear();
		type = SetType.NOT_ENOUGH_CARD;
	}

	public boolean receivedCard(Card card) {
		if (isEnough()) {
			return false;
		}

		for (Card card2 : cards) {
			if (card.getId() == card2.getId()) {
				return false;
			}
		}

		cards.add(card);
		Collections.sort(cards);

		if (isEnough()) {
			setType();
		}

		return true;
	}

	public int getType() {
		return type;
	}

	/**
	 * Nếu là thùng (5 con bài cùng màu, đồng chất) hoặc là thùng phá sảnh
	 * 
	 * @return
	 */
	public boolean isFlush() {
		return type == SetType.FLUSH || type == SetType.STRAIGHT_FLUSH;
	}

	/**
	 * Nếu là sảnh (5 con bài tăng dần) hoặc là thùng phá sảnh
	 * 
	 * @return
	 */
	public boolean isStraight() {
		return type == SetType.STRAIGHT || type == SetType.STRAIGHT_FLUSH;
	}

	/**
	 * So sánh chi
	 * 
	 * @param set
	 *            chi cần so
	 * @return
	 */
	public int getWinChiInComparisonWith(Set set) {
		switch (compareWith(set)) {
		case 1:
			return getWinChi();
		case 0:
			return 0;
		case -1:
			return -set.getWinChi();
		}

		return Integer.MIN_VALUE;
	}

	public int getWinChi() {
		if (getType() == -1) {
			return Integer.MIN_VALUE;
		}

		return 1;
	}

	public int compareWith(Set set) {
		if (set == null || type == -1 || set.getType() == -1) {
			return Integer.MIN_VALUE;
		}

		if (type < set.getType()) {
			return -1;
		}
		if (type > set.getType()) {
			return 1;
		}
		return compareWithSameType(set);
	}

	/**
	 * Có đủ bài hay không
	 * 
	 * @return
	 */
	public boolean isEnough() {
		return cards.size() >= cardNumber;
	}

	public String getName() {
		switch (getType()) {
		case 0:
			return MauBinhConfig.getInstance().getNameSetHighCard();
		case 1:
			return MauBinhConfig.getInstance().getNameSetOnePair();
		case 2:
			return MauBinhConfig.getInstance().getNameSetTwoPair();
		case 3:
			return MauBinhConfig.getInstance().getNameSetThreeOfKind();
		case 4:
			return MauBinhConfig.getInstance().getNameSetStraight();
		case 5:
			return MauBinhConfig.getInstance().getNameSetFlush();
		case 6:
			return MauBinhConfig.getInstance().getNameSetFullHouse();
		case 7:
			return MauBinhConfig.getInstance().getNameSetFourOfKind();
		case 8:
			return MauBinhConfig.getInstance().getNameSetStraightFlush();
		}
		return MauBinhConfig.getInstance().getNameSetUnknown();
	}

	protected void setType(int type) {
		this.type = type;
	}

	protected void setType() {
		if (cards == null || cards.isEmpty() || !isEnough()) {
			type = -1;
			return;
		}

		boolean isFlush = true;
		int pairNo = 0;
		int threeNo = 0;
		int sameCardNo = 0;
		for (int i = 1; i < cards.size(); i++) {
			if (isFlush && cards.get(i).getCardType() != cards.get(i - 1).getCardType()) {
				isFlush = false;
			}

			if (cards.get(i).getCardNumber() == cards.get(i - 1).getCardNumber()) {
				sameCardNo++;
			} else {
				switch (sameCardNo) {
				case 0:
					break;
				case 1:
					pairNo++;
					break;
				case 2:
					threeNo++;
					break;
				case 3:
					type = 7;
					return;
				}

				sameCardNo = 0;
			}
		}

		switch (sameCardNo) {
		case 0:
			break;
		case 1:
			pairNo++;
			break;
		case 2:
			threeNo++;
			break;
		case 3:
			type = 7;
			return;
		}

		if (threeNo > 0) {
			if (pairNo > 0) {
				type = 6;
				return;
			}

			type = 3;
			return;
		}

		if (pairNo == 2) {
			type = 2;
			return;
		}
		if (pairNo == 1) {
			type = 1;
			return;
		}

		if (isStraight(cards)) {
			if (isFlush) {
				type = 8;
				return;
			}

			type = 4;
			return;
		}

		if (isFlush) {
			type = 5;
			return;
		}

		type = 0;
	}

	private boolean isStraight(List<Card> cards) {
		if (cards == null || cards.isEmpty()) {
			return false;
		}

		return isNormalStraight(cards) || is2ndStraight(cards);
	}

	private boolean isNormalStraight(List<Card> cards) {
		if (cards == null || cards.isEmpty()) {
			return false;
		}

		int firstCardNumber = this.cards.get(0).getCardNumber();
		int lastCardNumber = this.cards.get(this.cards.size() - 1).getCardNumber();
		return lastCardNumber - firstCardNumber == this.cards.size() - 1;
	}

	private boolean is2ndStraight(List<Card> cards) {
		if (cards == null || cards.isEmpty()) {
			return false;
		}

		if (MauBinhCardSet.isAce(this.cards.get(this.cards.size() - 1))) {
			return ((this.cards.size() == 5) && (MauBinhCardSet.is5(this.cards.get(this.cards.size() - 2))))
					|| ((this.cards.size() == 3) && (MauBinhCardSet.is3(this.cards.get(this.cards.size() - 2))));
		}

		return false;
	}

	private int compareWithSameType(Set set) {
		switch (type) {
		case 0:
			return compareWithHighCard(set);
		case 1:
			return compareWithOnePair(set);
		case 2:
			return compareWithTwoPair(set);
		case 3:
			return compareWithThreeOfKind(set);
		case 4:
			return compareWithStright(set);
		case 5:
			return compareWithFlush(set);
		case 6:
			return compareWithFullHouse(set);
		case 7:
			return compareWithFourOfKind(set);
		case 8:
			return compareWithStraightFlush(set);
		}
		return Integer.MIN_VALUE;
	}

	/**
	 * Mậu thầu (Xét lá bài cao nhất)
	 * 
	 * @param set
	 * @return
	 */
	private int compareWithHighCard(Set set) {
		return GameChecker.compareCardByCard(cards, set.getCards());
	}

	private int compareWithOnePair(Set set) {
		Card pair01 = null;

		for (int i = 1; i < getCards().size(); i++) {
			if (getCards().get(i - 1).getCardNumber() == getCards().get(i).getCardNumber()) {
				pair01 = getCards().get(i);
				break;
			}
		}

		Card pair02 = null;
		for (int i = 1; i < set.getCards().size(); i++) {
			if (set.getCards().get(i - 1).getCardNumber() == set.getCards().get(i).getCardNumber()) {
				pair02 = set.getCards().get(i);
				break;
			}
		}

		if (pair01 == null || pair02 == null) {
			return Integer.MIN_VALUE;
		}

		if (pair01.getCardNumber() > pair02.getCardNumber())
			return 1;
		if (pair01.getCardNumber() < pair02.getCardNumber()) {
			return -1;
		}

		return GameChecker.compareCardByCard(getCards(), set.getCards());
	}

	private int compareWithTwoPair(Set set) {
		if (getCards() == null || set.getCards() == null || getCards().size() < 5 || set.getCards().size() < 5) {
			return Integer.MIN_VALUE;
		}

		Card pair01 = getCards().get(3);
		Card pair02 = set.getCards().get(3);

		if (pair01.getCardNumber() > pair02.getCardNumber())
			return 1;
		if (pair01.getCardNumber() < pair02.getCardNumber()) {
			return -1;
		}

		pair01 = getCards().get(1);
		pair02 = set.getCards().get(1);

		if (pair01.getCardNumber() > pair02.getCardNumber())
			return 1;
		if (pair01.getCardNumber() < pair02.getCardNumber()) {
			return -1;
		}

		return GameChecker.compareCardByCard(getCards(), set.getCards());
	}

	private int compareWithThreeOfKind(Set set) {
		if (getCards() == null || set.getCards() == null || getCards().size() < 3 || set.getCards().size() < 3) {
			return Integer.MIN_VALUE;
		}

		Card three01 = getCards().get(2);
		Card three02 = set.getCards().get(2);

		if (three01.getCardNumber() > three02.getCardNumber())
			return 1;
		if (three01.getCardNumber() < three02.getCardNumber()) {
			return -1;
		}
		return 0;
	}

	private int compareWithStright(Set set) {
		return GameChecker.compare2HighestCards(getCards(), set.getCards());
	}

	private int compareWithFlush(Set set) {
		return GameChecker.compareCardByCard(getCards(), set.getCards());
	}

	private int compareWithFullHouse(Set set) {
		if (getCards() == null || set.getCards() == null || getCards().size() < 5 || set.getCards().size() < 5) {
			return Integer.MIN_VALUE;
		}

		Card three01 = getCards().get(2);
		Card three02 = set.getCards().get(2);
		if (three01.getCardNumber() > three02.getCardNumber())
			return 1;

		if (three01.getCardNumber() < three02.getCardNumber()) {
			return -1;
		}
		return 0;
	}

	private int compareWithFourOfKind(Set set) {
		if (getCards() == null || set.getCards() == null || getCards().size() < 5 || set.getCards().size() < 5) {
			return Integer.MIN_VALUE;
		}

		Card four01 = getCards().get(2);
		Card four02 = set.getCards().get(2);

		if (four01.getCardNumber() > four02.getCardNumber())
			return 1;
		if (four01.getCardNumber() < four02.getCardNumber()) {
			return -1;
		}
		return 0;
	}

	private int compareWithStraightFlush(Set set) {
		return GameChecker.compare2HighestCards(getCards(), set.getCards());
	}
}