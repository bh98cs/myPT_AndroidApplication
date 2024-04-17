package daniel.southern.myptapplication;
import com.github.mikephil.charting.formatter.ValueFormatter;

/**
 * Custom {@link ValueFormatter} required for creating graph
 */
public class MyXAxisValueFormatter extends ValueFormatter{
    public static final String TAG = "MyXAxisValueFormatter";
    private String[] mValues;
    public MyXAxisValueFormatter(String[] values){
        this.mValues = values;
    }

    @Override
    public String getFormattedValue(float value) {
        return mValues[(int) value];
    }
}
