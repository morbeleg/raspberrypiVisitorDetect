package knockApi.beans;

import knockApi.FaceDetectionController;

public class TemporaryCache {
    static private TemporaryCache instance = new TemporaryCache();
    private TemporaryCache() {
    }

    public static TemporaryCache getInstance() {
        return instance;
    }


    public void clearAllDump()
    {
        System.out.println("Clear all Dump Requested");
        FaceDetectionController.getInstance().clearTemPreviewDir();
        FaceDetectionController.getInstance().clearDailyDumpAll();
    }

}
