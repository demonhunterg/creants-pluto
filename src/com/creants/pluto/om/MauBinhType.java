package com.creants.pluto.om;

/**
 * Các kiểu mậu binh (tới trắng) lớn dần theo điểm
 * 
 * @author LamHa
 *
 */
public class MauBinhType {
	public static final int BINH_LUNG = -2;

	public static final int NOT_MAU_BINH = -1;

	// 6 đôi
	public static final int SIX_PAIR = 0;

	// 3 sảnh
	public static final int THREE_STRAIGHT = 1;

	// 3 thùng
	public static final int THREE_FLUSH = 2;

	// 12 con cùng màu (Đồng màu 2)
	public static final int SAME_COLOR_12 = 3;

	// lục phé bổn có 5 đôi và 3 lá bài giống nhau
	public static final int SIX_PAIR_WITH_THREE = 4;

	// sảnh 13 con
	public static final int STRAIGHT_13 = 5;

	// 4 con giống nhau với 3 xám
	public static final int FOUR_OF_THREE = 6;

	// 13 con cùng màu (Đồng màu 1)
	public static final int SAME_COLOR_13 = 7;
}
