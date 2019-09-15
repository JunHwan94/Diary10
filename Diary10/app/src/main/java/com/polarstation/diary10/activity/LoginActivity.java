package com.polarstation.diary10.activity;

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
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.Profile;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.polarstation.diary10.R;
import com.polarstation.diary10.databinding.ActivityLoginBinding;
import com.polarstation.diary10.model.UserModel;
import com.polarstation.diary10.util.NetworkStatus;

import org.json.JSONObject;

import java.security.MessageDigest;

import static com.polarstation.diary10.util.NetworkStatus.TYPE_CONNECTED;

public class LoginActivity extends BaseActivity implements GoogleApiClient.OnConnectionFailedListener, View.OnClickListener {
    private ActivityLoginBinding binding;
    private FirebaseAuth authInstance;
    private FirebaseAuth.AuthStateListener authStateListener;
    private FirebaseDatabase dbInstance;
    private String uid;
    private int netStat;

    private CallbackManager callbackManager;

    private int SIGN_IN_REQUEST_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_login);

        netStat = NetworkStatus.getConnectivityStatus(getApplicationContext());
        if(netStat == TYPE_CONNECTED) {
            FirebaseApp.initializeApp(this);
            dbInstance = FirebaseDatabase.getInstance();
            authInstance = FirebaseAuth.getInstance();
            authStateListener = (FirebaseAuth firebaseAuth) -> {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    //로그인
                    Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
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
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
        }else Toast.makeText(getBaseContext(), getString(R.string.network_not_connected), Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        callbackManager.onActivityResult(requestCode, resultCode, data);
//        super.onActivityResult(requestCode, resultCode, data);
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
        netStat = NetworkStatus.getConnectivityStatus(getApplicationContext());
        if(netStat == TYPE_CONNECTED) {
            GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(getString(R.string.default_web_client_id))
                    .requestEmail()
                    .build();
            GoogleSignInClient googleSignInClient = GoogleSignIn.getClient(this, gso);

            Intent intent = googleSignInClient.getSignInIntent();
            startActivityForResult(intent, SIGN_IN_REQUEST_CODE);
        }else Toast.makeText(getBaseContext(), getString(R.string.network_not_connected), Toast.LENGTH_SHORT).show();
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount account){
        netStat = NetworkStatus.getConnectivityStatus(getApplicationContext());
        if(netStat == TYPE_CONNECTED) {
            setViewWhenLoading();
            AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
            String userName = account.getDisplayName();
            String profileImageUrl = String.valueOf(account.getPhotoUrl());
            String email = account.getEmail();
            processCredential(credential, userName, profileImageUrl, email);
        }else Toast.makeText(getBaseContext(), getString(R.string.network_not_connected), Toast.LENGTH_SHORT).show();
    }

    private void setFacebookLogIn(){
        netStat = NetworkStatus.getConnectivityStatus(getApplicationContext());
        if(netStat == TYPE_CONNECTED) {
            // 페이스북 로그인
            callbackManager = CallbackManager.Factory.create();
            binding.loginActivityFacebookLoginButton.setReadPermissions(getString(R.string.email));
            binding.loginActivityFacebookLoginButton.setLoginText(getString(R.string.log_in));
            binding.loginActivityFacebookLoginButton.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
                @Override
                public void onSuccess(LoginResult loginResult) {
//                    setViewWhenLoading();
                    checkHashOfEmail(loginResult);
                }

                @Override
                public void onCancel() {

                }

                @Override
                public void onError(FacebookException error) {
                    Toast.makeText(getBaseContext(), getString(R.string.auth_failed), Toast.LENGTH_LONG).show();
//                    Toast.makeText(getBaseContext(), error.toString(), Toast.LENGTH_LONG).show();
                    Log.w("LoginActivity", error.toString());
                }
            });
        }else Toast.makeText(getBaseContext(), getString(R.string.network_not_connected), Toast.LENGTH_SHORT).show();
    }

    private void handleFacebookAccessToken(AccessToken token){
        netStat = NetworkStatus.getConnectivityStatus(getApplicationContext());
        if(netStat == TYPE_CONNECTED) {
            AuthCredential credential = FacebookAuthProvider.getCredential(token.getToken());
            String name = Profile.getCurrentProfile().getName();
            String profileImageUrl = String.valueOf(Profile.getCurrentProfile().getProfilePictureUri(300,300));

            setViewWhenLoading();
            processCredential(credential, name, profileImageUrl, "");
        }else Toast.makeText(getBaseContext(), getString(R.string.network_not_connected), Toast.LENGTH_SHORT).show();
    }

    private void processCredential(AuthCredential credential, String userName, String profileImageUrl, String email){
        netStat = NetworkStatus.getConnectivityStatus(getApplicationContext());
        if(netStat == TYPE_CONNECTED) {
            authInstance.signInWithCredential(credential)
                    .addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                            checkUid(userName, profileImageUrl, email);
                        }else {
                            setViewWhenDone();
                            Toast.makeText(this, "task failed", Toast.LENGTH_SHORT).show();
                        }
                    });
        }else Toast.makeText(getBaseContext(), getString(R.string.network_not_connected), Toast.LENGTH_SHORT).show();
    }

    private void checkUid(String userName, String profileImageUrl, String email){
        String hash = createHashValue(email);
        netStat = NetworkStatus.getConnectivityStatus(getApplicationContext());
        if(netStat == TYPE_CONNECTED) {
            uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
            dbInstance.getReference().child(getString(R.string.fdb_users)).orderByChild(getString(R.string.fdb_uid)).equalTo(uid)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            if(dataSnapshot.getChildrenCount() == 0) {
                                addUserToFDB(userName, profileImageUrl, hash);
                                UserProfileChangeRequest userProfileChangeRequest = new UserProfileChangeRequest.Builder().setDisplayName(userName).build();
                                authInstance.getCurrentUser().updateProfile(userProfileChangeRequest);
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {
                            Toast.makeText(getBaseContext(), getString(R.string.account_save_failed), Toast.LENGTH_LONG).show();
                        }
            });
        }else Toast.makeText(getBaseContext(), getString(R.string.network_not_connected), Toast.LENGTH_SHORT).show();
    }

    private void addUserToFDB(String userName, String profileImageUrl, String hash){
        UserModel userModel = new UserModel.Builder()
                .setUserName(userName)
                .setProfileImageUrl(profileImageUrl)
                .setUid(uid)
                .setHash(hash)
                .build();
        netStat = NetworkStatus.getConnectivityStatus(getApplicationContext());
        if(netStat == TYPE_CONNECTED){
            dbInstance.getReference().child(getString(R.string.fdb_users)).child(uid).setValue(userModel)
                    .addOnSuccessListener( aVoid -> {
                        authInstance.addAuthStateListener(authStateListener);
                        Toast.makeText(this, R.string.auth_success, Toast.LENGTH_SHORT).show();
                    }
            );
        } else Toast.makeText(getBaseContext(), getString(R.string.network_not_connected), Toast.LENGTH_SHORT).show();
    }

    private void checkHashOfEmail(LoginResult loginResult){
        GraphRequest request = GraphRequest.newMeRequest(loginResult.getAccessToken(),
                new GraphRequest.GraphJSONObjectCallback() {
                    @Override
                    public void onCompleted(JSONObject object, GraphResponse response) {
                        try {
                            String email = response.getJSONObject().getString(getString(R.string.email));
                            String hash = createHashValue(email);
//                            Toast.makeText(LoginActivity.this, email + hash, Toast.LENGTH_SHORT).show();
                            dbInstance.getReference().child(getString(R.string.fdb_users)).orderByChild(getString(R.string.fdb_hash)).equalTo(hash)
                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    Toast.makeText(LoginActivity.this, ""+dataSnapshot.getChildrenCount(), Toast.LENGTH_SHORT).show();
                                    if(0 != dataSnapshot.getChildrenCount()) {
                                        Toast.makeText(getBaseContext(), getString(R.string.already_have_account), Toast.LENGTH_LONG).show();

                                        setViewWhenDone();
                                        LoginManager.getInstance().logOut();
                                    }else handleFacebookAccessToken(loginResult.getAccessToken());
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {
                                }
                            });
                        }catch(Exception e){
                            e.printStackTrace();
                        }
                    }
        });
        Bundle parameters = new Bundle();
        parameters.putString(getString(R.string.fields), getString(R.string.email));
        request.setParameters(parameters);
        request.executeAsync();
    }

    private String createHashValue(String email){
        String hash = "";
        try{
            MessageDigest sh = MessageDigest.getInstance(getString(R.string.sha_256));
            sh.update(email.getBytes());
            byte byteData[] = sh.digest();
            StringBuffer sb = new StringBuffer();
            for(int i = 0 ; i < byteData.length ; i++)
                sb.append(Integer.toString((byteData[i]&0xff) + 0x100, 16).substring(1));

            hash = sb.toString();
        }catch(Exception e){
            e.printStackTrace();
            hash = null;
        }
        return email.equals("") ? email : hash;
    }

    private void setViewWhenLoading(){
        binding.loginActivityProgressBar.setVisibility(View.VISIBLE);
        binding.loginActivityFacebookLoginButton.setEnabled(false);
        binding.loginActivityGoogleLoginButton.setEnabled(false);
    }

    private void setViewWhenDone(){
        binding.loginActivityProgressBar.setVisibility(View.INVISIBLE);
        binding.loginActivityFacebookLoginButton.setEnabled(true);
        binding.loginActivityGoogleLoginButton.setEnabled(true);
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