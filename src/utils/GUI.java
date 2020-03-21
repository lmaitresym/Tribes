package utils;

import core.actions.tribeactions.EndTurn;
import core.actions.unitactions.*;
import core.actors.units.Unit;
import core.game.Game;
import core.game.GameState;
import core.actions.Action;
import players.ActionController;
import players.KeyController;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static core.Constants.*;


public class GUI extends JFrame implements Runnable {
    private JLabel appTurn;
    private JLabel activeTribe;

    private GameState gs;
    private KeyController ki;
    private ActionController ac;

    private GameView view;
    private TribeView tribeView;
    private TechView techView;
    private InfoView infoView;

    private boolean finishedUpdate = true;

    // Zoomed screen dragging vars
    private Point2D startDrag, endDrag, panTranslate;

    /**
     * Constructor
     * @param title Title of the window.
     */
    public GUI(Game game, String title, KeyController ki, ActionController ac, boolean closeAppOnClosingWindow) {
        super(title);
        this.ki = ki;
        this.ac = ac;

        infoView = new InfoView();
        panTranslate = new Point2D.Double(0,0);
//        tribeView = new TribeView();
        view = new GameView(game.getBoard(), infoView, panTranslate);

        // Create frame layout
        GridBagLayout gbl = new GridBagLayout();
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTH;
        gbc.weighty = 0;

        setLayout(gbl);

        // Main panel definition
        JPanel mainPanel = createGamePanel();
        JPanel sidePanel = createSidePanel();

        gbc.gridx = 0;
        getContentPane().add(Box.createRigidArea(new Dimension(10, 0)), gbc);

        gbc.gridx++;
        getContentPane().add(mainPanel, gbc);

        gbc.gridx++;
        getContentPane().add(Box.createRigidArea(new Dimension(10, 0)), gbc);

        gbc.gridx++;
        getContentPane().add(sidePanel, gbc);

        gbc.gridx++;
        getContentPane().add(Box.createRigidArea(new Dimension(10, 0)), gbc);

        // Frame properties
        pack();
        this.setVisible(true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        if(closeAppOnClosingWindow){
            setDefaultCloseOperation(EXIT_ON_CLOSE);
        }
        repaint();
    }


    private JPanel createGamePanel()
    {
        JPanel mainPanel = new JPanel();

        mainPanel.addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
                //Only provide information if clicking on a visible tile
                Point2D translate = view.getPanTranslate();
                Point2D ep = new Point2D.Double(e.getX() - translate.getX(), e.getY() - translate.getY());
                Point2D p = GameView.rotatePointReverse((int)ep.getX(), (int)ep.getY());

                // If unit highlighted and action at new click valid for unit, execute action
                Action candidate = getActionAt((int)p.getX(), (int)p.getY(), infoView.getHighlightX(), infoView.getHighlightY());
                if (candidate != null) {
                    ac.addAction(candidate, gs);
                    infoView.resetHighlight();
                } else {
                    // Otherwise highlight new cell
                    infoView.setHighlight((int)p.getX(), (int)p.getY());
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                startDrag = new Point2D.Double(e.getX(), e.getY());
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                endDrag = new Point2D.Double(e.getX(), e.getY());
                if (startDrag != null && !(startDrag.equals(endDrag))) {
                    panTranslate = new Point2D.Double(+ endDrag.getX() - startDrag.getX(), + endDrag.getY() - startDrag.getY());
                    if (panTranslate.distance(0, 0) >= GUI_MIN_PAN) {
                        view.updatePan(panTranslate);
                        infoView.resetHighlight();
                    }
                }
            }

            @Override
            public void mouseEntered(MouseEvent e) {

            }

            @Override
            public void mouseExited(MouseEvent e) {

            }
        });
        mainPanel.addMouseWheelListener(e -> {
            Point2D mouseLocation = MouseInfo.getPointerInfo().getLocation();
            Point2D panelLocation = mainPanel.getLocationOnScreen();
            Point2D viewCenter = new Point2D.Double(view.getPreferredSize().width/2.0, view.getPreferredSize().height/2.0);
            Point2D diff = new Point2D.Double((mouseLocation.getX() - panelLocation.getX() - viewCenter.getX())/GUI_ZOOM_FACTOR,
                    (mouseLocation.getY() - panelLocation.getY() - viewCenter.getY())/GUI_ZOOM_FACTOR);
            if (e.getWheelRotation() < 0) {
                // Zooming in
                CELL_SIZE += GUI_ZOOM_FACTOR;
                diff = new Point2D.Double(diff.getX()*-1, diff.getY()*-1);
            } else {
                // Zooming out
                CELL_SIZE -= GUI_ZOOM_FACTOR;
            }
            view.updatePan(diff);
        });

        mainPanel.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.SOUTH;
        c.weighty = 0;

        c.gridy = 0;
        mainPanel.add(Box.createRigidArea(new Dimension(0, 5)), c);

