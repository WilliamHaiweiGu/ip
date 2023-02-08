package meggy.gui;

import java.io.IOException;
import java.util.Collections;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

/**
 * A custom control using FXML that represents a dialog box consisting of an ImageView to represent the speaker's face
 * and a label containing text from the speaker.
 */
public class DialogBox extends HBox {
    /** The label that contains query or response message. */
    @FXML
    private Label dialog;
    /** The Profile picture. */
    @FXML
    private ImageView profilePic;

    /**
     * Constructor that uses FXML.
     *
     * @throws RuntimeException If an FXML file {@link IOException} occurs.
     */
    private DialogBox(String text, Image img) {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(MainWindow.class.getResource("/view/DialogBox.fxml"));
            fxmlLoader.setController(this);
            fxmlLoader.setRoot(this);
            fxmlLoader.load();
        } catch (IOException e) {
            // If FXML file IO causes an error, program must crash.
            throw new RuntimeException(e);
        }
        dialog.setText(text);
        profilePic.setImage(img);
        final double profPicRad = Math.max(profilePic.getFitHeight(), profilePic.getFitWidth()) / 2;
        profilePic.setClip(new Circle(profPicRad, profPicRad, profPicRad));
    }

    /**
     * Creates the user's dialog box.
     *
     * @param text Non-null. User's raw input.
     * @return User's dialog box with avatar on the right
     */
    public static DialogBox ofUser(String text) {
        return new DialogBox(text, GuiUtil.USER_PROF_PIC);
    }

    /**
     * Creates the chatbot's dialog box.
     *
     * @param text Non-null. Meggy's response.
     * @return Meggy's dialog box with avatar on the right.
     */
    public static DialogBox ofMeggy(String text) {
        var db = new DialogBox(text, GuiUtil.MEGGY_PROF_PIC);
        db.flipToLeft();
        db.setBackground(new Background(
                new BackgroundFill(Color.ORANGE, new CornerRadii(GuiUtil.DIALOG_BOX_CORNER_RADIUS), null)
        ));
        return db;
    }

    /** Flips the dialog box such that the ImageView is on the left and text on the right. */
    private void flipToLeft() {
        ObservableList<Node> tmp = FXCollections.observableArrayList(this.getChildren());
        Collections.reverse(tmp);
        getChildren().setAll(tmp);
        setAlignment(Pos.TOP_LEFT);
    }
}

