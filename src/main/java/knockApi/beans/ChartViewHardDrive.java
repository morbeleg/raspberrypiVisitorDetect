package knockApi.beans;

import org.primefaces.model.chart.MeterGaugeChartModel;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.view.ViewScoped;
import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@ManagedBean
@ViewScoped
public class ChartViewHardDrive implements Serializable {

    private MeterGaugeChartModel meterGaugeModel1;

    @PostConstruct
    public void init() {
        createMeterGaugeModels();
    }

    public MeterGaugeChartModel getMeterGaugeModel1() {
        return meterGaugeModel1;
    }

    private MeterGaugeChartModel initMeterGaugeModelHardDrive() {
        List<Number> intervals = new ArrayList<Number>(){{
            add(0);
            add(8);
            add(16);
            add(32);
        }};

        return new MeterGaugeChartModel(File.listRoots()[0].getFreeSpace()/(1024*1024*1024), intervals);
    }

    private void createMeterGaugeModels() {
        meterGaugeModel1 = initMeterGaugeModelHardDrive();
        meterGaugeModel1.setTitle("Hard Disk Usage");
        meterGaugeModel1.setGaugeLabel("32GB");
    }

}