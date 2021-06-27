import java.util.*;
import java.util.stream.Collectors;

public class Puzzle {
    // https://www.dragonfjord.com/product/a-puzzle-a-day/
    /* private static String BOARD_STRING = """
XXXXXXXXX
X      XX
XX     XX
X       X
X    X  X
X       X
X       X
X   XXXXX
XXXXXXXXX
    """;
    private static String BLOCK_STRING = """
 OOO OOO OOOO   O O   OOOO OO   OO
 O O OOO   O  OOO O   O     OOO OOO
              O   OOO
    """; */
    // https://en.wikipedia.org/wiki/Pentomino 6x10 -> 9356 solutions
    private static String BOARD_STRING = """
XXXXXXXXXXXX
X          X
X          X
X          X
X          X
X          X
X          X
XXXXXXXXXXXX
    """;
    private static String BLOCK_STRING = """
 OOO OOOO   O O   OOOO OO   OO  OOOOO OO   O   O  OOO
 O O   O  OOO O   O     OOO OOO        OO OOO OOO  O
          O   OOO                       O  O  O    O
    """;

    private static char[][] toCharMatrix(String boardString) {
        return Arrays.stream(boardString.split("\n")).map(String::toCharArray).toArray(char[][]::new);
    }

    private record Point(int r, int c) { };

    private static List<List<Point>> cutBlocks(String blockString) {
        List<List<Point>> ans = new ArrayList<>();
        char[][] b = toCharMatrix(blockString);
        for (int i = 0; i < b.length; i++) {
            for (int j = 0; j < b[i].length; j++) {
                if (b[i][j] != ' ') {
                    List<Point> points = new ArrayList<>();
                    cutBlock(b, i, j, points);
                    ans.add(points);
                }
            }
        }
        return ans;
    }
    private static void cutBlock(char[][] b, int i, int j, List<Point> points) {
        if (i < 0 || i >= b.length || j < 0 || j >= b[i].length) return;
        if (b[i][j] == ' ') return;
        points.add(new Point(i, j));
        b[i][j] = ' ';
        cutBlock(b,i - 1, j, points);
        cutBlock(b,i + 1, j, points);
        cutBlock(b, i, j - 1, points);
        cutBlock(b, i, j + 1, points);
    }
    private static List<Point> normalize(List<Point> points) {
        points = new ArrayList<>(points);
        points.sort((p, q) ->     // totally ordered to prevent dup
                (p.r != q.r) ? Integer.compare(p.r, q.r) : Integer.compare(p.c, q.c));
        int r0 = points.get(0).r; // upper
        int c0 = points.get(0).c; // left
        return points.stream()
                .map(p -> new Point(p.r - r0, p.c - c0))
                .sorted(Comparator.comparing(p -> p.r * p.r + p.c * p.c)) // so we only have to check the "wall"
                .collect(Collectors.toList());
    }
    private static List<Point> rotate(List<Point> points) {
        return points.stream().map(p -> new Point(p.c, -p.r)).collect(Collectors.toList());
    }
    private static List<Point> flip(List<Point> points) {
        return points.stream().map(p -> new Point(p.r, -p.c)).collect(Collectors.toList());
    }
    private static Point[][][] getAllBlocks(String blockString) {
        List<List<Point>> origBlocks = cutBlocks(blockString);
        Point[][][] blocks = new Point[origBlocks.size()][][];
        for (int i = 0; i < origBlocks.size(); i++) {
            Set<List<Point>> variants = new LinkedHashSet<>();
            List<Point> block = origBlocks.get(i);
            for (int j = 0; j < 4 * 2; j++) {
                variants.add(normalize(block));
                block = rotate(block);
                if (j == 3) block = flip(block);
            }
            blocks[i] = variants.stream().map(lp -> lp.toArray(Point[]::new)).toArray(Point[][]::new);
        }
        return blocks;
    }
    private static void showBlock(Point[] points) {
        char[][] background = new char[9][9]; // TODO
        for (char[] row : background)
            Arrays.fill(row,'.');
        int i = 0;
        for (Point p : points)
            background[p.r + 4][p.c + 4] = (char) ('0' + (i++));
        for (char[] row : background)
            System.out.println(new String(row));
    }

    private static Point[][][] BLOCKS; // shapes / variants / points
    private static int ALL_USED;

    public static void main(String[] args) {
        BLOCKS = getAllBlocks(BLOCK_STRING);
        ALL_USED = (1 << BLOCKS.length) - 1;
        for (int shape = 0; shape < BLOCKS.length; shape++) {
            for (int variant = 0; variant < BLOCKS[shape].length; variant++) {
                System.out.println("shape: " + shape + " variant: " + variant);
                showBlock(BLOCKS[shape][variant]);
            }
        }

        char[][] board = toCharMatrix(BOARD_STRING);
        showBoard(board);

        find(board, 0, 0);
    }
    private static boolean fitSingle(char[][] board, int r, int c, Point[] block) {
        for (Point p : block)
            if (board[r + p.r][c + p.c] != ' ') return false; // will hit the "wall" first
        return true;
    }
    private static void fillSingle(char[][] board, int r, int c, Point[] block, char ch) {
        for (Point p : block)
            board[r + p.r][c + p.c] = ch;
    }
    private static int foundCount = 0;
    private static void find(char[][] board, int usedSet, int depth) { // depth for debugging
        if (usedSet == ALL_USED) {
            foundCount++;
            System.out.println("found: " + foundCount);
            showBoard(board);
            return;
        }

        for (int r = 0; r < board.length; r++) {
            for (int c = 0; c < board[r].length; c++) {
                if (board[r][c] != ' ') continue;

                for (int shape = 0; shape < BLOCKS.length; shape++) {
                    int mask = 1 << shape;
                    if ((usedSet & mask) != 0) continue;

                    for (Point[] vBlock : BLOCKS[shape]) {
                        if (fitSingle(board, r, c, vBlock)) {
                            fillSingle(board, r, c, vBlock, (char) ('0' + shape)); // ('0' + shape) or ('0' + depth)
                            usedSet |= mask;
                            find(board, usedSet, depth + 1);
                            usedSet &= ~mask;
                            fillSingle(board, r, c, vBlock, ' ');
                        }
                    }
                }
                return; // try to fill the first empty position only
            } // c
        } // r
    }

    private static int[] COLORS = { 41, 42, 43, 44, 45, 46, 101, 102, 103, 104, 105, 106 };
    private static void showBoard(char[][] board) {
        //for (char[] cs : board) System.out.println(new String(cs));
        StringBuilder sb = new StringBuilder();
        for (char[] row : board) {
            for (char c : row) {
                int color = (c - '0');
                if (0 <= color && color < COLORS.length) {
                    sb.append("\033[" + COLORS[color] + "m").append(' ').append("\033[0m");
                } else {
                    sb.append(c);
                }
            }
            sb.append('\n');
        }
        System.out.println(sb.toString());
    }
}

