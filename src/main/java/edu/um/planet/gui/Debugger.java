package edu.um.planet.gui;

import edu.um.planet.Universe;
import edu.um.planet.data.SpaceStateHandler;
import edu.um.planet.physics.PhysicalObject;
import edu.um.planet.math.Vector3;
import org.jdesktop.swingx.JXDatePicker;
//import jdk.management.cmm.internal.SystemResourcePressureImpl;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

public class Debugger {

    public static void main(String[] args) {
        new Debugger(null, true);
    }

    public static void lockSimulation(Runnable runnable) {
        synchronized (Debugger.mutex) {
            Debugger.mutex.set(true);
            runnable.run();
            Debugger.mutex.set(false);
        }
    }

    //---
    private JFrame frame = new JFrame();
    private Universe universe;
    private boolean doSelfUpdates;

    public static final AtomicBoolean mutex = new AtomicBoolean(false);

    public Debugger(Universe universe, boolean doSelfUpdates) {
        this.doSelfUpdates = doSelfUpdates;
        //SpaceStateHandler.populateCache();
        //this.universe = SpaceStateHandler.get("10years_24h_steps.space", 365 * 5);
        // initial state


        if(universe == null) {
            this.universe = new Universe();

            this.universe._TIME_DELTA = 60;
            this.universe._LOOP_ITERATIONS = 60 * 1;
        } else {
            this.universe = universe;
        }


        UIManager.LookAndFeelInfo[] looks = UIManager.getInstalledLookAndFeels();
        for (UIManager.LookAndFeelInfo look : looks) {
            if (look.getClassName().equals("com.sun.java.swing.plaf.windows.WindowsLookAndFeel")) {
                try {
                    UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
                } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {
                    System.out.println("Failed to load preferred styled. Default.");
                }

            }
        }

        start();
    }


    public void start() {

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1920, 1080);
        frame.setLayout(new BorderLayout());

        UniverseFrame universeFrame = new UniverseFrame(this);
        UnitBar unitBar = new UnitBar(universeFrame);
        FileBar fileBar = new FileBar(this);

        frame.add(fileBar, BorderLayout.NORTH);
        frame.add(universeFrame, BorderLayout.CENTER);
        frame.add(unitBar, BorderLayout.SOUTH);

        frame.setVisible(true);


        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    frame.getContentPane().repaint();
                    if(doSelfUpdates) {
                        synchronized (mutex) {
                            if(!mutex.get()) {
                                universe.update();
                            }
                        }
                    }

                    unitBar.update();

