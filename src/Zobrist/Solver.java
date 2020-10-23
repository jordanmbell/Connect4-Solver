package Zobrist;

public class Solver {


    public static void main (String[] args) {
        Connect4 g = new Connect4(3, 3, 3);
        long start = System.currentTimeMillis();
        g.solve();
        System.out.println(System.currentTimeMillis() - start);
        g.printInfo();
        g.play();
    }


}
