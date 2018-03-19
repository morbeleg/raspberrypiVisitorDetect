package knockApi.dao;

import knockApi.FaceDetectionController;
import knockApi.beans.EditServerTimelineController;
import knockApi.entity.ImageContainer;
import org.hibernate.*;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.engine.jdbc.BlobProxy;

import javax.faces.application.Application;
import javax.faces.context.FacesContext;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.math.BigInteger;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.*;


public class ImageContainerManager {
    private static String dirPathForTemp;
    private static ImageContainerManager imageContainer = new ImageContainerManager();
    private SessionFactory factory;
    private ExecutorService persistorExecutor = Executors.newSingleThreadExecutor();
    private List<ImageContainer> cacheAddVisitorsTimeline = Collections.synchronizedList(new ArrayList<>());

    private ImageContainerManager() {
        this.factory = DaoHandler.getLazyFactory();
        dirPathForTemp = FaceDetectionController.getInstance().getInitialWebResourcePath() +
                FaceDetectionController.fileSeperator + "tempDump" + FaceDetectionController.fileSeperator;
    }

    public static ImageContainerManager getInstance() {
        return imageContainer;
    }

    public static byte[] getImage(File file) {
        if (file.exists()) {
            try {
                BufferedImage bufferedImage = ImageIO.read(file);
                ByteArrayOutputStream byteOutStream = new ByteArrayOutputStream();
                ImageIO.write(bufferedImage, "jpg", byteOutStream);
                return byteOutStream.toByteArray();
            } catch (IOException e) {
                System.err.println(e.getMessage());
            }
        }
        return null;
    }

