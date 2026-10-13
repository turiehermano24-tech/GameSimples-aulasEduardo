package br.edu.uemg.gamesimples;

import android.media.AudioManager;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class MainActivityFragment extends Fragment {
    private CannonView cannonView; // custom view to display the game
    private Bundle savedGame;

    // called when Fragment's view needs to be created
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        // inflate the fragment_main.xml layout
        View view =
                inflater.inflate(R.layout.fragment_main, container, false);

        // get a reference to the CannonView
        cannonView = (CannonView) view.findViewById(R.id.cannonView);
        if (savedInstanceState != null)
            savedGame = savedInstanceState.getBundle("game");
        cannonView.restoreGameState(savedGame);
        return view;
    }

    // set up volume control once Activity is created
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // allow volume buttons to set game volume
        getActivity().setVolumeControlStream(AudioManager.STREAM_MUSIC);
    }

    @Override
    public void onResume() {
        super.onResume();
        cannonView.resumeGame();
    }

    // Interrompe o loop sem descartar a partida.
    @Override
    public void onPause() {
        super.onPause();
        cannonView.stopGame();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (cannonView != null) savedGame = cannonView.saveGameState();
        outState.putBundle("game", savedGame);
    }

    // A SurfaceView e os sons pertencem à view, não à vida inteira do Fragment.
    @Override
    public void onDestroyView() {
        cannonView.stopGame();
        savedGame = cannonView.saveGameState();
        cannonView.releaseResources();
        cannonView = null;
        super.onDestroyView();
    }
}
