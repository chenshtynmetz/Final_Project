//used this video https://www.youtube.com/watch?v=gD9uQf5UU-g&ab_channel=AtifPervaiz

package com.example.joinme;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.example.joinme.databinding.ActivityMainBinding;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    FirebaseFirestore db = FirebaseFirestore.getInstance();
    private static final int RC_SIGN_IN = 100;
    private GoogleSignInClient googleSignInClient;
    public static GoogleSignInOptions googleSignInOptions;
    private FirebaseAuth firebaseAuth;

    private static final String TAG = "GOOGLE_SIGN_IN_TAG";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        //confirm google signin
        googleSignInOptions = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, googleSignInOptions);

        //init firebase auth
        firebaseAuth = FirebaseAuth.getInstance();
        checkUser();

        //google SignInButton: Click to begin Google SignIn
        binding.googleSignInBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //begin google sign in
                Log.d(TAG, "onClick: begin Google SignIn");
                Intent intent = googleSignInClient.getSignInIntent();
                startActivityForResult(intent, RC_SIGN_IN);
            }
        });
    }

    private void checkUser() {
        //if user is already signed in then go to main page
        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
        if(firebaseUser != null){
            String email = firebaseUser.getEmail();
            String uid = firebaseUser.getUid();
            //admin logged in
            if(email.equals("jiayhb@gmail.com")){
                Log.d(TAG, "onSuccess: Admin logged in...\n"+email);
                Toast.makeText(MainActivity.this, "Admin logged in...\n"+email, Toast.LENGTH_SHORT).show();
                //start profile activity
                startActivity(new Intent(MainActivity.this, AdminMainPageActivity.class));
                finish();
            }
            else {
                blocked_user(uid);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        //result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if(requestCode == RC_SIGN_IN){
            Log.d(TAG, "onActivityResult: Google Signin intent result");
            Task<GoogleSignInAccount> accountTask = GoogleSignIn.getSignedInAccountFromIntent(data);
            try{
                //Google sign in success, now auth with firebase
                GoogleSignInAccount account = accountTask.getResult(ApiException.class);
                firebaseAuthWithGoogleAccount(account);
            }
            catch (Exception e){
                //failed google sign in
                Log.d(TAG, "onActivityResult: "+e.getMessage());
            }
        }
    }

    private void firebaseAuthWithGoogleAccount(GoogleSignInAccount account) {
        Log.d(TAG, "firebaseAuthWithGoogleAccount: begin firebase auth with google account");
        AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
        firebaseAuth.signInWithCredential(credential)
                .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                    @Override
                    public void onSuccess(AuthResult authResult) {
                        //login success
                        Log.d(TAG, "onSuccess: Logged In");

                        //get logged user
                        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
                        //get user info
                        String uid = firebaseUser.getUid();
                        String email = firebaseUser.getEmail();

                        Log.d(TAG, "onSuccess: Email: "+email);
                        Log.d(TAG, "onSuccess: UID: "+uid);

                        //admin logged in
                        if(email.equals("jiayhb@gmail.com")){
                            Log.d(TAG, "onSuccess: Admin logged in...\n"+email);
                            Toast.makeText(MainActivity.this, "Admin logged in...\n"+email, Toast.LENGTH_SHORT).show();
                            //start profile activity
                            startActivity(new Intent(MainActivity.this, AdminMainPageActivity.class));
                            finish();
                        }

                        //check if user is new or existing
                        else if(authResult.getAdditionalUserInfo().isNewUser()){
                            //user is new- account created
                            Log.d(TAG, "onSuccess: Account Created...\n"+email);
                            Toast.makeText(MainActivity.this, "Account Created...\n"+email, Toast.LENGTH_SHORT).show();
                            //start profile activity
                            startActivity(new Intent(MainActivity.this, FillDetailsActivity.class));
                            finish();
                        }
                        else {
                            //existing account
                            blocked_user(uid);
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        //login failed
                        Log.d(TAG, "onFailure: Loggin failed "+e.getMessage());
                    }
                });
    }

    private void blocked_user(String uid) {
        db.collection("blockUsers").document(uid).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                DocumentSnapshot document = task.getResult();
                if (task.isSuccessful()) {
                    if (document.exists()) {
                        Log.d(TAG, document.getId());
                        Toast.makeText(MainActivity.this, "You are blocked", Toast.LENGTH_SHORT).show();
                        firebaseAuth.signOut();
                        googleSignInClient.signOut();
                    } else {
                        //existing account
                        //start profile activity
                        startActivity(new Intent(MainActivity.this, MainPageActivity.class));
                        finish();
                    }
                } else {
                    Log.d(TAG, "Failed with: ", task.getException());
                }
            }
        });
    }
}
