package com.stuartfuljourneys.expensesartfulb;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class Expenses_LastTransactions_Activity extends BaseActivity {

    // UI Elements
    private DrawerLayout drawerLayout;
    private NavigationView navView;
    private MaterialToolbar topAppBar;
    private Spinner spinnerPeriodType, spinnerPeriodValue, spinnerCategory, spinnerSharedStatus, spinnerHouseholdMember;
    private RecyclerView recyclerViewTransactions;

    // Firebase
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String householdId;

    // Components & Data Lists
    private TransactionAdapter transactionAdapter;
    private List<Expense> expenseList = new ArrayList<>();
    private List<String> householdMemberNames = new ArrayList<>();
    private boolean isInitialSetup = true; // Safety flag

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_expenses_lasttransactions);

        // Standard setup
        drawerLayout = findViewById(R.id.drawerLayout);
        navView = findViewById(R.id.nav_view);
        topAppBar = findViewById(R.id.topAppBar);
        setupNavigation();
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Find all spinners
        spinnerPeriodType = findViewById(R.id.spinnerPeriodType);
        spinnerPeriodValue = findViewById(R.id.spinnerPeriodValue);
        spinnerCategory = findViewById(R.id.spinnerCategory);
        spinnerSharedStatus = findViewById(R.id.spinnerSharedStatus);
        spinnerHouseholdMember = findViewById(R.id.spinnerHouseholdMember);
        recyclerViewTransactions = findViewById(R.id.recyclerViewTransactions);

        // Setup RecyclerView
        setupRecyclerView();

        // Start the data loading process
        fetchHouseholdIdAndSetupSpinners();
    }

    private void setupRecyclerView() {
        recyclerViewTransactions.setLayoutManager(new LinearLayoutManager(this));
        transactionAdapter = new TransactionAdapter(expenseList);
        recyclerViewTransactions.setAdapter(transactionAdapter);
    }

    private void fetchHouseholdIdAndSetupSpinners() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) { return; }

        db.collection("users").document(currentUser.getUid()).get()
                .addOnSuccessListener(userDocument -> {
                    if (userDocument.exists()) {
                        this.householdId = userDocument.getString("household");
                        if (this.householdId != null && !this.householdId.isEmpty()) {
                            setupAllSpinners();
                        }
                    }
                });
    }

    private void setupAllSpinners() {
        setupStaticSpinners();

        // Fetch UIDs, then resolve them to nicknames
        db.collection("households").document(householdId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        List<String> memberUids = (List<String>) documentSnapshot.get("members");
                        if (memberUids != null) {
                            resolveUidsToNicknames(memberUids);
                        }
                    }
                });
    }

    /**
     * Takes the list of UIDs and fetches the nickname for each one.
     */
    private void resolveUidsToNicknames(List<String> memberUids) {
        List<Task<DocumentSnapshot>> tasks = new ArrayList<>();
        for (String uid : memberUids) {
            tasks.add(db.collection("users").document(uid).get());
        }

        // Tasks.whenAllSuccess will wait for all the user lookups to complete
        Tasks.whenAllSuccess(tasks).addOnSuccessListener(results -> {
            householdMemberNames.clear();
            householdMemberNames.add("All Members"); // Add default option

            for (Object result : results) {
                DocumentSnapshot userSnapshot = (DocumentSnapshot) result;
                if (userSnapshot.exists()) {
                    String nickname = userSnapshot.getString("nickname");
                    if (nickname != null) {
                        householdMemberNames.add(nickname);
                    }
                }
            }

            // Now that we have the real nicknames, set up the final spinner
            setupHouseholdSpinner();
        });
    }

    private void setupStaticSpinners() {
        ArrayAdapter<CharSequence> typeAdapter = ArrayAdapter.createFromResource(this, R.array.period_types, android.R.layout.simple_spinner_item);
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPeriodType.setAdapter(typeAdapter);

        ArrayAdapter<CharSequence> categoryAdapter = ArrayAdapter.createFromResource(this, R.array.expense_categories, android.R.layout.simple_spinner_item);
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(categoryAdapter);

        ArrayAdapter<CharSequence> sharedAdapter = ArrayAdapter.createFromResource(this, R.array.shared_status_options, android.R.layout.simple_spinner_item);
        sharedAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSharedStatus.setAdapter(sharedAdapter);
    }

    private void setupHouseholdSpinner() {
        ArrayAdapter<String> memberAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, householdMemberNames);
        memberAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerHouseholdMember.setAdapter(memberAdapter);
        spinnerHouseholdMember.setSelection(0, false);

        // Now that ALL spinners have adapters, set up the listeners
        setupAllListeners();

        // Flip the safety flag and trigger the first data load
        isInitialSetup = false;
        updatePeriodValueSpinner(spinnerPeriodType.getSelectedItem().toString());
    }

    private void setupAllListeners() {
        AdapterView.OnItemSelectedListener listener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (parent.getId() == R.id.spinnerPeriodType) {
                    updatePeriodValueSpinner(parent.getItemAtPosition(position).toString());
                }
                loadDataBasedOnFilters();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        };

        spinnerPeriodType.setOnItemSelectedListener(listener);
        spinnerPeriodValue.setOnItemSelectedListener(listener);
        spinnerCategory.setOnItemSelectedListener(listener);
        spinnerSharedStatus.setOnItemSelectedListener(listener);
        spinnerHouseholdMember.setOnItemSelectedListener(listener);
    }

    private void updatePeriodValueSpinner(String periodType) {
        ArrayAdapter<CharSequence> valueAdapter;
        if (periodType.equals("Transactions")) {
            valueAdapter = ArrayAdapter.createFromResource(this, R.array.transaction_values, android.R.layout.simple_spinner_item);
        } else if (periodType.equals("Days")) {
            valueAdapter = ArrayAdapter.createFromResource(this, R.array.time_values_days, android.R.layout.simple_spinner_item);
        } else {
            valueAdapter = ArrayAdapter.createFromResource(this, R.array.time_values, android.R.layout.simple_spinner_item);
        }
        valueAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPeriodValue.setAdapter(valueAdapter);

        if (periodType.equals("Transactions")) {
            spinnerPeriodValue.setSelection(1, false);
        }
    }

    private void loadDataBasedOnFilters() {
        if (isInitialSetup) {
            return;
        }

        String type = spinnerPeriodType.getSelectedItem().toString();
        int value = Integer.parseInt(spinnerPeriodValue.getSelectedItem().toString());
        String category = spinnerCategory.getSelectedItem().toString();
        String sharedStatus = spinnerSharedStatus.getSelectedItem().toString();
        String selectedMember = spinnerHouseholdMember.getSelectedItem().toString();

        Query query = db.collection("expenses").whereEqualTo("householdId", this.householdId);

        if (!category.equals("All Categories")) {
            query = query.whereEqualTo("category", category);
        }
        if (!sharedStatus.equals("All")) {
            boolean isShared = sharedStatus.equals("Shared");
            query = query.whereEqualTo("isShared", isShared);
        }
        if (!selectedMember.equals("All Members")) {
            query = query.whereEqualTo("paidBy_nickname", selectedMember);
        }

        if (type.equals("Transactions")) {
            query = query.orderBy("timestamp", Query.Direction.DESCENDING).limit(value);
        } else {
            Calendar cal = Calendar.getInstance();
            if (type.equals("Days")) { cal.add(Calendar.DAY_OF_YEAR, -value); }
            else if (type.equals("Weeks")) { cal.add(Calendar.WEEK_OF_YEAR, -value); }
            else if (type.equals("Months")) { cal.add(Calendar.MONTH, -value); }
            Date startDate = cal.getTime();
            query = query.whereGreaterThanOrEqualTo("timestamp", startDate)
                    .orderBy("timestamp", Query.Direction.DESCENDING);
        }

        query.get().addOnSuccessListener(queryDocumentSnapshots -> {
            expenseList.clear();
            for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                expenseList.add(document.toObject(Expense.class));
            }
            transactionAdapter.notifyDataSetChanged();
            Log.d("TransactionsActivity", "Successfully loaded " + expenseList.size() + " transactions.");
        }).addOnFailureListener(e -> {
            Log.e("TransactionsActivity", "Error loading transactions. Check Firestore index requirements.", e);
            Toast.makeText(this, "Failed to load. The database might need a new index.", Toast.LENGTH_LONG).show();
        });
    }
}
