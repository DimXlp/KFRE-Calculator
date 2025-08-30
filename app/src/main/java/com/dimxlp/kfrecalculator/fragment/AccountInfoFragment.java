package com.dimxlp.kfrecalculator.fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.dimxlp.kfrecalculator.databinding.FragmentAccountInfoBinding;
import com.dimxlp.kfrecalculator.util.UserPrefs;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class AccountInfoFragment extends Fragment {

    private static final String TAG = "RAFI|AccountInfoFragment";
    private FragmentAccountInfoBinding binding;

    private String userRole;
    private String userEmail;
    private String userFirstName;
    private String userLastName;
    private String userClinic;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Retrieve data from the arguments bundle
        if (getArguments() != null) {
            userFirstName = getArguments().getString("USER_FIRST_NAME", "");
            userLastName = getArguments().getString("USER_LAST_NAME", "");
            userEmail = getArguments().getString("USER_EMAIL", "");
            userRole = getArguments().getString("USER_ROLE", "Individual");
            userClinic = getArguments().getString("USER_CLINIC", "");
            Log.d(TAG, "User data received from arguments.");
        } else {
            Log.w(TAG, "Fragment arguments are null. Using default values.");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentAccountInfoBinding.inflate(inflater, container, false);
        Log.d(TAG, "View created with final modern design.");
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        populateUserDetails();

        // Show the clinic card only if the user is a doctor
        if ("Doctor".equalsIgnoreCase(userRole)) {
            Log.d(TAG, "User is a Doctor. Showing clinic card.");
            binding.clinicCard.setVisibility(View.VISIBLE);
        } else {
            Log.d(TAG, "User is an Individual. Hiding clinic card.");
            binding.clinicCard.setVisibility(View.GONE);
        }

        binding.saveAccountButton.setOnClickListener(v -> saveChanges());
    }

    private void populateUserDetails() {
        binding.accountFirstName.setText(userFirstName);
        binding.accountLastName.setText(userLastName);
        binding.accountEmail.setText(userEmail);
        binding.accountRole.setText(userRole);
        binding.accountClinic.setText(userClinic);
    }

    private void saveChanges() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(getContext(), "Error: Not logged in.", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Cannot save changes, user is not authenticated.");
            return;
        }

        String userId = currentUser.getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        String firstName = Objects.requireNonNull(binding.accountFirstName.getText()).toString().trim();
        String lastName = Objects.requireNonNull(binding.accountLastName.getText()).toString().trim();

        // Create a map to hold the data for updating
        Map<String, Object> updates = new HashMap<>();
        if (!firstName.isEmpty()) updates.put("firstName", firstName);
        if (!lastName.isEmpty()) updates.put("lastName", lastName);
        if (!firstName.isEmpty() && !lastName.isEmpty()) updates.put("fullName", firstName + " " + lastName);

        // Only add the clinic field if the user is a doctor
        if ("Doctor".equalsIgnoreCase(userRole)) {
            String clinic = Objects.requireNonNull(binding.accountClinic.getText()).toString().trim();
            if (!clinic.isEmpty()) updates.put("clinic", clinic);
            Log.i(TAG, "Updating clinic: " + clinic);
        }

        if (updates.isEmpty()) {
            Toast.makeText(getContext(), "Nothing to update.", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.i(TAG, "Attempting to update user document: " + userId);

        // Update the document in the "Users" collection
        db.collection("Users").document(userId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "User document successfully updated.");
                    Toast.makeText(getContext(), "Profile Updated!", Toast.LENGTH_SHORT).show();

                    FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
                    String email = u != null ? u.getEmail() : null;

                    String roleLower = userRole != null ? userRole.toLowerCase(Locale.ROOT) : "individual";

                    // Only keep clinic for doctors
                    String clinicToSave = "doctor".equals(roleLower)
                            ? Objects.requireNonNull(binding.accountClinic.getText()).toString().trim()
                            : null;

                    UserPrefs.save(
                            requireContext(),
                            firstName,
                            lastName,
                            email,
                            roleLower,
                            clinicToSave
                    );
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating user document", e);
                    Toast.makeText(getContext(), "Error: Could not update profile.", Toast.LENGTH_SHORT).show();
                });


        Toast.makeText(getContext(), "User information updated!", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null; // Important to avoid memory leaks
    }
}