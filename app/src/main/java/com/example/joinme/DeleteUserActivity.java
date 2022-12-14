package com.example.joinme;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.nfc.Tag;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import com.example.joinme.databinding.ActivityDeleteUserBinding;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class DeleteUserActivity extends AppCompatActivity implements RecycleViewInterface{

    FirebaseFirestore db = FirebaseFirestore.getInstance();
    //view binding
    private ActivityDeleteUserBinding binding;
    private GoogleSignInClient mGoogleSignInClient;
    private FirebaseAuth firebaseAuth;
    private static final int RC_SIGN_IN = 100;
    private static final String TAG = "DELETE_USER_TAG";
    List<UserRow> userRows = new ArrayList<>();
    private SearchView searchView;
    RecyclerView recyclerView;
    DeleteUserAdapter adapter = new DeleteUserAdapter(this, userRows, DeleteUserActivity.this);
    androidx.constraintlayout.widget.ConstraintLayout parent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDeleteUserBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        //init firebase auth
        firebaseAuth = FirebaseAuth.getInstance();
        mGoogleSignInClient = GoogleSignIn.getClient(this, MainActivity.googleSignInOptions);
        parent = findViewById(R.id.delPage);
        searchView = findViewById(R.id.searchV);
        searchView.clearFocus();
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
               fileList(newText);
                return true;
            }
        });

        recyclerView = findViewById(R.id.usersRcv);
        setUpUserRows();

        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));


        //handle click, logout
        binding.logoutBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                firebaseAuth.signOut();
                mGoogleSignInClient.signOut();
                Intent signInIntent = mGoogleSignInClient.getSignInIntent();
                //starting the activity for result
                startActivityForResult(signInIntent, RC_SIGN_IN);
            }
        });
        //handle click, back
        binding.backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(DeleteUserActivity.this, AdminMainPageActivity.class));
            }
        });
    }

    private void fileList(String text) {
        List<UserRow> filteredList = new ArrayList<>();
        for(UserRow user: userRows){
            if(user.getName().toLowerCase().contains(text.toLowerCase())){
                filteredList.add(user);
                Log.d(TAG, user.getName());
            }
        }
        if(filteredList.isEmpty()){
            Toast.makeText(this, "No users found", Toast.LENGTH_SHORT).show();
        }
        else{
            adapter.setFilteredList(filteredList);

        }
    }

    private void setUpUserRows(){
        db.collection("usersById").get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            RecyclerView recyclerView = findViewById(R.id.usersRcv);
                            if (task.getResult().isEmpty()){
                                TextView nonResultTxt = findViewById(R.id.nonResultTxt);
                                nonResultTxt.setText("No matching users were found");
                            }
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                userRows.add(new UserRow(document.getString("name"), document.getString("mail"),document.getId()));
                                Log.d(TAG, document.getId() + " => " + document.getData());
                            }
                            recyclerView.setAdapter(adapter);
                            recyclerView.setLayoutManager(new LinearLayoutManager(DeleteUserActivity.this));
                        } else {
                            Log.d(TAG, "Error getting documents: ", task.getException());
                        }
                    }
                });
    }

    @Override
    public void onDetailsClick(int position) {

    }

    @Override
    public void onJoinClick(int position) {

    }

    @Override
    public void onDeleteClick(int position) {
        onButtonShowPopupWindowClick(position);
    }

    public void onButtonShowPopupWindowClick(int pos) {

        // inflate the layout of the popup window
        LayoutInflater inflater = (LayoutInflater)
                getSystemService(LAYOUT_INFLATER_SERVICE);
        View popupView = inflater.inflate(R.layout.delete_user_popup, null);
        TextView tvMsg = popupView.findViewById(R.id.msgTxt);
        Button yesBtn = popupView.findViewById(R.id.yesBtn);
        Button noBtn = popupView.findViewById(R.id.noBtn);
        tvMsg.setText("Are you sure you want to delete " + userRows.get(pos).getName() + "?");
        // create the popup window
        int width = LinearLayout.LayoutParams.WRAP_CONTENT;
        int height = LinearLayout.LayoutParams.WRAP_CONTENT;
        boolean focusable = true; // lets taps outside the popup also dismiss it
        final PopupWindow popupWindow = new PopupWindow(popupView, width, height, focusable);

        // show the popup window
        // which view you pass in doesn't matter, it is only used for the window tolken
        popupWindow.showAtLocation(parent, Gravity.CENTER, 0, 0);
        yesBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                popupWindow.dismiss();
                String curr_uid = userRows.get(pos).getUid();
                Log.d(TAG, "yes");
                db.collection("usersById").document(userRows.get(pos).getUid()).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    DocumentSnapshot document = task.getResult();
                    if(document.exists()){
                        User block_user = document.toObject(User.class);
                        Log.d(TAG, "the user= " + block_user.toString());
                        db.collection("blockUsers").document(curr_uid).set(block_user);
                        }
                    }
                });
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                db.collection("usersById").document(curr_uid).delete();
                userRows.remove(pos);
                    }
                });

        noBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "no");
                popupWindow.dismiss();

            }
        });

    }



}