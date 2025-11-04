package com.stuartfuljourneys.expensesartfulb;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class Expenses_LastTransactions_Activity extends BaseActivity {

    // --- UI Elements ---
    private Spinner spinnerPeriodType, spinnerPeriodValue, spinnerCategory, spinnerSharedStatus;
    private RecyclerView recyclerViewTransactions;

    // --- Firebase ---
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String householdId;

    // --- RecyclerView Components ---
    private TransactionAdapter transactionAdapter;
    private List<Expense> expenseList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_expenses_lasttransactions);

        // --- Setup from BaseActivity for Navigation ---
        drawerLayout = findViewById(R.id.drawerLayout);
        navView = findViewById(R.id.nav_view);
        topAppBar = findViewById(R.id.topAppBar);
        setupNavigation();

        // --- Find UI elements ---
        spinnerPeriodType = findViewById(R.id.spinnerPeriodType);
        spinnerPeriodValue = findViewById(R.id.spinnerPeriodValue);
        spinnerCategory = findViewById(R.id.spinnerCategory);
        spinnerSharedStatus = findViewById(R.id.spinnerSharedStatus);
        recyclerViewTransactions = findViewById(R.id.recyclerViewTransactions);

        // --- Initialize Firebase ---
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // --- Setup the RecyclerView ---
        setupRecyclerView();

        // --- Start the data loading process ---
        fetchHouseholdIdAndSetupFilters();
    }

    /**
     * Configures the RecyclerView with a LayoutManager and the custom Adapter.
     */
    private void setupRecyclerView() {
        recyclerViewTransactions.setLayoutManager(new LinearLayoutManager(this));
        transactionAdapter = new TransactionAdapter(expenseList);
        recyclerViewTransactions.setAdapter(transactionAdapter);
    }

    /**
     * First, gets the current user's household ID. If successful,
     * it then calls the method to set up the interactive filters.
     */
    private void fetchHouseholdIdAndSetupFilters() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "You must be logged in.", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("users").document(currentUser.getUid()).get()
                .addOnSuccessListener(userDocument -> {
                    if (userDocument.exists()) {
                        this.householdId = userDocument.getString("household");
                        if (this.householdId != null && !this.householdId.isEmpty()) {
                            // Now that we have the household ID, we can set up the spinners.
                            // The listeners inside this method will trigger the first data load.
                            setupFilterSpinners();
                        } else {
                            Toast.makeText(this, "Household not found.", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to get user details.", Toast.LENGTH_SHORT).show();
                    Log.e("TransactionsActivity", "Error getting user details", e);
                });
    }

    /**
     * Sets up the adapters and listeners for the three filter spinners.
     */
    private void setupFilterSpinners() {
        // --- Period Type Spinner (Top Left) ---
        ArrayAdapter<CharSequence> typeAdapter = ArrayAdapter.createFromResource(this,
                R.array.period_types, android.R.layout.simple_spinner_item);
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPeriodType.setAdapter(typeAdapter);

        spinnerPeriodType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedType = parent.getItemAtPosition(position).toString();
                updatePeriodValueSpinner(selectedType);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // --- Period Value Spinner (Top Right) ---
        spinnerPeriodValue.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                loadDataBasedOnFilters();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // --- Category Spinner (Full Width) ---
        ArrayAdapter<CharSequence> categoryAdapter = ArrayAdapter.createFromResource(this,
                R.array.expense_categories, android.R.layout.simple_spinner_item);
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(categoryAdapter);
        spinnerCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                loadDataBasedOnFilters();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        ArrayAdapter<CharSequence> sharedAdapter = ArrayAdapter.createFromResource(this,
                R.array.shared_status_options, android.R.layout.simple_spinner_item);
        sharedAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSharedStatus.setAdapter(sharedAdapter);
        // The default selection is "Shared" (index 0), which is exactly what you want.

        spinnerSharedStatus.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                loadDataBasedOnFilters(); // Reload data when this filter changes
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Set the initial state for the value spinner, which will trigger the first data load
        updatePeriodValueSpinner(spinnerPeriodType.getSelectedItem().toString());
    }

    /**
     * Updates the contents of the second spinner (spinnerPeriodValue)
     * based on the selection of the first spinner (spinnerPeriodType).
     */
    private void updatePeriodValueSpinner(String periodType) {
        ArrayAdapter<CharSequence> valueAdapter;
        if (periodType.equals("Transactions")) {
            valueAdapter = ArrayAdapter.createFromResource(this, R.array.transaction_values, android.R.layout.simple_spinner_item);
        } else { // For "Days", "Weeks", and "Months"
            valueAdapter = ArrayAdapter.createFromResource(this, R.array.time_values, android.R.layout.simple_spinner_item);
        }
        valueAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPeriodValue.setAdapter(valueAdapter);

        // Set a default selection. For "Transactions", we default to "20".
        if (periodType.equals("Transactions")) {
            spinnerPeriodValue.setSelection(1); // "20" is at index 1
        }
    }


    /**
     * The main data loading method. Reads the current filter selections
     * and builds and executes the appropriate Firestore query.
     */
    private void loadDataBasedOnFilters() {
        // --- PRE-FLIGHT CHECKS ---
        // First, ensure we have the householdId before attempting any query.
        if (householdId == null) {
            return; // Exit if householdId isn't ready yet
        }
        // Second, ensure all spinners have loaded their default values before proceeding.
        if (spinnerPeriodType.getSelectedItem() == null ||
                spinnerPeriodValue.getSelectedItem() == null ||
                spinnerCategory.getSelectedItem() == null) {
            return; // Exit if spinners aren't ready yet
        }

        // --- GATHER FILTER VALUES ---
        // Get the current text from each of the three spinners.
        String type = spinnerPeriodType.getSelectedItem().toString();
        int value = Integer.parseInt(spinnerPeriodValue.getSelectedItem().toString());
        String category = spinnerCategory.getSelectedItem().toString();
        String sharedStatus = spinnerSharedStatus.getSelectedItem().toString();

        // --- BUILD THE QUERY INCREMENTALLY ---

        // 1. Start with the base query. This is ALWAYS applied.
        //    It ensures we only get expenses for the current user's household.
        Query query = db.collection("expenses")
                .whereEqualTo("householdId", this.householdId);

        // ========================= CHANGE #1 (Category Filter) =========================
        // 2. Conditionally add the category filter.
        //    This part is only added to the query if the user has selected something
        //    other than the default "All Categories".
        if (!category.equals("All Categories")) {
            query = query.whereEqualTo("category", category);
            Log.d("TransactionsActivity", "Filtering by category: " + category);
        }
        // ===============================================================================

        // ========== ADDED: Add the shared status filter ===========
        if (!sharedStatus.equals("All")) {
            boolean isShared = sharedStatus.equals("Shared"); // "Shared" -> true, "Not Shared" -> false
            //Log.d("QueryDebug", "Building query with isShared = " + isShared);
            query = query.whereEqualTo("isShared", isShared);
        }

        // 3. Add the final time/limit filter and the essential ordering.
        if (type.equals("Transactions")) {
            // If filtering by a simple count, add orderBy and limit.
            Log.d("TransactionsActivity", "Loading last " + value + " transactions.");
            query = query.orderBy("timestamp", Query.Direction.DESCENDING).limit(value);
        } else {
            // If filtering by a date range, calculate the start date.
            Calendar calendar = Calendar.getInstance();
            switch (type) {
                case "Days":
                    calendar.add(Calendar.DAY_OF_YEAR, -value);
                    break;
                case "Weeks":
                    calendar.add(Calendar.WEEK_OF_YEAR, -value);
                    break;
                case "Months":
                    calendar.add(Calendar.MONTH, -value);
                    break;
            }
            Date startDate = calendar.getTime();
            Log.d("TransactionsActivity", "Loading transactions since " + startDate.toString());

            // Add the date condition and final ordering.
            query = query.whereGreaterThanOrEqualTo("timestamp", startDate)
                    .orderBy("timestamp", Query.Direction.DESCENDING);
        }

        // --- EXECUTE THE FINAL, COMPOUND QUERY ---
        // At this point, 'query' has been perfectly built with all the necessary parts.
        query.get().addOnSuccessListener(queryDocumentSnapshots -> {
            expenseList.clear(); // Clear the list before adding new results
            for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                Expense expense = document.toObject(Expense.class);
                expenseList.add(expense);
            }
            transactionAdapter.notifyDataSetChanged(); // Refresh the on-screen list
            Log.d("TransactionsActivity", "Found " + expenseList.size() + " transactions.");
        }).addOnFailureListener(e -> {
            // This listener will fire if the query fails, for example, if a
            // Firestore Index is missing for the specific query combination.
            Toast.makeText(this, "Failed to load transactions: " + e.getMessage(), Toast.LENGTH_LONG).show();
            Log.e("TransactionsActivity", "Error loading filtered transactions", e);
        });
    }

}
