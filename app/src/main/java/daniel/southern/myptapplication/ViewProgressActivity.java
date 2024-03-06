package daniel.southern.myptapplication;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import java.util.ArrayList;

public class ViewProgressActivity extends AppCompatActivity {

    LineChart lineChart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_progress);

        lineChart = findViewById(R.id.line_chart);

        LineDataSet lineDataSet = new LineDataSet(lineChartDataSet(), "data set");
        ArrayList<ILineDataSet> iLineDataSets = new ArrayList<>();
        iLineDataSets.add(lineDataSet);
        LineData lineData = new LineData(iLineDataSets);
        lineChart.setData(lineData);
        lineChart.invalidate();
    }

    private ArrayList<Entry> lineChartDataSet(){
        ArrayList<Entry> dataset = new ArrayList<>();
        dataset.add(new Entry(0,10));
        dataset.add(new Entry(1,12));
        dataset.add(new Entry(2,34));
        dataset.add(new Entry(3,35));
        dataset.add(new Entry(4,23));
        dataset.add(new Entry(5,67));
        dataset.add(new Entry(6,34));
        dataset.add(new Entry(7,12));
        dataset.add(new Entry(8,35));
        dataset.add(new Entry(9,57));

        return dataset;
    }
}