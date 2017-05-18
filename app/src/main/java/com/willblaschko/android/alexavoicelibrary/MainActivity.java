package com.willblaschko.android.alexavoicelibrary;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.willblaschko.android.alexavoicelibrary.actions.ActionsFragment;
import com.willblaschko.android.alexavoicelibrary.actions.BaseListenerFragment;
import com.willblaschko.android.alexavoicelibrary.actions.SendAudioActionFragment;
import ai.kitt.snowboy.AppResCopy;
import ai.kitt.snowboy.Constants;


import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import static com.willblaschko.android.alexavoicelibrary.R.id.frame;

/**
 * Our main launch activity where we can change settings, see about, etc.
 */
public class MainActivity extends BaseActivity implements ActionsFragment.ActionFragmentInterface, FragmentManager.OnBackStackChangedListener {
    private final static String TAG = "MainActivity";
    private final static String TAG_FRAGMENT = "CurrentFragment";

    private View statusBar;
    private TextView status;
    private View loading;
    private MediaPlayer player = new MediaPlayer();
    private static String strEnvWorkSpace = Constants.DEFAULT_WORK_SPACE;

    private RecyclerView mRecyclerView;
    private MyAdapter myAdapter;
    private ArrayList<PayloadCard> payloadList;

    MqttAndroidClient mqttAndroidClient;
    //final String serverUri = "tcp://broker.hivemq.com:1883";
    final String serverUri = "tcp://13.126.2.187:1883";
    final String customer_id = "33336369";


    String clientId = "VoicePayClient" + (Math.random() * 1000 );
    final String user_topic = "/text/" + customer_id + "/messages/user";
    final String alexa_topic = "/text/" + customer_id + "/messages/alexa";
    final String result_topic = "/text/" + customer_id + "/messages/result";

    final String publishTopic = "exampleAndroidPublishTopic";
    final String publishMessage = "Hello World!";

    final static public boolean DEBUG = false;

    final int WRITE_STORAGE = 1;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        getSupportActionBar().setTitle(R.string.app_name);
        //Listen for changes in the back stack
        getSupportFragmentManager().addOnBackStackChangedListener(this);
        //Handle when activity is recreated like on orientation Change
        shouldDisplayHomeUp();

        statusBar = findViewById(R.id.status_bar);
        status = (TextView) findViewById(R.id.status);
        loading = findViewById(R.id.loading);




        //loadFragment(fragment, false);

        mRecyclerView = (RecyclerView) findViewById(R.id.my_recycler_view);
        payloadList = new ArrayList<>();

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        mRecyclerView.setHasFixedSize(true);

