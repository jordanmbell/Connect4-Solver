package Tight;

import java.io.*;
import java.util.*;
import org.apache.commons.math3.util.CombinatoricsUtils;

public class Connect4 {

    public static class Tuple<X, Y> implements Serializable{
        public final X x;
        public final Y y;
        public Tuple(X x, Y y) {
            this.x = x;
            this.y = y;
        }
    }

    public Piece[] getStartingPosition() {
        return startingPosition;
    }



    public enum Piece {
        EMPTY,
        RED,
        BLUE;

        public Piece opposite() {
            switch(this) {
                case RED: return BLUE;
                case BLUE: return RED;
                default: throw new IllegalStateException("This should never happen: " + this + " has no opposite.");
            }
        }
    }

    int width;
    int height;
    int win;
    private final Piece[] startingPosition;
    int[] offsets;
    private final HashMap<int[], Integer> savedBias;
    HashMap<Integer,Byte> memo = new HashMap<>();
    Random rand = new Random();

    /** Pieces stored in column major order, starting from bottom right*/
    public Connect4(int w, int h, int wi) {
        width = w;
        height = h;
        win = wi;
        startingPosition = new Piece[w*h];
        Arrays.fill(startingPosition, Piece.EMPTY);
        savedBias = new HashMap<>();
        setOffsets();
    }

    private void setOffsets() {
        offsets = new int[width*height +1];
        offsets[0] = 1;
        for (int i = 1; i < offsets.length; i++) {
            if (i % 2 == 0) {
                offsets[i] = offsets[i-1] +  rearrange(i/2, i/2, startingPosition.length);
            } else {
                offsets[i] = offsets[i-1] + rearrange((i/2) + 1, i/2, startingPosition.length);
            }
        }
        for (int i = offsets.length - 1; i > 0; i--) {
            offsets[i] = offsets[i - 1];
        }
        offsets[0] = 0;
    }

    private int rearrange(int x, int o, int s) {
        if (s == 0) {
            return 0;
        }
        int[] temp = new int[]{x,o,s};
        if (savedBias.containsKey(temp)) {
            return savedBias.get(temp);
        }
        long sFact = CombinatoricsUtils.factorial(s);
        long oFact = CombinatoricsUtils.factorial(o);
        long xFact = CombinatoricsUtils.factorial(x);
        long diffFact = CombinatoricsUtils.factorial(s - x - o);
        double ret = (double) sFact / (oFact * xFact * diffFact);
        if (ret % 1 == 0) {
            int temper = (int) ret;
            savedBias.put(temp, temper);
            return temper;
        }
        throw new IllegalStateException("ret should be an int, not " + ret);
    }

    private Tuple<Primitive, Integer> getValue(int location) {
        if (!memo.containsKey(location)) {
            return null;
        }
        byte b = memo.get(location);
        int val = Byte.toUnsignedInt(b);
        int remoteness = (val << 26) >>> 26;
        Primitive p;
        switch((val) >>> 6) {
            case 0:
                p = Primitive.NOT_PRIMITIVE;
                break;
            case 1:
                p = Primitive.LOSS;
                break;
            case 2:
                p = Primitive.WIN;
                break;
            case 3:
                p = Primitive.TIE;
                break;
            default:
                throw new IllegalStateException("two bits should only have those options");
        }
        return new Tuple<>(p, remoteness);
    }

    private byte byteValue(Tuple<Primitive, Integer> p) {
        Integer temp;
        switch (p.x) {
            case NOT_PRIMITIVE:
                temp = 0;
                break;
            case LOSS:
                temp = 1;
                break;
            case WIN:
                temp = 2;
                break;
            case TIE:
                temp = 3;
                break;
            default:
                throw new IllegalStateException("shhouldnt happpen");
        }
        temp = temp << 6;
        temp += p.y;
        return temp.byteValue();
    }

