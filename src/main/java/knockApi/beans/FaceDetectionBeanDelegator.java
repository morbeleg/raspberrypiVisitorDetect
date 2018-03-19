package knockApi.beans;

import knockApi.FaceDetectionController;
import knockApi.dao.ImageContainerManager;

import javax.annotation.PreDestroy;
import javax.faces.bean.ApplicationScoped;
import javax.faces.bean.ManagedBean;

@ManagedBean
@ApplicationScoped
public class FaceDetectionBeanDelegator {

    private static FaceDetectionController faceDetectionController = FaceDetectionController.getInstance();

    public String startCameraExecution() {
        faceDetectionController.run();
        return String.valueOf(faceDetectionController.getCurruntState());
    }

    public void stopForce() {
        faceDetectionController.stopAcquisition();
    }

    public String getServerStatus() {
        if (faceDetectionController != null)
            return String.valueOf(faceDetectionController.getCurruntState());
        else return "Server not initialized";
    }

    public void clearImageView() {
        ImageContainerManager.getInstance().getCacheAddVisitorsTimeline().clear();
    }


    @PreDestroy
   public void clearDumps()
   {
       TemporaryCache.getInstance().clearAllDump();
   }


}
