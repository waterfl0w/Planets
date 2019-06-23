package edu.um.landing;

import edu.um.landing.lander.ControllerMode;
import edu.um.landing.lander.LandingModule;
import edu.um.planet.math.Vector3;
import org.knowm.xchart.*;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;

public class Simulator {

    public static void main(String[] args) {
        new Simulator(true, new LandingModule(false, new Vector3(-12000,  3E5 , 10000),
                new Vector3(120, -30, -120),
                -Math.PI+0.005,
                0.1,
                ControllerMode.CLOSED));
    }

    //---
    private DialChart dialChart =  new DialChartBuilder().title("Speed").height(400).width(400).build();
    private DialChart speedChart = new DialChartBuilder().title("rotation").height(400).width(400).build();
    private XYChart windChart = new XYChartBuilder().title("wind").height(400).width(400).build();
    private XYChart dragChart = new XYChartBuilder().title("drag").height(400).width(400).build();
    private XYChart radarChart = new XYChartBuilder().title("horizontal").height(400).width(400).build();
    private XYChart height = new XYChartBuilder().title("height").height(400).width(400).build();

    private LandingModule landingModule;
    private boolean headless;
    /*
    new LandingModule(
                               5.48433e5
            new Vector3(-1200,  1.5E5 , 1000),
            new Vector3(12, -30, -12),
            -Math.PI+0.005,
            0.1,
    ControllerMode.CLOSED)*/

    //private UI ui = new UI();
    private JFrame wrapper;
    public Simulator(boolean headless, LandingModule landingModule) {
        this.landingModule = landingModule;
        this.headless = headless;
        if(!headless) {
            dialChart.addSeries("speed", 1);
            radarChart.addSeries("horz", new double[] { 1 });
            height.addSeries("height", new double[] {1});
            speedChart.addSeries("rotation", 1);
            windChart.addSeries("wind", new double[] {1});
            dragChart.addSeries("drag", new double[] { 1 });

            speedChart.getStyler().setArcAngle(360);

            wrapper = new SwingWrapper(Arrays.asList(dialChart, radarChart, height, speedChart, windChart, dragChart)).displayChartMatrix();

        }

        System.out.println(String.format("ps=%s, vs=%s, t=%.4f, t'=%.4f", landingModule.getPosition(), landingModule.getVelocity(),
                landingModule.getTheta(), landingModule.getThetaVelocity()));
        update();
    }

    public LandingModule getLandingModule() {
        return this.landingModule;
    }

