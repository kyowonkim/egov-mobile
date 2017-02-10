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
import android.os.Bundle;
import android.support.v7.widget.CardView;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import com.google.gson.Gson;

import org.egovernments.egoverp.R;
import org.egovernments.egoverp.api.ApiController;
import org.egovernments.egoverp.config.Config;
import org.egovernments.egoverp.helper.AppUtils;
import org.egovernments.egoverp.models.PaymentHistoryRequest;
import org.egovernments.egoverp.models.PropertyTaxCallback;
import org.egovernments.egoverp.models.PropertyViewRequest;
import org.egovernments.egoverp.models.TaxDetail;
import org.egovernments.egoverp.models.TaxOwnerDetail;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;

import static org.egovernments.egoverp.config.Config.REFERER_IP_CONFIG_KEY;

public class PropertyTaxViewActivity extends BaseActivity {

    Button btnBreakups;
    List<TaxDetail> listBreakups;
    CardView propertyTaxDetailsView;
    CardView paymentCardView;
    ScrollView scrollViewPropertyTax;
    EditText etAmountToPay;
    EditText etMobileNo;
    EditText etMailAddress;
    boolean isVacantLand=false;
    double arrearsTotal=0, arrearsPenalty=0, currentTotal=0, currentPenalty=0, total =0;
    String referrerIp;
    int ulbCode;
    String category;
    Call<PropertyTaxCallback> propertyTaxCallbackCall;
    Button paymentHistoryViewButton, btnPay;
    String assessmentNo;
    private TextView tvAssessmentNo;
    private TextView tvAddress;
    private TextView tvOwnerNamePhone;
    private TextView tvArrearsTotal, tvArrearsPenalty, tvCurrentTotal, tvCurrentPenalty, tvTotal;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.view_propertytax);
        setContentView(R.layout.activity_propertytax_view);


        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        referrerIp=getIntent().getStringExtra(REFERER_IP_CONFIG_KEY);

        listBreakups= new ArrayList<>();
        progressBar = (ProgressBar) findViewById(R.id.propertytax_progressbar);

        btnPay = (Button) findViewById(R.id.btnPay);
        btnPay.setVisibility(View.GONE);

        btnPay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(TextUtils.isEmpty(etMobileNo.getText()) || TextUtils.isEmpty(etAmountToPay.getText()) || TextUtils.isEmpty(etMailAddress.getText()))
                {
                    showSnackBar(getString(R.string.please_fill_payment_details));
                    return;
                }
                else if(!AppUtils.isValidEmail(etMailAddress.getText().toString()))
                {
                    showSnackBar(getString(R.string.please_enter_valid_email));
                    return;
                }
                else if(etMobileNo.getText().toString().length()<10)
                {
                    showSnackBar(getString(R.string.please_enter_valid_mobile_no));
                    return;
                }

                int amountToPay= Integer.parseInt(etAmountToPay.getText().toString());
                if(amountToPay<=0)
                {
                    showSnackBar(getString(R.string.payment_amount_greater_than_0));
                }
                else if(amountToPay> total)
                {
                    showSnackBar(getString(R.string.payment_amount_should_not_greater) + "(" + Math.round(total) + ")");
                }
                else
                {

                    String paymentGatewayUrl=isVacantLand? configManager.getString(Config.APP_PAYMENT_GATEWAY_VACANTLAND_TAX)
                            :configManager.getString(Config.APP_PAYMENT_GATEWAY_PROPERTY_TAX);
                    paymentGatewayUrl=sessionManager.getBaseURL()+paymentGatewayUrl;

                    paymentGatewayUrl=paymentGatewayUrl.replace("{assessmentNo}", tvAssessmentNo.getText().toString());
                    paymentGatewayUrl=paymentGatewayUrl.replace("{ulbCode}", String.valueOf(ulbCode));
                    paymentGatewayUrl=paymentGatewayUrl.replace("{amountToPay}", String.valueOf(amountToPay));
                    paymentGatewayUrl=paymentGatewayUrl.replace("{mobileNo}", etMobileNo.getText().toString());
                    paymentGatewayUrl=paymentGatewayUrl.replace("{emailId}", etMailAddress.getText().toString());

                    Intent intent=new Intent(PropertyTaxViewActivity.this, PaymentGatewayActivity.class);
                    intent.putExtra(PaymentGatewayActivity.PAYMENT_GATEWAY_URL, paymentGatewayUrl);
                    startActivityForResult(intent,1);
                }

            }
        });

        scrollViewPropertyTax = (ScrollView) findViewById(R.id.scrollviewpropertytax);

        propertyTaxDetailsView = (CardView)findViewById(R.id.propertypropertytax_layout);
        propertyTaxDetailsView.setVisibility(View.GONE);

        paymentCardView = (CardView)findViewById(R.id.cvPayment);
        paymentCardView.setVisibility(View.GONE);

        tvAssessmentNo = (TextView) findViewById(R.id.propertytax_assessmentno);
        tvAddress = (TextView) findViewById(R.id.propertytax_address);
        tvOwnerNamePhone = (TextView) findViewById(R.id.propertytax_ownernamecontact);

        tvArrearsTotal = (TextView) findViewById(R.id.propertytax_arrears_total);
        tvArrearsPenalty = (TextView) findViewById(R.id.propertytax_arrears_penalty);
        tvCurrentTotal = (TextView) findViewById(R.id.propertytax_current_total);
        tvCurrentPenalty = (TextView) findViewById(R.id.propertytax_current_penalty);
        tvTotal = (TextView) findViewById(R.id.propertytax_total);

        btnBreakups=(Button)findViewById(R.id.btnbreakups);
        paymentHistoryViewButton = (Button) findViewById(R.id.btnViewPaymentHistory);

        etAmountToPay=(EditText) findViewById(R.id.etAmount);
        etMobileNo=(EditText) findViewById(R.id.etMobileNo);
        etMailAddress=(EditText)findViewById(R.id.etMail);

        sessionManager = AppUtils.getSessionManger(getApplicationContext());

        btnBreakups.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                Intent intentViewBreakup=new Intent(PropertyTaxViewActivity.this, PropertyTaxBreakupsDetails.class);
                intentViewBreakup.putExtra("breakupsList", new Gson().toJson(listBreakups));
                startActivity(intentViewBreakup);
            }
        });

        paymentHistoryViewButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intentPaymentHistory = new Intent(PropertyTaxViewActivity.this, PaymentHistoryActivity.class);
                intentPaymentHistory.putExtra(PaymentHistoryActivity.REFERRER_IP, referrerIp);
                intentPaymentHistory.putExtra(PaymentHistoryActivity.SERVICE_NAME,
                        PaymentHistoryRequest.ServiceName.PROPERTY_TAX.name());
                intentPaymentHistory.putExtra(PaymentHistoryActivity.CONSUMER_CODE, assessmentNo);

                startActivity(intentPaymentHistory);
            }
        });


        if (getSupportActionBar() != null)
            getSupportActionBar().setElevation(0);


        isVacantLand=getIntent().getBooleanExtra(PropertyTaxSearchActivity.IS_VACANT_LAND, false);
        if(isVacantLand)
        {
            getSupportActionBar().setTitle(R.string.view_vacantlandtax);
            category=PropertyTaxSearchActivity.VLT_CATEGORY_VALUE;
        }
        else
        {
            category=PropertyTaxSearchActivity.PT_CATEGORY_VALUE;
        }

        //load assessment details from intent param
        ulbCode=getIntent().getIntExtra(SearchResultActivity.ULB_CODE, 0);
        assessmentNo = getIntent().getStringExtra(SearchResultActivity.ASSESSMENT_NO);
        submit(assessmentNo, category);

    }

    private void submit(final String code, final String category) {

        propertyTaxDetailsView.setVisibility(View.GONE);
        paymentCardView.setVisibility(View.GONE);
        btnPay.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);

        if (code.length() < 10) {
            showSnackBar(getString(R.string.assement_no_10_chars));
            progressBar.setVisibility(View.GONE);
            return;
        }

        propertyTaxCallbackCall = ApiController.getRetrofit2API(getApplicationContext())
                .getPropertyTax(referrerIp, new PropertyViewRequest(String.format(Locale.getDefault(), "%04d", ulbCode), code, category));

        propertyTaxCallbackCall.enqueue(new retrofit2.Callback<PropertyTaxCallback>() {
            @Override
            public void onResponse(Call<PropertyTaxCallback> call, retrofit2.Response<PropertyTaxCallback> response) {
                showPropertyTaxDetails(response);
            }

            @Override
            public void onFailure(Call<PropertyTaxCallback> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                listBreakups.clear();
            }
        });
    }

    private void showPropertyTaxDetails(retrofit2.Response<PropertyTaxCallback> response) {
        PropertyTaxCallback propertyTaxCallback = response.body();

        if (propertyTaxCallback.getErrorDetail().getErrorMessage().equals("SUCCESS")) {

            tvAssessmentNo.setText(propertyTaxCallback.getAssessmentNo());
            tvAddress.setText(propertyTaxCallback.getPropertyAddress());

            String ownersMobileNos = "";
            int check = 0;

            for (TaxOwnerDetail taxOwnerDetail : propertyTaxCallback.getTaxOwnerDetails()) {

                if (check > 0) {
                    ownersMobileNos += ", ";
                }

                if (check == 0) {
                    if (!TextUtils.isEmpty(taxOwnerDetail.getMobileNo())) {
                        etMobileNo.setText(taxOwnerDetail.getMobileNo());
                    }
                }

                ownersMobileNos += taxOwnerDetail.getOwnerName() + (TextUtils.isEmpty(taxOwnerDetail.getMobileNo()) ? "" : "/" + taxOwnerDetail.getMobileNo());
                check++;
            }

            arrearsTotal = 0;
            arrearsPenalty = 0;
            currentTotal = 0;
            currentPenalty = 0;
            total = 0;

            for (TaxDetail taxDetail : propertyTaxCallback.getTaxDetails()) {
                if (getCurrentFinancialYearInstallments().contains(taxDetail.getInstallment())) {
                    currentTotal = currentTotal + taxDetail.getTaxAmount();
                    currentPenalty = currentPenalty + taxDetail.getPenalty();
                } else {
                    arrearsTotal += taxDetail.getTaxAmount();
                    arrearsPenalty += taxDetail.getPenalty();
                }
            }

            total = arrearsTotal + arrearsPenalty + currentPenalty + currentTotal;

            NumberFormat nf1 = NumberFormat.getInstance(new Locale("hi", "IN"));
            //nf1.setMinimumFractionDigits(2);
            // nf1.setMaximumFractionDigits(2);

            tvArrearsTotal.setText(getString(R.string.rupee_value, nf1.format(arrearsTotal)));
            tvArrearsPenalty.setText(getString(R.string.rupee_value, nf1.format(arrearsPenalty)));
            tvCurrentTotal.setText(getString(R.string.rupee_value, nf1.format(currentTotal)));
            tvCurrentPenalty.setText(getString(R.string.rupee_value, nf1.format(currentPenalty)));
            tvTotal.setText(getString(R.string.rupee_value, nf1.format(Math.round(total))));

            if (total > 0) {
                etAmountToPay.setText(String.valueOf(Math.round(total)));
                if (TextUtils.isEmpty(etMobileNo.getText())) {
                    etMobileNo.setText(sessionManager.getMobile());
                }

                if (!TextUtils.isEmpty(sessionManager.getEmail())) {
                    etMailAddress.setText(sessionManager.getEmail());
                }

                btnPay.setVisibility(View.VISIBLE);
                paymentCardView.setVisibility(View.VISIBLE);
            } else {
                float scale = getResources().getDisplayMetrics().density;
                int dpAsPixels = (int) (10 * scale + 0.5f);
                scrollViewPropertyTax.setPadding(0, 0, 0, dpAsPixels);
                btnBreakups.setVisibility(View.GONE);
            }

            tvOwnerNamePhone.setText(ownersMobileNos);
            listBreakups = propertyTaxCallback.getTaxDetails();

            listBreakups.add(new TaxDetail(total));

            propertyTaxDetailsView.setVisibility(View.VISIBLE);
            propertyTaxDetailsView.requestFocus();

        } else {
            showSnackBar(propertyTaxCallback.getErrorDetail().getErrorMessage());
            listBreakups.clear();
        }
        progressBar.setVisibility(View.GONE);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                this.finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public ArrayList<String> getCurrentFinancialYearInstallments()
    {

        ArrayList<String> currentYearInstallments=new ArrayList<>();

        List<Integer> firstInstallment= Arrays.asList(3, 4, 5, 6, 7, 8);
        //List<Integer> secondInstallment=Arrays.asList(9, 10, 11, 0, 1, 2);

        Calendar now = Calendar.getInstance();
        int currentMonth=now.get(Calendar.MONTH);
        int currentYear=now.get(Calendar.YEAR);
        int nextYear=currentYear+1;
        int prevYear=currentYear-1;
        if(firstInstallment.contains(currentMonth))
        {
            currentYearInstallments.add(currentYear+"-"+nextYear+"-1");
            currentYearInstallments.add(currentYear+"-"+nextYear+"-2");
        }
        else
        {
            if(currentMonth<=2)
            {
                currentYearInstallments.add(prevYear+"-"+currentYear+"-1");
                currentYearInstallments.add(prevYear+"-"+currentYear+"-2");
            }
            else
            {
                currentYearInstallments.add(currentYear+"-"+nextYear+"-1");
                currentYearInstallments.add(currentYear+"-"+nextYear+"-2");
            }
        }

        return currentYearInstallments;

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        submit(getIntent().getStringExtra("assessmentNo"), category);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (propertyTaxCallbackCall != null && !propertyTaxCallbackCall.isCanceled())
            propertyTaxCallbackCall.cancel();
    }
}

