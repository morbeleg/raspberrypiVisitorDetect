package knockApi.beans;

import knockApi.dao.ImageContainerManager;
import knockApi.entity.ImageContainer;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.faces.application.Application;
import javax.faces.bean.ManagedBean;
import javax.faces.context.FacesContext;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

@ManagedBean
public class ImagesView {
    private List<ImageContainer> images;
    private Integer totalTempVisitor = ImageContainerManager.getInstance().totalVisitorCount();
    @PostConstruct
    public synchronized void init() {
        System.out.println("called init");
        FacesContext context = FacesContext.getCurrentInstance();
        Application application = context.getApplication();
        EditServerTimelineController timelineControllerBean = application.evaluateExpressionGet(context, "#{editServerTimelineController}", EditServerTimelineController.class);
        timelineControllerBean.addDetectedVisitorOnTimeLine();
    }

    public List<String> getImages() {
        System.out.println("called images");

        images = new LinkedList<>(ImageContainerManager.getInstance().getCacheAddVisitorsTimeline());
        Collections.reverse(images);
        return images.stream().map(ImageContainer::getDailyDynamicRelativePath).collect(Collectors.toCollection(LinkedList::new));
    }

    public void setImages(List<ImageContainer> images) {
        this.images = images;
    }

    public String getImageCount() {
        return String.valueOf(totalTempVisitor);
    }

    @PreDestroy
    public void destroy()
    {
       // images.clear();
        System.out.println("called destroy");

    }
}
