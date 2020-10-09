public class Solver {


    public static void main (String[] args) {

        Connect4 g = new Connect4(3, 10, 3);
        g.deserialize("src/Solves/Connect4_3by10win3");
        g.play();
    }


}
