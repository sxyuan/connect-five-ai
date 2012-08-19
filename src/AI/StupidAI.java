/*
 * StupidAI: Copied over from previous version, with modifications.
 */

package AI;

import Main.MainPanel;
import java.awt.Point;
import java.util.Random;

/**
 *
 * @author samuel
 */
public class StupidAI extends BaseAI {

    protected static final int BUFFER = 1;
    protected static final Random RNG = new Random();
    
    protected int[][] board = new int[19][19];
    protected int lastX, lastY;
    protected Point upperLeft = new Point(0, 0), lowerRight = new Point(0, 0);

    public StupidAI(MainPanel parent) {
        super(parent);
    }

    /* Stretch window to contain given point */
    protected static void stretch(Point ul, Point lr, int x, int y) {
        if (x < ul.x)
            ul.x = x;
        else if (x > lr.x)
            lr.x = x;
        if (y < ul.y)
            ul.y = y;
        else if (y > lr.y)
            lr.y = y;
    }

    /* Update internal state through game state */
    public void update() {
        lastX = parent.lastX;
        lastY = parent.lastY;
        if (MainPanel.inBoard(lastX, lastY) && board[lastX][lastY] == MainPanel.EMPTY) {
            board[lastX][lastY] = MainPanel.BLACK;
            if (upperLeft.x == 0 && lowerRight.x == 0) {
                upperLeft.x = lowerRight.x = parent.lastX;
                upperLeft.y = lowerRight.y = parent.lastY;
            } else
                stretch(upperLeft, lowerRight, parent.lastX, parent.lastY);
        }
    }

    /* Update internal state through own move */
    public void update(int x, int y) {
        board[x][y] = MainPanel.WHITE;
        stretch(upperLeft, lowerRight, x, y);
    }

    /* Produce next move */
    public Point getMove() {
        int x, y;
        do {
            x = upperLeft.x - BUFFER + RNG.nextInt(lowerRight.x - upperLeft.x + 2 * BUFFER);
            y = upperLeft.y - BUFFER + RNG.nextInt(lowerRight.y - upperLeft.y + 2 * BUFFER);
        } while (!MainPanel.inBoard(x, y) || board[x][y] != MainPanel.EMPTY);
        return new Point(x, y);
    }

}