    private int calculateLocation(Piece[] position, int numPieces) {
        int location = offsets[numPieces];
        int numX = (numPieces / 2) + (numPieces % 2);
        int numO = numPieces / 2;
        int numBlanks = position.length - numPieces;
        int s = position.length;
        for (int i = position.length - 1; i >= 0; i--) {
            if (s == numX || s == numO || s == numBlanks) {
                break;
            }
            switch (position[i]) {
                case BLUE:
                    if (numO > 0) {
                        location += rearrange(numX, numO - 1, s - 1);
                    }
                    if (numBlanks > 0) {
                        location += rearrange(numX, numO, s - 1);
                    }
                    numX -= 1;
                    break;
                case RED:
                    if (numBlanks > 0) {
                        location += rearrange(numX, numO, s - 1);
                    }
                    numO -= 1;
                    break;
                case EMPTY:
                    numBlanks -= 1;
                    break;
            }
            s -= 1;
        }
        return location;
    }

    private Piece[] doMove(Piece[] position, int move, Piece p) {
        Piece[] newPosition = new Piece[getSize()];
        System.arraycopy(position, 0, newPosition, 0, position.length);
        newPosition[move] = p;
        return newPosition;
    }


    private List<Integer> generateMoves(Piece[] position) {
        List<Integer> ret = new ArrayList<>();
        for (int i = 0; i < getSize(); i++) {
            if (position[i] == Piece.EMPTY) {
                ret.add(i);
                i = (i + height) / height * height; //Move to next multiple of height
                i -= 1;
            }
        }
        return ret;
    }

    private Tuple<Primitive, Integer> isPrimitive(Piece[] position, Piece placed) {
        boolean full = true;
        for (int column = 0; column < width; column++) {
            int row = height - 1;
            Piece atP = position[row + column * height];
            if (atP == Piece.EMPTY) {
                full = false;
            }
            while(atP == Piece.EMPTY && row > 0) {
                row --;
                atP = position[row + column * height];
            }
            if (atP != placed) {
                continue;
            }
            //Now we now we are at a piece of placed type on top of column
            // Vertical wins
            if (row - win + 1 >= 0) {
                for (int r = row - 1; r >= row - win + 1; r--) {
                    if (position[r + column*height] != placed) {
                        break;
                    }
                    if (r == row - win + 1) {
                        return new Tuple<>(Primitive.LOSS, 0);
                    }
                }
            }

            //Horizontal wins
            if (win <= width) {
                int in_a_row = 1;
                for (int c = column - 1; c >=0; c--) {
                    if (position[row + c*height] != placed) {
                        break;
                    } else {
                        in_a_row++;
                    }
                }
                for (int c = column + 1; c < width; c++) {
                    if (position[row + c*width] != placed) {
                        break;
                    } else {
                        in_a_row++;
                    }
                }
                if (in_a_row >= win) {
                    return new Tuple<>(Primitive.LOSS, 0);
                }
            }

            // Diag Left High
            if (win <= width && win <= height) {
                int in_a_diag = 1;
                int found = row + column*height;
                for (int f = found + 1 + height; f < width*height && f % height != 0; f += 1 + height) {
                    if (position[f] != placed) {
                        break;
                    } else {
                        in_a_diag++;
                    }
                }
                for (int f = found - 1 - height; f >= 0 && (f + 1) % height != 0; f -= 1 + height) {
                    if (position[f] != placed) {
                        break;
                    } else {
                        in_a_diag++;
                    }
                }
                if (in_a_diag >= win) {
                    return new Tuple<>(Primitive.LOSS, 0);
                }
            }

            //Diag Right High
            if (win <= width && win <= height) {
                int in_a_diag = 1;
                int found = row + column*height;
                for (int f = found + 1 - height; f >= 0 && f % height != 0; f += 1 - height) {
                    if (position[f] != placed) {
                        break;
                    } else {
                        in_a_diag++;
                    }
                }
                for (int f = found - 1 + height; f < width*height && (f + 1) % height != 0; f -= 1 - height) {
                    if (position[f] != placed) {
                        break;
                    } else {
                        in_a_diag++;
                    }
                }
                if (in_a_diag >= win) {
                    return new Tuple<>(Primitive.LOSS, 0);
                }
            }
        }
        if (full) {
            return new Tuple<>(Primitive.TIE, 0);
        } else {
            return new Tuple<>(Primitive.NOT_PRIMITIVE, 0);
        }
    }






    public void solve() {
        solve(getStartingPosition(), 0, Piece.BLUE);
    }

