package com.stuartfuljourneys.expensesartfulb;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;

public class Expenses_Enter_Activity extends BaseActivity {

    // --- UI Elements ---
    private Spinner spinnerName;
    private TextView textSelectedCategory, welcomeText, textMemberUp, textMemberDiff, debugText;
    private EditText editAmount, editDiscount, editNote;
    private Switch switchSharedExp; // RESTORED
    private ImageButton currentlySelectedIcon = null;
    private Button btnSubmit;

    // --- Firebase ---
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    // --- Data Holders ---
    private String householdId;
    private List<String> memberUids = new ArrayList<>();
    private Map<String, String> memberNicknames = new HashMap<>(); // <UID, Nickname>

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_expenses_enter);

        // --- Setup from BaseActivity ---
        drawerLayout = findViewById(R.id.drawerLayout);
        navView = findViewById(R.id.nav_view);
        topAppBar = findViewById(R.id.topAppBar);
        setupNavigation();

        // --- Find UI elements ---
        welcomeText = findViewById(R.id.textWelcome);
        spinnerName = findViewById(R.id.spinnerName);
        textSelectedCategory = findViewById(R.id.textSelectedCategory);
        textMemberUp = findViewById(R.id.textMemberUp);
        textMemberDiff = findViewById(R.id.textMemberDiff);
        editAmount = findViewById(R.id.editTextAmount);
        editDiscount = findViewById(R.id.editTextDiscount);
        editNote = findViewById(R.id.editTextNote);
        debugText = findViewById(R.id.debug_text);
        btnSubmit = findViewById(R.id.btnSubmit);
        switchSharedExp = findViewById(R.id.switchSharedExp); // RESTORED

        // --- Initialize Firebase ---
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // --- Start Loading ---
        loadInitialUserDetails();
        setupCategoryIconListeners();

        btnSubmit.setOnClickListener(v -> saveExpense());
    }

    private void loadInitialUserDetails() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "You must be logged in.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String currentUserUid = currentUser.getUid();
        Log.d("ExpenseApp", "Current User UID: " + currentUserUid);

        db.collection("users").document(currentUserUid).get()
                .addOnSuccessListener(userDocument -> {
                    if (!userDocument.exists()) {
                        Toast.makeText(this, "User profile not found.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    final String currentUserNickname = userDocument.getString("nickname");
                    final String localHouseholdId = userDocument.getString("household");

                    welcomeText.setText("Let's do this " + currentUserNickname + "!");
                    debugText.setText(String.format("Debug Info:\nUID: %s\nHousehold ID: %s", currentUserUid, localHouseholdId));

                    if (localHouseholdId == null || localHouseholdId.isEmpty()) {
                        Toast.makeText(this, "You are not part of a household.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    this.householdId = localHouseholdId;

                    db.collection("households").document(this.householdId).get()
                            .addOnSuccessListener(householdDocument -> {
                                if (!householdDocument.exists()) {
                                    Toast.makeText(this, "Household data not found for ID: " + this.householdId, Toast.LENGTH_LONG).show();
                                    return;
                                }

                                memberUids = (List<String>) householdDocument.get("members");
                                if (memberUids == null || memberUids.isEmpty()) return;

                                Log.d("ExpenseApp", "Found Member UIDs: " + memberUids.toString());

                                List<Task<DocumentSnapshot>> tasks = new ArrayList<>();
                                for (String uid : memberUids) {
                                    tasks.add(db.collection("users").document(uid).get());
                                }

                                Tasks.whenAllSuccess(tasks).addOnSuccessListener(documents -> {
                                    for (Object doc : documents) {
                                        DocumentSnapshot snapshot = (DocumentSnapshot) doc;
                                        if (snapshot.exists()) {
                                            memberNicknames.put(snapshot.getId(), snapshot.getString("nickname"));
                                        }
                                    }
                                    populateSpinner(new ArrayList<>(memberNicknames.values()), currentUserNickname);
                                    calculateAndDisplayMemberDifference(this.householdId);
                                    setupSwitchListener(); // Call this here after data is ready
                                });
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to get user details: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    Log.e("ExpenseApp", "Error fetching user document", e);
                });
    }

    private void populateSpinner(List<String> nicknames, String currentUserNickname) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.spinner_item_custom, nicknames);
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item_custom);
        spinnerName.setAdapter(adapter);

        if (currentUserNickname != null) {
            int spinnerPosition = adapter.getPosition(currentUserNickname);
            spinnerName.setSelection(spinnerPosition);
        }
    }

    private void calculateAndDisplayMemberDifference(String householdId) {
        db.collection("expenses")
                .whereEqualTo("householdId", householdId)
                .whereEqualTo("isShared", true)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Map<String, Double> totals = new HashMap<>();
                    for (String uid : memberUids) {
                        totals.put(uid, 0.0);
                    }

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String paidByUid = doc.getString("paidBy_uid");
                        Double amount = doc.getDouble("finalAmount");
                        if (paidByUid != null && amount != null && totals.containsKey(paidByUid)) {
                            totals.put(paidByUid, totals.get(paidByUid) + amount);
                        }
                    }

                    if(memberUids.size() < 2) return;

                    String member1Uid = memberUids.get(0);
                    String member2Uid = memberUids.get(1);
                    double member1Total = totals.get(member1Uid);
                    double member2Total = totals.get(member2Uid);
                    double difference = Math.abs(member1Total - member2Total);

                    if (difference < 0.01) {
                        textMemberUp.setText("Totals are even");
                        textMemberDiff.setText("$0.00");
                    } else if (member1Total > member2Total) {
                        textMemberUp.setText(memberNicknames.get(member1Uid) + " is up by:");
                        textMemberDiff.setText(String.format(Locale.US, "$%.2f", difference));
                    } else {
                        textMemberUp.setText(memberNicknames.get(member2Uid) + " is up by:");
                        textMemberDiff.setText(String.format(Locale.US, "$%.2f", difference));
                    }
                });
    }

    // RESTORED to use the standard Switch
    private void setupSwitchListener() {
        // Set the listener to hide/show the tally
        switchSharedExp.setOnCheckedChangeListener((buttonView, isChecked) -> {
            textMemberUp.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            textMemberDiff.setVisibility(isChecked ? View.VISIBLE : View.GONE);
        });
        // Default to ON and trigger the listener to set the initial UI state
        switchSharedExp.setChecked(true);
    }

    // This method is perfect and does not need to be changed
    private void setupCategoryIconListeners() {
        ImageButton iconGroceries = findViewById(R.id.icon_groceries);
        ImageButton iconEatingOut = findViewById(R.id.icon_eating_out);
        ImageButton iconUtilities = findViewById(R.id.icon_utilities);
        ImageButton iconEntertainment = findViewById(R.id.icon_entertainment);
        ImageButton iconCar = findViewById(R.id.icon_car);
        ImageButton iconHouse = findViewById(R.id.icon_house);
        ImageButton iconClothes = findViewById(R.id.icon_clothes);
        ImageButton iconAlcohol = findViewById(R.id.icon_alcohol);
        ImageButton iconHealth = findViewById(R.id.icon_health);
        ImageButton iconTech = findViewById(R.id.icon_tech);
        ImageButton iconHolidays = findViewById(R.id.icon_holidays);
        ImageButton iconOther = findViewById(R.id.icon_other);
        View.OnClickListener categoryClickListener = view -> {
            if (currentlySelectedIcon != null) {
                currentlySelectedIcon.setSelected(false);
            }
            view.setSelected(true);
            currentlySelectedIcon = (ImageButton) view;
            textSelectedCategory.setText(view.getContentDescription());
        };
        iconGroceries.setOnClickListener(categoryClickListener);
        iconEatingOut.setOnClickListener(categoryClickListener);
        iconUtilities.setOnClickListener(categoryClickListener);
        iconEntertainment.setOnClickListener(categoryClickListener);
        iconCar.setOnClickListener(categoryClickListener);
        iconHouse.setOnClickListener(categoryClickListener);
        iconClothes.setOnClickListener(categoryClickListener);
        iconAlcohol.setOnClickListener(categoryClickListener);
        iconHealth.setOnClickListener(categoryClickListener);
        iconTech.setOnClickListener(categoryClickListener);
        iconHolidays.setOnClickListener(categoryClickListener);
        iconOther.setOnClickListener(categoryClickListener);
    }

    private void saveExpense() {

        // --- 0. DISABLE THE BUTTON TO PREVENT DOUBLE TAPS ---
        btnSubmit.setEnabled(false);

        if (currentlySelectedIcon == null) {
            Toast.makeText(this, "Please select a category.", Toast.LENGTH_SHORT).show();
            btnSubmit.setEnabled(true); // Re-enable on validation failure
            return;
        }

        String amountStr = editAmount.getText().toString();
        if (amountStr.isEmpty()) {
            Toast.makeText(this, "Please enter an amount.", Toast.LENGTH_SHORT).show();
            btnSubmit.setEnabled(true); // Re-enable on validation failure
            return;
        }

        if (spinnerName.getSelectedItem() == null) {
            Toast.makeText(this, "Household members are still loading.", Toast.LENGTH_SHORT).show();
            btnSubmit.setEnabled(true); // Re-enable on validation failure
            return;
        }

        String selectedNickname = spinnerName.getSelectedItem().toString();
        String paidByUid = "";
        for (Map.Entry<String, String> entry : memberNicknames.entrySet()) {
            if (entry.getValue().equals(selectedNickname)) {
                paidByUid = entry.getKey();
                break;
            }
        }
        if (paidByUid.isEmpty()){
            Toast.makeText(this, "Could not find selected user.", Toast.LENGTH_SHORT).show();
            btnSubmit.setEnabled(true); // Re-enable on validation failure
            return;
        }

        String category = currentlySelectedIcon.getContentDescription().toString();
        double amount = Double.parseDouble(amountStr);
        String discountStr = editDiscount.getText().toString();
        double discountPercent = discountStr.isEmpty() ? 0 : Double.parseDouble(discountStr);
        double finalAmount = amount - (amount * (discountPercent / 100.0));
        String note = editNote.getText().toString();

        boolean isShared = switchSharedExp.isChecked();

        //String isSharedString = String.valueOf(isShared);

        Map<String, Object> expense = new HashMap<>();
        expense.put("householdId", this.householdId);
        expense.put("paidBy_uid", paidByUid);
        expense.put("paidBy_nickname", selectedNickname);
        expense.put("amount", amount);
        expense.put("discountPercent", discountPercent);
        expense.put("finalAmount", finalAmount);
        expense.put("category", category);
        expense.put("isShared", isShared);
        //expense.put("isShared", isSharedString);
        expense.put("note", note);
        expense.put("timestamp", FieldValue.serverTimestamp());

        db.collection("expenses").add(expense)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "Expense saved successfully!", Toast.LENGTH_SHORT).show();
                    calculateAndDisplayMemberDifference(this.householdId);
                    editAmount.setText("");
                    editDiscount.setText("");
                    editNote.setText("");
                    if (currentlySelectedIcon != null) {
                        currentlySelectedIcon.setSelected(false);
                        currentlySelectedIcon = null;
                    }
                    textSelectedCategory.setText("Select a Category");

                    // RE-ENABLE THE BUTTON FOR THE NEXT ENTRY
                    btnSubmit.setEnabled(true);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error saving expense: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    Log.e("ExpenseApp", "Error saving expense", e);
                    btnSubmit.setEnabled(true);
                });
    }
}