    public static File saveImage(InputStream stream, String imageId) {

        File file = new File(dirPathForTemp);

        boolean mkdirs;
        if (!file.exists()) {
            mkdirs = file.mkdirs();
            if (mkdirs)
                System.out.println(file.getAbsolutePath() + " is created");
        }
        File tempFile = new File(file.getPath() + FaceDetectionController.fileSeperator + "tempVisitorDump" + imageId + ".jpg");
        try (FileOutputStream outputStream = new FileOutputStream(tempFile)) {
            BufferedImage bufferedImage = ImageIO.read(stream);
            ImageIO.write(bufferedImage, "jpg", outputStream);
            System.out.println("Image file location: " + file.getCanonicalPath());
            outputStream.flush();
            return tempFile;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

    }

    public String getDirPathForTemp() {
        return dirPathForTemp;
    }

    public List<ImageContainer> getCacheAddVisitorsTimeline() {
        return cacheAddVisitorsTimeline;
    }

    public void setCacheAddVisitorsTimeline(List<ImageContainer> cacheAddVisitorsTimeline) {
        this.cacheAddVisitorsTimeline = cacheAddVisitorsTimeline;
    }

    public Future<Integer> addVisitor(ImageContainer imageContainer) {
        if (factory.isClosed())
            DaoHandler.getLazyFactory();
        return persistorExecutor.submit(insertVisitor(imageContainer));
    }

    private Callable<Integer> insertVisitor(ImageContainer imageContainer) {
        return () -> {
            Session session = factory.openSession();
            Transaction tx = null;
            Integer primaryKey = null;
            try {
                tx = session.beginTransaction();
                byte[] image = getImage(imageContainer.getImageFile());
                session.doWork(connection -> imageContainer.setImageContent(
                        BlobProxy.generateProxy(Objects.requireNonNull(image))
                ));

                primaryKey = (Integer) session.save(imageContainer);
                tx.commit();
                //not carry image content
                imageContainer.setImageContent(null);
                cacheAddVisitorsTimeline.add(imageContainer);
            } catch (HibernateException e) {
                if (tx != null) tx.rollback();
                e.printStackTrace();
            } finally {
                session.close();
            }
            return primaryKey;
        };
    }

    public void deleteVisitor(ImageContainer imageContainer) {
        Session session = factory.openSession();
        Transaction tx = null;
        Integer primaryKey = null;
        try {
            tx = session.beginTransaction();
            ImageContainer load = session.load(ImageContainer.class, imageContainer.getId());
            session.delete(load);
            tx.commit();
            //not carry image content

            cacheAddVisitorsTimeline.remove(imageContainer);
        } catch (HibernateException e) {
            if (tx != null) tx.rollback();
            e.printStackTrace();
        } finally {
            session.close();
        }
    }

    //todo update comment
    public void updateVisitorComment(ImageContainer imageContainer) {

        Session session = factory.openSession();
        Transaction tx = null;
        Integer primaryKey = null;
        try {
            tx = session.beginTransaction();
            session.update(imageContainer);
            tx.commit();
            //not carry image content
            imageContainer.setImageContent(null);

        } catch (HibernateException e) {
            if (tx != null) tx.rollback();
            e.printStackTrace();
        } finally {
            session.close();
        }

    }

    public synchronized Integer totalVisitorCount() {
        Session session = factory.openSession();
        Transaction tx = null;
        try {
            tx = session.beginTransaction();
            return ((Number) session.createCriteria(ImageContainer.class).setProjection(Projections.rowCount()).uniqueResult()).intValue();
        } catch (HibernateException e) {
            if (tx != null) tx.rollback();
            e.printStackTrace();
        } finally {
            if (tx != null) {
                tx.commit();
            }
            session.close();
        }
        return -1;
    }

    public Future<List<ImageContainer>> getVisitorNameListFromOneMothBefore() {
        if (factory.isClosed())
            DaoHandler.getLazyFactory();
        return persistorExecutor.submit(getVisitorBeforeOneMonthInternal());
    }

    public ImageContainer getSingleVisitorWithTimeImagePreview() {
        if (factory.isClosed())
            DaoHandler.getLazyFactory();

        FacesContext context = FacesContext.getCurrentInstance();
        Application application = context.getApplication();
        EditServerTimelineController timelineControllerBean = application.evaluateExpressionGet(context, "#{editServerTimelineController}", EditServerTimelineController.class);
        ImageContainer currentFrontImageContainer;
        //below if for first request there I guess there is a bug in JFS
        if (timelineControllerBean.getEvent() != null)
            currentFrontImageContainer = (ImageContainer) timelineControllerBean.getEvent().getData();
        else {
            currentFrontImageContainer = (ImageContainer) timelineControllerBean.getModel().getEvents().get(0).getData();
        }
        Session session = factory.openSession();
        Transaction tx = null;
        try {

            tx = session.beginTransaction();
            currentFrontImageContainer = session.get(ImageContainer.class, currentFrontImageContainer.getId());
            File Flushed = saveImage(currentFrontImageContainer.getImageContent().getBinaryStream(), String.valueOf(currentFrontImageContainer.getId()));
            currentFrontImageContainer.setTempDumpPreviewImage(Flushed);
            return currentFrontImageContainer;
        } catch (HibernateException e) {
            if (tx != null) tx.rollback();
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            Objects.requireNonNull(tx).commit();
            session.close();
        }
        return null;
    }

    public ImageContainer getSingleVisitor(Integer id) {
        if (factory.isClosed())
            DaoHandler.getLazyFactory();
        Session session = factory.openSession();
        Transaction tx = null;
        try {
            tx = session.beginTransaction();
            //File Flushed = saveImage(imageContainerTemp.getImageContent().getBinaryStream(), String.valueOf(id));
            //imageContainerTemp.setTempDumpPreviewImage(Flushed);
            return session.get(ImageContainer.class, id);
        } catch (HibernateException e) {
            if (tx != null) tx.rollback();
            e.printStackTrace();
        } finally {
            Objects.requireNonNull(tx).commit();
            session.close();
        }
        return null;
    }


    private Callable<List<ImageContainer>> getVisitorBeforeOneMonthInternal() {
        return () -> {
            List results = new ArrayList<ImageContainer>();
            Session session = factory.openSession();
            Transaction tx = null;
            try {
                tx = session.beginTransaction();
                Criteria criteria = session.createCriteria(ImageContainer.class);
                Calendar instance = Calendar.getInstance();
                instance.add(Calendar.MONTH, -1);
                criteria.add(Restrictions.gt("createdDate", instance.getTime()));
                results = criteria.list();
                tx.commit();
            } catch (HibernateException e) {
                if (tx != null) tx.rollback();
                e.printStackTrace();
            } finally {
                session.close();
            }
            return results;
        };
    }

    public <T> T getFutureTask(Future<T> future) {
        try {
            return future.get(10, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Integer sequenceValue() {
        Session session = factory.openSession();
        Transaction tx = null;
        try {
            tx = session.beginTransaction();
            SQLQuery sqlQuery = session.createSQLQuery("select currval('postgres.public.visitors_entityid_seq')");
            BigInteger next = (BigInteger) sqlQuery.list().get(0);
            tx.commit();
            return next.intValue();
        } catch (HibernateException e) {
            if (tx != null) tx.rollback();
            e.printStackTrace();
        } finally {
            session.close();
        }
        return null;
    }
}


