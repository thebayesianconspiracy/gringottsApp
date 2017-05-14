package com.willblaschko.android.alexavoicelibrary;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.willblaschko.android.alexavoicelibrary.actions.ActionsFragment;
import com.willblaschko.android.alexavoicelibrary.actions.BaseListenerFragment;
import com.willblaschko.android.alexavoicelibrary.actions.SendAudioActionFragment;

import org.w3c.dom.Text;

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

    private RecyclerView mRecyclerView;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        //Listen for changes in the back stack
        getSupportFragmentManager().addOnBackStackChangedListener(this);
        //Handle when activity is recreated like on orientation Change
        shouldDisplayHomeUp();

        statusBar = findViewById(R.id.status_bar);
        status = (TextView) findViewById(R.id.status);
        loading = findViewById(R.id.loading);

        //ActionsFragment fragment = new ActionsFragment();
        loadFragment(new SendAudioActionFragment(), false);
        //loadFragment(fragment, false);

        mRecyclerView = (RecyclerView) findViewById(R.id.my_recycler_view);

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        mRecyclerView.setHasFixedSize(true);

        // use a linear layout manager
        LinearLayoutManager mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);

        // specify an adapter (see also next example)
        String[] myDataset = new String[2];
        myDataset[0] = "String 0 ";
        myDataset[1] = "String 1";
        MyAdapter mAdapter = new MyAdapter(myDataset);

        mRecyclerView.setAdapter(mAdapter);
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
            loading.setVisibility(View.GONE);
            statusBar.animate().alpha(1);
        }
    }
    protected void stateProcessing(){

        if(status != null) {
            status.setText(R.string.status_processing);
            loading.setVisibility(View.VISIBLE);
            statusBar.animate().alpha(1);
        }
    }
    protected void stateSpeaking(){

        if(status != null) {
            status.setText(R.string.status_speaking);
            loading.setVisibility(View.VISIBLE);
            statusBar.animate().alpha(1);
        }
    }
    protected void statePrompting(){

        if(status != null) {
            status.setText("");
            loading.setVisibility(View.VISIBLE);
            statusBar.animate().alpha(1);
        }
    }
    protected void stateFinished(){
        if(status != null) {
            status.setText("");
            loading.setVisibility(View.GONE);
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

    public class MyAdapter extends RecyclerView.Adapter<MyAdapter.ViewHolder> {
        private String[] mDataset;

        // Provide a reference to the views for each data item
        // Complex data items may need more than one view per item, and
        // you provide access to all the views for a data item in a view holder
        public class ViewHolder extends RecyclerView.ViewHolder {
            // each data item is just a string in this case
            public LinearLayout mTextView;
            public ViewHolder(LinearLayout v) {
                super(v);
                mTextView = v;
            }
        }

        // Provide a suitable constructor (depends on the kind of dataset)
        public MyAdapter(String[] myDataset) {
            mDataset = myDataset;
        }

        // Create new views (invoked by the layout manager)
        @Override
        public MyAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                       int viewType) {
            // create a new view
            LinearLayout v = (LinearLayout) LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.cardview, parent, false);

            ViewHolder vh = new ViewHolder(v);
            return vh;
        }

        // Replace the contents of a view (invoked by the layout manager)
        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            // - get element from your dataset at this position
            // - replace the contents of the view with that element
            TextView textView = (TextView) holder.mTextView.findViewById(R.id.info_text);
            textView.setText(mDataset[position]);

        }

        // Return the size of your dataset (invoked by the layout manager)
        @Override
        public int getItemCount() {
            return mDataset.length;
        }
    }


}