        c.gridy++;
        mainPanel.add(view, c);

        c.gridy++;
        mainPanel.add(Box.createRigidArea(new Dimension(0, 5)), c);

        return mainPanel;
    }

    private JPanel createSidePanel()
    {
        JPanel sidePanel = new JPanel();
        sidePanel.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.SOUTH;
        c.weighty = 0;

        JLabel appTitle = new JLabel("Tribes");
        Font textFont = new Font(appTitle.getFont().getName(), Font.PLAIN, 16);
        appTitle.setFont(textFont);

        appTurn = new JLabel("Turn: 0");
        appTurn.setFont(textFont);
        activeTribe = new JLabel("Tribe acting: ");
        activeTribe.setFont(textFont);

        JTabbedPane tribeResearchInfo = new JTabbedPane();
        tribeView = new TribeView();
        techView = new TechView(ac);
        tribeResearchInfo.setPreferredSize(new Dimension(400, 300));
        tribeResearchInfo.add("Tribe Info", tribeView);
        tribeResearchInfo.add("Tech Tree", new JScrollPane(techView));

        c.gridy = 0;
        sidePanel.add(appTitle, c);

        c.gridy++;
        sidePanel.add(Box.createRigidArea(new Dimension(0, 5)), c);

        c.gridy++;
        sidePanel.add(appTurn, c);

        c.gridy++;
        sidePanel.add(Box.createRigidArea(new Dimension(0, 5)), c);

        c.gridy++;
        sidePanel.add(activeTribe, c);

        c.gridy++;
        sidePanel.add(Box.createRigidArea(new Dimension(0, 5)), c);

        c.gridy++;
        sidePanel.add(infoView, c);

        c.gridy++;
        sidePanel.add(Box.createRigidArea(new Dimension(0, 5)), c);

        c.gridy++;
        sidePanel.add(tribeResearchInfo, c);

        c.gridy++;
        sidePanel.add(Box.createRigidArea(new Dimension(0, 5)), c);

        c.gridy++;
        JButton endTurn = new JButton("End Turn");
        endTurn.addActionListener(e -> ac.addAction(new EndTurn(), gs));
        sidePanel.add(endTurn, c);

        c.gridy++;
        sidePanel.add(Box.createRigidArea(new Dimension(0, 5)), c);

        return sidePanel;
    }


    /**
     * Paints the GUI, to be called at every game tick.
     */
    public void update(GameState gs) {
        if (this.gs == null || this.gs.getActiveTribeID() != gs.getActiveTribeID()) {
            infoView.resetHighlight();  // Reset highlights on turn change
            view.setPanToTribe(gs);  // Pan camera to tribe capital on turn change
            ac.reset();  // Clear action queue on turn change
        }
        this.gs = gs;
    }

    public boolean nextMove() {
        return finishedUpdate;
    }

    /**
     * Retrieves action at specific location given by (actionX, actionY) coordinates, to be performed by
     * unit at coordinates (unitX, unitY).
     */
    private Action getActionAt(int actionX, int actionY, int unitX, int unitY) {
        HashMap<Integer, ArrayList<Action>> possibleActions = gs.getUnitActions();
        for (Map.Entry<Integer, ArrayList<Action>> e: possibleActions.entrySet()) {
            Unit u = (Unit) gs.getActor(e.getKey());

            // Only draw actions for highlighted unit
            if (u.getPosition().x == unitY && u.getPosition().y == unitX) {
                for (Action a: e.getValue()) {
                    Vector2d pos = getActionPosition(gs, a);
                    if (pos != null && pos.x == actionY && pos.y == actionX) return a;
                }
                return null;
            }
        }
        return null;
    }

    @Override
    public void run() {
        finishedUpdate = false;
        view.paint(gs);
        tribeView.paint(gs);
        techView.paint(gs);
        infoView.paint(gs);
        appTurn.setText("Turn: " + gs.getTick());
        if (gs.getActiveTribe() != null) {
            activeTribe.setText("Tribe acting: " + gs.getActiveTribe().getName());
        }
        try {
            Thread.sleep(1);
//            Thread.sleep(FRAME_DELAY);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        finishedUpdate = true;
    }


    public static Vector2d getActionPosition(GameState gs, Action a) {
        Vector2d pos = null;
        if (a instanceof Move) {
            pos = new Vector2d(((Move) a).getDestination().x, ((Move) a).getDestination().y);
        } else if (a instanceof Attack) {
            Unit target = (Unit) gs.getActor(((Attack) a).getTargetId());
            pos = target.getPosition();
        } else if (a instanceof Capture || a instanceof Convert || a instanceof Disband || a instanceof Recover ||
                a instanceof Upgrade) {
            Unit u = (Unit) gs.getActor(((UnitAction) a).getUnitId());
            pos = u.getPosition();
        }
        return pos;
    }
}
