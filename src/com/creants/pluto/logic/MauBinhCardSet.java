package com.creants.pluto.logic;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.creants.pluto.om.MauBinhType;
import com.creants.pluto.om.card.Card;
import com.creants.pluto.om.card.CardSet;

/**
 * @author LamHa
 *
 */
public class MauBinhCardSet extends CardSet {
	public static final int NUMBER_ACE = 13;
	public static final int NUMBER_FIVE = 4;
	public static final int NUMBER_THREE = 2;
	public static final int NUMBER_TWO = 1;
	public static final int TYPE_HEART = 3;
	public static final int TYPE_DIAMOND = 2;
	public static final int TYPE_CLUB = 1;
	public static final int TYPE_SPADE = 0;

	/**
	 * Con xì
	 */
	public static boolean isAce(Card card) {
		return card.getCardNumber() == NUMBER_ACE;
	}

	/**
	 * Con 3
	 */
	public static boolean is3(Card card) {
		return card.getCardNumber() == 1;
	}

	/**
	 * Con heo
	 */
	public static boolean is2(Card card) {
		return card.getCardNumber() == 0;
	}

	/**
	 * Con 5
	 */
	public static boolean is5(Card card) {
		return card.getCardNumber() == NUMBER_FIVE;
	}

	/**
	 * Quân bài đỏ
	 * 
	 * @param card
	 * @return
	 */
	public static boolean isRed(Card card) {
		return card.getCardType() == TYPE_HEART || card.getCardType() == TYPE_DIAMOND;
	}

	private static List<Card> getMauBinhCards(int maubinhType) {
		Random random = new Random();
		List<Card> cardList = new ArrayList<Card>();
		byte id;
		switch (maubinhType) {
		case MauBinhType.SAME_COLOR_13:
			for (int i = 0; i < 13; i++) {
				cardList.add(CardSet.getCard((byte) (i * 4 + random.nextInt(1))));
			}

			return cardList;
		case MauBinhType.FOUR_OF_THREE:
			id = 0;
			for (int i = 0; i < 13; i++) {
				cardList.add(CardSet.getCard(id));
				id = (byte) (id + 1);
				if (id % 4 == 3) {
					id = (byte) (id + 1);
				}
			}

			return cardList;
		case MauBinhType.STRAIGHT_13:
			for (int i = 0; i < 13; i++) {
				cardList.add(CardSet.getCard((byte) (i * 4 + random.nextInt(3))));
			}

			return cardList;
		case MauBinhType.FIVE_PAIR_WITH_THREE:
			for (int i = 0; i < 6; i++) {
				cardList.add(CardSet.getCard((byte) (i * 4)));
				cardList.add(CardSet.getCard((byte) (i * 4 + 1)));
			}

			cardList.add(CardSet.getCard((byte) (random.nextInt(6) * 4 + 2)));
			return cardList;
		case MauBinhType.SAME_COLOR_12:
			for (int i = 1; i < 13; i++) {
				cardList.add(CardSet.getCard((byte) (i * 4 + random.nextInt(1))));
			}

			cardList.add(CardSet.getCard((byte) (2 + random.nextInt(1))));
			return cardList;
		case MauBinhType.THREE_FLUSH:
			for (int i = 0; i < 5; i++) {
				cardList.add(CardSet.getCard((byte) (random.nextInt(13) * 4)));
			}

			for (int i = 0; i < 5; i++) {
				cardList.add(CardSet.getCard((byte) (random.nextInt(13) * 4 + 1)));
			}

			for (int i = 0; i < 3; i++) {
				cardList.add(CardSet.getCard((byte) (random.nextInt(13) * 4 + 2)));
			}

			return cardList;
		case MauBinhType.THREE_STRAIGHT:
			id = (byte) random.nextInt(8);
			for (int i = 0; i < 5; i++) {
				cardList.add(CardSet.getCard((byte) ((id + i) * 4 + random.nextInt(4))));
			}

			id = (byte) random.nextInt(8);
			for (int i = 0; i < 5; i++) {
				cardList.add(CardSet.getCard((byte) ((id + i) * 4 + random.nextInt(4))));
			}

			id = (byte) random.nextInt(10);
			for (int i = 0; i < 3; i++) {
				cardList.add(CardSet.getCard((byte) ((id + i) * 4 + random.nextInt(4))));
			}

			return cardList;
		case MauBinhType.SIX_PAIR:
			for (int i = 0; i < 6; i++) {
				cardList.add(CardSet.getCard((byte) (i * 4)));
				cardList.add(CardSet.getCard((byte) (i * 4 + 1)));
			}

			cardList.add(CardSet.getCard((byte) ((6 + random.nextInt(6)) * 4 + 2)));
			return cardList;
		}

		return null;
	}

