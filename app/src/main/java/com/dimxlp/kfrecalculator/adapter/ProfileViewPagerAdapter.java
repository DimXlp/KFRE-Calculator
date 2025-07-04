// adapter/ProfileViewPagerAdapter.java
package com.dimxlp.kfrecalculator.adapter;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.dimxlp.kfrecalculator.fragment.AccountInfoFragment;
//import com.dimxlp.kfrecalculator.fragment.ExportFragment;
//import com.dimxlp.kfrecalculator.fragment.PreferencesFragment;

public class ProfileViewPagerAdapter extends FragmentStateAdapter {

    private Bundle fragmentArguments;

    public void setFragmentArguments(Bundle args) {
        this.fragmentArguments = args;
    }

    public ProfileViewPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        Fragment fragment;
        switch (position) {
            case 0:
                fragment = new AccountInfoFragment();
                break;
//            case 1:
//                fragment = new PreferencesFragment();
//                break;
//            case 2:
//                fragment = new ExportFragment();
//                break;
            default:
                fragment = new AccountInfoFragment();
        }
        // Set the arguments for the fragment
        fragment.setArguments(this.fragmentArguments);
        return fragment;
    }

    @Override
    public int getItemCount() {
        return 3;
    }
}