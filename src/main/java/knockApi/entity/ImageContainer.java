package knockApi.entity;

import org.primefaces.model.timeline.TimelineEvent;

import java.io.File;
import java.io.Serializable;
import java.sql.Blob;
import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;

public class ImageContainer implements Serializable {
    private Integer id;
    private File imageFile;
    private String imagePathName;
    private Date createdDate;
    private String singleInternalImageName;
    private String dailyDynamicRelativePath;
    private TimelineEvent timelineEvent;
    private Blob imageContent;
    private String commentAboutVisitor;
    private File tempDumpPreviewImage;
    private String formatedDate;

    public ImageContainer() {
    }

    public String getFormatedDate() {
        return formatedDate = DateFormat.getDateInstance(DateFormat.SHORT, Locale.getDefault()).format(createdDate);
    }

    public void setFormatedDate(String formatedDate) {
        this.formatedDate = formatedDate;
    }

    public File getTempDumpPreviewImage() {
        return tempDumpPreviewImage;
    }

    public void setTempDumpPreviewImage(File tempDumpPreviewImage) {
        this.tempDumpPreviewImage = tempDumpPreviewImage;
    }

    public String getCommentAboutVisitor() {
        return commentAboutVisitor;
    }

    public void setCommentAboutVisitor(String commentAboutVisitor) {
        this.commentAboutVisitor = commentAboutVisitor;
    }

    public Blob getImageContent() {
        return imageContent;
    }

    public void setImageContent(Blob imageContent) {
        this.imageContent = imageContent;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public TimelineEvent getTimelineEvent() {
        return timelineEvent;
    }

    public void setTimelineEvent(TimelineEvent timelineEvent) {
        this.timelineEvent = timelineEvent;
    }

    public String getDailyDynamicRelativePath() {
        return dailyDynamicRelativePath;
    }

    public void setDailyDynamicRelativePath(String dailyDynamicRelativePath) {
        this.dailyDynamicRelativePath = dailyDynamicRelativePath;
    }

    public String getSingleInternalImageName() {
        return singleInternalImageName;
    }

    public void setSingleInternalImageName(String singleInternalImageName) {
        this.singleInternalImageName = singleInternalImageName;
    }

    public File getImageFile() {
        return imageFile;
    }

    public void setImageFile(File imageFile) {
        this.imageFile = imageFile;
    }

    public String getImagePathName() {
        return imagePathName;
    }

    public void setImagePathName(String imagePathName) {
        this.imagePathName = imagePathName;
    }

    public Date getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
    }
}
