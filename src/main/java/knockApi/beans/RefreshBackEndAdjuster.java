package knockApi.beans;

import org.primefaces.event.SlideEndEvent;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.context.FacesContext;

@ManagedBean
public class RefreshBackEndAdjuster {
    private int refreshGallerySliderValue = 10;

    public int getRefreshGallerySliderValue() {
        return refreshGallerySliderValue;
    }

    public void setRefreshGallerySliderValue(int refreshGallerySliderValue) {
        this.refreshGallerySliderValue = refreshGallerySliderValue;
    }

    public void onSlideEndRefreshGallery(SlideEndEvent event) {
        FacesMessage message;
        Integer intervalForComponent = event.getValue();
        if (intervalForComponent < 5) {
            message = new FacesMessage("Current refresh Value is assigned for " + intervalForComponent + " 5 sec(Min Value)", "Current Value: " + 2);
            refreshGallerySliderValue = 5;
        } else {
            message = new FacesMessage("Current refresh Value is assigned for \"" + intervalForComponent +"", "Current Value: " + intervalForComponent);
            refreshGallerySliderValue = intervalForComponent;
        }
        FacesContext.getCurrentInstance().addMessage(null, message);
    }




}
