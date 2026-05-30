package com.example.stepup.ui;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.stepup.data.GoalRepository;
import com.example.stepup.data.entities.GoalWithReminder;

import java.util.List;
import java.util.concurrent.Executors;

/**
 * GoalsViewModel אחראית על ניהול ה-State של מסך הבית.
 *
 * למה ViewModel ולא ישירות ב-Fragment?
 * כאשר המשתמש מסובב את המסך (Configuration Change), ה-Fragment נהרס ונוצר מחדש.
 * ה-ViewModel לא נהרסת – היא שורדת את הסיבוב ושומרת על הנתונים.
 * כך המטרות לא נטענות מחדש מה-DB בכל סיבוב.
 *
 * AndroidViewModel (במקום ViewModel רגיל) מקבלת Application Context –
 * בטוחה לשימוש ולא גורמת ל-memory leak.
 */
public class GoalsViewModel extends AndroidViewModel {

    private final GoalRepository repository;

    // MutableLiveData – ניתן לשנות מבפנים
    private final MutableLiveData<List<GoalWithReminder>> goalsLiveData = new MutableLiveData<>();

    public GoalsViewModel(@NonNull Application application) {
        super(application);
        repository = GoalRepository.getInstance(application);
    }

    // LiveData – ה-Fragment מקבל גרסה לקריאה בלבד, לא יכול לשנות ישירות
    public LiveData<List<GoalWithReminder>> getGoalsLiveData() {
        return goalsLiveData;
    }

    /**
     * טוענת את רשימת המטרות מה-DB ב-background thread.
     * כשהטעינה מסתיימת, מעדכנת את goalsLiveData –
     * ה-Fragment יקבל את הנתונים דרך ה-observer שהגדיר.
     */
    public void loadGoals() {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<GoalWithReminder> goals = repository.getGoalsWithReminders();
            goalsLiveData.postValue(goals);  // postValue בטוח לקריאה מ-background thread
        });
    }

    /**
     * מוחקת מטרה ואז מרעננת את הרשימה.
     * onGeofenceRemoved מועבר מה-Fragment כי הסרת Geofence דורשת Context של UI.
     */
    public void deleteGoal(long goalId, Runnable onGeofenceRemoved) {
        Executors.newSingleThreadExecutor().execute(() -> {
            repository.deleteGoalWithReminder(goalId);
            if (onGeofenceRemoved != null) onGeofenceRemoved.run();
            loadGoals();
        });
    }

    /** מוודאת שקטגוריות ברירת המחדל קיימות (בפעם הראשונה בלבד) */
    public void ensureDefaultCategories() {
        Executors.newSingleThreadExecutor().execute(repository::ensureDefaultCategories);
    }
}
