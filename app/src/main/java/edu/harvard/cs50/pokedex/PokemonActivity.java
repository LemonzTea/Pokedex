package edu.harvard.cs50.pokedex;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URL;

public class PokemonActivity extends AppCompatActivity {
    private TextView nameTextView;
    private TextView numberTextView;
    private TextView type1TextView;
    private TextView type2TextView;
    private TextView pokemonDesc;
    private String url;
    private RequestQueue requestQueue;
    private Button catchButton;
    private ImageView pokemonImage;

    private final static String pref = "pokedex";
    private final static String STATE_CATCH = "Catch";
    private final static String STATE_RELEASE = "Release";

    private SharedPreferences mPreferences;
    private SharedPreferences.Editor mEditor;

    private String imageURL;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pokemon);

        requestQueue = Volley.newRequestQueue(getApplicationContext());
        url = getIntent().getStringExtra("url");
        nameTextView = findViewById(R.id.pokemon_name);
        numberTextView = findViewById(R.id.pokemon_number);
        type1TextView = findViewById(R.id.pokemon_type1);
        type2TextView = findViewById(R.id.pokemon_type2);
        catchButton = findViewById(R.id.catchButton);
        pokemonImage = findViewById(R.id.pokemonImg);
        pokemonDesc = findViewById(R.id.pokemon_desc);

        load();
    }

    public void load() {
        type1TextView.setText("");
        type2TextView.setText("");

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    // Set Name & Number
                    nameTextView.setText(response.getString("name"));
                    numberTextView.setText(String.format("#%03d", response.getInt("id")));

                    // Get the Pokemon Type
                    JSONArray typeEntries = response.getJSONArray("types");
                    for (int i = 0; i < typeEntries.length(); i++) {
                        JSONObject typeEntry = typeEntries.getJSONObject(i);
                        int slot = typeEntry.getInt("slot");
                        String type = typeEntry.getJSONObject("type").getString("name");

                        if (slot == 1) {
                            type1TextView.setText(type);
                        }
                        else if (slot == 2) {
                            type2TextView.setText(type);
                        }
                    }

                    // Get Pokemon Image
                    JSONObject SpriteEntry = response.getJSONObject("sprites");
                    imageURL = SpriteEntry.getString("front_default");
                    new DownloadSpriteTask().execute(imageURL);



                    // Get Shared Preferences
                    mPreferences = getSharedPreferences(pref, Context.MODE_PRIVATE);
                    mEditor = mPreferences.edit();


                    // Check preferences
                    String pokemon_status = mPreferences.getString(nameTextView.getText().toString(), null);
                    Log.d("Pref", mPreferences.getString(nameTextView.getText().toString(), "error"));

                    // Update Button
                    catchButton.setText(pokemon_status);

                    loadDesc();
                } catch (JSONException e) {
                    Log.e("cs50", "Pokemon json error", e);
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e("cs50", "Pokemon details error", error);
            }
        });

        requestQueue.add(request);
    }

    private void loadDesc() {
        String descURL = ("https://pokeapi.co/api/v2/pokemon-species/" + Integer.parseInt(numberTextView.getText().toString().substring(1)) + "/");
        JsonObjectRequest request2 = new JsonObjectRequest(Request.Method.GET, descURL, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    JSONArray descArray = response.getJSONArray("flavor_text_entries");
                    JSONObject descObj = descArray.getJSONObject(0);
                    pokemonDesc.setText(descObj.getString("flavor_text").replace("\n", " "));

                } catch (JSONException e) {
                    Log.e("cs50", "Pokemon Desc json error", e);
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e("cs50", "Pokemon Desc error", error);
            }
        });

        requestQueue.add(request2);
    }

    public void toggleCatch(View view) {
        // gotta catch 'em all!
        if (catchButton.getText().equals(STATE_CATCH)) {
            mEditor.putString(nameTextView.getText().toString(), STATE_RELEASE);
            mEditor.commit();
            catchButton.setText(STATE_RELEASE);

        } else {
            mEditor.putString(nameTextView.getText().toString(), STATE_CATCH);
            mEditor.commit();
            catchButton.setText(STATE_CATCH);
        }
        Log.d("Pref", mPreferences.getString(nameTextView.getText().toString(), "error"));
    }

    private class DownloadSpriteTask extends AsyncTask<String, Void, Bitmap> {
        @Override
        protected Bitmap doInBackground(String... strings) {
            try {
                URL url = new URL(imageURL);
                return BitmapFactory.decodeStream(url.openStream());
            } catch (IOException e) {
                Log.e("cs50", "download spite error", e);
                return null;
            }
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            //load the bitmap into the imageView!
            pokemonImage.setImageBitmap(bitmap);
        }
    }


}
