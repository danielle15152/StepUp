package com.example.stepup;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.stepup.data.AppDatabase;
import com.example.stepup.data.Dao;
import com.example.stepup.data.entities.GoalWithReminder;
import com.example.stepup.ui.GoalsAdapter;

import java.util.List;
import java.util.concurrent.Executors;

public class GoalsActivity extends AppCompatActivity {
    View fragmentContainer;// מיכל שלו נכנסים כל פעם הפרגמנט המתאים

    private Dao dao; //מה שמדבר ועושה פעולות על המסד נתונים. כמו להכניס, למחוק ...
    private GoalsAdapter adapter;// הופך את הקוד לויזואלי

    // --- הוספת המנגנון לבקשת הרשאה ---
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                // אפשר להוסיף כאן לוגיקה אם רוצים - למשל, להראות הודעה אם המשתמש סירב
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);//פעולה ליצירת המסך

        EdgeToEdge.enable(this);//פורס את המסך על כל שטח המכשיר
        setContentView(R.layout.activity_goals);//מחבר בין הקוד לxml

        // --- קריאה לפונקציה שמבקשת הרשאה ---
        requestNotificationPermission();

        // --- שימוש בשיטה המרכזית לקבלת מופע של מסד הנתונים ---
        AppDatabase db = AppDatabase.getDatabase(getApplicationContext());
        dao = db.dao();


        View root = findViewById(R.id.root);
        fragmentContainer = findViewById(R.id.fragmentContainer);
        if (root != null) {
            root.setAlpha(0f);
            root.animate().alpha(1f).setDuration(300).start();
        }

        ImageButton settings = findViewById(R.id.btnSettings);
        if (settings != null) {
            settings.setOnClickListener(v -> {//מעבר בין מסכים בין המסך הנוכחי להגדרות
                Intent intent = new Intent(GoalsActivity.this, ActivitySetting.class);
                startActivity(intent);
            });
        }

        RecyclerView rvGoals = findViewById(R.id.rvGoals);//מצג את הרשימה של המטרות
        rvGoals.setLayoutManager(new LinearLayoutManager(this));//מסדר את הרשימה אחד מתחת לשני (תור)
getSupportFragmentManager().addOnBackStackChangedListener(()->{
    //fragmentContainer.setVisibility(View.GONE);
    if(getSupportFragmentManager().getFragments().isEmpty())
        fragmentContainer.setVisibility(View.GONE);//מחביא את הפרגמנט כדי לא להסתיר את הרשימה של המטרות
    else
        fragmentContainer.setVisibility(View.VISIBLE);//מראה את הפרגמנט
});
        adapter = new GoalsAdapter(new GoalsAdapter.GoalActionsListener() {
            @Override
            public void onEditClicked(GoalWithReminder item) {
                long goalId = item.goal.id;
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragmentContainer, EditGoalFragment.newInstance(goalId))// מכניסים לקונטיינר את הפרגמנט של עריכה לפי הid המתאים
                        .addToBackStack("edit_goal")
                        .commit();
            }

            @Override
            public void onDeleteClicked(GoalWithReminder item) {
                showDeleteConfirmDialog(item);//פותחים דיאלוג בעת לחיצה על כפתור מחיקה כדי לאשר את המחיקה
            }
        }, dao);

        rvGoals.setAdapter(adapter);
        // 1. מוצאים את כפתור הפלוס מה-XML (נניח שקראת לו fabAddGoal)
        View fabAdd = findViewById(R.id.fabAddGoal);
        if (fabAdd != null) {
            fabAdd.setOnClickListener(v -> {
                // 2. פותחים את הפרגמנט עם ID של 1- כדי לסמן "מטרה חדשה"
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragmentContainer, EditGoalFragment.newInstance(-1))
                        .addToBackStack("add_goal")
                        .commit();
            });
        }

        // הוספת קטגוריות בסיס אם הרשימה ריקה (רצוי להריץ בטרד נפרד)
        Executors.newSingleThreadExecutor().execute(() -> {
            if (dao.getAllCategories().isEmpty()) {
                dao.insertCategory(new com.example.stepup.data.entities.Category("Health", true));
                dao.insertCategory(new com.example.stepup.data.entities.Category("Education", true));
                dao.insertCategory(new com.example.stepup.data.entities.Category("Sports", true));
                dao.insertCategory(new com.example.stepup.data.entities.Category("Finance", true));
            }
        });

        loadGoals();//פונקציה שמביאה את הנתונים
    }

    // --- הוספת הפונקציה שמבקשת הרשאה ---
    private void requestNotificationPermission() {
        // רלוונטי רק לאנדרואיד 13 (API 33) ומעלה
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // בודקים אם ההרשאה *לא* אושרה עדיין
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) !=
                    PackageManager.PERMISSION_GRANTED) {
                // מקפיצים את חלון הבקשה
                requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }


    public Dao getDao() {
        return dao;
    }

    public void loadGoals() {
        Executors.newSingleThreadExecutor().execute(() -> {//עוברים לטרד רקע כדי להביא נתונים מהמסד נתונים

            List<GoalWithReminder> items = dao.getGoalsWithReminders();
            runOnUiThread(() -> adapter.setItems(items));//חוזרים לטרד הראשי כדי לעדכן את המסך
        });
    }

    private void showDeleteConfirmDialog(GoalWithReminder item) {
        String goalName = (item.goal != null && item.goal.name != null) ? item.goal.name : "this goal";

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Delete goal?")
                .setMessage("Are you sure you want to delete \"" + goalName + "\"? This cannot be undone.")
                .setNegativeButton("Cancel", (d, which) -> d.dismiss())
                .setPositiveButton("Delete", null) // נצמיד handler אחרי show כדי לשים אייקון/אדום
                .create();

        dialog.setOnShowListener(d -> {
            Button deleteBtn = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            if (deleteBtn != null) {
                // אדום
                deleteBtn.setTextColor(Color.RED);

                // אייקון פח
                Drawable trashIcon = ContextCompat.getDrawable(this, android.R.drawable.ic_menu_delete);
                if (trashIcon != null) {
                    deleteBtn.setCompoundDrawablesWithIntrinsicBounds(trashIcon, null, null, null);
                    deleteBtn.setCompoundDrawablePadding(16);
                }

                // הפעולה בפועל
                deleteBtn.setOnClickListener(v -> {
                    dialog.dismiss();
                    deleteGoal(item);
                });
            }
        });

        dialog.show();
    }

    private void deleteGoal(GoalWithReminder item) {
        long goalId = item.goal.id;

        Executors.newSingleThreadExecutor().execute(() -> {//טרד משני ולא ראשי
            dao.deleteGoalWithReminder(goalId);//מחיקה מהמסד נתונים(ברקע)
            List<GoalWithReminder> refreshed = dao.getGoalsWithReminders();//שולף מחדש את הרשימה המעודכנת
            runOnUiThread(() -> adapter.setItems(refreshed));//חוזרים לטרד הראשי ומציגים את העדכון החדש של המסך אחרי המחיקה
        });
    }
}
