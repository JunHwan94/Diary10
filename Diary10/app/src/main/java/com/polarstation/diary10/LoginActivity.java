package com.polarstation.diary10;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.databinding.DataBindingUtil;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.Profile;
import com.facebook.login.LoginResult;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.polarstation.diary10.databinding.ActivityLoginBinding;
import com.polarstation.diary10.model.UserModel;

public class LoginActivity extends BaseActivity implements GoogleApiClient.OnConnectionFailedListener, View.OnClickListener {
    private ActivityLoginBinding binding;
    private FirebaseAuth authInstance;
    private FirebaseAuth.AuthStateListener authStateListener;
    private FirebaseDatabase dbInstance;
    private String uid;
    private boolean isInFdb;

    private CallbackManager callbackManager;

    private int SIGN_IN_REQUEST_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FirebaseApp.initializeApp(this);
        dbInstance = FirebaseDatabase.getInstance();
        binding = DataBindingUtil.setContentView(this, R.layout.activity_login);

        authInstance = FirebaseAuth.getInstance();
        authStateListener = (FirebaseAuth firebaseAuth) -> {
            FirebaseUser user = firebaseAuth.getCurrentUser();
            if (user != null) {
                //로그인
                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP|Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
            } else {
                //로그아웃
                firebaseAuth.signOut();
            }
        };

        binding.loginActivityGoogleLoginButton.setOnClickListener(this);
        setFacebookLogIn();

        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},  1);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        callbackManager.onActivityResult(requestCode, resultCode, data);
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == SIGN_IN_REQUEST_CODE){
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                if(account != null) firebaseAuthWithGoogle(account);
            }catch(ApiException e){
                Log.w("LoginActivity", e.getStatusCode()+"");
            }
        }
    }

    private void googleSignIn(){
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build();
        GoogleSignInClient googleSignInClient = GoogleSignIn.getClient(this, gso);

        Intent intent = googleSignInClient.getSignInIntent();
        startActivityForResult(intent, SIGN_IN_REQUEST_CODE);
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount account){
        AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
        String userName = account.getDisplayName();
        String profileImageUrl = String.valueOf(account.getPhotoUrl());
        processCredential(credential, userName, profileImageUrl);
    }

    private void setFacebookLogIn(){
        // 페이스북 로그인
        callbackManager = CallbackManager.Factory.create();
        binding.loginActivityFacebookLoginButton.setReadPermissions(getString(R.string.email));
        binding.loginActivityFacebookLoginButton.setLoginText(getString(R.string.log_in));
        binding.loginActivityFacebookLoginButton.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                handleFacebookAccessToken(loginResult.getAccessToken());
            }

            @Override
            public void onCancel() {

            }

            @Override
            public void onError(FacebookException error) {
//                Toast.makeText(getBaseContext(), "코드 : " + error.toString(), Toast.LENGTH_LONG).show();
                Log.w("LoginActivity", error.toString());
            }
        });
    }

    private void handleFacebookAccessToken(AccessToken token){
        AuthCredential credential = FacebookAuthProvider.getCredential(token.getToken());
        String name = Profile.getCurrentProfile().getName();
        String profileImageUrl = String.valueOf(Profile.getCurrentProfile().getProfilePictureUri(200,200));
        processCredential(credential, name, profileImageUrl);
    }

    private void processCredential(AuthCredential credential, String userName, String profileImageUrl){
        authInstance.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, R.string.auth_success, Toast.LENGTH_SHORT).show();
                        checkUid(userName, profileImageUrl);
                    } else {
                        Toast.makeText(this, R.string.auth_failed, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void checkUid(String userName, String profileImageUrl){
        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        dbInstance.getReference().child(getString(R.string.fdb_users)).orderByChild(uid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if(dataSnapshot.getChildrenCount() == 0)
                            addUserToFDB(userName, profileImageUrl);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
        });
    }

    private void addUserToFDB(String userName, String profileImageUrl){
        UserModel userModel = new UserModel.Builder()
                .setUserName(userName)
                .setProfileImageUrl(profileImageUrl)
                .setUid(uid)
                .build();
        dbInstance.getReference().child(getString(R.string.fdb_users)).child(uid).setValue(userModel);
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()){
            case R.id.loginActivity_googleLoginButton:
                googleSignIn();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        authInstance.addAuthStateListener(authStateListener);
    }

    @Override
    protected void onStop() {
        super.onStop();
        authInstance.removeAuthStateListener(authStateListener);
    }
}