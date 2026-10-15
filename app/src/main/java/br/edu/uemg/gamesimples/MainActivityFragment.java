package br.edu.uemg.gamesimples;

import android.app.AlertDialog;
import android.media.AudioManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.fragment.app.Fragment;

public class MainActivityFragment extends Fragment {
    private CannonView cannonView;
    private View menu;
    private Button playButton;
    private Button continueButton;
    private Button musicButton;
    private Button modeButton;
    private TextView modeDescription;
    private TextView recordText;
    private GameRecords records;
    private boolean selectedArcade;
    private GameMusic gameMusic;
    private AlertDialog menuDialog;
    private OnBackPressedCallback backCallback;
    private Bundle savedGame;
    private boolean menuVisible = true;
    private boolean hasMatch;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_main, container, false);
        cannonView = view.findViewById(R.id.cannonView);
        menu = view.findViewById(R.id.mainMenu);
        menu.setBackground(new ArcadeMenuBackground());
        playButton = view.findViewById(R.id.playButton);
        continueButton = view.findViewById(R.id.continueButton);
        musicButton = view.findViewById(R.id.musicButton);
        modeButton = view.findViewById(R.id.modeButton);
        modeDescription = view.findViewById(R.id.modeDescription);
        recordText = view.findViewById(R.id.recordText);
        records = new GameRecords(requireContext());
        gameMusic = new GameMusic(requireContext());
        if (savedInstanceState != null) {
            savedGame = savedInstanceState.getBundle("game");
            menuVisible = savedInstanceState.getBoolean("menuVisible", true);
            hasMatch = savedInstanceState.getBoolean("hasMatch", false);
            selectedArcade = savedInstanceState.getBoolean("selectedArcade", false);
        }
        cannonView.restoreGameState(savedGame);
        cannonView.setOnMenuRequestedListener(this::showMenu);
        playButton.setOnClickListener(button -> {
            if (hasMatch && cannonView.canContinueGame()) {
                menuDialog = new AlertDialog.Builder(requireContext())
                        .setTitle(R.string.replace_match_title)
                        .setMessage(R.string.replace_match_message)
                        .setNegativeButton(R.string.cancel, null)
                        .setPositiveButton(R.string.new_match, (dialog, which) -> showGame(true)).show();
            } else showGame(true);
        });
        continueButton.setOnClickListener(button -> showGame(false));
        modeButton.setOnClickListener(button -> {
            selectedArcade = !selectedArcade;
            updateMode();
        });
        musicButton.setOnClickListener(button -> {
            gameMusic.setEnabled(!gameMusic.isEnabled());
            updateMusicButton();
        });
        view.findViewById(R.id.helpButton).setOnClickListener(button ->
                menuDialog = new AlertDialog.Builder(requireContext())
                        .setTitle(R.string.how_to_play)
                        .setMessage(getString(R.string.arcade_instructions, getString(R.string.game_instructions)))
                        .setPositiveButton(R.string.understood, null).show());
        updateMenu();
        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        backCallback = new OnBackPressedCallback(!menuVisible) {
            @Override public void handleOnBackPressed() { showMenu(); }
        };
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), backCallback);
        requireActivity().setVolumeControlStream(AudioManager.STREAM_MUSIC);
    }

    private void updateMusicButton() {
        musicButton.setText(gameMusic.isEnabled() ? R.string.music_on : R.string.music_off);
    }

    private void updateMode() {
        modeButton.setText(selectedArcade ? R.string.mode_arcade : R.string.mode_classic);
        modeDescription.setText(selectedArcade ? R.string.arcade_description : R.string.classic_description);
        recordText.setText(selectedArcade
                ? getString(R.string.arcade_record, records.bestScore(true), records.bestLevel())
                : getString(R.string.classic_record, records.bestScore(false)));
    }

    private void updateMenu() {
        menu.setVisibility(menuVisible ? View.VISIBLE : View.GONE);
        cannonView.setVisibility(menuVisible ? View.INVISIBLE : View.VISIBLE);
        boolean canContinue = hasMatch && cannonView.canContinueGame();
        continueButton.setVisibility(canContinue ? View.VISIBLE : View.GONE);
        if (canContinue) {
            Bundle match = cannonView.saveGameState();
            String mode = match.getBoolean("arcade")
                    ? getString(R.string.arcade_short, match.getInt("level", 1))
                    : getString(R.string.classic_short);
            continueButton.setText(getString(R.string.continue_mode, mode));
        }
        playButton.setText(canContinue ? R.string.new_match : R.string.play_game);
        playButton.setBackgroundResource(canContinue ? R.drawable.arcade_button_secondary
                : R.drawable.arcade_button_primary);
        playButton.setTextColor(canContinue ? 0xFFE2E8FA : 0xFF17162A);
        updateMusicButton();
        updateMode();
        if (backCallback != null) backCallback.setEnabled(!menuVisible);
    }

    private void showGame(boolean fresh) {
        if (fresh) cannonView.newGame(selectedArcade);
        hasMatch = true;
        menuVisible = false;
        updateMenu();
        if (isResumed()) {
            if (fresh) cannonView.resumeGame();
            else cannonView.continueGame();
        }
    }

    private void showMenu() {
        cannonView.stopGame();
        menuVisible = true;
        updateMenu();
    }

    @Override
    public void onResume() {
        super.onResume();
        WindowInsetsControllerCompat bars = WindowCompat.getInsetsController(
                requireActivity().getWindow(), requireActivity().getWindow().getDecorView());
        bars.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        bars.hide(WindowInsetsCompat.Type.systemBars());
        if (!menuVisible) cannonView.resumeGame();
        gameMusic.setActive(true);
        updateMode();
    }

    @Override
    public void onPause() {
        gameMusic.setActive(false);
        cannonView.stopGame();
        if (menuDialog != null) { menuDialog.dismiss(); menuDialog = null; }
        super.onPause();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (cannonView != null && hasMatch) savedGame = cannonView.saveGameState();
        outState.putBundle("game", savedGame);
        outState.putBoolean("menuVisible", menuVisible);
        outState.putBoolean("hasMatch", hasMatch);
        outState.putBoolean("selectedArcade", selectedArcade);
    }

    @Override
    public void onDestroyView() {
        cannonView.stopGame();
        if (hasMatch) savedGame = cannonView.saveGameState();
        cannonView.setOnMenuRequestedListener(null);
        cannonView.releaseResources();
        gameMusic.release();
        cannonView = null;
        gameMusic = null;
        menu = null;
        playButton = null;
        continueButton = null;
        musicButton = null;
        modeButton = null;
        modeDescription = null;
        recordText = null;
        backCallback = null;
        super.onDestroyView();
    }

    boolean isMusicPlaying() { return gameMusic != null && gameMusic.isPlaying(); }
}