	private static List<Card> getBigStraightCards(int type) {
		List<Card> cardList = new ArrayList<Card>();
		switch (type) {
		case 1:
			cardList.add(CardSet.getCard((byte) 33));
			cardList.add(CardSet.getCard((byte) 38));
			cardList.add(CardSet.getCard((byte) 41));
			cardList.add(CardSet.getCard((byte) 46));
			cardList.add(CardSet.getCard((byte) 50));

			cardList.add(CardSet.getCard((byte) 0));
			cardList.add(CardSet.getCard((byte) 1));
			cardList.add(CardSet.getCard((byte) 6));
			cardList.add(CardSet.getCard((byte) 7));
			cardList.add(CardSet.getCard((byte) 8));
			cardList.add(CardSet.getCard((byte) 10));
			cardList.add(CardSet.getCard((byte) 11));
			cardList.add(CardSet.getCard((byte) 13));

			return cardList;
		case 2:
			cardList.add(CardSet.getCard((byte) 1));
			cardList.add(CardSet.getCard((byte) 6));
			cardList.add(CardSet.getCard((byte) 9));
			cardList.add(CardSet.getCard((byte) 14));
			cardList.add(CardSet.getCard((byte) 50));

			cardList.add(CardSet.getCard((byte) 17));
			cardList.add(CardSet.getCard((byte) 19));
			cardList.add(CardSet.getCard((byte) 23));
			cardList.add(CardSet.getCard((byte) 24));
			cardList.add(CardSet.getCard((byte) 25));
			cardList.add(CardSet.getCard((byte) 27));
			cardList.add(CardSet.getCard((byte) 28));
			cardList.add(CardSet.getCard((byte) 30));

			return cardList;
		}
		return null;
	}

