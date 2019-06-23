package edu.um.planet.gui;

import edu.um.planet.Universe;
import edu.um.planet.data.SpaceStateHandler;
import edu.um.planet.physics.PhysicalObject;
import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.Scene;
import javafx.scene.SubScene;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.DrawMode;
import javafx.scene.shape.Sphere;
import javafx.stage.Stage;

public class Debugger3D extends Application {

    private Universe universe;
    private PerspectiveCamera camera = new PerspectiveCamera(true);
    private double scalar = 0.000001;
    private PhysicalObject center;

    public static void main(String[] args) {
        launch(args);

    }

    public Debugger3D() {
        SpaceStateHandler.populateCache();
        universe= new Universe();
        //universe.getBodies().add(new CannonBallLowMass(universe.getCelestialBody(399)));
        universe._TIME_DELTA = 60;
        universe._LOOP_ITERATIONS = (24 * 60 * 7) / 30;
        this.center = universe.getCelestialBody(10);
        System.out.println(universe.getBodies().size());
    }

    public Group createContent() {
        // Build the Scene Graph
        Group root = new Group();
        root.getChildren().add(camera);


        for(PhysicalObject body : this.universe.getBodies()) {
            Sphere sphere = new Sphere(Universe._AU);
            sphere.setMaterial(new PhongMaterial(Color.RED));
            sphere.setDrawMode(DrawMode.FILL);
            sphere.setTranslateX(body.getPosition().getX() - center.getPosition().getX());
            sphere.setTranslateY(body.getPosition().getY() - center.getPosition().getY());
            sphere.setTranslateZ(body.getPosition().getZ() - center.getPosition().getZ());
            root.getChildren().add(sphere);
            System.out.println(sphere);
        }

        // Use a SubScene
        SubScene subScene = new SubScene(root, 1280,720);
        subScene.setCamera(camera);
        camera.setScaleX(camera.getScaleX() * Universe._AU);
        camera.setScaleY(camera.getScaleY() * Universe._AU);
        camera.setScaleZ(camera.getScaleZ() * Universe._AU);
        camera.setFieldOfView(90);

        Group group = new Group();
        group.getChildren().add(subScene);
        return group;
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        primaryStage.setResizable(false);
        Scene scene = new Scene(createContent());
        primaryStage.setScene(scene);
        primaryStage.show();
    }
}
