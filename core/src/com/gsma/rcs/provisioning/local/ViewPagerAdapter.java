
package com.gsma.rcs.provisioning.local;

import com.gsma.rcs.provider.settings.RcsSettings;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class ViewPagerAdapter extends FragmentPagerAdapter {

    private final static int PAGE_COUNT = 5;
    private final RcsSettings mRcsSettings;
    private final String[] mTitles;
    private Map<String, IProvisioningFragment> fragments;

    public ViewPagerAdapter(FragmentManager fm, String[] titles, RcsSettings rcsSettings) {
        super(fm);
        mTitles = titles;
        mRcsSettings = rcsSettings;
        fragments = new HashMap<>();
    }

    @Override
    public Fragment getItem(int position) {
        switch (position) {
            case 0:
                ProfileProvisioning profileFragment = ProfileProvisioning.newInstance(mRcsSettings);
                fragments.put(ProfileProvisioning.class.getName(), profileFragment);
                return profileFragment;

            case 1:
                StackProvisioning stackFragment = StackProvisioning.newInstance(mRcsSettings);
                fragments.put(StackProvisioning.class.getName(), stackFragment);
                return stackFragment;

            case 2:
                ServiceProvisioning serviceFragment = ServiceProvisioning.newInstance(mRcsSettings);
                fragments.put(ServiceProvisioning.class.getName(), serviceFragment);
                return serviceFragment;

            case 3:
                CapabilitiesProvisioning capaFragment = CapabilitiesProvisioning.newInstance(mRcsSettings);
                fragments.put(CapabilitiesProvisioning.class.getName(), capaFragment);
                return capaFragment;

            case 4:
                LoggerProvisioning loggerFragment = LoggerProvisioning.newInstance(mRcsSettings);
                fragments.put(LoggerProvisioning.class.getName(), loggerFragment);
                return loggerFragment;
        }
        return null;
    }

    public CharSequence getPageTitle(int position) {
        return mTitles[position];
    }

    @Override
    public int getCount() {
        return PAGE_COUNT;
    }

    public Collection<IProvisioningFragment> getFragments() {
        return fragments.values();
    }
}