    public void update() {
        int i = 0;
        double maxSpeed = 0;

        while (!landingModule.isLanded()) {
            landingModule.updateVelocity();
            landingModule.updateController();
            landingModule.updatePosition();
            //System.out.println(landingModule.toString());

            //---
            if(!headless) {
                double[] positionX = landingModule.getDataLogger().getData("positionX");
                double[] positionZ = landingModule.getDataLogger().getData("positionZ");
                double[] positionY = landingModule.getDataLogger().getData("positionY");
                double[] realPositionX = landingModule.getDataLogger().getData("realPositionX");
                double[] realPositionZ = landingModule.getDataLogger().getData("realPositionZ");
                double[] realPositionY = landingModule.getDataLogger().getData("realPositionY");
                double[] windStrength = landingModule.getDataLogger().getData("windStrength");
                double[] dragStrength = landingModule.getDataLogger().getData("realDrag");

                {
                    double[] error = new double[positionX.length];
                    for (int ei = 0; ei < error.length; ei++) {
                        error[ei] = Math.abs(Math.sqrt(positionX[ei] * positionX[ei] + positionZ[ei] * positionZ[ei])
                                - Math.sqrt(realPositionX[ei] * realPositionX[ei] + realPositionZ[ei] * realPositionZ[ei]));
                    }
                    radarChart.updateXYSeries("horz",
                            positionX,
                            positionZ,
                            error);
                    error = null;
                }

                {
                    double[] error = new double[positionX.length];
                    for (int ei = 0; ei < error.length; ei++) {
                        error[ei] = Math.abs(Math.abs(realPositionY[ei]) - Math.abs(positionY[ei]));
                    }
                    height.updateXYSeries("height",
                            positionX,
                            positionY,
                            error);
                    error = null;
                }

                double[] null_error = new double[positionX.length];
                for (int ei = 0; ei < null_error.length; ei++) {
                    null_error[ei] = 0;
                }

                {
                    windChart.updateXYSeries("wind",
                            windStrength,
                            positionY,
                            null_error);

                    dragChart.updateXYSeries("drag",
                            dragStrength,
                            positionY,
                            null_error);

                }

                speedChart.getSeriesMap().get("rotation").setValue(Math.toDegrees(landingModule.getRotation()) / 360);

                maxSpeed = Math.max(landingModule.getRealVelocity().length(), maxSpeed);
                dialChart.getSeriesMap().get("speed").setValue(landingModule.getRealVelocity().length() / maxSpeed);
                wrapper.repaint();
                try {
                    Thread.sleep(33);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }

        }

        System.out.println(landingModule);
        System.out.println("Fuel Usage: " + landingModule.getFuelTracker().getUsage());
    }

    public class UI extends JPanel {

        private JFrame frame = new JFrame();

        private BufferedImage background;

        {
            try {
                background = ImageIO.read(new File("src/main/resources/background.png"));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public UI() {
            this.frame.add(this);
            this.frame.setSize(1280, 720);
            this.frame.setVisible(true);
            this.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        }

        @Override
        public void paintComponent(Graphics g) {
            super.paintComponent(g);

            //g.drawImage(background, 0, 0, null);

            g.setColor(Color.GREEN);
            g.drawRect(0, 0, 1280, 720);

            {
                g.setColor(Color.BLUE);
                g.drawLine(0, 1280, frame.getWidth(), 1280);
            }

            {
                Vector3 screen = toScreenCoordinates(landingModule.getPosition());
                g.setColor(Color.RED);
                g.drawRect((int) (screen.getX()-(landingModule.getHeight()/2D)), (int) (screen.getY()-(landingModule.getHeight()/2D)), (int) landingModule.getHeight(), (int) landingModule.getHeight());


                Vector3 shadow = toScreenCoordinates(landingModule.getRealPositions());
                g.setColor(Color.PINK);
                g.drawRect((int) (shadow.getX()-(landingModule.getHeight()/2D)), (int) (shadow.getY()-(landingModule.getHeight()/2D)), (int) landingModule.getHeight(), (int) landingModule.getHeight());

                {
                    if(landingModule.leftThruster.isBurning()) {
                        Vector3 left = screen.add(new Vector3(-20, 0, 0));
                        g.drawLine((int) screen.getX(), (int) screen.getY(), (int) left.getX(), (int) left.getY());
                    }
                    if(landingModule.rightThruster.isBurning()) {
                        Vector3 left = screen.add(new Vector3(20, 0, 0));
                        g.drawLine((int) screen.getX(), (int) screen.getY(), (int) left.getX(), (int) left.getY());
                    }

                    if(landingModule.downThruster.isBurning()) {
                        Vector3 left = screen.add(new Vector3(0, 20, 0));
                        g.drawLine((int) screen.getX(), (int) screen.getY(), (int) left.getX(), (int) left.getY());
                    }
                }

            }

            {
                g.setColor(Color.CYAN);
                Vector3 screen = toScreenCoordinates(new Vector3(0, 0, 0));
                g.drawOval((int) screen.getX(), (int) screen.getX(), 2, 2);
            }

        }

        public Vector3 toScreenCoordinates(Vector3 vec) {
            return new Vector3((vec.getX() + 640.0), (720 - vec.getY()), vec.getZ());
        }

    }

}
