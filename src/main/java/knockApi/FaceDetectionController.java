package knockApi;

import knockApi.beans.RefreshBackEndAdjuster;
import knockApi.dao.DaoHandler;
import knockApi.dao.ImageContainerManager;
import knockApi.entity.ImageContainer;
import knockApi.utils.Utils;
import org.apache.commons.io.FileUtils;
import org.hibernate.SessionFactory;
import org.opencv.core.*;
import org.opencv.core.Point;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.objdetect.Objdetect;
import org.opencv.videoio.VideoCapture;
import org.primefaces.model.timeline.TimelineEvent;

import javax.faces.application.Application;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.imageio.ImageIO;
import javax.servlet.ServletContext;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public class FaceDetectionController implements Runnable {
    private static final String visitorTestPicture = "visitorTestPicture";
    public static String fileSeperator = System.getProperty("file.separator");
    private static sate curruntState;
    private static FaceDetectionController instance = getInstance();
    private AtomicInteger visitorCounter = new AtomicInteger(0);
    private String oneDriveUploadFolder;
    private Date currentDate = new Date();
    private String dailyGlobalDirectoryPrefix;
    private ServletContext servletContext = (ServletContext) FacesContext
            .getCurrentInstance().getExternalContext().getContext();
    private String initialWebResourcePath = servletContext.getRealPath("/");
    private ScheduledExecutorService timer;
    private VideoCapture capture;
    private AtomicInteger fps = new AtomicInteger(4);
    private CascadeClassifier faceCascade;
    private int absoluteFaceSize;
    private Future<SessionFactory> lazyLoadFactory;
    private String oneDriveUploadPrefix = "oneDriveUpload";

    private FaceDetectionController() {
        lazyLoadFactory = Executors.newSingleThreadExecutor().submit(DaoHandler::getLazyFactory);
        curruntState = sate.notInitialized;
        Calendar cal = Calendar.getInstance();
        cal.setTime(currentDate);
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH);
        int day = cal.get(Calendar.DAY_OF_MONTH);

        oneDriveUploadFolder = oneDriveUploadPrefix + fileSeperator + String.valueOf(year) + String.valueOf(month) + String.valueOf(day);
        dailyGlobalDirectoryPrefix = initialWebResourcePath + fileSeperator + oneDriveUploadFolder;
        try {
            lazyLoadFactory.get();
        } catch (InterruptedException | ExecutionException e) {
            System.err.println(e.getMessage());
        }
    }

    public static FaceDetectionController getInstance() {
        if (instance == null)
            return instance = new FaceDetectionController();
        else
            return instance;
    }

    public String getInitialWebResourcePath() {
        return initialWebResourcePath;
    }

    public String getOneDriveUploadPrefix() {
        return oneDriveUploadPrefix;
    }

    public sate getCurruntState() {
        return curruntState;
    }

    /**
     * Init the controller, at start time
     */

    private void init() {
        try {
            String OS = System.getProperty("os.name").toLowerCase();
            if (OS.contains("win")) {
                System.out.println(OS);
                System.setProperty("java.library.path", "E:\\Compilers\\OpenCvMate\\opencv\\build\\java\\x64");
                Field fieldSysPath = ClassLoader.class.getDeclaredField("sys_paths");
                fieldSysPath.setAccessible(true);
                fieldSysPath.set(null, null);
                System.out.println("SystemPath is set");
            }
            System.loadLibrary(Core.NATIVE_LIBRARY_NAME); //dont worry to call this second time
            System.out.println("Library is loaded");
        } catch (Exception ex) {
            System.err.println("exception Happened while lib load" + ex.getMessage());
        }


        File oneDrive = new File(dailyGlobalDirectoryPrefix);
        if (!oneDrive.exists()) {
            boolean mkdirOne = oneDrive.mkdirs();
            if (mkdirOne) {
                System.out.println("oneDriveFolder Created");
            } else System.out.println("oneDriveFolder Created!! and not exist");
        }
        if (capture == null) {
            this.capture = new VideoCapture();
        }
        this.faceCascade = new CascadeClassifier();
        this.absoluteFaceSize = 0;
        try {
            lazyLoadFactory.get(15, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            System.err.println("interrupted exeception at lazyFactory Load " + e.getMessage());
        } catch (ExecutionException e) {
            System.err.println("Someting happened while hibernate Factory initialized " + e.getMessage());
        } catch (TimeoutException e) {
            System.err.println("Timeout Factory cant initialized " + e.getMessage());
        }
        curruntState = sate.initialized;
    }

    /**
     * The action triggered by pushing the button on the GUI
     */
    private void startCamera() {
        System.out.println("start camera action");
        try {
            if (!this.capture.isOpened()) {
                boolean open = this.capture.open(0);
                if (open) System.out.println("start camera action open is" + open);
                else System.out.println("start camera action false camera is not found" + open);

                // grab a frame every 33 ms (30 frames/sec)
                AtomicLong cheeseTime = new AtomicLong();
                AtomicReference<Long> gcForcer = new AtomicReference<>(0L);
                System.gc();
                Runnable frameGrabber = () -> {
                    // effectively grab and process a single frame

                    grabFrame(cheeseTime);
                    gcForcer.getAndSet(gcForcer.get() + 1);
                    if (gcForcer.get() == 500) {
                        System.gc();
                        System.out.println("garbageCollected");
                        gcForcer.set(0L);

                    }
                    // convert and show the frame
                    //	Image imageToShow = Utils.mat2Image(frame);

                };

                this.timer = Executors.newSingleThreadScheduledExecutor();
                this.timer.scheduleAtFixedRate(frameGrabber, 500, 1000 / fps.get(), TimeUnit.MILLISECONDS);
                curruntState = sate.Running;

            } else {
                curruntState = sate.stoprequested;
                FacesContext context = FacesContext.getCurrentInstance();
                context.addMessage(null, new FacesMessage("Camera Already Opened ", ""));
            }
        } catch (Exception ex) {
            FacesContext context = FacesContext.getCurrentInstance();
            context.addMessage(null, new FacesMessage("Camera start failed", "Exception message: " + ex.getMessage()));
            stopAcquisition();
        }
    }

    /**
     * Get a frame from the opened video stream (if any)
     *
     * @return the {@link Image} to show
     */
    private void grabFrame(AtomicLong tresholdPic) {
        Mat frame = new Mat();

        // check if the capture is open
        if (this.capture.isOpened()) {
            try {

                // read the current frame

                this.capture.read(frame);

                // if the frame is not empty, process it
                if (!frame.empty()) {
                    // face detection
                    detectAndDisplay(frame, tresholdPic);
                }

            } catch (Exception e) {
                // log the (full) error
                System.err.println("Exception during the image elaboration: " + e);
            } finally {
                frame.release();
            }
        }
    }

    /**
     * Method for face detection and tracking
     *
     * @param frame it looks for faces in this frame
     */
    private void detectAndDisplay(Mat frame, AtomicLong cheeseTime) {
        MatOfRect faces = new MatOfRect();
        Mat grayFrame = new Mat();
        try {

            frame = resizeImage(frame);
            // convert the frame in gray scale
            Imgproc.cvtColor(frame, grayFrame, Imgproc.COLOR_BGR2GRAY);
            // equalize the frame histogram to improve the result
            Imgproc.equalizeHist(grayFrame, grayFrame);

            // compute minimum face size (20% of the frame height, in our case)
            if (this.absoluteFaceSize == 0) {
                int height = grayFrame.rows();
                if (Math.round(height * 0.2f) > 0) {
                    this.absoluteFaceSize = Math.round(height * 0.2f);
                }
            }

            // detect faces
            this.faceCascade.detectMultiScale(grayFrame, faces, 1.1, 2, Objdetect.CASCADE_SCALE_IMAGE,
                    new Size(this.absoluteFaceSize, this.absoluteFaceSize), new Size());

            // each rectangle in faces is a face: draw them!
            Rect[] facesArray = faces.toArray();

            DateFormat dateFormat = SimpleDateFormat.getDateInstance(SimpleDateFormat.SHORT);
            String formatedDate = dateFormat.format(new Date());
            for (int i = 0; i < facesArray.length; i++) {

                Point pointOfrectangle = facesArray[i].tl();
                Point textTextPoint = new Point(pointOfrectangle.x, pointOfrectangle.y - pointOfrectangle.y / 10);
                Point dateTextPoint = new Point(pointOfrectangle.x, textTextPoint.y - textTextPoint.y / 5);
                Imgproc.rectangle(frame, pointOfrectangle, facesArray[i].br(), new Scalar(0, 255, 0), 2);
                Imgproc.putText(frame, "Knock! Knock!", textTextPoint, 1, 1d, new Scalar(0, 255, 0), 2);
                Imgproc.putText(frame, formatedDate, dateTextPoint, 1, 1d, new Scalar(0, 0, 255), 2);

                try {

                    if (cheeseTime.get() != 0 && System.currentTimeMillis() - cheeseTime.get() < 3000) {
                        System.out.println("to much ->" + cheeseTime);
                        return;
                    } else {
                        ImageContainer imageContainer = new ImageContainer();
                        imageContainer.setCreatedDate(new Date());
                        imageContainer.setImagePathName(dailyGlobalDirectoryPrefix
                                + fileSeperator + "visitor"
                                + visitorCounter.get()
                                + ".jpg");
                        imageContainer.setSingleInternalImageName("visitor" + visitorCounter.getAndIncrement() + ".jpg");
                        imageContainer.setDailyDynamicRelativePath(oneDriveUploadFolder + fileSeperator + imageContainer.getSingleInternalImageName());
                        imageContainer.setTimelineEvent(new TimelineEvent(imageContainer.getSingleInternalImageName(), imageContainer.getCreatedDate(), false));

                        FileOutputStream fios = new FileOutputStream(
                                imageContainer.getImagePathName());
                        BufferedImage imageToShow = Utils.mat2Image(frame);
                        if (imageToShow != null) {
                            imageToShow.flush();
                            ImageIO.write(imageToShow, "jpg", fios);
                            //       this.addMessageVisitorEncountered(fios.getName());
                            System.out.println("Visitor photo has been saved");
                            fios.flush();
                            fios.close();
                            File tempFile = new File(imageContainer.getImagePathName());
                            imageContainer.setImageFile(tempFile);
                            ImageContainerManager.getInstance().addVisitor(imageContainer);
                            cheeseTime.set(System.currentTimeMillis());
                        }
                    }

                } catch (IOException e) {
                    System.out.println("Error happened" + e.getMessage());
                }
            }
        } finally {
            grayFrame.release();
            faces.release();
            frame.release();
            grayFrame = null;
            faces = null;
            frame = null; //perhaps necessary for gb
        }
    }

    public Mat resizeImage(Mat frameImage) {
        Mat frameImageOut = new Mat();
        try {
            int width = frameImage.width();
            int height = frameImage.height();
            Rect rect1 = new Rect(width / 4, height / 4, width / 2, height / 2);
            // Imgproc.rectangle(frameImage, rect1.tl(), rect1.br(), new Scalar(0, 255, 0), 2);
            Mat submat = frameImage.submat(rect1);
            Imgproc.resize(submat, frameImageOut, frameImage.size(), 0, 0, Imgproc.INTER_CUBIC);
            return frameImageOut;
        } finally {
            frameImage.release();
        }


    }

    public ImageContainer grapFrameAndPersistDir() throws IOException {
        if (sate.notInitialized.equals(this.getCurruntState())) {
            init();
        }

        boolean open = this.capture.open(0);
        if (open) System.out.println("start camera action For test picture " + open);
        else System.out.println("start camera action false camera is not found" + open);
        if (this.capture.isOpened()) {
            Mat frameImage = new Mat();
            boolean read = capture.read(frameImage);
            frameImage = resizeImage(frameImage);

            try {
                if (read) {
                    BufferedImage imageToShow = Utils.mat2Image(frameImage);
                    if (imageToShow != null) {
                        long time = new Date().getTime();
                        ImageContainer imageContainer = new ImageContainer();
                        imageContainer.setImagePathName(dailyGlobalDirectoryPrefix
                                + fileSeperator + visitorTestPicture + time
                                + ".jpg");
                        imageContainer.setCreatedDate(new Date());
                        imageContainer.setSingleInternalImageName(visitorTestPicture + time + ".jpg");
                        imageContainer.setDailyDynamicRelativePath(oneDriveUploadFolder + fileSeperator + imageContainer.getSingleInternalImageName());
                        imageContainer.setTimelineEvent(new TimelineEvent(imageContainer.getSingleInternalImageName(), imageContainer.getCreatedDate(), false));
                        imageContainer.setCommentAboutVisitor("This picture has been taken for test purpose");
                        FileOutputStream fios = new FileOutputStream(imageContainer.getImagePathName());
                        ImageIO.write(imageToShow, "jpg", fios);
                        fios.close();
                        File tempFile = new File(imageContainer.getImagePathName());
                        imageContainer.setImageFile(tempFile);
                        System.out.println("Visitor photo has been saved");
                        ImageContainerManager.getInstance().addVisitor(imageContainer);
                        return imageContainer;
                    }
                }
            } finally {
                frameImage.release();
                stopAcquisition();
            }

        }

        return null;
    }

    /**
     * The action triggered by selecting the Haar Classifier checkbox. It loads
     * the trained set to be used for frontal face detection.
     */
    private void haarSelected() {
        // check whether the lpb checkbox is selected and deselect it
        File sdf = null;
        try {
            URL systemResource = this.getClass().getResource("/haarcascades/haarcascade_frontalface_alt.xml");
            sdf = new File(systemResource.getPath());
            System.out.println(sdf.getAbsolutePath());
        } catch (Exception ex) {
            System.err.println(ex.getMessage());
        }


        this.checkboxSelection(sdf.getAbsolutePath());
        //this.checkboxSelection("../resources/haarcascades/haarcascade_frontalface_alt.xml");
    }

    /**
     * Method for loading a classifier trained set from disk
     *
     * @param classifierPath the path on disk where a classifier trained set is located
     */
    private void checkboxSelection(String classifierPath) {
        // load the classifier(s)
        boolean cascadeLoad = this.faceCascade.load(classifierPath);
        if (!cascadeLoad)
            System.err.println("Cascade cant be not load");

        // now the video capture can start
        //this.cameraButton.setDisable(false);
    }

    /**
     * Stop the acquisition from the camera and release all the resources
     */
    public void stopAcquisition() {
        try {
            if (this.timer != null && !this.timer.isShutdown()) {
                System.out.println("server stop called");
                // stop the timer
                this.timer.shutdown();
//                videoWriter.release();

                this.timer.awaitTermination(33, TimeUnit.MILLISECONDS);
            }
        } catch (InterruptedException e) {
            // log any exception
            System.err.println("Exception in stopping the frame capture, trying to release the camera now... " + e);
        } finally {
            //TemporaryCache.getInstance().getImagePathCache().clear();
            curruntState = sate.stoprequested;
        }
        System.out.println("capture is closing");
        if (this.capture != null && this.capture.isOpened()) {
            // release the camera
            System.out.println("capture is not null and opened now its released");
            this.capture.release();
            System.out.println("capture is not null and opened now its released");
        }
        System.out.println("capture is closed succesfully");
        System.out.println("Refreshers are set Maximum");
        FacesContext context = FacesContext.getCurrentInstance();
        Application application = context.getApplication();
        RefreshBackEndAdjuster refreshGalleria = application.evaluateExpressionGet(context, "#{refreshBackEndAdjuster}", RefreshBackEndAdjuster.class);
        refreshGalleria.setRefreshGallerySliderValue(Integer.MAX_VALUE);
        System.out.println("Refreshers have been set to Maximum");
        System.out.println("Closing Dao");
        System.out.println("Dao Closed");


        curruntState = sate.stopped;
    }

    public void clearTemPreviewDir() {
        ImageContainerManager instance = ImageContainerManager.getInstance();
        try {
            String dirPathForTemp = instance.getDirPathForTemp();
            if (new File(dirPathForTemp).exists()) {
                ImageContainerManager.getInstance().getCacheAddVisitorsTimeline().clear();
                FileUtils.deleteDirectory(new File(dirPathForTemp));
            }
        } catch (IOException e) {
            System.out.println("Error happened while cleaning tempdir folder" + e.getMessage());
        }
    }

    public void clearDailyDumpAll() {
        try {
            FileUtils.deleteDirectory(new File(FaceDetectionController.getInstance().getInitialWebResourcePath() + FaceDetectionController.fileSeperator + FaceDetectionController.getInstance().getOneDriveUploadPrefix()));
        } catch (IOException ignored) {
        }
    }


    /**
     * On application close, stop the acquisition from the camera
     */
    @Override
    public void run() {
        if (isServerStoped()) {
            init();
            haarSelected();
            startCamera();
        } else
            System.out.println("Server already running!!!!!!!!");
    }

    public boolean isServerStoped() {
        return curruntState.equals(sate.stopped) ||
                curruntState.equals(sate.notInitialized) ||
                curruntState.equals(sate.initialized) ||
                curruntState.equals(sate.Closed);
    }

    public enum sate {
        Running, initialized, Closed, stoprequested, stopped, notInitialized
    }
}
