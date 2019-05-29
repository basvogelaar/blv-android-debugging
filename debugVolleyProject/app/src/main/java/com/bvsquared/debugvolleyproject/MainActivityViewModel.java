package com.bvsquared.debugvolleyproject;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.MutableLiveData;
import android.support.annotation.NonNull;
import android.util.Log;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.RequestFuture;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class MainActivityViewModel extends AndroidViewModel {

    private boolean receivedResponse = false;
    private MutableLiveData<Integer> result;
    private MutableLiveData<Integer> pbBug;

    public MainActivityViewModel(Application application) {
        super(application);
    }

    /**
     * Sends a dummy request to a free endpoint using volley.
     * @return 1 if successful, 3 if an expected error occurred, 2 if we run into a bug.
     */

    protected int sendRequest() {

        RequestQueue requestQueue = VolleySingleton.getInstance(getApplication().getApplicationContext()).getRequestQueue(getApplication().getApplicationContext());
        String roomName = "AAAA";

        try {
            // Use a dummy endpoint
            String URL = "https://postman-echo.com/get";
            RequestFuture<JSONObject> future = RequestFuture.newFuture();

            JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, URL, null, future, future) {
                @Override
                public Map<String, String> getHeaders() {
                    Map<String, String > map = new HashMap<>();
                    map.put("roomName", roomName);
                    return map;
                }

                /**
                 * Override the parseNetworkResponse method to verify that we do receive a response, but it still times out.
                 * @param response - the network response
                 * @return a JSON object containing the response
                 */
                @Override
                protected Response<JSONObject> parseNetworkResponse(NetworkResponse response) {
                    JSONObject JSONAltResponse = new JSONObject();
                    if (response != null) {
                        try {
                            // Store the response
                            JSONAltResponse = new JSONObject(new String(response.data));
                            receivedResponse = true;
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                    return Response.success(JSONAltResponse, HttpHeaderParser.parseCacheHeaders(response));
                }
            };

            // Add the request
            requestQueue.add(request);

            try {
                // Send the request. Feel free to change the timeout to any value.
                JSONObject data = future.get(5, TimeUnit.SECONDS);
                // If nothing goes wrong, return 1
                return 1;
            } catch (InterruptedException e) {
                return 3;
            } catch (ExecutionException e) {
                return 3;
            } catch (TimeoutException e) {
                // If we run into a timeout, but the tracker variable hasn't been changed
                if (!receivedResponse) {
                    return 3;
                }
                return 2;
            }
        } catch (Exception e) {
            return 3;
        }
    }

    /**
     * Send an update to firebase with a random UUID, just to be (reasonably) sure it will trigger the listener.
     */
    protected void pingFirebase() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String, UUID> ping = new HashMap<>();
        ping.put("AAAA", UUID.randomUUID());
        db.collection("games").document("AAAA").set(ping)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d("blub", "game updated");
                    }
                })

                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w("blub", "error updating game");
                    }
                });
    }

    protected MutableLiveData<Integer> getResult() {
        if (result == null) {
            result = new MutableLiveData<>();
        }
        return result;
    }

    protected MutableLiveData<Integer> getPbBug() {
        if (pbBug == null) {
            pbBug = new MutableLiveData<>();
        }
        return pbBug;
    }
}
