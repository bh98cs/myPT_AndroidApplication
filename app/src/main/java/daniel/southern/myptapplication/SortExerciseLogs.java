package daniel.southern.myptapplication;

import java.util.Comparator;

/**
 * Used for sorting a list of exercise logs by date
 */
public class SortExerciseLogs implements Comparator<ExerciseLog> {
    @Override
    public int compare(ExerciseLog o1, ExerciseLog o2) {
        //compare objects to sort by date in ascending order
        return o1.getDate().compareTo(o2.getDate());
    }
}
