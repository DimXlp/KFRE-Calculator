// adapter/ProfileViewPagerAdapter.java
package com.dimxlp.kfrecalculator.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.dimxlp.kfrecalculator.fragment.AccountInfoFragment;
import com.dimxlp.kfrecalculator.fragment.ExportFragment;
import com.dimxlp.kfrecalculator.fragment.PreferencesFragment;

public class ProfileViewPagerAdapter extends FragmentStateAdapter {

    public ProfileViewPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return new AccountInfoFragment();
            case 1:
                return new PreferencesFragment();
            case 2:
                return new ExportFragment();
            default:
                // Should never happen
                return new AccountInfoFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 3;
    }
}