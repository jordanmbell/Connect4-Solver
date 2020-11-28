package Tier;

import Games.Connect4;
import Helpers.Piece;
import Helpers.Primitive;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TierRunner {

    public static void main(String[] args) {
        int w = 4;
        int h = 4;
        int win = 4;
        long start = System.currentTimeMillis();
        Connect4 game = new Connect4(w,h,win);
        SharedVars sharedVars = new SharedVars();
        int n = Runtime.getRuntime().availableProcessors();
        //n = 2;
        System.out.println(n + " Threads");
        // First memoize all positions in each tier
        int numTiers = w*h;
        SolverTier parallel = new SolverTier(w, h, win, sharedVars);
        List<List<Piece[]>> tiers = parallel.exploreTiers();
        parallel.bottumUp(tiers);

    }

    public static class SharedVars {
        public volatile HashSet<Long> solving = new HashSet<>();
        public volatile long maxLocationWritten = -1;
    }

}