                    if(!doSelfUpdates) {
                        try {
                            Thread.sleep(33);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }).start();


    }

    public static class FileBar extends JPanel {

        private Debugger debugger;
        private JButton button = new JButton("Load");
        private JXDatePicker datePicker = new JXDatePicker();


        public FileBar(Debugger debugger) {
            this.debugger = debugger;

            this.setSize(1280, 20);
            this.setFocusable(false);
            this.setLayout(new FlowLayout());

            this.add(button);
            this.add(datePicker);
            this.datePicker.setLightWeightPopupEnabled(true);
            this.button.addActionListener(e -> {
                Instant jumpTo = this.datePicker.getDate().toInstant();
                for(Map.Entry<String, SpaceStateHandler.SpaceFileMeta> entry : SpaceStateHandler.getCache().entrySet()) {
                    if(entry.getKey().endsWith("test.space")) {
                        SpaceStateHandler.SpaceFileMeta meta = entry.getValue();
                        Instant startTime = Instant.ofEpochMilli(meta.getStartTime());
                        //TODO also check end timeInSeconds
                        if (jumpTo.isAfter(startTime)) {
                            int jumps = (int) (jumpTo.minusMillis(startTime.toEpochMilli()).toEpochMilli() / (double) meta.getTimeOffset());
                            this.debugger.universe = SpaceStateHandler.get(entry.getKey(), jumps);
                            this.debugger.universe._TIME_DELTA = 1;
                            this.debugger.universe._LOOP_ITERATIONS = 30;
                            break;
                        }
                    }
                }
            });
        }

    }

    public static class UnitBar extends JPanel {

        private UniverseFrame universeFrame;

        private JLabel centerLabel = new JLabel("Center:");
        private JComboBox<String> centerBodies;
        private JLabel centerInfo = new JLabel();

        private JLabel selectLabel = new JLabel("Select:");
        private JComboBox<String> selectBodies;
        private JLabel selectInfo = new JLabel();

        private JLabel time = new JLabel();
        private JLabel planeLabel = new JLabel();
        private JLabel scaleLabel = new JLabel();

        private TextField scalar = new TextField();


        public UnitBar(UniverseFrame universeFrame) {
            this.setSize(1280, 20);
            this.setFocusable(false);
            this.setLayout(new FlowLayout());
            this.universeFrame = universeFrame;

            String[] bodies = universeFrame.debugger.universe.getBodies().stream().map(PhysicalObject::getName).sorted().toArray(String[]::new);
            this.centerBodies = new JComboBox<>(bodies);
            this.selectBodies = new JComboBox<>(bodies);

            this.centerBodies.addActionListener(e -> universeFrame.center = universeFrame.debugger.universe.getObject(centerBodies.getSelectedItem().toString()).getId());
            this.selectBodies.addActionListener(e -> universeFrame.selected = universeFrame.debugger.universe.getObject(selectBodies.getSelectedItem().toString()).getId());
            this.planeLabel.addMouseListener(new MouseAdapter() {

                @Override
                public void mouseClicked(MouseEvent e) {
                    if(universeFrame.plane == UniverseFrame.Plane.X_Y) {
                        universeFrame.plane = UniverseFrame.Plane.X_Z;
                    } else if(universeFrame.plane == UniverseFrame.Plane.X_Z) {
                        universeFrame.plane = UniverseFrame.Plane.X_Y;
                    }
                }

            });

            this.add(centerLabel);
            this.add(centerBodies);
            this.add(centerInfo);

            this.add(selectLabel);
            this.add(selectBodies);
            this.add(selectInfo);


            this.add(this.time);
            this.add(planeLabel);

            this.add(scalar);
            this.scalar.addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e) {
                    if(e.getKeyCode() == KeyEvent.VK_ENTER) {
                        universeFrame.SCALER = Double.parseDouble(scalar.getText());
                    }
                }
            });
        }

        public void update() {
            PhysicalObject centerObject = this.universeFrame.debugger.universe.getCelestialBody(this.universeFrame.center);
            this.centerInfo.setText(String.format("[%03.2fkm/s,%03.2fkm/s,%03.2fkm/s]", centerObject.getVelocity().getX() * 1E-3, centerObject.getVelocity().getY() * 1E-3, centerObject.getVelocity().getZ() * 1E-3));

            if(this.universeFrame.debugger.universe.has(this.universeFrame.selected)) {
                PhysicalObject selectedObject = this.universeFrame.debugger.universe.getCelestialBody(this.universeFrame.selected);
                Vector3 vel = selectedObject.getVelocity();
                this.selectInfo.setText(String.format("[%03.4fkm/s,%03.4fkm/s,%03.4fkm/s] (%03.4fkm/s)", vel.getX() * 1E-3, vel.getY() * 1E-3, vel.getZ() * 1E-3, vel.length() * 1E-3));
            } else {
                this.selectInfo.setText("-");
            }

            Universe universe = universeFrame.debugger.universe;
            this.time.setText(String.format("Time: %s (dT: %dsec)", universe.getCurrentTime().toString(), universeFrame.debugger.universe._TIME_DELTA * universeFrame.debugger.universe._LOOP_ITERATIONS));
            this.planeLabel.setText(String.format("Plane: %s", universeFrame.plane.name()));
            this.scaleLabel.setText(String.format("Scale: %.4fAU/px", ( 40 / universeFrame._SCREEN_AU_SCALE)));
        }

    }

    public static class UniverseFrame extends JPanel {

        private Debugger debugger;

        private double _SCREEN_AU_SCALE = 40;
        private double SCALER = 10;
        private int center;
        private int selected;

        private Plane plane = Plane.X_Y;

        public final static Image BACKGROUND_IMAGE = Toolkit.getDefaultToolkit().createImage("res/background.jpg");


        public UniverseFrame(Debugger debugger) {
            this.setSize(1280, 1000);
            this.debugger = debugger;
            this.center = 10;
            this.setDoubleBuffered(true);

            this.setFocusable(true);

            this.addMouseWheelListener(new MouseAdapter() {
                @Override
                public void mouseWheelMoved(MouseWheelEvent e) {
                    double delta =e.getPreciseWheelRotation() / 1;
                    _SCREEN_AU_SCALE = Math.max(_SCREEN_AU_SCALE - delta, 1);
                }
            });

            this.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    double x = (e.getX() - UniverseFrame.this.getSize().getWidth() / 2) * Universe._AU / (_SCREEN_AU_SCALE * SCALER);
                    double y = -((e.getY() - UniverseFrame.this.getSize().getHeight() / 2)) * Universe._AU / (_SCREEN_AU_SCALE * SCALER);

                    Optional<PhysicalObject> body= debugger.universe.getBodies().stream()
                            .min((o1, o2) -> {

                                if(o1.getName().equals("Sun")) {
                                    return 1;
                                } else if(o2.getName().equals("Sun")) {
                                    return -1;
                                }

                                Vector3 pos1 = o1.getPosition().clone();
                                Vector3 pos2 = o2.getPosition().clone();

                                if(plane == Plane.X_Y) {
                                    double r1 = pos1.subtract(new Vector3(x, y, pos1.getZ())).length() / o1.getRadius();
                                    double r2 = pos2.subtract(new Vector3(x, y, pos2.getZ())).length() / o2.getRadius();
                                    return Double.compare(r1, r2);
                                } else if(plane == Plane.X_Z) {
                                    //z=y here... a bit confusing I know
                                    double r1 = pos1.subtract(new Vector3(x, pos1.getY(), y)).length() / o1.getRadius();
                                    double r2 = pos2.subtract(new Vector3(x, pos2.getY(), y)).length() / o2.getRadius();
                                    return Double.compare(r1, r2);
                                } else {
                                    throw new IllegalStateException();
                                }

                            });

                    body.ifPresent(celestialBody -> {
                        if(e.getButton() == MouseEvent.BUTTON1) {
                            selected = celestialBody.getId();
                        } else if(e.getButton() == MouseEvent.BUTTON3) {
                            center = celestialBody.getId();
                        }
                    });

                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            g.drawImage(BACKGROUND_IMAGE, 0, 0, null);


            for (PhysicalObject body : debugger.universe.getBodies()) {

                if(body.getId() == this.selected) {
                    g.setColor(Color.RED);
                } else {
                    g.setColor(body.getColor());
                }

                {
                    Vector3 screenCoordinates = toScreenCoordinates(body.getPosition());
                    int x = (int) screenCoordinates.getX();
                    int y = (int) (plane == Plane.X_Y ? screenCoordinates.getY() : screenCoordinates.getZ());

                    g.drawString(body.getName(), x, y);
                    int r = Math.max(2, (int) ((body.getRadius() / Universe._AU) * _SCREEN_AU_SCALE * SCALER));
                    g.drawOval(x - r / 2, y - r / 2, r, r);
                }

            }

        }

        public Vector3 toScreenCoordinates(Vector3 vector) {
            PhysicalObject centerObject = debugger.universe.getCelestialBody(this.center);
            double x = ((((vector.getX() - centerObject.getPosition().getX()) / Universe._AU) * _SCREEN_AU_SCALE * SCALER) + this.getSize().getWidth() / 2);
            double y = ((-((vector.getY() - centerObject.getPosition().getY()) / Universe._AU) * _SCREEN_AU_SCALE * SCALER) + this.getSize().getHeight() / 2);
            double z = ((((vector.getZ() - centerObject.getPosition().getZ()) / Universe._AU) * _SCREEN_AU_SCALE * SCALER) + this.getSize().getHeight() / 2);
            return new Vector3(x, y, z);
        }

        public enum Plane {
            X_Y,
            X_Z
        }
    }
}