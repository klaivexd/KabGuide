package com.example.kabguide;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.GraphRequest;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

import org.json.JSONException;

public class LoginActivity extends AppCompatActivity {

    private FirebaseAuth firebaseAuth;
    GoogleSignInClient googleSignInClient;
    private CallbackManager mCallbackManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mCallbackManager = CallbackManager.Factory.create();

        Button googleLoginBtn = findViewById(R.id.googleLoginBtn);

        FirebaseApp.initializeApp(this);
        FacebookSdk.sdkInitialize(getApplicationContext());

        // Initialize Firebase Auth
        firebaseAuth = FirebaseAuth.getInstance();

        // Initialize sign in options the client-id is copied form google-services.json file
        GoogleSignInOptions googleSignInOptions =
                new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestIdToken(getString(R.string.default_web_client_id))
                        .requestEmail()
                        .build();

        // Initialize sign in client
        googleSignInClient = GoogleSignIn.getClient(LoginActivity.this, googleSignInOptions);

        googleLoginBtn.setOnClickListener(view -> {
            // Initialize sign in intent
            Intent intent = googleSignInClient.getSignInIntent();
            // Start activity for result
            startActivityForResult(intent, 100);
        });

        LoginButton loginButton = findViewById(R.id.login_button);

        //Setting the permission that we need to read
        loginButton.setReadPermissions("public_profile","email", "user_birthday");

        //Registering callback!
        loginButton.registerCallback(mCallbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                //Sign in completed
                Log.i("FB", "onSuccess: logged in successfully");

                //handling the token for Firebase Auth
                handleFacebookAccessToken(loginResult.getAccessToken());

                //Getting the user information
                GraphRequest request = GraphRequest.newMeRequest(loginResult.getAccessToken(), (jsonObject, graphResponse) -> {
                    // Application code
                    Log.i("FB", "onCompleted: response: " + graphResponse.toString());
                    try {
                        String email = jsonObject.getString("email");
                        String birthday = jsonObject.getString("birthday");

                        Log.i("FB", "onCompleted: Email: " + email);
                        Log.i("FB", "onCompleted: Birthday: " + birthday);

                        startActivity(
                                new Intent(
                                        LoginActivity.this,
                                        MapActivity.class
                                ).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        );
                        displayToast("Authentication successful");

                    } catch (JSONException e) {
                        e.printStackTrace();
                        Log.i("FB", "onCompleted: JSON exception");
                    }
                });

                Bundle parameters = new Bundle();
                parameters.putString("fields", "id,name,email,gender,birthday");
                request.setParameters(parameters);
                request.executeAsync();
            }

            @Override
            public void onCancel() {
                Log.d("FB", "facebook:onCancel");
            }

            @Override
            public void onError(@NonNull FacebookException e) {
                Log.d("FB", "facebook:onError", e);
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
        if (firebaseUser != null) {
            // When user already sign in redirect to profile activity
            startActivity(
                    new Intent(
                            LoginActivity.this,
                            MapActivity.class
                    ).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            );
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        mCallbackManager.onActivityResult(requestCode, resultCode, data);
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 100) {// When request code is equal to 100 initialize task
            Task<GoogleSignInAccount> signInAccountTask =
                    GoogleSignIn.getSignedInAccountFromIntent(data);
            // check condition
            if (signInAccountTask.isSuccessful()) {
                // When google sign in successful initialize string
                String s = "Google sign in successful";
                // Display Toast
                displayToast(s);
                // Initialize sign in account
                try {
                    // Initialize sign in account
                    GoogleSignInAccount googleSignInAccount =
                            signInAccountTask.getResult(ApiException.class);
                    // Check condition
                    if (googleSignInAccount != null) {
                        // When sign in account is not equal to null initialize auth credential
                        AuthCredential auth =
                                GoogleAuthProvider.getCredential(
                                        googleSignInAccount.getIdToken(),
                                        null
                                );
                        // Check credential
                        firebaseAuth.signInWithCredential(auth).addOnCompleteListener(
                                this,
                                task -> {
                                    // Check condition
                                    if (task.isSuccessful()) {
                                        // When task is successful redirect to map activity
                                        startActivity(
                                                new Intent(
                                                        LoginActivity.this,
                                                        MapActivity.class
                                                ).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        );
                                        displayToast("Authentication successful");
                                    } else {
                                        // When task is unsuccessful display Toast
                                        displayToast(
                                                "Authentication Failed :" +
                                                        task.getException().getMessage()
                                        );
                                    }
                                }
                        );
                    }
                } catch (ApiException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void displayToast(String s) {
        Toast.makeText(getApplicationContext(), s, Toast.LENGTH_SHORT).show();
    }

    private void handleFacebookAccessToken(AccessToken token) {
        Log.d("FB", "handleFacebookAccessToken:" + token);

        AuthCredential credential = FacebookAuthProvider.getCredential(token.getToken());
        firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Sign in success, update UI with the signed-in user's information
                        Log.d("FB", "signInWithCredential:success");
                        FirebaseUser user = firebaseAuth.getCurrentUser();
                        Log.i("FB", "onComplete: login completed with user: " + user.getDisplayName());
                    } else {
                        // If sign in fails, display a message to the user.
                        Log.w("FB", "signInWithCredential:failure", task.getException());
                        Toast.makeText(LoginActivity.this, "Authentication failed.",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }
}