package com.example.joinme.ViewModel;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.example.joinme.Model.Category;
import com.example.joinme.Model.Group;
import com.example.joinme.Model.User;
import com.example.joinme.Model.api.RetrofitClient;
import com.example.joinme.R;
import com.example.joinme.databinding.ActivityGroupDetailsBinding;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class GroupDetailsActivity extends AppCompatActivity {
    private ActivityGroupDetailsBinding binding;
    private GoogleSignInClient mGoogleSignInClient;     //A client for interacting with the Google Sign In API.
    private FirebaseAuth firebaseAuth;
    private static final int RC_SIGN_IN = 100;              //Request code used to invoke sign in user interactions.
    private static final String TAG = "GOOGLE_SIGN_IN_TAG";
    public String title;
    public int min_participants;
    public int num_of_participant;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        /** onCreate() is called when the when the activity is first created.
         *  @param  savedInstanceState –is a reference to the Bundle object that is passed to the onCreate method of each Android activity.
         *                             Activities have the ability, under special circumstances,to restore themselves to a previous state
         *                             using the data stored in this package.
         */
        super.onCreate(savedInstanceState);
        binding = ActivityGroupDetailsBinding.inflate(getLayoutInflater());//Using this function this binding variable can be used to access GUI components.
        setContentView(binding.getRoot());                                //Set the activity content to an explicit view.
        //init firebase auth
        firebaseAuth = FirebaseAuth.getInstance();
        mGoogleSignInClient = GoogleSignIn.getClient(this, MainActivity.googleSignInOptions);
        String id = getIntent().getStringExtra("ID");
        getDetailsFromDb(id);


        //handle click, back
        binding.backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent thisIntent = getIntent();
                String from = thisIntent.getStringExtra("from");
                if(from.equals("RelevantGroups")){
                    Intent intent = new Intent(GroupDetailsActivity.this, RelevantGroupsActivity.class);
                    intent.putExtra("Title", title);
                    intent.putExtra("City", "");
                    startActivity(intent);
                }
                else if(from.equals("Map")){
                    Intent intent = new Intent(GroupDetailsActivity.this, SearchOnMapActivity.class);
                    intent.putExtra("Title", title);
                    startActivity(intent);
                }
                else{
                    Intent intent = new Intent(GroupDetailsActivity.this, GroupSuggestionActivity.class);
                    intent.putExtra("Title", thisIntent.getStringExtra("Title"));
                    startActivity(intent);
                }

                finish();
            }
        });

        binding.joinbttn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String groupID = id;
                Call<ResponseBody> call = RetrofitClient.getInstance().getAPI().addUserToGroup(groupID, firebaseAuth.getUid());
                call.enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                        if (response.code() == 200) {
                            Toast.makeText(GroupDetailsActivity.this, "Joined group successfully", Toast.LENGTH_SHORT).show();
                            Log.d("Add user to group", "Success");
                            Call<Group> call2 = RetrofitClient.getInstance().getAPI().getGroupDetails(groupID);
                            if(min_participants == num_of_participant+1){
                                call2.enqueue(new Callback<Group>() {
                                    @Override
                                    public void onResponse(Call<Group> call, Response<Group> response) {
                                        Group group = response.body();
                                        String head_id = group.getHead_of_group();
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
                            else if(min_participants < num_of_participant+1) {
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
                            Intent intent = new Intent(GroupDetailsActivity.this, GroupSuggestionActivity.class);
                            intent.putExtra("Title", title);
                            startActivity(intent);
                        } else {
                            Toast.makeText(GroupDetailsActivity.this, "You are already in this group", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<ResponseBody> call, Throwable t) {
                        Log.d("Add user to group", "Fail");
                    }
                });
            }
        });
    }

    // menu
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()){
            case R.id.item1:
                return true;
            case R.id.subitem1:
                startActivity(new Intent(GroupDetailsActivity.this, HistoryCreatedActivity.class));
                return true;
            case R.id.subitem2:
                startActivity(new Intent(GroupDetailsActivity.this, HistoryJoinedActivity.class));
                return true;
            case R.id.item2:
                startActivity(new Intent(GroupDetailsActivity.this, UpdateDetailsActivity.class));
                return true;
            case R.id.item3:
                firebaseAuth.signOut();
                mGoogleSignInClient.signOut();
                startActivity(new Intent(GroupDetailsActivity.this, MainActivity.class));
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

    //get all the details of the group and present them
    private void getDetailsFromDb(String id){
        Call<Group> call = RetrofitClient.getInstance().getAPI().getGroupDetails(id);
        call.enqueue(new Callback<Group>() {
            @Override
            public void onResponse(Call<Group> call, Response<Group> response) {
                binding.titleTxt.setText(response.body().getTitle());
                title = response.body().getTitle();
                binding.addressTxt.setText(response.body().getAddress());
                binding.dateTxt.setText(response.body().getDate());
                binding.timeTxt.setText(response.body().getTime());
                binding.numPartTxt.setText("Current number of participants in the group: " + response.body().getNum_of_participant());
                binding.headTxt.setText("The head of the group is:\n " + response.body().getHead_of_group());
                min_participants = response.body().getMin_participants();
                num_of_participant = response.body().getNum_of_participant();
            }

            @Override
            public void onFailure(Call<Group> call, Throwable t) {
                Log.d("fail", t.getMessage());
            }
        });
    }


}
