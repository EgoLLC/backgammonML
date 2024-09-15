package art.vilolon.backgammon;

import static art.vilolon.backgammon.MainKt.start;

public class SetUp {
    public static void main(String[] args) {
        System.out.println("Start main pid:${ProcessHandle.current().pid()}");
        start();
    }
}