    private Tuple<Primitive, Integer> solve(Piece[] position, int numPieces, Piece next) {
        if (Arrays.equals(position, new Piece[]{Piece.RED, Piece.BLUE, Piece.BLUE, Piece.RED})) {
            System.out.println("sdf");
        }
        if (Arrays.equals(position, new Piece[]{Piece.BLUE, Piece.EMPTY, Piece.RED, Piece.BLUE})) {
            System.out.println("sdf");
        }
        int location = calculateLocation(position, numPieces);
        if (location == 37) {
            System.out.println("");
        }
        Tuple<Primitive, Integer> solvedVal = getValue(location);
        if (solvedVal != null) {
            return solvedVal;
        }

        Piece placed = next.opposite();
        Tuple<Primitive, Integer> p = isPrimitive(position, placed);
        if (p.x != Primitive.NOT_PRIMITIVE) {
            memo.put(location, byteValue(p));
            return p;
        }
        List<Integer> moves = generateMoves(position);
        ArrayList<Tuple<Primitive, Integer>> nextPositionValues = new ArrayList<>(moves.size());
        for (int move : moves) {
            Piece[] newPosition = doMove(position, move, next);
            nextPositionValues.add(solve(newPosition, numPieces + 1, placed));
        }
        int lossRemote = Integer.MAX_VALUE;
        int tieRemote = -1;
        int winRemote = -1;
        for (Tuple<Primitive, Integer> val: nextPositionValues) {
            if (val.x == Primitive.LOSS && val.y < lossRemote) {
                lossRemote = val.y;
            } else if (val.x == Primitive.TIE  && val.y > tieRemote) {
                tieRemote = val.y;
            } else if (val.y > winRemote){
                winRemote = val.y;
            }
        }
        if (lossRemote != Integer.MAX_VALUE) {
            Tuple<Primitive, Integer> temp = new Tuple<>(Primitive.WIN, lossRemote + 1);
            memo.put(location, byteValue(temp));
            return temp;
        } else if (tieRemote != -1) {
            Tuple<Primitive, Integer> temp = new Tuple<>(Primitive.TIE, tieRemote + 1);
            memo.put(location, byteValue(temp));
            return temp;
        } else {
            Tuple<Primitive, Integer> temp = new Tuple<>(Primitive.LOSS, winRemote + 1);
            memo.put(location, byteValue(temp));
            return temp;
        }
    }

    private int symMove(int move) {
        return (move % height) + (width - (move / height) - 1) * height;
    }

    public int getSize() {
        return width*height;
    }

    public void printInfo() {
        int loc = calculateLocation(getStartingPosition(), 0);
        System.out.println(getValue(loc).x);
        System.out.println(memo.size());
    }

//    public void serialize(String filename) {
//        try {
//            FileOutputStream fos = new FileOutputStream(filename);
//            ObjectOutputStream oos = new ObjectOutputStream(fos);
//            oos.writeLong(seed);
//            oos.writeObject(memo);
//        } catch (IOException ioe) {
//            ioe.printStackTrace();
//        }
//    }
//
//    public void deserialize(String filename) {
//        try {
//            FileInputStream fis = new FileInputStream(filename);
//            ObjectInputStream ois = new ObjectInputStream(fis);
//            seed = ois.readLong();
//            Object temp = ois.readObject();
//            initZobrist();
//            memo = (HashMap<Long, Tuple<Primitive, Integer>>) temp;
//        } catch (IOException | ClassNotFoundException e) {
//            e.printStackTrace();
//        }
//    }

    public void printBoard(Piece[] board) {
        StringBuilder stb = new StringBuilder();
        for (int r = height - 1; r >= 0; r--) {
            for (int c = width - 1; c >= 0; c--) {
                switch(board[r + c * height]) {
                    case RED:
                        stb.append("|O");
                        break;
                    case BLUE:
                        stb.append("|X");
                        break;
                    case EMPTY:
                        stb.append("| ");
                }
            }
            stb.append("|\n");
        }
        for (int c = width - 1; c >= 0; c--) {
            stb.append(' ');
            stb.append(c + 1);
        }

        System.out.println(stb.toString());
    }


