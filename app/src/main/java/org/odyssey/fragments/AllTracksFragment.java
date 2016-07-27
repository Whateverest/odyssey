package org.odyssey.fragments;

import android.content.Context;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import org.odyssey.R;
import org.odyssey.adapter.TracksListViewAdapter;
import org.odyssey.listener.OnArtistSelectedListener;
import org.odyssey.loaders.TrackLoader;
import org.odyssey.models.TrackModel;
import org.odyssey.playbackservice.PlaybackServiceConnection;
import org.odyssey.utils.MusicLibraryHelper;
import org.odyssey.utils.ThemeUtils;

import java.util.List;

public class AllTracksFragment extends OdysseyFragment implements LoaderManager.LoaderCallbacks<List<TrackModel>>, AdapterView.OnItemClickListener {

    /**
     * Adapter used for the ListView
     */
    private TracksListViewAdapter mTracksListViewAdapter;

    /**
     * Listener to open an artist
     */
    private OnArtistSelectedListener mArtistSelectedCallback;

    /**
     * Save the root ListView for later usage.
     */
    private ListView mRootList;

    /**
     * Save the swipe layout for later usage
     */
    private SwipeRefreshLayout mSwipeRefreshLayout;

    /**
     * Save the last scroll position to resume there
     */
    private int mLastPosition;

    /**
     * ServiceConnection object to communicate with the PlaybackService
     */
    private PlaybackServiceConnection mServiceConnection;

    /**
     * Called to create instantiate the UI of the fragment.
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.list_refresh, container, false);

        // get listview
        mRootList = (ListView) rootView.findViewById(R.id.list_refresh_listview);

        // get swipe layout
        mSwipeRefreshLayout = (SwipeRefreshLayout) rootView.findViewById(R.id.list_refresh_swipe_layout);
        // set swipe colors
        mSwipeRefreshLayout.setColorSchemeColors(ThemeUtils.getThemeColor(getContext(), R.attr.colorAccent),
                ThemeUtils.getThemeColor(getContext(), R.attr.colorPrimary));
        // set swipe refresh listener
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {

            @Override
            public void onRefresh() {
                refresh();
            }
        });

        mTracksListViewAdapter = new TracksListViewAdapter(getActivity());

        mRootList.setAdapter(mTracksListViewAdapter);
        mRootList.setOnItemClickListener(this);

        registerForContextMenu(mRootList);

        return rootView;
    }

    /**
     * Called when the fragment is first attached to its context.
     */
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        try {
            mArtistSelectedCallback = (OnArtistSelectedListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement OnArtistSelectedListener");
        }
    }

    /**
     * Called when the fragment resumes.
     * Reload the data and create the PBS connection.
     */
    @Override
    public void onResume() {
        super.onResume();

        // change refresh state
        mSwipeRefreshLayout.setRefreshing(true);
        // Prepare loader ( start new one or reuse old )
        getLoaderManager().initLoader(0, getArguments(), this);

        mServiceConnection = new PlaybackServiceConnection(getActivity().getApplicationContext());
        mServiceConnection.openConnection();
    }

    /**
     * This method creates a new loader for this fragment.
     *
     * @param id     The id of the loader
     * @param bundle Optional arguments
     * @return Return a new Loader instance that is ready to start loading.
     */
    @Override
    public Loader<List<TrackModel>> onCreateLoader(int id, Bundle bundle) {
        return new TrackLoader(getActivity(), "", -1);
    }

    /**
     * Called when the loader finished loading its data.
     *
     * @param loader The used loader itself
     * @param data   Data of the loader
     */
    @Override
    public void onLoadFinished(Loader<List<TrackModel>> loader, List<TrackModel> data) {
        mTracksListViewAdapter.swapModel(data);
        // Reset old scroll position
        if (mLastPosition >= 0) {
            mRootList.setSelection(mLastPosition);
            mLastPosition = -1;
        }

        // change refresh state
        mSwipeRefreshLayout.setRefreshing(false);
    }

    /**
     * If a loader is reset the model data should be cleared.
     *
     * @param loader Loader that was resetted.
     */
    @Override
    public void onLoaderReset(Loader<List<TrackModel>> loader) {
        mTracksListViewAdapter.swapModel(null);
    }

    /**
     * generic method to reload the dataset displayed by the fragment
     */
    @Override
    public void refresh() {
        // reload data
        getLoaderManager().restartLoader(0, getArguments(), this);
    }

    /**
     * Play the clicked track.
     */
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        playTrack(position);
    }


    /**
     * Create the context menu.
     */
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getActivity().getMenuInflater();
        inflater.inflate(R.menu.context_menu_all_tracks_fragment, menu);
    }

    /**
     * Hook called when an menu item in the context menu is selected.
     *
     * @param item The menu item that was selected.
     * @return True if the hook was consumed here.
     */
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();

        if (info == null) {
            return super.onContextItemSelected(item);
        }

        switch (item.getItemId()) {
            case R.id.fragment_all_tracks_action_enqueue:
                enqueueTrack(info.position, false);
                return true;
            case R.id.fragment_all_tracks_action_enqueueasnext:
                enqueueTrack(info.position, true);
                return true;
            case R.id.fragment_all_tracks_action_play:
                playTrack(info.position);
                return true;
            case R.id.fragment_all_tracks_showartist:
                showArtist(info.position);
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    /**
     * Callback to open a view for the artist of the selected track.
     *
     * @param position the position of the selected track in the adapter
     */
    private void showArtist(int position) {
        // identify current artist

        TrackModel clickedTrack = (TrackModel) mTracksListViewAdapter.getItem(position);
        String artistTitle = clickedTrack.getTrackArtistName();

        long artistID = MusicLibraryHelper.getArtistIDFromName(artistTitle, getActivity());

        // Send the event to the host activity
        mArtistSelectedCallback.onArtistSelected(artistTitle, artistID);
    }

    /**
     * Call the PBS to play the selected track.
     * A previous playlist will be cleared.
     *
     * @param position the position of the selected track in the adapter
     */
    private void playTrack(int position) {
        // clear playlist and play current track

        try {
            mServiceConnection.getPBS().clearPlaylist();
            enqueueTrack(position, false);
            mServiceConnection.getPBS().jumpTo(0);
        } catch (RemoteException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
    }

    /**
     * Call the PBS to enqueue the selected track.
     *
     * @param position the position of the selected track in the adapter
     * @param asNext   flag if the track should be enqueued as next
     */
    private void enqueueTrack(int position, boolean asNext) {

        TrackModel track = (TrackModel) mTracksListViewAdapter.getItem(position);

        try {
            mServiceConnection.getPBS().enqueueTrack(track, asNext);
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
