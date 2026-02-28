package rpg;

import java.util.ArrayList;

public class Game {

	public static void main(String[] args) {
		Console.print("Hello,RPGツクレール(改行なし)");
		System.out.println();
		Console.print("Hello,RPGツクレール(改行なし・表示速度指定)", 100);
		System.out.println();

		Console.println("Hello,RPGツクレール(改行あり)");
		Console.println("Hello,RPGツクレール(改行あり・表示速度指定)", 200);

		ArrayList<String> msgList = new ArrayList<String>();
		for (int i = 1; i <= 3; i++) {
			msgList.add("メッセージ" + i);
		}
		Console.printEnter("メッセージ表示後Enterキーで進む");
		Console.println(msgList);
		Console.printLine("-", 30);

		Console.clearScreen();

		Console.printEnter("メッセージ表示後Enterキーで進む(表示速度指定)");
		Console.println(msgList, 300);
		Console.printLine("-", 30);

		Console.input("何かを入力してください>");
		Console.input("何かを入力してください(表示速度指定)>", 400);
	}
}
