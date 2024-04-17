package daniel.southern.myptapplication;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.protobuf.StringValue;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class MyAdapter extends FirestoreRecyclerAdapter<ExerciseLog, MyAdapter.ExerciseLogHolder>{


    /**
     * Create a new RecyclerView adapter that listens to a Firestore Query.  See {@link
     * FirestoreRecyclerOptions} for configuration options.
     *
     * @param options configuration options
     */
    public MyAdapter(@NonNull FirestoreRecyclerOptions<ExerciseLog> options) {
        super(options);
    }

    @Override
    public void updateOptions(@NonNull FirestoreRecyclerOptions<ExerciseLog> options) {
        super.updateOptions(options);
    }

    @Override
    protected void onBindViewHolder(@NonNull ExerciseLogHolder holder, int position, @NonNull ExerciseLog model) {

        //set format for date
        DateFormat df = new SimpleDateFormat("dd-MMM-yyyy", Locale.getDefault());
        //convert date to a string
        String date = df.format(model.getDate());

        //load details of exercise into recycler view using getter methods from class
        holder.textViewDate.setText(date);
        holder.textViewSet1.setText(String.valueOf(model.getSet1()));
        holder.textViewSet2.setText(String.valueOf(model.getSet2()));
        holder.textViewSet3.setText(String.valueOf(model.getSet3()));
        holder.textViewWeight.setText(String.valueOf(model.getWeight()));
        holder.textViewNotes.setText(model.getNotes());

    }

    /**
     * Retrieves the Firebase ID of the document at the given position
     * @param position position of the document within the recylerview
     * @return Firebase ID for the document
     */
    public String getItemFirebaseId(int position){
        //return the id of the item at the specified position in the recyclerview
        return getSnapshots().getSnapshot(position).getReference().getId();
    }

    /**
     * Deletes the document at the given position from the Firestore database
     * @param position position of the document within the recyclerview
     */
    public void deleteItem(int position){
        //delete item from Firebase
        getSnapshots().getSnapshot(position).getReference().delete();
    }

    @NonNull
    @Override
    public ExerciseLogHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.exercise_log_item, parent, false);
        return new ExerciseLogHolder(v);
    }

    /**
     * Inner class to display data from each Firestore document within recycler view
     */
    class ExerciseLogHolder extends RecyclerView.ViewHolder{
        //declare views containing exercise details
        TextView textViewDate;
        TextView textViewSet1;
        TextView textViewSet2;
        TextView textViewSet3;
        TextView textViewWeight;
        TextView textViewNotes;

        /**
         * {@link ExerciseLogHolder} class constructor
         * @param itemView
         */
        public ExerciseLogHolder(@NonNull View itemView) {
            super(itemView);
            //initialise views
            textViewDate = itemView.findViewById(R.id.textViewDate);
            textViewSet1 = itemView.findViewById(R.id.textViewSet1);
            textViewSet2 = itemView.findViewById(R.id.textViewSet2);
            textViewSet3 = itemView.findViewById(R.id.textViewSet3);
            textViewWeight = itemView.findViewById(R.id.textViewWeight);
            textViewNotes = itemView.findViewById(R.id.textViewNotes);
        }
    }
}

