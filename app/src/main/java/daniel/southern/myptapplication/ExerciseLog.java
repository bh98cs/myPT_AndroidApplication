package daniel.southern.myptapplication;

public class ExerciseLog {
    //declare variables to hold details about exercises
    private String exerciseType;
    private String date;
    private int set1;
    private int set2;
    private int set3;
    private int weight;
    private String notes;
    private String user;


    public ExerciseLog(){
        //empty constructor needed for Firebase
    }

    //class constructor with parameters
    public ExerciseLog(String exerciseType, String date, int set1, int set2, int set3, int weight, String notes){
        this.exerciseType = exerciseType;
        this.date = date;
        this.set1 = set1;
        this.set2 = set2;
        this.set3 = set3;
        this.weight = weight;
        this.notes = notes;
    }

    //getter methods for all variables
    public String getExerciseType() {
        return exerciseType;
    }

    public String getDate() {
        return date;
    }

    public int getSet1() {
        return set1;
    }

    public int getSet2() {
        return set2;
    }

    public int getSet3() {
        return set3;
    }

    public int getWeight() {
        return weight;
    }

    public String getNotes() {
        return notes;
    }
    public String getUser(){return user;}

}
