package com.example.joinme.ViewModel;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupWindow;

import com.example.joinme.Model.Category;
import com.example.joinme.Model.CountCategory;
import com.example.joinme.Model.api.RetrofitClient;
import com.example.joinme.R;
import com.example.joinme.databinding.ActivityStatisticsBinding;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.AggregateQuery;
import com.google.firebase.firestore.AggregateQuerySnapshot;
import com.google.firebase.firestore.AggregateSource;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
//import com.jjoe64.graphview.GraphView;

import org.eazegraph.lib.charts.PieChart;
import org.eazegraph.lib.models.PieModel;

import java.util.ArrayList;
import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class StatisticsActivity extends AppCompatActivity {

    FirebaseFirestore db = FirebaseFirestore.getInstance();
    //view binding
    private ActivityStatisticsBinding binding;
    private GoogleSignInClient mGoogleSignInClient;
    private FirebaseAuth firebaseAuth;
    private static final int RC_SIGN_IN = 100;
    private static final String TAG = "STATISTICS_TAG";
    PieChart pieChart;
    public ArrayList<BarEntry> barArrayList;
    BarChart barChart;
    BarChart hBarChart;
    ArrayList<BarEntry> hBarArrayList;
    String[] hBarUsers;
    androidx.constraintlayout.widget.ConstraintLayout parent;
    private final String[] categories = {"Minnian", "Football", "Basketball", "Group games", "Volunteer", "Hang out"};
    private final String[] colors = {"#FFBB86FC", "#00FFFF", "#0000FF", "#00FF00", "#800000", "#FFFF00"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityStatisticsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        //init firebase auth
        firebaseAuth = FirebaseAuth.getInstance();
        mGoogleSignInClient = GoogleSignIn.getClient(this, MainActivity.googleSignInOptions);
        pieChart = findViewById(R.id.piechart);
        barChart = findViewById(R.id.barChart);
        barArrayList = new ArrayList<>();
        hBarArrayList = new ArrayList<>();
        hBarChart = findViewById(R.id.fragment_horizontalbarchart_chart);
        hBarUsers = new String[3];
        parent = findViewById(R.id.statistics_page);
        try {
            createPieChart();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        try {
            createBarChart();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        createHorizontalBarChart();

        //handle click, back
        binding.backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(StatisticsActivity.this, AdminMainPageActivity.class));
            }
        });
    }

    // menu
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()){
            case R.id.item1:
                onButtonShowPopupWindowClick();
                return true;
            case R.id.item2:
                firebaseAuth.signOut();
                mGoogleSignInClient.signOut();
                Intent signInIntent = mGoogleSignInClient.getSignInIntent();
                //starting the activity for result
                startActivityForResult(signInIntent, RC_SIGN_IN);
                startActivity(new Intent(StatisticsActivity.this, MainActivity.class));
                return true;
            default: return super.onOptionsItemSelected(item);
        }
    }
    public void onButtonShowPopupWindowClick(){
        // inflate the layout of the popup window
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        //Inflate a new view hierarchy from the specified xml resource.
        View popupView = inflater.inflate(R.layout.add_category_popup, null);
        //confirm the deletion of the user
        EditText tvCategory = popupView.findViewById(R.id.add_categoryTxt);
        Button addBtn = popupView.findViewById(R.id.addBtn);
        Button xBtn = popupView.findViewById(R.id.xBtn);


        // create the popup window
        int width = LinearLayout.LayoutParams.WRAP_CONTENT;
        int height = LinearLayout.LayoutParams.WRAP_CONTENT;
        boolean focusable = true; // lets taps outside the popup also dismiss it

        //This class represents a popup window that can be used to display an arbitrary view.
        //The popup window is a floating container that appears on top of the current activity.
        final PopupWindow popupWindow = new PopupWindow(popupView, width, height, focusable);

        // show the popup window
        // which view you pass in doesn't matter, it is only used for the window token
        popupWindow.showAtLocation(parent, Gravity.CENTER, 0, 0);
        xBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                popupWindow.dismiss();
            }
        });
        addBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String category = tvCategory.getText().toString();
                if(category.isEmpty()){
                    tvCategory.setError("please enter category");
                }
                else{
                    Call<ArrayList<Category>> call = RetrofitClient.getInstance().getAPI().getCategories();
                    call.enqueue(new Callback<ArrayList<Category>>() {
                        @Override
                        public void onResponse(Call<ArrayList<Category>> call, Response<ArrayList<Category>> response) {
                            for(int i=0; i<response.body().size(); i++){
                                Log.d("category", response.body().get(i).getName());
                                if(category.equals(response.body().get(i).getName())){
                                    Log.d("in", "hi");
                                    tvCategory.setError("this category already exist");
                                    return;
                                }
                            }
                            Call<ResponseBody> call2 = RetrofitClient.getInstance().getAPI().addCategory(category);
                            call2.enqueue(new Callback<ResponseBody>() {
                                @Override
                                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                                    Log.d("add", "add category");
                                }

                                @Override
                                public void onFailure(Call<ResponseBody> call, Throwable t) {
                                    Log.d("fail", t.getMessage());
                                }
                            });
                            popupWindow.dismiss();
                        }

                        @Override
                        public void onFailure(Call<ArrayList<Category>> call, Throwable t) {
                            Log.d("fail", t.getMessage());
                        }
                    });
                }
            }
        });
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.admin_menu, menu);
        return true;
    }

    private void createPieChart() throws InterruptedException {
        //add data to pie chart
        Call<ArrayList<CountCategory>> call = RetrofitClient.getInstance().getAPI().countCategories();
        call.enqueue(new Callback<ArrayList<CountCategory>>() {
            @Override
            public void onResponse(Call<ArrayList<CountCategory>> call, Response<ArrayList<CountCategory>> response) {
                for(int i=0; i<categories.length; i++){
                    CountCategory curr=response.body().get(i);
                    pieChart.addPieSlice(
                            new PieModel(
                                    curr.getName(),
                                    curr.getCount(),
                                    Color.parseColor(colors[i])));
                }
            }

            @Override
            public void onFailure(Call<ArrayList<CountCategory>> call, Throwable t) {
                Log.d("Fail", t.getMessage());
            }
        });

        Thread.sleep(3000);
        // To animate the pie chart
        pieChart.startAnimation();
    }

    private void createBarChart() throws InterruptedException {
        //add data to bar chart
        Call<ArrayList<ArrayList<CountCategory>>> call = RetrofitClient.getInstance().getAPI().compareHappened();
        call.enqueue(new Callback<ArrayList<ArrayList<CountCategory>>>() {
            @Override
            public void onResponse(Call<ArrayList<ArrayList<CountCategory>>> call, Response<ArrayList<ArrayList<CountCategory>>> response) {
                ArrayList<CountCategory> total = response.body().get(0);
                ArrayList<CountCategory> happened = response.body().get(1);
                for(int i=0; i<total.size(); i++){
                    barArrayList.add(new BarEntry(i, total.get(i).getCount()));
                    barArrayList.add(new BarEntry(i, happened.get(i).getCount()));
                }
                BarDataSet barDataSet = new BarDataSet(barArrayList, "Compare groups that happened and didn't happen");
                BarData barData = new BarData(barDataSet);
                barChart.setData(barData);
                barChart.invalidate();
                barDataSet.setColors(Color.BLUE, Color.CYAN, Color.BLUE, Color.CYAN,Color.BLUE, Color.CYAN,Color.BLUE, Color.CYAN,Color.BLUE, Color.CYAN,Color.BLUE, Color.CYAN);
                barDataSet.setValueTextColor(Color.BLACK);
                barDataSet.setStackLabels(categories);
                barDataSet.setValueTextSize(10f);

                //set column names
                XAxis xAxis = barChart.getXAxis();
                xAxis.setTextSize(9f);
                xAxis.setValueFormatter(new ValueFormatter() {
                    @Override
                    public String getFormattedValue(float value) {
                        return total.get((int) value).getName();
                    }
                });
                barChart.getDescription().setEnabled(true);
            }

            @Override
            public void onFailure(Call<ArrayList<ArrayList<CountCategory>>> call, Throwable t) {
                Log.d("Fail", t.getMessage());
            }
        });
    }

    private void createHorizontalBarChart() {
        /**
         * takes the number of groups the top 3 users opened and present it in horizontal bar chart
         */
        Call<ArrayList<CountCategory>> call = RetrofitClient.getInstance().getAPI().getTopUsers();
        call.enqueue(new Callback<ArrayList<CountCategory>>() {
            @Override
            public void onResponse(Call<ArrayList<CountCategory>> call, Response<ArrayList<CountCategory>> response) {
                for(int i=0; i<3; i++){
                    CountCategory curr=response.body().get(i);
                    hBarUsers[i] = curr.getName();
                    hBarArrayList.add(new BarEntry(i, curr.getCount()));
                }
                //create bar chart
                BarDataSet barDataSet = new BarDataSet(hBarArrayList, "Top 3 users by number of groups the opened");
                BarData barData = new BarData(barDataSet);
                barDataSet.setColors(ColorTemplate.COLORFUL_COLORS);
                barDataSet.setValueTextColor(Color.BLACK);
                barDataSet.setValueTextSize(10f);

                //set column names
                XAxis xAxis = hBarChart.getXAxis();
                xAxis.setValueFormatter(new com.github.mikephil.charting.formatter.IndexAxisValueFormatter(hBarUsers));

                ArrayList<IBarDataSet> dataSets = new ArrayList<>();
                dataSets.add(barDataSet);
                hBarChart.getDescription().setEnabled(true);

                hBarChart.setData(barData);
                hBarChart.invalidate();
            }

            @Override
            public void onFailure(Call<ArrayList<CountCategory>> call, Throwable t) {
                Log.d("Fail", t.getMessage());
            }
        });
    }
}