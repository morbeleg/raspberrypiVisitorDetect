package knockApi.beans;

import knockApi.dao.ImageContainerManager;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

@ManagedBean
public class SingleLazyImageView {
    private StreamedContent graphicText;
    ImageContainerManager imageContainerManager = ImageContainerManager.getInstance();

    @PostConstruct
    public void init() {
        try {

        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public StreamedContent getSingleVisitorImage() {
        try {
            graphicText=new DefaultStreamedContent(new FileInputStream(imageContainerManager.getSingleVisitorWithTimeImagePreview().getTempDumpPreviewImage()));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return graphicText;
    }

}
