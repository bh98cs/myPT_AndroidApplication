package daniel.southern.myptapplication;

import java.util.Date;

/**
 * Class for an Exercise Log
 */
public class ExerciseLog {
    //declare variables to hold details about exercises
    private String exerciseType;
    private Date date;
    private int set1;
    private int set2;
    private int set3;
    private int weight;
    private String notes;
    private String user;


    public ExerciseLog(){
        //empty constructor needed for Firebase
    }

    /**
     * Class constructor
     * @param exerciseType name of the exercise completed
     * @param date date of completing the exercise
     * @param set1 number of repetitions in the first set
     * @param set2 number of repetitions in the second set
     * @param set3 number of repetitions in the third set
     * @param weight the weight used for the exercise
     * @param notes any additional notes made by the user
     */
    public ExerciseLog(String exerciseType, Date date, int set1, int set2, int set3, int weight, String notes){
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

    public Date getDate() {
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