	public static List<Card> getCardList(int index) {
		List<Card> cardList = new ArrayList<Card>();
		switch (index) {
		case 0:
			cardList.add(CardSet.getCard((byte) 0));
			cardList.add(CardSet.getCard((byte) 4));
			cardList.add(CardSet.getCard((byte) 7));
			cardList.add(CardSet.getCard((byte) 12));
			cardList.add(CardSet.getCard((byte) 13));
			cardList.add(CardSet.getCard((byte) 14));
			cardList.add(CardSet.getCard((byte) 15));
			cardList.add(CardSet.getCard((byte) 17));
			cardList.add(CardSet.getCard((byte) 20));
			cardList.add(CardSet.getCard((byte) 22));
			cardList.add(CardSet.getCard((byte) 24));
			cardList.add(CardSet.getCard((byte) 27));
			cardList.add(CardSet.getCard((byte) 36));
			break;
		case 1:
			cardList.add(CardSet.getCard((byte) 1));
			cardList.add(CardSet.getCard((byte) 3));
			cardList.add(CardSet.getCard((byte) 5));
			cardList.add(CardSet.getCard((byte) 9));
			cardList.add(CardSet.getCard((byte) 17));
			cardList.add(CardSet.getCard((byte) 29));
			cardList.add(CardSet.getCard((byte) 32));
			cardList.add(CardSet.getCard((byte) 35));
			cardList.add(CardSet.getCard((byte) 40));
			cardList.add(CardSet.getCard((byte) 41));
			cardList.add(CardSet.getCard((byte) 43));
			cardList.add(CardSet.getCard((byte) 48));
			cardList.add(CardSet.getCard((byte) 50));
			break;
		case 2:
			cardList.add(CardSet.getCard((byte) 2));
			cardList.add(CardSet.getCard((byte) 6));
			cardList.add(CardSet.getCard((byte) 11));
			cardList.add(CardSet.getCard((byte) 18));
			cardList.add(CardSet.getCard((byte) 28));
			cardList.add(CardSet.getCard((byte) 33));
			cardList.add(CardSet.getCard((byte) 34));
			cardList.add(CardSet.getCard((byte) 38));
			cardList.add(CardSet.getCard((byte) 42));
			cardList.add(CardSet.getCard((byte) 46));
			cardList.add(CardSet.getCard((byte) 47));
			cardList.add(CardSet.getCard((byte) 49));
			cardList.add(CardSet.getCard((byte) 51));
			break;
		case 3:
			cardList.add(CardSet.getCard((byte) 8));
			cardList.add(CardSet.getCard((byte) 10));
			cardList.add(CardSet.getCard((byte) 19));
			cardList.add(CardSet.getCard((byte) 21));
			cardList.add(CardSet.getCard((byte) 23));
			cardList.add(CardSet.getCard((byte) 25));
			cardList.add(CardSet.getCard((byte) 26));
			cardList.add(CardSet.getCard((byte) 30));
			cardList.add(CardSet.getCard((byte) 31));
			cardList.add(CardSet.getCard((byte) 37));
			cardList.add(CardSet.getCard((byte) 39));
			cardList.add(CardSet.getCard((byte) 44));
			cardList.add(CardSet.getCard((byte) 45));
			break;
		case 4:
			cardList.add(CardSet.getCard((byte) 8));
			cardList.add(CardSet.getCard((byte) 10));
			cardList.add(CardSet.getCard((byte) 19));
			cardList.add(CardSet.getCard((byte) 21));
			cardList.add(CardSet.getCard((byte) 23));
			cardList.add(CardSet.getCard((byte) 25));
			cardList.add(CardSet.getCard((byte) 26));
			cardList.add(CardSet.getCard((byte) 30));
			cardList.add(CardSet.getCard((byte) 32));
			cardList.add(CardSet.getCard((byte) 36));
			cardList.add(CardSet.getCard((byte) 40));
			cardList.add(CardSet.getCard((byte) 44));
			cardList.add(CardSet.getCard((byte) 48));
			break;
		case 5:
			cardList.add(CardSet.getCard((byte) 8));
			cardList.add(CardSet.getCard((byte) 10));
			cardList.add(CardSet.getCard((byte) 19));
			cardList.add(CardSet.getCard((byte) 21));
			cardList.add(CardSet.getCard((byte) 23));
			cardList.add(CardSet.getCard((byte) 25));
			cardList.add(CardSet.getCard((byte) 26));
			cardList.add(CardSet.getCard((byte) 30));
			cardList.add(CardSet.getCard((byte) 28));
			cardList.add(CardSet.getCard((byte) 32));
			cardList.add(CardSet.getCard((byte) 36));
			cardList.add(CardSet.getCard((byte) 40));
			cardList.add(CardSet.getCard((byte) 44));
			break;
		case 6:
			cardList.add(CardSet.getCard((byte) 10));
			cardList.add(CardSet.getCard((byte) 19));
			cardList.add(CardSet.getCard((byte) 21));
			cardList.add(CardSet.getCard((byte) 23));
			cardList.add(CardSet.getCard((byte) 25));
			cardList.add(CardSet.getCard((byte) 26));
			cardList.add(CardSet.getCard((byte) 30));
			cardList.add(CardSet.getCard((byte) 32));
			cardList.add(CardSet.getCard((byte) 48));
			cardList.add(CardSet.getCard((byte) 0));
			cardList.add(CardSet.getCard((byte) 4));
			cardList.add(CardSet.getCard((byte) 8));
			cardList.add(CardSet.getCard((byte) 12));
			break;
		case 7:
			cardList.add(CardSet.getCard((byte) 8));
			cardList.add(CardSet.getCard((byte) 10));
			cardList.add(CardSet.getCard((byte) 19));
			cardList.add(CardSet.getCard((byte) 21));
			cardList.add(CardSet.getCard((byte) 28));
			cardList.add(CardSet.getCard((byte) 32));
			cardList.add(CardSet.getCard((byte) 36));
			cardList.add(CardSet.getCard((byte) 40));
			cardList.add(CardSet.getCard((byte) 44));
			cardList.add(CardSet.getCard((byte) 48));
			cardList.add(CardSet.getCard((byte) 49));
			cardList.add(CardSet.getCard((byte) 50));
			cardList.add(CardSet.getCard((byte) 51));
			break;
		case 8:
			cardList.add(CardSet.getCard((byte) 0));
			cardList.add(CardSet.getCard((byte) 1));
			cardList.add(CardSet.getCard((byte) 7));
			cardList.add(CardSet.getCard((byte) 12));
			cardList.add(CardSet.getCard((byte) 13));
			cardList.add(CardSet.getCard((byte) 14));
			cardList.add(CardSet.getCard((byte) 15));
			cardList.add(CardSet.getCard((byte) 17));
			cardList.add(CardSet.getCard((byte) 20));
			cardList.add(CardSet.getCard((byte) 22));
			cardList.add(CardSet.getCard((byte) 49));
			cardList.add(CardSet.getCard((byte) 50));
			cardList.add(CardSet.getCard((byte) 51));
			break;
		case 9:
			cardList.add(CardSet.getCard((byte) 1));
			cardList.add(CardSet.getCard((byte) 4));
			cardList.add(CardSet.getCard((byte) 5));
			cardList.add(CardSet.getCard((byte) 9));
			cardList.add(CardSet.getCard((byte) 17));
			cardList.add(CardSet.getCard((byte) 29));
			cardList.add(CardSet.getCard((byte) 32));
			cardList.add(CardSet.getCard((byte) 35));
			cardList.add(CardSet.getCard((byte) 40));
			cardList.add(CardSet.getCard((byte) 41));
			cardList.add(CardSet.getCard((byte) 43));
			cardList.add(CardSet.getCard((byte) 48));
			cardList.add(CardSet.getCard((byte) 50));
			break;
		case 10:
			cardList.add(CardSet.getCard((byte) 6));
			cardList.add(CardSet.getCard((byte) 7));
			cardList.add(CardSet.getCard((byte) 10));
			cardList.add(CardSet.getCard((byte) 12));
			cardList.add(CardSet.getCard((byte) 14));
			cardList.add(CardSet.getCard((byte) 18));
			cardList.add(CardSet.getCard((byte) 19));
			cardList.add(CardSet.getCard((byte) 21));
			cardList.add(CardSet.getCard((byte) 22));
			cardList.add(CardSet.getCard((byte) 25));
			cardList.add(CardSet.getCard((byte) 31));
			cardList.add(CardSet.getCard((byte) 33));
			cardList.add(CardSet.getCard((byte) 48));
			break;
		case 11:
			cardList.add(CardSet.getCard((byte) 4));
			cardList.add(CardSet.getCard((byte) 9));
			cardList.add(CardSet.getCard((byte) 15));
			cardList.add(CardSet.getCard((byte) 19));
			cardList.add(CardSet.getCard((byte) 23));
			cardList.add(CardSet.getCard((byte) 24));
			cardList.add(CardSet.getCard((byte) 29));
			cardList.add(CardSet.getCard((byte) 31));
			cardList.add(CardSet.getCard((byte) 32));
			cardList.add(CardSet.getCard((byte) 35));
			cardList.add(CardSet.getCard((byte) 36));
			cardList.add(CardSet.getCard((byte) 45));
			cardList.add(CardSet.getCard((byte) 48));
			break;
		case 12:
			cardList.add(CardSet.getCard((byte) 1));
			cardList.add(CardSet.getCard((byte) 2));
			cardList.add(CardSet.getCard((byte) 3));
			cardList.add(CardSet.getCard((byte) 9));
			cardList.add(CardSet.getCard((byte) 13));
			cardList.add(CardSet.getCard((byte) 14));
			cardList.add(CardSet.getCard((byte) 15));
			cardList.add(CardSet.getCard((byte) 16));
			cardList.add(CardSet.getCard((byte) 27));
			cardList.add(CardSet.getCard((byte) 36));
			cardList.add(CardSet.getCard((byte) 45));
			cardList.add(CardSet.getCard((byte) 46));
			cardList.add(CardSet.getCard((byte) 47));
			break;
		case 13:
			cardList.add(CardSet.getCard((byte) 5));
			cardList.add(CardSet.getCard((byte) 6));
			cardList.add(CardSet.getCard((byte) 8));
			cardList.add(CardSet.getCard((byte) 9));
			cardList.add(CardSet.getCard((byte) 13));
			cardList.add(CardSet.getCard((byte) 20));
			cardList.add(CardSet.getCard((byte) 21));
			cardList.add(CardSet.getCard((byte) 24));
			cardList.add(CardSet.getCard((byte) 28));
			cardList.add(CardSet.getCard((byte) 31));
			cardList.add(CardSet.getCard((byte) 32));
			cardList.add(CardSet.getCard((byte) 34));
			cardList.add(CardSet.getCard((byte) 44));
			break;
		case 14:
			cardList.add(CardSet.getCard((byte) 2));
			cardList.add(CardSet.getCard((byte) 12));
			cardList.add(CardSet.getCard((byte) 13));
			cardList.add(CardSet.getCard((byte) 14));
			cardList.add(CardSet.getCard((byte) 37));
			cardList.add(CardSet.getCard((byte) 39));
			cardList.add(CardSet.getCard((byte) 41));
			cardList.add(CardSet.getCard((byte) 43));
			cardList.add(CardSet.getCard((byte) 45));
			cardList.add(CardSet.getCard((byte) 46));
			cardList.add(CardSet.getCard((byte) 47));
			cardList.add(CardSet.getCard((byte) 48));
			cardList.add(CardSet.getCard((byte) 51));
			break;
		case 15:
			cardList.add(CardSet.getCard((byte) 1));
			cardList.add(CardSet.getCard((byte) 6));
			cardList.add(CardSet.getCard((byte) 9));
			cardList.add(CardSet.getCard((byte) 10));
			cardList.add(CardSet.getCard((byte) 14));
			cardList.add(CardSet.getCard((byte) 15));
			cardList.add(CardSet.getCard((byte) 19));
			cardList.add(CardSet.getCard((byte) 23));
			cardList.add(CardSet.getCard((byte) 24));
			cardList.add(CardSet.getCard((byte) 25));
			cardList.add(CardSet.getCard((byte) 31));
			cardList.add(CardSet.getCard((byte) 48));
			cardList.add(CardSet.getCard((byte) 50));
			break;
		case 16:
			cardList.add(CardSet.getCard((byte) 8));
			cardList.add(CardSet.getCard((byte) 12));
			cardList.add(CardSet.getCard((byte) 14));
			cardList.add(CardSet.getCard((byte) 18));
			cardList.add(CardSet.getCard((byte) 22));
			cardList.add(CardSet.getCard((byte) 23));
			cardList.add(CardSet.getCard((byte) 26));
			cardList.add(CardSet.getCard((byte) 27));
			cardList.add(CardSet.getCard((byte) 28));
			cardList.add(CardSet.getCard((byte) 31));
			cardList.add(CardSet.getCard((byte) 32));
			cardList.add(CardSet.getCard((byte) 42));
			cardList.add(CardSet.getCard((byte) 48));
			break;
		}

		return cardList;
	}
}
