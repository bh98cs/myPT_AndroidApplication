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

public class MyAdapter extends FirestoreRecyclerAdapter<ExerciseLog, MyAdapter.ExerciseLogHolder>{


    /**
     * Create a new RecyclerView adapter that listens to a Firestore Query.  See {@link
     * FirestoreRecyclerOptions} for configuration options.
     *
     * @param options
     */
    public MyAdapter(@NonNull FirestoreRecyclerOptions<ExerciseLog> options) {
        super(options);
    }

    @Override
    protected void onBindViewHolder(@NonNull ExerciseLogHolder holder, int position, @NonNull ExerciseLog model) {

        //load details of exercise into recycler view using getter methods from class
        holder.textViewDate.setText(model.getDate());
        //TODO: may get runtime error as trying to insert an int
        holder.textViewSet1.setText(model.getSet1());
        holder.textViewSet2.setText(model.getSet2());
        holder.textViewSet3.setText(model.getSet3());
        holder.textViewWeight.setText(model.getWeight() + "KG");
        holder.textViewNotes.setText(model.getNotes());

    }

    @NonNull
    @Override
    public ExerciseLogHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.exercise_log_item, parent, false);
        return new ExerciseLogHolder(v);
    }

    class ExerciseLogHolder extends RecyclerView.ViewHolder{
        //declare views containing exercise details
        TextView textViewDate;
        TextView textViewSet1;
        TextView textViewSet2;
        TextView textViewSet3;
        TextView textViewWeight;
        TextView textViewNotes;

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