    public void play() {
        int loc = calculateLocation(getStartingPosition(), 0);
        if (!memo.containsKey((loc))) {
            solve();
        }
        Scanner input = new Scanner(System.in);
        Piece[] board = getStartingPosition();
        int numPieces = 0;
        Piece nextP = Piece.BLUE;
        while (true) {
            printBoard(board);
            Tuple<Primitive, Integer> prim = isPrimitive(board, nextP.opposite());
            if (prim.x != Primitive.NOT_PRIMITIVE) {
                if (prim.x == Primitive.TIE) {
                    System.out.println("Tie Zobrist.Game");

                } else {
                    switch(nextP) {
                        case BLUE:
                            System.out.println("O WINS!");
                            break;
                        case RED:
                            System.out.println("X WINS!");
                            break;
                    }
                }
                break;
            }
            loc = calculateLocation(board, numPieces);
            Tuple<Primitive, Integer> should = getValue(loc);
            System.out.println(should.x);
            if (should.x == Primitive.TIE) {
                System.out.println("Game should Tie");
            } else if (should.x == Primitive.WIN) {
                switch(nextP) {
                    case BLUE:
                        System.out.println("X should win");
                        break;
                    case RED:
                        System.out.println("O should win");
                        break;
                }
            } else {
                switch(nextP) {
                    case RED:
                        System.out.println("X should win");
                        break;
                    case BLUE:
                        System.out.println("O should win");
                        break;
                }
            }
            System.out.println("in " + should.y);
            int next;
            numPieces ++;
            if (nextP == Piece.RED) {
                List<Integer> moves = generateMoves(board);
                Collections.shuffle(moves);
                ArrayList<Tuple<Primitive, Integer>> nextPositionValues = new ArrayList<>(moves.size());
                for (int move : moves) {
                    Piece[] newPosition = doMove(board, move, nextP);
                    loc = calculateLocation(newPosition, numPieces);
                    nextPositionValues.add(getValue(loc));

                }
                int lossRemote = Integer.MAX_VALUE;
                int tieRemote = -1;
                int winRemote = -1;
                Primitive curPrim = Primitive.WIN;
                int tempMove = -1;
                for (int i = 0; i < nextPositionValues.size(); i++) {
                    Tuple<Primitive, Integer> val = nextPositionValues.get(i);
                    if (val.x == Primitive.LOSS && val.y <= lossRemote) {
                        if (!(lossRemote == val.y && rand.nextBoolean())) {
                            lossRemote = val.y;
                        }
                        curPrim = Primitive.LOSS;
                        tempMove = moves.get(i);
                    } else if (curPrim != Primitive.LOSS && val.x == Primitive.TIE  && val.y >= tieRemote) {
                        if (!(tieRemote == val.y && rand.nextBoolean())) {
                            tieRemote = val.y;
                        }
                        curPrim = Primitive.TIE;
                        tempMove = moves.get(i);
                    } else if ((curPrim == Primitive.WIN) && val.y >= winRemote){
                        if (!(winRemote == val.y && rand.nextBoolean())) {
                            winRemote = val.y;
                        }
                        curPrim = Primitive.WIN;
                        tempMove = moves.get(i);
                    }
                }
                next = tempMove;
                next = (next / height) + 1;


            } else {
                next = input.nextInt();
                while (next > width || next < 1 || board[height - 1 + (next - 1)* height] != Piece.EMPTY) {
                    System.out.println("Please chose a valid location");
                    next = input.nextInt();
                }
            }

            int r = height - 1;
            while (r > 0 && board[(r - 1) + (next - 1)* height] == Piece.EMPTY) {
                r--;
            }
            board[r + (next - 1)*height] = nextP;
            nextP = nextP.opposite();
            System.out.println("-------------------------------------------------");
        }

    }



    /*
      R   R
      R R B
    B B R B
    B B B R
    */
    public void test() {
        Piece[] board = new Piece[] {Piece.RED, Piece.BLUE, Piece.EMPTY,
                                     Piece.BLUE, Piece.RED, Piece.BLUE,
                                     Piece.EMPTY, Piece.EMPTY, Piece.EMPTY,
                                      };
        Tuple<Primitive, Integer> should = isPrimitive(board, Piece.BLUE);
        System.out.println(should.x);
        System.out.println(should.y);
    }
}


