package knockApi.beans;

import org.primefaces.model.chart.MeterGaugeChartModel;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import java.util.ArrayList;
import java.util.List;

@ManagedBean
public class ChartViewRam {


    private MeterGaugeChartModel meterGaugeModel2;

    @PostConstruct
    public void init() {
        createMeterGaugeModels();
    }


    public MeterGaugeChartModel getMeterGaugeModel2() {
        return meterGaugeModel2;
    }

    private MeterGaugeChartModel initMeterGaugeModelRam() {
        List<Number> intervals = new ArrayList<Number>(){{
            add(0);
            add(250);
            add(500);
            add(1000);
        }};

        return new MeterGaugeChartModel(Runtime.getRuntime().freeMemory()/(1024*1024), intervals);
    }

    private void createMeterGaugeModels() {

        meterGaugeModel2 =initMeterGaugeModelRam();
        meterGaugeModel2.setTitle("Free RAM");
        meterGaugeModel2.setGaugeLabel("Mb");
    }
}
