package com.bvsquared.debugvolleyproject;

import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;

public class MainActivity extends AppCompatActivity {

    Button buClick;
    TextView tvResult;
    ProgressBar pbBug;

    MainActivityViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        viewModel = ViewModelProviders.of(this).get(MainActivityViewModel.class);

        // Link UI elements to code
        buClick = findViewById(R.id.buClick);
        tvResult = findViewById(R.id.tvResult);
        pbBug = findViewById(R.id.pbBug);
        pbBug.setVisibility(View.INVISIBLE);

        // Create observers for changes outside the main UI thread
        createObservers();
        createFirebaseListener();
    }


    /**
     * Sends a ping to firebase, which in turn will trigger a volley request.
     * @param view - the view that's been clicked.
     */
    public void sendVolleyRequest(View view) {
        // Change visibility of the progress bar, to show we're waiting for a response
        pbBug.setVisibility(View.VISIBLE);
        // Ping firebase
        viewModel.pingFirebase();
    }

    /**
     * Creates two observers which receive changes from the viewmodel, one for the text and one for the progressbar
     */
    private void createObservers() {
        final Observer<Integer> resultObserver = result -> tvResult.setText(result);
        viewModel.getResult().observe(this, resultObserver);

        final Observer<Integer> pbObserver = visibility -> pbBug.setVisibility(visibility);
        viewModel.getPbBug().observe(this, pbObserver);
    }

    /**
     * Creates a firebase listener, which listens to changes in collection "games", path "AAAA"
     */
    private void createFirebaseListener() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        final DocumentReference roomRef = db.collection("games").document("AAAA");
        roomRef.addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot snapshot, @Nullable FirebaseFirestoreException e) {
                if (e != null) {
                    Log.w("DEBUG", "Listen failed.", e);
                    return;
                }

                // If we notice a change, send a Volley request.

                if (snapshot != null && snapshot.exists()) {
                    int result = viewModel.sendRequest();
                    // If it's successful, display a success message
                    if (result == 1) {
                        viewModel.getResult().postValue(R.string.tv_success);
                        // If it's not successful, display an error message
                    } else if (result == 3) {
                        viewModel.getResult().postValue(R.string.tv_fail);
                        // If we ran into the bug where we did receive a response, but it still throws a timeout exception, display the final message.
                    } else if (result == 2) {
                        viewModel.getResult().postValue(R.string.tv_bug);
                    }
                    viewModel.getPbBug().postValue(View.INVISIBLE);

                    //The following block of code works just fine. Only difference is, it's ran in yet another thead.

//                    new Thread(new Runnable() {
//                        @Override
//                        public void run() {
//                            int result = viewModel.sendRequest();
//                            // If it's successful, display a success message
//                            if (result == 1) {
//                                viewModel.getResult().postValue(R.string.tv_success);
//                                // If it's not successful, display an error message
//                            } else if (result == 3) {
//                                viewModel.getResult().postValue(R.string.tv_fail);
//                                // If we ran into the bug where we did receive a response, but it still throws a timeout exception, display the final message.
//                            } else if (result == 2) {
//                                viewModel.getResult().postValue(R.string.tv_bug);
//                            }
//                            viewModel.getPbBug().postValue(View.INVISIBLE);
//
//                        }
//                    }).start();

                } else {
                    Log.d("DEBUG", "Current data: null");
                }
            }
        });
    }
}
