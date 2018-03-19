package knockApi.beans;

import knockApi.FaceDetectionController;
import knockApi.dao.ImageContainerManager;
import knockApi.entity.ImageContainer;

import javax.faces.bean.ManagedBean;
import java.io.IOException;

@ManagedBean
public class MenuView {

    public void takeAFhoto() {
        try {

            ImageContainer imageContainer = FaceDetectionController.getInstance().grapFrameAndPersistDir();

            ImageContainerManager imageContainerManager=ImageContainerManager.getInstance();
            ImageContainer imageContainerPersisted;
            try {
                imageContainerPersisted = imageContainerManager.getSingleVisitor(imageContainer.getId());
                TimeLineHandler.getInstance().addVisitorToTimeLine(imageContainerPersisted);
            } catch (Exception e) {
                System.err.println("Error happened while getting testPic");
                throw new RuntimeException("Error happened while getting testPic from DB");
            }
        } catch (IOException e) {
            System.out.println("Error happened test photo is not taken" + e.getMessage());
        }
    }


}
