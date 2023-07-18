package com.example.joinme.ViewModel;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.example.joinme.Model.Contact;
import com.example.joinme.Model.Group;
import com.example.joinme.Model.Logic;
import com.example.joinme.Model.api.RetrofitClient;
import com.example.joinme.R;
import com.example.joinme.databinding.ActivityGroupSuggestionBinding;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.Arrays;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class GroupSuggestionActivity extends AppCompatActivity implements RecycleViewInterface {

    private ActivityGroupSuggestionBinding binding;
    private GoogleSignInClient mGoogleSignInClient;     //A client for interacting with the Google Sign In API.
    private FirebaseAuth firebaseAuth;
    private static final int RC_SIGN_IN = 100;              //Request code used to invoke sign in user interactions.
    private static final String TAG = "GOOGLE_SIGN_IN_TAG";
    ArrayList<Contact> contacts = new ArrayList<>();
    Logic logic = new Logic();
    ContactsAdapter adapter = new ContactsAdapter(GroupSuggestionActivity.this, contacts, GroupSuggestionActivity.this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        /** onCreate() is called when the when the activity is first created.
         *  @param  savedInstanceState –is a reference to the Bundle object that is passed to the onCreate method of each Android activity.
         *                             Activities have the ability, under special circumstances,to restore themselves to a previous state
         *                             using the data stored in this package.
         */
        super.onCreate(savedInstanceState);
        binding = ActivityGroupSuggestionBinding.inflate(getLayoutInflater()); //Using this function this binding variable can be used to access GUI components.

        //init firebase auth
        firebaseAuth = FirebaseAuth.getInstance();
        mGoogleSignInClient = GoogleSignIn.getClient(this, MainActivity.googleSignInOptions);
        setContentView(binding.getRoot());
        setUpContact();

        //handle click, back
        binding.backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(GroupSuggestionActivity.this, MainPageActivity.class));
            }
        });

    }

    // menu
    @Override
    public boolean onOptionsItemSelected(@androidx.annotation.NonNull MenuItem item) {
        switch (item.getItemId()){
            case R.id.item1:
                return true;
            case R.id.subitem1:
                startActivity(new Intent(GroupSuggestionActivity.this, HistoryCreatedActivity.class));
                return true;
            case R.id.subitem2:
                startActivity(new Intent(GroupSuggestionActivity.this, HistoryJoinedActivity.class));
                return true;
            case R.id.item2:
                startActivity(new Intent(GroupSuggestionActivity.this, UpdateDetailsActivity.class));
                return true;
            case R.id.item3:
                firebaseAuth.signOut();
                mGoogleSignInClient.signOut();
                startActivity(new Intent(GroupSuggestionActivity.this, MainActivity.class));
                return true;
            default: return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.our_menu, menu);
        return true;
    }

    private void setUpContact(){
        Intent intent = getIntent();
        String title = intent.getStringExtra("Title");
        String [] today = new String[3];

        //if true it means the device running the app has Android SDK 26 or up
        //otherwise- the SDK version is lower than 26. (SDK 25 or lower)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            today = java.time.LocalDate.now().toString().split("-");
            Log.d(TAG, "the date is: " + Arrays.toString(today));
        }
        String[] finalToday = today;
        Call<ArrayList<Contact>> call = RetrofitClient.getInstance().getAPI().getRelatedGroups(title);
        call.enqueue(new Callback<ArrayList<Contact>>() {
            @Override
            public void onResponse(Call<ArrayList<Contact>> call, Response<ArrayList<Contact>> response) {
                Log.d( "/////",response.body().toString());
//                if(response.body().isEmpty()){
//                    startActivity(new Intent(GroupSuggestionActivity.this, MainPageActivity.class));
//                }
                presentRelevantGroups(response.body(), finalToday);
            }

            @Override
            public void onFailure(Call<ArrayList<Contact>> call, Throwable t) {
                Log.d("Relevant groups", "fail");
            }
        });

    }

    @Override
    public void onDetailsClick(int position) {
        Intent thisIntent = getIntent();
        Intent intent = new Intent(GroupSuggestionActivity.this, GroupDetailsActivity.class);
        intent.putExtra("ID", contacts.get(position).getId());
        intent.putExtra("Title",thisIntent.getStringExtra("Title"));
        intent.putExtra("from", "GroupSuggestion");
        startActivity(intent);
    }

    @Override
    public void onJoinClick(int position) {
        String groupID = contacts.get(position).getId();
        Call<ResponseBody> call = RetrofitClient.getInstance().getAPI().addUserToGroup(groupID, firebaseAuth.getUid());
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.code() == 200) {
                    Toast.makeText(GroupSuggestionActivity.this, "Joined group successfully", Toast.LENGTH_SHORT).show();
                    Log.d("Add user to group", "Success");
                    Call<Group> call2 = RetrofitClient.getInstance().getAPI().getGroupDetails(groupID);
                    if(contacts.get(position).getMin_participants() == contacts.get(position).getNum_of_participant()+1){
                        call2.enqueue(new Callback<Group>() {
                            @Override
                            public void onResponse(Call<Group> call, Response<Group> response) {
                                Group group = response.body();
                                String head_id = group.getHead_of_group(); //todo: add notification
                                Call<ResponseBody> whatsappCall = RetrofitClient.getInstance().getAPI().openWhatsappGroup(groupID);
                                whatsappCall.enqueue(new Callback<ResponseBody>() {
                                    @Override
                                    public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                                        Log.d("done", "done");
                                    }

                                    @Override
                                    public void onFailure(Call<ResponseBody> call, Throwable t) {
                                        Log.d("fail", t.getMessage());
                                    }
                                });

                            }

                            @Override
                            public void onFailure(Call<Group> call, Throwable t) {
                                Log.d("fail", t.getMessage());
                            }
                        });
                    }
                    else if(contacts.get(position).getMin_participants() < contacts.get(position).getNum_of_participant()+1){
                        Call<ResponseBody> call3 = RetrofitClient.getInstance().getAPI().joinToWhatsappGroup(groupID, firebaseAuth.getUid());
                        call3.enqueue(new Callback<ResponseBody>() {
                            @Override
                            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                                Log.d("join", "you join to whatsapp group");
                            }

                            @Override
                            public void onFailure(Call<ResponseBody> call, Throwable t) {
                                Log.d("fail", t.getMessage());
                            }
                        });
                    }

                } else {
                    Toast.makeText(GroupSuggestionActivity.this, "You are already in this group", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.d("Add user to group", "Fail");
            }
        });


    }

    @Override
    public void onDeleteClick(int position) {

    }

    @Override
    public void onReportClick(int position) {

    }

    @Override
    public void onHappenedClick(int position, Boolean flag) {

    }

    //Displaying the list in which the relevant groups appear
    private void presentRelevantGroups(ArrayList<Contact> response, String[] finalToday){
        RecyclerView recyclerView = findViewById(R.id.rvBox);
        TextView nonResultTxt = findViewById(R.id.nonResultTxt);
        if (response.isEmpty()){
            nonResultTxt.setText("No matching groups were found");
        }
        for (Contact contact : response) {
            String [] groupDate = contact.getDate().split("/|\\ ");
            //Checking if it is possible to add without exceeding the conditions set for the group
            if(contact.getMax_participants() > contact.getNum_of_participant() &&
                    logic.checkDate(Integer.parseInt(finalToday[0]), Integer.parseInt(finalToday[1]), Integer.parseInt(finalToday[2]),
                            Integer.parseInt(groupDate[2]), Integer.parseInt(groupDate[1]), Integer.parseInt(groupDate[0]))) {
                contacts.add(contact);
            }
        }

        if(contacts.isEmpty()){
//            startActivity(new Intent(GroupSuggestionActivity.this, MainPageActivity.class));
            nonResultTxt.setText("No matching groups were found");
        }

        //Here we need to create the adapter which will actually populate the data into the RecyclerView.
        // The adapter's role is to convert an object at a position into a list row item to be inserted.
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(GroupSuggestionActivity.this));

    }
}