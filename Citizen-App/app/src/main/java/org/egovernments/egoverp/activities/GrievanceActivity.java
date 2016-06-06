/*
 * ******************************************************************************
 *  eGov suite of products aim to improve the internal efficiency,transparency,
 *      accountability and the service delivery of the government  organizations.
 *
 *        Copyright (C) <2016>  eGovernments Foundation
 *
 *        The updated version of eGov suite of products as by eGovernments Foundation
 *        is available at http://www.egovernments.org
 *
 *        This program is free software: you can redistribute it and/or modify
 *        it under the terms of the GNU General Public License as published by
 *        the Free Software Foundation, either version 3 of the License, or
 *        any later version.
 *
 *        This program is distributed in the hope that it will be useful,
 *        but WITHOUT ANY WARRANTY; without even the implied warranty of
 *        MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *        GNU General Public License for more details.
 *
 *        You should have received a copy of the GNU General Public License
 *        along with this program. If not, see http://www.gnu.org/licenses/ or
 *        http://www.gnu.org/licenses/gpl.html .
 *
 *        In addition to the terms of the GPL license to be adhered to in using this
 *        program, the following additional terms are to be complied with:
 *
 *    	1) All versions of this program, verbatim or modified must carry this
 *    	   Legal Notice.
 *
 *    	2) Any misrepresentation of the origin of the material is prohibited. It
 *    	   is required that all modified versions of this material be marked in
 *    	   reasonable ways as different from the original version.
 *
 *    	3) This license does not grant any rights to any user of the program
 *    	   with regards to rights under trademark law for use of the trade names
 *    	   or trademarks of eGovernments Foundation.
 *
 *      In case of any queries, you can reach eGovernments Foundation at contact@egovernments.org.
 *  *****************************************************************************
 */

package org.egovernments.egoverp.activities;


import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.egovernments.egoverp.R;
import org.egovernments.egoverp.fragments.GrievanceFragment;
import org.egovernments.egoverp.network.ApiController;
import org.egovernments.egoverp.network.ApiUrl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

/**
 * The activity containing grievance list
 **/


public class GrievanceActivity extends BaseActivity {

    public static int ACTION_UPDATE_REQUIRED = 111;
    ViewPager viewPager;
    ProgressBar pbHome;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentViewWithTabs(R.layout.activity_grievance);
        viewPager=(ViewPager)findViewById(R.id.viewPager);
        pbHome=(ProgressBar)findViewById(R.id.pbhome);

        com.melnykov.fab.FloatingActionButton newComplaintButtonCompat = (com.melnykov.fab.FloatingActionButton) findViewById(R.id.list_fabcompat);

        View.OnClickListener onClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Activity refreshes if NewGrievanceActivity finishes with success
                startActivityForResult(new Intent(GrievanceActivity.this, NewGrievanceActivity.class), ACTION_UPDATE_REQUIRED);

            }
        };
        newComplaintButtonCompat.setOnClickListener(onClickListener);
        loadGrievanceCategories();
    }

    public void highlightTabTextView(View view, boolean isSelected)
    {
        if(view!=null) {
            TextView tabTextView = (TextView) view.findViewById(R.id.title);
            if (!isSelected) {
                tabTextView.setTextColor(Color.WHITE);
                tabTextView.setAlpha(0.5f);
            } else {
                tabTextView.setTextColor(Color.WHITE);
                tabTextView.setAlpha(1f);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_grievance_home, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if(id == R.id.action_refresh)
        {
            loadGrievanceCategories();
        }

        return super.onOptionsItemSelected(item);
    }

    //Handles result when NewGrievanceActivity finishes
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == ACTION_UPDATE_REQUIRED && resultCode == RESULT_OK) {
           loadGrievanceCategories();
        }

    }

    public void loadGrievanceCategories()
    {
        showLoader();

        ApiController.getAPI(GrievanceActivity.this).getComplaintCategoriesWithCount(sessionManager.getAccessToken(), new Callback<JsonObject>() {
            @Override
            public void success(JsonObject jsonObject, Response response) {

                loadViewPager(jsonObject.get("result").getAsJsonObject());

            }

            @Override
            public void failure(RetrofitError error) {
                Toast.makeText(getApplicationContext(), error.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
            }
        });

    }

    public void showLoader()
    {
        viewPager.setVisibility(View.GONE);
        pbHome.setVisibility(View.VISIBLE);
    }

    public void hideLoader()
    {
        viewPager.setVisibility(View.VISIBLE);
        pbHome.setVisibility(View.GONE);
    }

    public void loadViewPager(JsonObject categories)
    {

        hideLoader();

        int selectedIdx=-1;

        if(viewPager.getAdapter()!=null)
        {
            selectedIdx=viewPager.getCurrentItem();
        }

        GrievanceFragmentPagerAdapter pagerAdapter=new GrievanceFragmentPagerAdapter(getSupportFragmentManager(), categories);
        viewPager.setAdapter(pagerAdapter);
        tabLayout.removeAllTabs();
        tabLayout.setupWithViewPager(viewPager);
        setTabCount(pagerAdapter);
        viewPager.setOffscreenPageLimit(viewPager.getAdapter().getCount()-1);

        if(selectedIdx!=-1)
        {
            viewPager.setCurrentItem(selectedIdx);
            return;
        }

        tabLayout.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                highlightTabTextView(tab.getCustomView(), true);
                viewPager.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                highlightTabTextView(tab.getCustomView(), false);
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });

    }

    void setTabCount(GrievanceFragmentPagerAdapter pagerAdapter)
    {
        //tabLayout.setTabTextColors(Color.parseColor("#000"), Color.parseColor("#fff"));
        for (int i = 0; i < tabLayout.getTabCount(); i++) {
            TabLayout.Tab tab = tabLayout.getTabAt(i);
            View view = tab.getCustomView();
            if(view !=null)
            {
                view = pagerAdapter.getTabView(i);
            }
            else {
                tab.setCustomView(pagerAdapter.getTabView(i));
            }
        }
        highlightTabTextView(tabLayout.getTabAt(0).getCustomView(), true);
    }

    private class GrievanceFragmentPagerAdapter extends FragmentPagerAdapter {

        JsonObject categories;
        List<String> titles;

        public GrievanceFragmentPagerAdapter(FragmentManager fm, JsonObject categories) {
            super(fm);
            this.categories=categories;
            titles=new ArrayList<>();
            for (Map.Entry<String, JsonElement> e : categories.entrySet()) {
                titles.add(e.getKey());
            }
        }

        @Override
        public int getCount() {
            return titles.size();
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return titles.get(position);
        }

        @Override
        public Fragment getItem(int position) {
            return GrievanceFragment.instantiateItem(sessionManager.getAccessToken(), titles.get(position), position, sessionManager.getBaseURL()+ ApiUrl.COMPLAINT_DOWNLOAD_IMAGE);
        }

        public View getTabView(int position) {
            String currentKey=titles.get(position).toString();
            View v = LayoutInflater.from(getApplicationContext()).inflate(R.layout.tab_default_layout, null);
            TextView tvTitle = (TextView) v.findViewById(R.id.title);
            tvTitle.setText(currentKey);
            TextView tvCount = (TextView) v.findViewById(R.id.count);
            tvCount.setText(categories.get(currentKey).toString());
            return v;
        }

    }


}

