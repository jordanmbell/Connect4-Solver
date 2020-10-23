package Zobrist;

import java.io.*;
import java.util.*;
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
    long[][] zobristValues;
    Piece[] startingPosition;
    Random rand;
    long seed;
    HashMap<Long, Tuple<Primitive, Integer>> memo;

    /** Pieces stored in column major order, starting from bottom right*/
    public Connect4(int w, int h, int wi) {
        width = w;
        height = h;
        win = wi;
        startingPosition = new Piece[width*height];
        Arrays.fill(startingPosition, Piece.EMPTY);
        seed = System.currentTimeMillis();
        rand = new Random(seed);
        initZobrist();
        memo = new HashMap<>();
    }


    public Piece[] doMove(Piece[] position, int move, Piece p) {
        Piece[] newPosition = new Piece[getSize()];
        System.arraycopy(position, 0, newPosition, 0, position.length);
        newPosition[move] = p;
        return newPosition;
    }


    public List<Integer> generateMoves(Piece[] position) {
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

    public Tuple<Primitive, Integer> isPrimitive(Piece[] position, Piece placed) {
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
                    try {
                        if (position[row + c*height] != placed) {
                            break;
                        } else {
                            in_a_row++;
                        }
                    } catch (IndexOutOfBoundsException e) {
                        System.out.println(row);
                        System.out.println(column);
                        System.out.println(c);
                        throw e;
                    }
//                    if (position[row + c*width] != placed) {
//                        break;
//                    } else {
//                        in_a_row++;
//                    }
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
                for (int f = found - 1 - height; f >= 0 && f + 1 % height != 0; f -= 1 + height) {
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
                for (int f = found - 1 + height; f < width*height && f + 1 % height != 0; f -= 1 - height) {
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


    public long hash(Piece[] position) {
        long hash = 0;
        for (int i = 0; i < position.length; i++) {
            switch (position[i]) {
                case EMPTY:
                    hash ^= zobristValues[i][0];
                    break;
                case RED:
                    hash ^= zobristValues[i][1];
                    break;
                case BLUE:
                    hash ^= zobristValues[i][2];
                    break;
            }
        }
        return Math.min(hash, rev_hash(position));
    }

    public long rev_hash(Piece[] position) {
        long hash = 0;
        for (int i = 0; i < position.length; i++) {
            switch (position[symMove(i)]) {
                case EMPTY:
                    hash ^= zobristValues[i][0];
                    break;
                case RED:
                    hash ^= zobristValues[i][1];
                    break;
                case BLUE:
                    hash ^= zobristValues[i][2];
                    break;
            }
        }
        return hash;
    }

    private void initZobrist() {
        zobristValues = new long[width*height][];
        for (int j = 0; j < height*width; j++) {
            zobristValues[j] = new long[3];
            for (int k = 0; k < 3; k++) {
                zobristValues[j][k] = rand.nextLong();
            }
        }
    }

    private long addHash(long prevHash, Piece p, int position) {
        switch (p) {
            case RED:
                return prevHash ^ zobristValues[position][1] ^ zobristValues[position][0];
            case BLUE:
                return prevHash ^ zobristValues[position][2] ^ zobristValues[position][0];
        }
        throw new IllegalStateException("This should never happen");
    }

    public void solve() {
        solve(getStartingPosition(), hash(getStartingPosition()), hash(getStartingPosition()), Piece.BLUE);
    }

    private Tuple<Primitive, Integer> solve(Piece[] position, long hash, long symHash, Piece next) {
        if (memo.containsKey(Math.min(hash, symHash))) {
            return memo.get(Math.min(hash, symHash));
        }

        Piece placed = next.opposite();
        Tuple<Primitive, Integer> p = isPrimitive(position, placed);
        if (p.x != Primitive.NOT_PRIMITIVE) {
            memo.put(Math.min(hash, symHash), p);
            return p;
        }
        List<Integer> moves = generateMoves(position);
        ArrayList<Tuple<Primitive, Integer>> nextPositionValues = new ArrayList<>(moves.size());
        for (int move : moves) {
            Piece[] newPosition = doMove(position, move, next);
            nextPositionValues.add(solve(newPosition, addHash(hash, next, move), addHash(symHash, next, symMove(move)), placed));
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
            memo.put(Math.min(hash, symHash), temp);
            return temp;
        } else if (tieRemote != -1) {
            Tuple<Primitive, Integer> temp = new Tuple<>(Primitive.TIE, tieRemote + 1);
            memo.put(Math.min(hash, symHash), temp);
            return temp;
        } else {
            Tuple<Primitive, Integer> temp = new Tuple<>(Primitive.LOSS, winRemote + 1);
            memo.put(Math.min(hash, symHash), temp);
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
        System.out.println(memo.get(hash(startingPosition)).x);
        System.out.println(memo.size());
    }

    public void serialize(String filename) {
        try {
            FileOutputStream fos = new FileOutputStream(filename);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeLong(seed);
            oos.writeObject(memo);
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    public void deserialize(String filename) {
        try {
            FileInputStream fis = new FileInputStream(filename);
            ObjectInputStream ois = new ObjectInputStream(fis);
            seed = ois.readLong();
            Object temp = ois.readObject();
            initZobrist();
            memo = (HashMap<Long, Tuple<Primitive, Integer>>) temp;
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

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
        if (!memo.containsKey(hash(getStartingPosition()))) {
            solve();
        }
        Scanner input = new Scanner(System.in);
        Piece[] board = getStartingPosition();

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
            Tuple<Primitive, Integer> should = memo.get(hash(board));
            System.out.println(should.x);
            if (should.x == Primitive.TIE) {
                System.out.println("Zobrist.Game should Tie");
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
            if (nextP == Piece.EMPTY) {
                List<Integer> moves = generateMoves(board);
                Collections.shuffle(moves);
                ArrayList<Tuple<Primitive, Integer>> nextPositionValues = new ArrayList<>(moves.size());
                for (int move : moves) {
                    Piece[] newPosition = doMove(board, move, nextP);
                    nextPositionValues.add(memo.get(hash(newPosition)));
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
        Piece[] board = new Piece[] {Piece.EMPTY, Piece.EMPTY, Piece.EMPTY,
                                     Piece.BLUE, Piece.RED, Piece.BLUE,
                                     Piece.RED, Piece.BLUE, Piece.EMPTY, };
        Tuple<Primitive, Integer> should = isPrimitive(board, Piece.BLUE);
        System.out.println(should.x);
        System.out.println(should.y);
    }
}


