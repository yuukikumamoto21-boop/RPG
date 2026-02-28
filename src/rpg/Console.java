package rpg;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

/**
 * コンソールの処理クラス
 */
public class Console {
	/** コンソールの表示方法切替（true：ゆっくり、false：瞬時に） */
	private static final boolean VIEW_SLOW = true;

	/** デフォルトの表示速度(ミリ秒単位) */
	private static final int TIME = 10;// 値が大きいほど、表示が遅くなる

	private static Scanner scanner;

	/**
	 * コンソールにメッセージを表示するメソッド
	 *
	 * @param msg 表示メッセージ
	 */
	public static void print(String msg) {
		dispMsg(msg, TIME);
	}

	/**
	 * コンソールにメッセージを表示するメソッド
	 *
	 * @param msg  表示メッセージ
	 * @param time 表示速度（ミリ秒単位で設定。値が大きいほど、表示が遅くなる）
	 */
	public static void print(String msg, int time) {
		dispMsg(msg, time);
	}

	/**
	 * コンソールにメッセージを表示するメソッド（改行あり）
	 *
	 * @param msg 表示メッセージ
	 */
	public static void println(String msg) {
		dispMsg(msg, TIME);
		System.out.println();
	}

	/**
	 * コンソールにメッセージを表示するメソッド（改行あり）
	 *
	 * @param msg  表示メッセージ
	 * @param time 表示速度（ミリ秒単位で設定。値が大きいほど、表示が遅くなる）
	 */
	public static void println(String msg, int time) {
		dispMsg(msg, time);
		System.out.println();
	}

	/**
	 * コンソールに複数のメッセージを表示するメソッド
	 *
	 * @param msgList 表示メッセージのリスト
	 */
	public static void println(ArrayList<String> msgList) {
		for (String msg : msgList) {
			println(msg, TIME);
		}
	}

	/**
	 * コンソールに複数のメッセージを表示するメソッド
	 *
	 * @param msgList 表示メッセージのリスト
	 * @param time    表示速度（ミリ秒単位で設定。値が大きいほど、表示が遅くなる）
	 */
	public static void println(ArrayList<String> msgList, int time) {
		for (String msg : msgList) {
			println(msg, time);
		}
	}

	/**
	 * コンソールにメッセージを表示し、Enterキーか何かを入力することで次へ進むメソッド
	 *
	 * @param msg 表示メッセージ
	 */
	public static void printEnter(String msg) {
		dispMsg(msg, TIME);
		input(" -ENTER-");
	}

	/**
	 * コンソールにメッセージを表示し、Enterキーか何かを入力することで次へ進むメソッド
	 *
	 * @param msg  表示メッセージ
	 * @param time 表示速度（ミリ秒単位で設定。値が大きいほど、表示が遅くなる）
	 */
	public static void printEnter(String msg, int time) {
		dispMsg(msg, time);
		input(" -ENTER-");
	}

	/**
	 * ラインを表示するプライベートメソッド
	 *
	 * @param s   ライン文字列
	 * @param len 長さ
	 */
	public static void printLine(String s, int len) {
		for (int i = 1; i <= len; i++) {
			System.out.print(s);
		}
		System.out.println();
	}

	/**
	 * プレイヤーからの入力を受け付けるメソッド
	 *
	 * @param msg 入力前に表示するメッセージ
	 * @return 入力文字列
	 */
	public static String input(String msg) {
		System.out.print(msg);
		scanner = new Scanner(System.in, "shift-jis");
		return scanner.nextLine();
	}

	/**
	 * プレイヤーからの入力を受け付けるメソッド
	 *
	 * @param msg  入力前に表示するメッセージ
	 * @param time 表示速度（ミリ秒単位で設定。値が大きいほど、表示が遅くなる）
	 * @return 入力文字列
	 */
	public static String input(String msg, int time) {
		dispMsg(msg, time);
		scanner = new Scanner(System.in, "shift-jis");
		return scanner.nextLine();
	}

	/**
	 * コマンドプロンプト画面の切り替え
	 */
	public static void clearScreen() {
		try {
			new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
		} catch (IOException a) {
			System.out.println(a);
			System.exit(1);
		} catch (InterruptedException b) {
			System.out.println(b);
			System.exit(1);
		}
	}

	/**
	 * コンソールにメッセージを表示するプライベートメソッド<br>
	 * 指定されたミリ秒数の間、スリープ(一時的に実行を停止)させる。
	 *
	 * @param msg  表示メッセージ
	 * @param time 表示速度
	 */
	private static void dispMsg(String msg, int time) {
		for (int i = 0; i < msg.length(); i++) {
			if (VIEW_SLOW) {
				try {
					Thread.sleep(time);
				} catch (InterruptedException e) {
					// pass
				}
			}
			System.out.print(msg.charAt(i));
		}
	}
}
