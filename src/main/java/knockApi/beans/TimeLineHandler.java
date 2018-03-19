package knockApi.beans;

import knockApi.entity.ImageContainer;
import org.primefaces.component.timeline.TimelineUpdater;
import org.primefaces.model.timeline.TimelineEvent;

import javax.faces.application.Application;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;

public class TimeLineHandler {

    private static final TimeLineHandler timeLineHandler = new TimeLineHandler();

    private TimeLineHandler()
    {}

    public static TimeLineHandler getInstance()
    {
        return timeLineHandler;
    }

    public void addVisitorToTimeLine(ImageContainer visitorPersisted)
    {
        FacesContext context = FacesContext.getCurrentInstance();
        addVisitorToTimeLine(context,visitorPersisted);

    }

    public void addVisitorToTimeLine(FacesContext context,ImageContainer visitorPersisted)
    {
        TimelineEvent timelineAddTestEvent = new TimelineEvent(visitorPersisted, visitorPersisted.getCreatedDate(), true, null);
        Application application = context.getApplication();
        EditServerTimelineController timelineControllerBean = application.evaluateExpressionGet(context, "#{editServerTimelineController}", EditServerTimelineController.class);
        timelineControllerBean.getModel().add(timelineAddTestEvent);
        TimelineUpdater timelineUpdater = TimelineUpdater.getCurrentInstance(":mainForm:timeline");
        timelineUpdater.add(timelineAddTestEvent);
        addMessage("New Visitor", "New visitor added to the Timeline");
    }

    private void addMessage(String summary, String detail) {
        FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_INFO, summary, detail);
        FacesContext.getCurrentInstance().addMessage(null, message);
    }

}
