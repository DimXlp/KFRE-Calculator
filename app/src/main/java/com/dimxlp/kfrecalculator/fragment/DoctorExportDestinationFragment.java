package com.dimxlp.kfrecalculator.fragment;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioGroup;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.dimxlp.kfrecalculator.R;
import com.dimxlp.kfrecalculator.handler.OnNextHandler;
import com.dimxlp.kfrecalculator.viewmodel.DoctorExportViewModel;

public class DoctorExportDestinationFragment extends Fragment implements OnNextHandler {
    private static final String TAG = "RAFI|DoctorExportDest";

    private DoctorExportViewModel vm;
    private RadioGroup destGroup;
    private View pickFolderBtn;

    private final ActivityResultLauncher<Intent> openDocTree =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri treeUri = result.getData().getData();
                    if (treeUri != null) {
                        // Persist read/write access
                        final int flags = (result.getData().getFlags()
                                & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION));
                        try {
                            requireContext().getContentResolver()
                                    .takePersistableUriPermission(treeUri, flags);
                        } catch (SecurityException ignored) { /* some providers may not allow */ }
                        vm.setSaveTreeUri(treeUri);
                        Log.d(TAG, "Picked folder: " + treeUri);
                        Toast.makeText(requireContext(), R.string.export_folder_chosen, Toast.LENGTH_SHORT).show();
                    }
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.i(TAG, "onCreateView()");
        View root = inflater.inflate(R.layout.fragment_doctor_export_destination, container, false);

        vm = new ViewModelProvider(requireActivity()).get(DoctorExportViewModel.class);
        destGroup = root.findViewById(R.id.destGroup);
        pickFolderBtn = root.findViewById(R.id.btnPickFolder);

        // Restore selection
        destGroup.check(vm.getDestination() == DoctorExportViewModel.Destination.SAVE ? R.id.rbSave : R.id.rbShare);
        toggleFolderPicker();

        destGroup.setOnCheckedChangeListener((g, id) -> {
            if (id == R.id.rbSave) vm.setDestination(DoctorExportViewModel.Destination.SAVE);
            else vm.setDestination(DoctorExportViewModel.Destination.SHARE);
            toggleFolderPicker();
        });

        pickFolderBtn.setOnClickListener(v -> openFolderPicker());
        return root;
    }

    private void toggleFolderPicker() {
        pickFolderBtn.setVisibility(
                vm.getDestination() == DoctorExportViewModel.Destination.SAVE ? View.VISIBLE : View.GONE
        );
    }

    private void openFolderPicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        // Optionally: start in previously chosen tree
        if (vm.getSaveTreeUri() != null) {
            intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, vm.getSaveTreeUri());
        }
        openDocTree.launch(intent);
    }

    @Override
    public boolean onNext() {
        // Optional enforcement: if saving, require a folder
        if (vm.getDestination() == DoctorExportViewModel.Destination.SAVE && vm.getSaveTreeUri() == null) {
            // You can relax this: return true to allow choosing path later
            Toast.makeText(requireContext(), R.string.export_err_folder_required, Toast.LENGTH_SHORT).show();
            Log.w(TAG, "Validation failed: SAVE selected but no folder chosen");
            return false;
        }
        Log.d(TAG, "onNext(): destination=" + vm.getDestination() + ", treeUri=" + vm.getSaveTreeUri());
        return true;
    }
}
