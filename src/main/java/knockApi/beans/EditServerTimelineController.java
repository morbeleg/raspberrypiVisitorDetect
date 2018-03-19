package knockApi.beans;

import knockApi.FaceDetectionController;
import knockApi.dao.ImageContainerManager;
import knockApi.entity.ImageContainer;
import org.primefaces.component.timeline.TimelineUpdater;
import org.primefaces.event.timeline.TimelineAddEvent;
import org.primefaces.event.timeline.TimelineModificationEvent;
import org.primefaces.model.timeline.TimelineEvent;
import org.primefaces.model.timeline.TimelineModel;

import javax.annotation.PostConstruct;
import javax.faces.application.Application;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ApplicationScoped;
import javax.faces.bean.ManagedBean;
import javax.faces.context.FacesContext;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

@ApplicationScoped
@ManagedBean
public class EditServerTimelineController implements Serializable {

    private TimelineModel model;
    private TimelineEvent event; // current event to be changed, edited, deleted or added
    private long zoomMax;
    private Date start;
    private Date end;
    private TimeZone timeZone = TimeZone.getDefault();
    private boolean timeChangeable = true;


    @PostConstruct
    protected void initialize() {

        zoomMax = 1000L * 60 * 60 * 24 * 30;
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.set(Calendar.getInstance().get(Calendar.YEAR),  Calendar.getInstance().get(Calendar.MONTH), Calendar.getInstance().get(Calendar.DATE)-10);
        start = cal.getTime();
        cal.set(Calendar.YEAR,  Calendar.getInstance().get(Calendar.MONTH)+2, 0);
        end = cal.getTime();
        model = new TimelineModel();
        FaceDetectionController.getInstance().clearTemPreviewDir();
        initOldVisitorsEvents();

    }

    public void onChange(TimelineModificationEvent e) {

        event = e.getTimelineEvent();
        TimelineEvent oldEvent = model.getEvent(model.getIndex(event));
        TimelineUpdater timelineUpdater = TimelineUpdater.getCurrentInstance(":mainForm:timeline");
        model.update(oldEvent, timelineUpdater);
    }

    public void onEdit(TimelineModificationEvent e) {
        event = e.getTimelineEvent();
    }

    public void onAdd(TimelineAddEvent e) {
        FacesMessage msg =
                new FacesMessage(FacesMessage.SEVERITY_INFO, "TimeLine updated", null);
        FacesContext.getCurrentInstance().addMessage(null, msg);

    }

    public void onDelete(TimelineModificationEvent e) {
        event = e.getTimelineEvent();
    }

    public void delete() {
        TimelineUpdater timelineUpdater = TimelineUpdater.getCurrentInstance(":mainForm:timeline");
        ImageContainerManager.getInstance().deleteVisitor((ImageContainer) event.getData());
        model.delete(event, timelineUpdater);

        FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_INFO, "The visitor " + getVisitorId() + " has been deleted", null);
        FacesContext.getCurrentInstance().addMessage(null, msg);
    }

    public void saveDetails() {
        TimelineUpdater timelineUpdater = TimelineUpdater.getCurrentInstance(":mainForm:timeline");
        model.update(event, timelineUpdater);
        ImageContainerManager.getInstance().updateVisitorComment((ImageContainer) event.getData());

        FacesMessage msg =
                new FacesMessage(FacesMessage.SEVERITY_INFO, "The booking details " + getVisitorId() + " have been saved", null);
        FacesContext.getCurrentInstance().addMessage(null, msg);
    }

    public TimelineModel getModel() {
        return model;
    }

    public TimelineEvent getEvent() {
        return event;
    }

    public void setEvent(TimelineEvent event) {
        this.event = event;
    }

    public long getZoomMax() {
        return zoomMax;
    }

    public Date getStart() {

        return start;
    }

    public void setStart(Date start) {
        this.start = start;
    }

    public Date getEnd() {

        return end;
    }

    public void setEnd(Date end) {
        this.end = end;
    }

    public TimeZone getTimeZone() {
        return timeZone;
    }

    public boolean isTimeChangeable() {
        return timeChangeable;
    }

    public void toggleTimeChangeable() {
        timeChangeable = !timeChangeable;
    }

    public String getDeleteMessage() {
        Integer visitorId = ((ImageContainer) event.getData()).getId();
        if (visitorId == null) {
            return "Do you really want to delete the visitor image from Database?";
        }

        return "Do you really want to delete the visitor " + visitorId + "?";
    }

    public String getVisitorId() {
        Integer id = ((ImageContainer) event.getData()).getId();
        if (id == null) {
            return "(new Visitor)";
        } else {
            return "(Visitor " + id + ")";
        }
    }

    public void initOldVisitorsEvents() {
        model.clear();
        ImageContainerManager imageContainerManager = ImageContainerManager.getInstance();
        Future<List<ImageContainer>> visitorNameListFromOneMothBefore = imageContainerManager.getVisitorNameListFromOneMothBefore();
        List<ImageContainer> visitorsAll = imageContainerManager.getFutureTask(visitorNameListFromOneMothBefore);
        List<TimelineEvent> collected = visitorsAll.stream().map(imageContainer -> {
            imageContainer.setTimelineEvent(new TimelineEvent(imageContainer, imageContainer.getCreatedDate(), true));
            return imageContainer.getTimelineEvent();
        }).collect(Collectors.toList());
        model.addAll(collected);
    }

    public void addDetectedVisitorOnTimeLine() {
        System.out.println("submitted");
        long start = System.currentTimeMillis();
        List<ImageContainer> visitorTimeLineAddList =new ArrayList<>(ImageContainerManager.getInstance().getCacheAddVisitorsTimeline()); //that will calculated via subtraction
        visitorTimeLineAddList = visitorTimeLineAddList.stream().sorted(Comparator.comparingInt(ImageContainer::getId)).collect(Collectors.toList());
        List<TimelineEvent> collected = visitorTimeLineAddList.stream().map(imageContainer -> {
            imageContainer.setTimelineEvent(new TimelineEvent(imageContainer, imageContainer.getCreatedDate(), true));
            return imageContainer.getTimelineEvent();
        }).collect(Collectors.toList());
        TimelineUpdater timelineUpdater = TimelineUpdater.getCurrentInstance("mainForm:timeline");
        FacesContext currentInstance = FacesContext.getCurrentInstance();
        Application application = currentInstance.getApplication();
        EditServerTimelineController timelineControllerBean = application.evaluateExpressionGet(currentInstance, "#{editServerTimelineController}", EditServerTimelineController.class);
        timelineControllerBean.getModel().addAll(collected,timelineUpdater);
        System.out.println("got it "+String.valueOf(System.currentTimeMillis()-start));

    }



}