package Tier;

import Games.Connect4;
import Helpers.Piece;
import Helpers.Primitive;

import java.util.*;

public class DownwardThread extends Thread{


    int w;
    int h;
    int win;
    Piece nextP;
    int start;
    int end;
    List<Piece[]> pastPositions;
    HashSet<List<Piece>> nextTier;
    public DownwardThread(int w, int h, int win, Piece nextP , int start, int end, List<Piece[]> pastPositions) {
        this.w = w;
        this.h = h;
        this.win = win;
        this.nextP = nextP;
        this.start = start;
        this.end = end;
        this.pastPositions = pastPositions;
        this.nextTier = new HashSet<>();
    }

    @Override
    public void run() {
        Connect4 game = new Connect4(w,h,win);
        for (int i = start; i < end; i++) {
            Piece[] pastPosition = pastPositions.get(i);
            if (game.isPrimitive(pastPosition, nextP.opposite()).x != Primitive.NOT_PRIMITIVE) {
                continue;
            }
            List<Integer> moves = game.generateMoves(pastPosition);
            for (int move: moves) {
                nextTier.add(Arrays.asList(game.doMove(pastPosition, move, nextP)));
            }
        }
    }
}
