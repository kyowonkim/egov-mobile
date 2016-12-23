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

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import org.egovernments.egoverp.R;
import org.egovernments.egoverp.api.ApiController;
import org.egovernments.egoverp.api.ApiUrl;
import org.egovernments.egoverp.config.Config;
import org.egovernments.egoverp.helper.AppUtils;
import org.egovernments.egoverp.helper.ConfigManager;
import org.egovernments.egoverp.helper.CustomAutoCompleteTextView;
import org.egovernments.egoverp.helper.NothingSelectedSpinnerAdapter;
import org.egovernments.egoverp.models.City;
import org.egovernments.egoverp.models.District;
import org.egovernments.egoverp.services.UpdateService;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import okhttp3.HttpUrl;
import retrofit2.Call;

import static org.egovernments.egoverp.config.Config.API_MULTICITIES;

/**
 * The login screen activity
 **/

@SuppressWarnings("ALL")
public class LoginActivity extends BaseActivity {

    public static final String STARTUP_MESSAGE = "startUpMessage";
    List<District> districtsList;
    List<City> citiesList;
    ImageView imgLogo;
    List<String> districts;
    private String username;
    private String password;
    private ProgressBar progressBar;
    private FloatingActionButton loginButton;
    private com.melnykov.fab.FloatingActionButton loginButtonCompat;
    private TextView forgotLabel;
    private Button signupButton;
    private EditText username_edittext;
    private EditText password_edittext;
    private Handler handler;
    private ConfigManager configManager;
    private CustomAutoCompleteTextView cityAutocompleteTextBox;
    private CustomAutoCompleteTextView districtAutocompleteTextBox;
    private Spinner spinnerCity;
    private Spinner spinnerDistrict;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentViewWithNavBar(R.layout.activity_login, false);

        //Checks if session manager believes that the user is logged in.
        if (sessionManager.isLoggedIn()) {
            sessionManager.setAppVersionCode(AppUtils.getAppVersionCode(LoginActivity.this));
            //If user is logged in and has a stored access token, immediately login user
            if (sessionManager.getAccessToken() != (null)) {
                //Start fetching data from server
                startService(new Intent(LoginActivity.this, UpdateService.class).putExtra(UpdateService.KEY_METHOD, UpdateService.UPDATE_PROFILE));
                Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
                startActivityAnimation(intent, false);
                finish();
            } else {
                showSnackBar(getString(R.string.session_expired));
            }
        }

        String startupMessage = getIntent().getStringExtra(STARTUP_MESSAGE);

        if (!TextUtils.isEmpty(startupMessage)) {
            showSnackBar(startupMessage);
        }


        spinnerCity = (Spinner) findViewById(R.id.signin_city);
        spinnerDistrict = (Spinner) findViewById(R.id.spinner_district);

        loginButton = (FloatingActionButton) findViewById(R.id.signin_submit);
        loginButtonCompat = (com.melnykov.fab.FloatingActionButton) findViewById(R.id.signin_submit_compat);

        forgotLabel = (TextView) findViewById(R.id.signin_forgot);
        signupButton = (Button) findViewById(R.id.signin_register);

        progressBar = (ProgressBar) findViewById(R.id.loginprogressBar);

        username_edittext = (EditText) findViewById(R.id.signin_username);
        password_edittext = (EditText) findViewById(R.id.signin_password);

        cityAutocompleteTextBox = (CustomAutoCompleteTextView) findViewById(R.id.login_spinner_autocomplete);
        districtAutocompleteTextBox = (CustomAutoCompleteTextView) findViewById(R.id.autocomplete_district);

        cityAutocompleteTextBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSnackBar(getString(R.string.fetch_list_municipality));
            }
        });

        districtAutocompleteTextBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSnackBar(getString(R.string.fetch_district_list));
            }
        });

        final InputMethodManager imm = (InputMethodManager)LoginActivity.this.getSystemService(Context.INPUT_METHOD_SERVICE);

        View.OnClickListener onClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                username = username_edittext.getText().toString().trim();
                password = password_edittext.getText().toString().trim();
                City selectedCity = getCityByName(cityAutocompleteTextBox.getText().toString());
                if (validateInputFields(username, password, selectedCity) && validateInternetConnection()) {
                    loginWithUsernameAndPwd(username, password, selectedCity);
                }
            }
        };

        imgLogo=(ImageView)findViewById(R.id.applogoBig);
        imgLogo.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                sessionManager.setDemoMode(!sessionManager.isDemoMode());
                showSnackBar("Demo Mode is " + (sessionManager.isDemoMode() ? "Enabled" : "Disabled"));
                return false;
            }
        });

        //To make fab compatible in older android versions
        if (Build.VERSION.SDK_INT >= 21) {
            loginButton.setOnClickListener(onClickListener);
        } else {
            loginButton.setVisibility(View.GONE);
            loginButtonCompat.setVisibility(View.VISIBLE);
            loginButtonCompat.setOnClickListener(onClickListener);
        }

        forgotLabel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (configManager.getString(API_MULTICITIES).equals("true"))
                {
                    City selectedCity=getCityByName(cityAutocompleteTextBox.getText().toString());
                    if(!isValidDistrictAndMunicipality(selectedCity))
                    {
                        return;
                    }

                    sessionManager.setBaseURL(selectedCity.getUrl(), selectedCity.getCityName(),
                            selectedCity.getCityCode(), selectedCity.getModules()!=null?selectedCity.getModules().toString():null);
                }

                Intent intent = new Intent(LoginActivity.this, ForgotPasswordActivity.class);
                startActivity(intent);
            }
        });

        signupButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
                startActivityAnimation(intent, true);
            }
        });


       /* password_edittext.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    username = username_edittext.getText().toString().trim();
                    password = password_edittext.getText().toString().trim();
                    validateInputFields(username, password);
                    if(imm != null){
                        imm.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, 0);
                    }
                    return true;
                }
                return false;
            }
        });*/

        handler = new Handler();

        try {
            configManager = AppUtils.getConfigManager(getApplicationContext());
        } catch (IOException e) {
            e.printStackTrace();
        }

        new GetAllCitiesTask().execute();
    }

    public void showLoginProgress()
    {
        progressBar.setVisibility(View.VISIBLE);
        loginButton.setVisibility(View.GONE);
        loginButtonCompat.setVisibility(View.GONE);
        forgotLabel.setVisibility(View.INVISIBLE);
        signupButton.setVisibility(View.INVISIBLE);
    }

    public void hideLoginProgress()
    {
        progressBar.setVisibility(View.GONE);
        forgotLabel.setVisibility(View.VISIBLE);
        signupButton.setVisibility(View.VISIBLE);
        if (Build.VERSION.SDK_INT >= 21) {
            loginButton.setVisibility(View.VISIBLE);
        } else
            loginButtonCompat.setVisibility(View.VISIBLE);
    }

    //Invokes call to API
    private Boolean validateInputFields(final String username, final String password, final City selectedCity) {

            if (TextUtils.isEmpty(username) || TextUtils.isEmpty(password)) {
                showSnackBar(R.string.login_field_empty_prompt);
                progressBar.setVisibility(View.GONE);
                forgotLabel.setVisibility(View.VISIBLE);
                signupButton.setVisibility(View.VISIBLE);

                if (Build.VERSION.SDK_INT >= 21) {
                    loginButton.setVisibility(View.VISIBLE);
                } else
                    loginButtonCompat.setVisibility(View.VISIBLE);

                return false;

            } else {

                if(!isValidDistrictAndMunicipality(selectedCity))
                {
                    return false;
                }
            }

        return true;
    }

    private void startActivityAnimation(Intent intent, Boolean withAnimation) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && withAnimation)
            startActivity(intent, AppUtils.getTransitionBundle(LoginActivity.this));
        else
            startActivity(intent);
    }

    public void hideMultiCityComponents()
    {
        handler.post(new Runnable() {
            @Override
            public void run() {
                username_edittext.setBackgroundResource(R.drawable.top_edittext);
                ((LinearLayout)findViewById(R.id.multicityoptions)).setVisibility(View.GONE);

                int marginTopInDp = (int) TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP, 40, getResources()
                                .getDisplayMetrics());

                int marginBottomInDp = (int) TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP, 10, getResources()
                                .getDisplayMetrics());

                ViewGroup.MarginLayoutParams marginParams = new ViewGroup.MarginLayoutParams(imgLogo.getLayoutParams());
                marginParams.setMargins(0,marginTopInDp,0,marginBottomInDp);
                imgLogo.setLayoutParams(new LinearLayout.LayoutParams(marginParams));

            }
        });
    }

    public void loadDistrictDropdown() throws IOException
    {

        Type type = new TypeToken<List<District>>() {
        }.getType();
        HttpUrl url = HttpUrl.parse(configManager.getString(Config.API_MULTIPLE_CITIES_URL));
        districtsList = new Gson().fromJson(ApiController.getResponseFromUrl(getApplicationContext(), url)
                , type);

        if (districtsList != null) {

            districts = new ArrayList<>();

            for (int i = 0; i < districtsList.size(); i++) {
                districts.add(districtsList.get(i).getDistrictName());
            }

            loadDropdownsWithData(districts, spinnerDistrict, districtAutocompleteTextBox, true);

        } else {
            resetAndRefreshDropdownValues();
        }
    }

    @SuppressWarnings("unchecked")
    public void loadCityDropdown()
    {
        if(districtsList==null){
            return;
        }
        if(citiesList!=null){ citiesList.clear(); }

        cityAutocompleteTextBox.setHint("Loading");
        cityAutocompleteTextBox.setOnClickListener(null);
        cityAutocompleteTextBox.setAdapter(null);

        spinnerCity.setAdapter(null);

        String s = districtAutocompleteTextBox.getText().toString().toUpperCase();
        List<String> cities=new ArrayList<>();

        for (District district : districtsList) {
            if (s.equals(district.getDistrictName().toUpperCase())) {
                districtAutocompleteTextBox.setText(s.toUpperCase());
                cityAutocompleteTextBox.requestFocus();
                //noinspection unchecked
                @SuppressWarnings("unchecked") ArrayList<City> citiesArrayList=(ArrayList)district.getCities();
                citiesList= (ArrayList)citiesArrayList.clone();
                for(City city: citiesList)
                {
                    cities.add(city.getCityName());
                }
                break;
            }
        }

        loadDropdownsWithData(cities, spinnerCity, cityAutocompleteTextBox, false);
    }

    public void loadDropdownsWithData(final List<String> autocompleteList, final Spinner autoCompleteSpinner, final CustomAutoCompleteTextView autocompleteTextBox, final Boolean isDistrict)
    {
        handler.post(new Runnable() {
            @Override
            public void run() {
                ArrayAdapter<String> dropdownAdapter = new ArrayAdapter<>(LoginActivity.this, android.R.layout.simple_spinner_dropdown_item, autocompleteList);
                dropdownAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                autoCompleteSpinner.setAdapter(new NothingSelectedSpinnerAdapter(dropdownAdapter, android.R.layout.simple_spinner_dropdown_item, LoginActivity.this));
                autoCompleteSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                        if ((position - 1) > -1) {
                            if (isDistrict) {
                                autocompleteTextBox.setText(districtsList.get(position - 1).getDistrictName());
                                loadDropdownsWithData(districts, spinnerDistrict, districtAutocompleteTextBox, true);
                                loadCityDropdown();
                            } else {
                                City selectedCity = citiesList.get(position - 1);
                                autocompleteTextBox.setText(selectedCity.getCityName());
                                username_edittext.requestFocus();
                            }
                        }
                        autocompleteTextBox.dismissDropDown();
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {

                    }
                });

                ArrayAdapter<String> autoCompleteAdapter = new ArrayAdapter<>(LoginActivity.this, android.R.layout.simple_spinner_dropdown_item, autocompleteList);
                //String hintText = (autocompleteList.size() > 0 ? (isDistrict ? "District" : "Municipality") : (isDistrict ? "Districts Not Found!" : "Municipalities Not Found!"));

                autocompleteTextBox.setHint((isDistrict ? getString(R.string.district) : getString(R.string.municipality)));
                autocompleteTextBox.setCompoundDrawablesWithIntrinsicBounds((isDistrict ? R.drawable.ic_place_black_24dp : R.drawable.ic_location_city_black_24dp), 0, (autocompleteList.size() > 0 ? R.drawable.ic_keyboard_arrow_down_black_24dp : 0), 0);
                autocompleteTextBox.setOnClickListener(null);
                autocompleteTextBox.setAdapter(autoCompleteAdapter);
                autocompleteTextBox.setThreshold(1);
                autocompleteTextBox.setDrawableClickListener(new CustomAutoCompleteTextView.DrawableClickListener() {
                    @Override
                    public void onClick(DrawablePosition target) {
                        if (target == DrawablePosition.RIGHT) {
                            autoCompleteSpinner.performClick();
                        }
                    }
                });

                autocompleteTextBox.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                    }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                        if (isDistrict) {
                            if (citiesList != null) {
                                citiesList.clear();
                            }
                            cityAutocompleteTextBox.setText("");
                            cityAutocompleteTextBox.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_location_city_black_24dp, 0, 0, 0);
                        }
                    }

                    @Override
                    public void afterTextChanged(Editable s) {

                    }
                });

                autocompleteTextBox.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                    @Override
                    public void onFocusChange(final View v, boolean hasFocus) {

                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                if (!v.hasFocus()) {
                                    if (isDistrict && !TextUtils.isEmpty(districtAutocompleteTextBox.getText().toString())) {
                                        loadCityDropdown();
                                    }
                                }
                            }
                        });

                    }
                });

                autocompleteTextBox.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        if (isDistrict) {
                            loadCityDropdown();
                        }
                    }
                });

            }
        });
    }

    public boolean isValidDistrictAndMunicipality(City selectedCity)
    {
        if (selectedCity == null && configManager.getString(API_MULTICITIES).equals("true")) {

            String errorMsg = (TextUtils.isEmpty(cityAutocompleteTextBox.getText().toString()) ? getString(R.string.please_select_your_district_municipality) : getString(R.string.municipality_not_found));
            showSnackBar(errorMsg);
            CustomAutoCompleteTextView controlToFocus = (TextUtils.isEmpty(districtAutocompleteTextBox.getText().toString()) ? districtAutocompleteTextBox : cityAutocompleteTextBox);
            controlToFocus.requestFocus();
            return false;
        }
        return true;
    }

    public City getCityByName(String cityName)
    {
        if(citiesList!=null) {
            for (City city : citiesList) {
                if (cityName.equals(city.getCityName())) {
                    return city;
                }
            }
        }
        return null;
    }

    public void resetAndRefreshDropdownValues()
    {
        handler.post(new Runnable() {
            @Override
            public void run() {
                districtAutocompleteTextBox.setOnClickListener(null);
                districtAutocompleteTextBox.setHint(getString(R.string.loading_failed));
                districtAutocompleteTextBox.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_place_black_24dp, 0, R.drawable.ic_refresh_black_24dp, 0);
                districtAutocompleteTextBox.setDrawableClickListener(new CustomAutoCompleteTextView.DrawableClickListener() {
                    @Override
                    public void onClick(DrawablePosition target) {
                        if (target == DrawablePosition.RIGHT) {
                            districtAutocompleteTextBox.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_place_black_24dp, 0, 0, 0);
                            districtAutocompleteTextBox.setDrawableClickListener(null);
                            districtAutocompleteTextBox.setHint(getString(R.string.loading_label));
                            new GetAllCitiesTask().execute();
                        }
                    }
                });

                cityAutocompleteTextBox.setOnClickListener(null);
                cityAutocompleteTextBox.setHint(R.string.loading_failed);
                cityAutocompleteTextBox.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_location_city_black_24dp, 0, R.drawable.ic_refresh_black_24dp, 0);
                cityAutocompleteTextBox.setDrawableClickListener(new CustomAutoCompleteTextView.DrawableClickListener() {
                    @Override
                    public void onClick(DrawablePosition target) {
                        if (target == DrawablePosition.RIGHT) {
                            loadCityDropdown();
                        }
                    }
                });
            }
        });
    }

    public void loadDropdowns()
    {
        try {

            if (configManager.getString(API_MULTICITIES).equals("false")) {
                hideMultiCityComponents();
            } else {
                loadDistrictDropdown();
            }
        } catch (IOException e) {
            e.printStackTrace();
            resetAndRefreshDropdownValues();
        }
    }

    public void loginWithUsernameAndPwd(final String username, final String password, final City selectedCity) {

        if (configManager.getString(API_MULTICITIES).equals("true"))
            sessionManager.setBaseURL(selectedCity.getUrl(), selectedCity.getCityName(),
                    selectedCity.getCityCode(), selectedCity.getModules() != null ? selectedCity.getModules().toString() : null);

        showLoginProgress();

        Call<JsonObject> login = ApiController.getRetrofit2API(getApplicationContext(), selectedCity.getUrl())
                .login(ApiUrl.AUTHORIZATION, username, "read write", password, "password");

        login.enqueue(new retrofit2.Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, retrofit2.Response<JsonObject> response) {

                if (response.isSuccessful()) {

                    JsonObject jsonObject = response.body();

                    //Stores access token in session manager
                    sessionManager.loginUser(username, password, AppUtils.getNullAsEmptyString(jsonObject.get("name")),
                            AppUtils.getNullAsEmptyString(jsonObject.get("mobileNumber")), AppUtils.getNullAsEmptyString(jsonObject.get("emailId")),
                            jsonObject.get("access_token").getAsString(), jsonObject.get("cityLat").getAsDouble(), jsonObject.get("cityLng").getAsDouble());
                    startService(new Intent(LoginActivity.this, UpdateService.class).putExtra(UpdateService.KEY_METHOD, UpdateService.UPDATE_ALL));

                    Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
                    startActivityAnimation(intent, true);

                    finish();
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                showSnackBar(t.getLocalizedMessage());
                hideLoginProgress();
            }
        });

    }

    class GetAllCitiesTask extends AsyncTask<String, Integer, Object> {
        @Override
        protected Object doInBackground(String... params) {
            loadDropdowns();
            return null;
        }
    }
}


