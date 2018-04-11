/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package opencvfouriertransform;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;

/**
 *
 * @author asmin
 */
public class FXMLDocumentController implements Initializable {

    private Label label;
    @FXML
    private ImageView originalImage;
    @FXML
    private Insets x2;
    @FXML
    private Insets x1;
    @FXML
    private Button transformButton;
    @FXML
    private Button antiTransformButton;
    @FXML
    private ImageView transformedImage;
    @FXML
    private ImageView antiTransformedImage;

    private FileChooser fileChooser = new FileChooser();
    private Mat image = new Mat();
    private Mat complexImage = new Mat();
    private List<Mat> planes = new ArrayList<>();
    private Stage stage;

    private void handleButtonAction(ActionEvent event) {
        System.out.println("You clicked me!");
        label.setText("Hello World!");
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // TODO
    }

    @FXML
    private void loadImage(ActionEvent event) {
        File file = new File("res/");

        this.fileChooser.setInitialDirectory(file);
        file = this.fileChooser.showOpenDialog(this.stage);

        this.image = Imgcodecs.imread(file.getAbsolutePath(), Imgcodecs.CV_LOAD_IMAGE_GRAYSCALE);
        this.originalImage.setImage(this.mat2Image(this.image));

        this.transformButton.setDisable(false);

        if (!this.planes.isEmpty()) {
            this.planes.clear();
            this.transformedImage.setImage(null);
            this.antiTransformedImage.setImage(null);
        }
    }

    @FXML
    private void transformImage(ActionEvent event) {
        int addPixelRows = Core.getOptimalDFTSize(image.rows());
        int addPixelCols = Core.getOptimalDFTSize(image.cols());

        Mat padded = new Mat();
        Core.copyMakeBorder(image, padded, 0, addPixelRows - image.rows(), 0,
                addPixelCols - image.cols(), Core.BORDER_CONSTANT, Scalar.all(0));
        padded.convertTo(padded, CvType.CV_32F);
        this.planes.add(padded);
        this.planes.add(Mat.zeros(padded.size(), CvType.CV_32F));
        Core.merge(this.planes, this.complexImage);

        Core.dft(this.complexImage, this.complexImage);

        List<Mat> newPlanes = new ArrayList<>();
        Mat mag = new Mat();
        Core.split(complexImage, newPlanes);
        Core.magnitude(newPlanes.get(0), newPlanes.get(1), mag);
        Core.add(Mat.ones(mag.size(), CvType.CV_32F), mag, mag);
        Core.log(mag, mag);

        image = image.submat(new Rect(0, 0, image.cols() & -2, image.rows() & -2));
        int cx = image.cols() / 2;
        int cy = image.rows() / 2;

        Mat q0 = new Mat(image, new Rect(0, 0, cx, cy));
        Mat q1 = new Mat(image, new Rect(cx, 0, cx, cy));
        Mat q2 = new Mat(image, new Rect(0, cy, cx, cy));
        Mat q3 = new Mat(image, new Rect(cx, cy, cx, cy));

        Mat tmp = new Mat();
        q0.copyTo(tmp);
        q3.copyTo(q0);
        tmp.copyTo(q3);

        q1.copyTo(tmp);
        q2.copyTo(q1);
        tmp.copyTo(q2);
        
        mag.convertTo(mag, CvType.CV_8UC1);
        Core.normalize(mag, mag, 0, 255, Core.NORM_MINMAX, CvType.CV_8UC1);
        this.transformedImage.setFitWidth(250);
        // preserve image ratio
        this.transformedImage.setPreserveRatio(true);
        this.transformedImage.setImage(this.mat2Image(mag));

        

        // enable the button for performing the antitransformation
        this.antiTransformButton.setDisable(false);
        // disable the button for applying the dft
        this.transformButton.setDisable(true);

    }

    @FXML
    private void antiTransformImage(ActionEvent event) {
        Core.idft(this.complexImage, this.complexImage);
        Mat restoredImage = new Mat();
        Core.split(this.complexImage, this.planes);
        Core.normalize(this.planes.get(0), restoredImage, 0, 255, Core.NORM_MINMAX);
        this.antiTransformedImage.setImage(this.mat2Image(restoredImage));

        // disable the button for performing the antitransformation
        this.antiTransformButton.setDisable(true);

    }

    private Image mat2Image(Mat frame) {
        MatOfByte buffer = new MatOfByte();
        Imgcodecs.imencode(".png", frame, buffer);
        Image imageToShow = new Image(new ByteArrayInputStream(buffer.toArray()));
        return imageToShow;
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }
}