        // use a linear layout manager
        LinearLayoutManager mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);

        // specify an adapter (see also next example)
        //payloadList.add("String 0");
        myAdapter = new MyAdapter(payloadList);

        mRecyclerView.setAdapter(myAdapter);
        initMQTT();

        if (ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {


                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

            } else {

                // No explanation needed, we can request the permission.

                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        WRITE_STORAGE);

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        } else {
            initAlexa();
        }
    }

    private void initAlexa() {
        AppResCopy.copyResFromAssetsToSD(this);
        loadFragment(new SendAudioActionFragment(), false);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case WRITE_STORAGE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    initAlexa();
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.

                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    @Override
    protected void startListening() {

        Fragment fragment = getSupportFragmentManager().findFragmentByTag(TAG_FRAGMENT);
        if (fragment != null && fragment.isVisible()) {
            // add your code here
            if(fragment instanceof BaseListenerFragment){
                ((BaseListenerFragment) fragment).startListening();
            }
        }
    }

    @Override
    public void loadFragment(Fragment fragment, boolean addToBackStack){
        FragmentTransaction transaction = getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(android.R.anim.slide_in_left, android.R.anim.slide_out_right, android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(frame, fragment, TAG_FRAGMENT);
        if(addToBackStack){
            transaction.addToBackStack(fragment.getClass().getSimpleName());
        }
        transaction.commit();
    }


    protected void stateListening(){

        if(status != null) {
            status.setText(R.string.status_listening);
            //loading.setVisibility(View.GONE);
            statusBar.animate().alpha(1);
        }
    }
    protected void stateProcessing(){

        if(status != null) {
            status.setText(R.string.status_processing);
            //loading.setVisibility(View.VISIBLE);
            statusBar.animate().alpha(1);
        }
    }
    protected void stateSpeaking(){

        if(status != null) {
            status.setText(R.string.status_speaking);
            //loading.setVisibility(View.VISIBLE);
            statusBar.animate().alpha(1);
        }
    }
    protected void statePrompting(){

        if(status != null) {
            status.setText("");
            //loading.setVisibility(View.VISIBLE);
            statusBar.animate().alpha(1);
        }
    }
    protected void stateFinished(){
        if(status != null) {
            status.setText("");
            //loading.setVisibility(View.GONE);
            statusBar.animate().alpha(0);
        }
    }
    protected void stateNone(){
        statusBar.animate().alpha(0);
    }


    @Override
    public void onBackStackChanged() {
        shouldDisplayHomeUp();
    }

    public void shouldDisplayHomeUp(){
        //Enable Up button only  if there are entries in the back stack
        boolean canback = (getSupportFragmentManager().getBackStackEntryCount() > 0);
        if(getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(canback);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        //This method is called when the up button is pressed. Just the pop back stack.
        getSupportFragmentManager().popBackStack();
        return true;
    }

    void showToast(CharSequence msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    public void updateLog(String text) {
        Log.i(text, "snow logs");
    }

    public class MyAdapter extends RecyclerView.Adapter<MyAdapter.ViewHolder> {
        private ArrayList<PayloadCard> mDataset;
        private String cardType;

        // Provide a reference to the views for each data item
        // Complex data items may need more than one view per item, and
        // you provide access to all the views for a data item in a view holder
        public class ViewHolder extends RecyclerView.ViewHolder {
            public LinearLayout mTextView;
            public ViewHolder(LinearLayout v) {
                super(v);
                mTextView = v;
            }
        }

        // Provide a suitable constructor (depends on the kind of dataset)
        public MyAdapter(ArrayList<PayloadCard> myDataset) {
            mDataset = myDataset;
        }

        // Create new views (invoked by the layout manager)
        @Override
        public MyAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                       int viewType) {
            LinearLayout v = null;
            Log.i("viewType", "view " + viewType);
            if (viewType == 1) {
                // create a new view
                 v = (LinearLayout) LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.cardview_alexa, parent, false);
            }
            if (viewType == 0) {
                // create a new view
                 v = (LinearLayout) LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.cardview_user, parent, false);
            }
            if (viewType == 2) {
                // create a new view
                v = (LinearLayout) LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.cardview_result, parent, false);
            }
            return new ViewHolder(v);
        }
        @Override
        public int getItemViewType(int position) {
            //Implement your logic here
            PayloadCard vo = mDataset.get(position);
            if (vo.getType().equals("user")) {
                return 0;
            } else if (vo.getType().equals("alexa")) {
                return 1;
            } else if (vo.getType().equals("result")) {
                return 2;
            }
            return 0;
        }
        // Replace the contents of a view (invoked by the layout manager)
        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            // - get element from your dataset at this position
            // - replace the contents of the view with that element
            if (!mDataset.get(position).getType().equals("result")) {
                TextView textView = (TextView) holder.mTextView.findViewById(R.id.info_text);
                textView.setText(mDataset.get(position).getAlexaMessge());
            } else {
                TextView title = (TextView) holder.mTextView.findViewById(R.id.title);
                TextView subheading = (TextView) holder.mTextView.findViewById(R.id.subheading);
                TextView info_text = (TextView) holder.mTextView.findViewById(R.id.info_text);
                TextView amount = (TextView) holder.mTextView.findViewById(R.id.money);
                ImageView imageView = (ImageView) holder.mTextView.findViewById(R.id.image);
                try {
                    JSONObject obj = new JSONObject(mDataset.get(position).getAlexaMessge());
                    if (mDataset.get(position).getIntent().equals("transfer")) {
                        title.setText("TRANSFER");
                        imageView.setImageResource(R.mipmap.right_vectors3);
                    } else if (mDataset.get(position).getIntent().equals("paybill")) {
                        title.setText("PAY BILL");
                        imageView.setImageResource(R.mipmap.right_vectors);
                    }
                    subheading.setText("Name : " + obj.get("name").toString());
                    info_text.setText(" Amount : " + obj.get("amount").toString());
                    amount.setText(obj.get("balance").toString());
                } catch (JSONException e) {
                    e.printStackTrace();
                    Log.e("My App", "Could not parse malformed JSON: ");

                }
            }

        }

        // Return the size of your dataset (invoked by the layout manager)
        @Override
        public int getItemCount() {
            return mDataset.size();
        }


        public String getCardType() {
            return cardType;
        }

        public void setCardType(String cardType) {
            this.cardType = cardType;
        }


    }

    private void initMQTT() {
        mqttAndroidClient = new MqttAndroidClient(getApplicationContext(), serverUri, clientId);
        mqttAndroidClient.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean reconnect, String serverURI) {

                if (reconnect) {
                    if (DEBUG)
                        Toast.makeText(MainActivity.this, "Reconnected to : " + serverURI, Toast.LENGTH_LONG).show();

                    // Because Clean Session is true, we need to re-subscribe
                    subscribeToTopic();
                } else {
                    if (DEBUG)
                        Toast.makeText(MainActivity.this, "Connected to: " + serverURI, Toast.LENGTH_LONG).show();

                }
            }

            @Override
            public void connectionLost(Throwable cause) {
                Toast.makeText(MainActivity.this, "The Connection was lost.", Toast.LENGTH_LONG).show();
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                if (DEBUG)
                    Toast.makeText(MainActivity.this, "Incoming message: " + message.toString(), Toast.LENGTH_LONG).show();

                if (topic.equals(result_topic)) {
                    try {

                        JSONObject obj = new JSONObject(new String(message.getPayload()));
                        PayloadCard payloadCard = new PayloadCard();
                        payloadCard.setType("result");
                        payloadCard.setIntent(obj.get("intent").toString());
                        payloadCard.setAlexaMessge(obj.get("alexaData").toString());

                        addAlexaCard(payloadCard);

                    } catch (Throwable t) {
                        t.printStackTrace();
                        Log.e("My App", "Could not parse malformed JSON: ");
                    }
                }
                if (topic.equals(alexa_topic)) {
                    try {

                        JSONObject obj = new JSONObject(new String(message.getPayload()));
                        PayloadCard payloadCard = new PayloadCard();
                        payloadCard.setType("alexa");
                        payloadCard.setIntent(obj.get("intent").toString());
                        payloadCard.setAlexaMessge(obj.get("text").toString());


                        addAlexaCard(payloadCard);

                    } catch (Throwable t) {
                        t.printStackTrace();
                        Log.e("My App", "Could not parse malformed JSON: ");
                    }
                }
                if (topic.equals(user_topic)) {
                    try {

                        JSONObject obj = new JSONObject(new String(message.getPayload()));
                        PayloadCard payloadCard = new PayloadCard();
                        payloadCard.setType("user");
                        payloadCard.setIntent(obj.get("intent").toString());
                        if (obj.get("intent").toString().equals("BalanceIntent"))
                        {
                            payloadCard.setAlexaMessge("What is my account balance?");

                        }
                        if (obj.get("intent").toString().equals("SplitwiseBalanceIntent"))
                        {

                            payloadCard.setAlexaMessge("What is my Splitwise balance?");

                        }
                        if (obj.get("intent").toString().equals("SplitwiseMaxOweIntent"))
                        {
                            payloadCard.setAlexaMessge("Whom do I owe the most on Splitwise?");

                        }
                        if (obj.get("intent").toString().equals("MoneySpentIntent"))
                        {
                            JSONObject a = new JSONObject(obj.get("slots").toString());
                            payloadCard.setAlexaMessge("How much money did I spend in the last "+(a).get("days").toString()+" days?");
                        }
                        if (obj.get("intent").toString().equals("CheckBillIntent"))
                        {
                            JSONObject a = new JSONObject(obj.get("slots").toString());
                            payloadCard.setAlexaMessge("What is my "+(a).get("billName").toString()+" bill for " + (a).get("billDate") + "?");
                        }
                        if (obj.get("intent").toString().equals("TransferIntent"))
                        {
                            JSONObject a = new JSONObject(obj.get("slots").toString());
                            payloadCard.setAlexaMessge("Transfer "+(a).get("payeeAmount").toString()+" rupees to " + (a).get("payeeName"));
                        }
                        if (obj.get("intent").toString().equals("PayBillIntent"))
                        {
                            JSONObject a = new JSONObject(obj.get("slots").toString());
                            payloadCard.setAlexaMessge("Pay "+(a).get("billName").toString()+" bill");
                        }
                        if (obj.get("intent").toString().equals("AddPayeeIntent"))
                        {
                            JSONObject a = new JSONObject(obj.get("slots").toString());
                            payloadCard.setAlexaMessge("Add "+(a).get("payeeName").toString()+" with VPA " + (a).get("payeeVPA").toString() + " as my payee");
                        }

                        if (obj.get("intent").toString().equals("CustomerCareIntent"))
                        {
                            payloadCard.setAlexaMessge("Call customer care.");
                        }

                        if (obj.get("intent").toString().equals("CCOptionIntent"))
                        {
                            JSONObject a = new JSONObject(obj.get("slots").toString());
                            payloadCard.setAlexaMessge("Query about : "+(a).get("answer").toString());
                        }

                        if (obj.get("intent").toString().equals("CardBlockIntent"))
                        {
                            payloadCard.setAlexaMessge("Block my card");
                        }
                        if (obj.get("intent").toString().equals("AuthQ"))
                        {
                            payloadCard.setAlexaMessge("Authorize me");
                        }

                        addAlexaCard(payloadCard);

                    } catch (Throwable t) {
                        t.printStackTrace();
                        Log.e("My App", "Could not parse malformed JSON: ");
                    }
                }
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {

            }
        });

        MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
        mqttConnectOptions.setAutomaticReconnect(true);
        mqttConnectOptions.setCleanSession(true);

        try {
            mqttAndroidClient.connect(mqttConnectOptions, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    DisconnectedBufferOptions disconnectedBufferOptions = new DisconnectedBufferOptions();
                    disconnectedBufferOptions.setBufferEnabled(true);
                    disconnectedBufferOptions.setBufferSize(100);
                    disconnectedBufferOptions.setPersistBuffer(false);
                    disconnectedBufferOptions.setDeleteOldestMessages(false);
                    mqttAndroidClient.setBufferOpts(disconnectedBufferOptions);
                    subscribeToTopic();
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    exception.printStackTrace();
                    Toast.makeText(MainActivity.this, "Failed to connect to: " + serverUri, Toast.LENGTH_LONG).show();

                }
            });


        } catch (MqttException ex){
            ex.printStackTrace();
        }


    }

    public void subscribeToTopic(){
        try {
            mqttAndroidClient.subscribe(user_topic, 0, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    if (DEBUG)
                        Toast.makeText(MainActivity.this, "Subscribed!", Toast.LENGTH_LONG).show();

                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Toast.makeText(MainActivity.this, "Failed to subscribe", Toast.LENGTH_LONG).show();
                }
            });
            mqttAndroidClient.subscribe(alexa_topic, 0, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    if (DEBUG)
                        Toast.makeText(MainActivity.this, "Subscribed!", Toast.LENGTH_LONG).show();

                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Toast.makeText(MainActivity.this, "Failed to subscribe", Toast.LENGTH_LONG).show();

                }
            });
            mqttAndroidClient.subscribe(result_topic, 0, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    if (DEBUG)
                        Toast.makeText(MainActivity.this, "Subscribed!", Toast.LENGTH_LONG).show();

                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Toast.makeText(MainActivity.this, "Failed to subscribe", Toast.LENGTH_LONG).show();

                }
            });

        } catch (MqttException ex){
            System.err.println("Exception whilst subscribing");
            ex.printStackTrace();
        }
    }

    private void addAlexaCard(PayloadCard payloadCard) {
        payloadList.add(payloadCard);
        myAdapter.setCardType(payloadCard.getType());
        myAdapter.notifyDataSetChanged();
        myAdapter.notifyDataSetChanged();
    }

}
